#!/usr/bin/env bash
set -euo pipefail

# YAMP Release Build Script
# Usage:
#   ./scripts/build-release.sh major    # Bump major, build, tag, publish to GitHub
#   ./scripts/build-release.sh minor    # Bump minor, build, tag, publish to GitHub
#   ./scripts/build-release.sh patch    # Bump patch, build, tag, publish to GitHub
#   ./scripts/build-release.sh chore    # Release current version as-is (no bump)

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
BUILD_GRADLE="$PROJECT_DIR/app/build.gradle.kts"
OUTPUT_DIR="$PROJECT_DIR/releases"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

log()   { echo -e "${GREEN}[YAMP]${NC} $1"; }
warn()  { echo -e "${YELLOW}[YAMP]${NC} $1"; }
error() { echo -e "${RED}[YAMP]${NC} $1" >&2; }
info()  { echo -e "${CYAN}[YAMP]${NC} $1"; }

usage() {
    echo "Usage: $0 [major|minor|patch|chore]"
    echo ""
    echo "  major   Bump major version (X.0.0), build, tag, and publish GitHub Release"
    echo "  minor   Bump minor version (x.Y.0), build, tag, and publish GitHub Release"
    echo "  patch   Bump patch version (x.y.Z), build, tag, and publish GitHub Release"
    echo "  chore   Release current version as-is without bumping"
    echo ""
    echo "Requires: gh (GitHub CLI), git, gradle"
    exit 1
}

# Validate prerequisites
command -v gh >/dev/null 2>&1 || { error "gh (GitHub CLI) is required. Install: https://cli.github.com"; exit 1; }
command -v git >/dev/null 2>&1 || { error "git is required"; exit 1; }

# Ensure we're in a git repo with a remote
cd "$PROJECT_DIR"
REPO_URL=$(git remote get-url origin 2>/dev/null) || { error "No git remote 'origin' configured"; exit 1; }
# Extract owner/repo from URL (handles both HTTPS and SSH)
GITHUB_REPO=$(echo "$REPO_URL" | sed -E 's|.*github\.com[:/]||' | sed 's/\.git$//')
log "GitHub repo: $GITHUB_REPO"

# Ensure working tree is clean (except build.gradle.kts which we may modify)
if [ -n "$(git status --porcelain -- ':!app/build.gradle.kts')" ]; then
    warn "Working tree has uncommitted changes (besides build.gradle.kts)."
    warn "Consider committing or stashing first."
fi

# Extract current version
get_version_part() {
    grep "val version$1" "$BUILD_GRADLE" | grep -o '[0-9]*'
}

MAJOR=$(get_version_part "Major")
MINOR=$(get_version_part "Minor")
PATCH=$(get_version_part "Patch")
CURRENT_VERSION="$MAJOR.$MINOR.$PATCH"

log "Current version: $CURRENT_VERSION"

# Parse argument
BUMP_TYPE="${1:-}"
[ -z "$BUMP_TYPE" ] && usage

case "$BUMP_TYPE" in
    major)
        MAJOR=$((MAJOR + 1)); MINOR=0; PATCH=0
        ;;
    minor)
        MINOR=$((MINOR + 1)); PATCH=0
        ;;
    patch)
        PATCH=$((PATCH + 1))
        ;;
    chore)
        log "Chore release: releasing v$CURRENT_VERSION as-is (no version bump)"
        ;;
    *)
        error "Unknown bump type: $BUMP_TYPE"
        usage
        ;;
esac

NEW_VERSION="$MAJOR.$MINOR.$PATCH"
TAG_NAME="v$NEW_VERSION"

# Check if tag already exists (for non-chore)
if git rev-parse "$TAG_NAME" >/dev/null 2>&1; then
    if [ "$BUMP_TYPE" != "chore" ]; then
        error "Tag $TAG_NAME already exists. Use a different bump type or delete the tag."
        exit 1
    fi
    # For chore, check if GH release exists
    if gh release view "$TAG_NAME" --repo "$GITHUB_REPO" >/dev/null 2>&1; then
        warn "GitHub Release $TAG_NAME already exists. It will be updated with new assets."
        RELEASE_EXISTS=true
    else
        RELEASE_EXISTS=false
    fi
else
    RELEASE_EXISTS=false
fi

# Update version in build.gradle.kts if bumping
if [ "$BUMP_TYPE" != "chore" ]; then
    log "Bumping version to: $NEW_VERSION"
    sed -i "s/val versionMajor = [0-9]*/val versionMajor = $MAJOR/" "$BUILD_GRADLE"
    sed -i "s/val versionMinor = [0-9]*/val versionMinor = $MINOR/" "$BUILD_GRADLE"
    sed -i "s/val versionPatch = [0-9]*/val versionPatch = $PATCH/" "$BUILD_GRADLE"
fi

# Build
log "Cleaning project..."
cd "$PROJECT_DIR"
./gradlew clean

log "Running unit tests..."
./gradlew testDebugUnitTest
info "All tests passed."

log "Building release APK..."
./gradlew assembleRelease

# Verify signing config exists
if [ ! -f "$PROJECT_DIR/keystore.properties" ]; then
    error "keystore.properties not found! Release APK must be signed."
    error "Create keystore.properties with: storeFile, storePassword, keyAlias, keyPassword"
    exit 1
fi

# Locate and copy APK (prefer signed over unsigned)
mkdir -p "$OUTPUT_DIR"
APK_DEST="$OUTPUT_DIR/yamp-v${NEW_VERSION}.apk"

# AGP outputs app-release.apk when signed, app-release-unsigned.apk when not
APK_SOURCE="$PROJECT_DIR/app/build/outputs/apk/release/app-release.apk"
if [ ! -f "$APK_SOURCE" ]; then
    # Fallback: search for any release APK (signed preferred)
    APK_SOURCE=$(find "$PROJECT_DIR/app/build/outputs" -name "*.apk" -path "*/release/*" ! -name "*unsigned*" -type f | head -1)
    if [ -z "$APK_SOURCE" ]; then
        # Last resort: check for unsigned (will warn)
        APK_SOURCE=$(find "$PROJECT_DIR/app/build/outputs" -name "*.apk" -path "*/release/*" -type f | head -1)
        if [ -n "$APK_SOURCE" ]; then
            warn "Only unsigned APK found - it will NOT install on devices!"
        else
            error "No release APK found in build outputs"
            exit 1
        fi
    fi
fi
cp "$APK_SOURCE" "$APK_DEST"

log "Release APK: $APK_DEST ($(du -h "$APK_DEST" | cut -f1))"

# Verify APK is signed
if command -v apksigner >/dev/null 2>&1; then
    if apksigner verify --print-certs "$APK_DEST" >/dev/null 2>&1; then
        info "APK signature verified (v1+v2+v3)"
    else
        warn "APK signature verification failed!"
    fi
elif command -v jarsigner >/dev/null 2>&1; then
    if jarsigner -verify "$APK_DEST" >/dev/null 2>&1; then
        info "APK signature verified (jarsigner)"
    else
        warn "APK signature verification failed!"
    fi
else
    warn "Neither apksigner nor jarsigner found - cannot verify signature"
fi

# Generate SHA-256 checksum
CHECKSUM_FILE="$OUTPUT_DIR/yamp-v${NEW_VERSION}.sha256"
sha256sum "$APK_DEST" | awk '{print $1}' > "$CHECKSUM_FILE"
APK_SHA256=$(cat "$CHECKSUM_FILE")
log "SHA-256: $APK_SHA256"

# Create source archive
log "Creating source archive..."
SOURCE_ARCHIVE="$OUTPUT_DIR/yamp-v${NEW_VERSION}-source.tar.gz"
cd "$PROJECT_DIR"
git archive --format=tar.gz --prefix="yamp-v${NEW_VERSION}/" -o "$SOURCE_ARCHIVE" HEAD 2>/dev/null || \
    tar czf "$SOURCE_ARCHIVE" \
        --exclude='.git' \
        --exclude='build' \
        --exclude='.gradle' \
        --exclude='releases' \
        --exclude='local.properties' \
        --transform "s,^\.,yamp-v${NEW_VERSION}," \
        .

# Generate release notes from git log
log "Generating release notes..."
PREVIOUS_TAG=$(git tag --sort=-v:refname | head -1 2>/dev/null || echo "")
if [ -n "$PREVIOUS_TAG" ]; then
    CHANGELOG=$(git log --pretty=format:"- %s" "$PREVIOUS_TAG"..HEAD 2>/dev/null || echo "- Release $TAG_NAME")
else
    CHANGELOG=$(git log --pretty=format:"- %s" -20 2>/dev/null || echo "- Initial release")
fi

RELEASE_NOTES=$(cat <<EOF
## YAMP $TAG_NAME

### Changes
$CHANGELOG

### Checksums
| File | SHA-256 |
|------|---------|
| yamp-v${NEW_VERSION}.apk | \`$APK_SHA256\` |

### Install
Download \`yamp-v${NEW_VERSION}.apk\` and install on your Android device (API 26+).
EOF
)

# Git operations: commit version bump, create tag
cd "$PROJECT_DIR"
if [ "$BUMP_TYPE" != "chore" ]; then
    log "Committing version bump..."
    git add app/build.gradle.kts
    git commit -m "chore: bump version to $NEW_VERSION" || true
fi

if ! git rev-parse "$TAG_NAME" >/dev/null 2>&1; then
    log "Creating git tag: $TAG_NAME"
    git tag -a "$TAG_NAME" -m "Release $TAG_NAME"
fi

# Push to remote
log "Pushing to origin..."
git push origin "$(git branch --show-current)" --tags

# Create or update GitHub Release
if [ "$RELEASE_EXISTS" = "true" ] 2>/dev/null; then
    log "Updating existing GitHub Release: $TAG_NAME"
    # Delete old assets first
    gh release delete-asset "$TAG_NAME" "yamp-v${NEW_VERSION}.apk" --repo "$GITHUB_REPO" --yes 2>/dev/null || true
    gh release delete-asset "$TAG_NAME" "yamp-v${NEW_VERSION}.sha256" --repo "$GITHUB_REPO" --yes 2>/dev/null || true
    gh release delete-asset "$TAG_NAME" "yamp-v${NEW_VERSION}-source.tar.gz" --repo "$GITHUB_REPO" --yes 2>/dev/null || true
    # Upload new assets
    gh release upload "$TAG_NAME" \
        "$APK_DEST" \
        "$CHECKSUM_FILE" \
        "$SOURCE_ARCHIVE" \
        --repo "$GITHUB_REPO" \
        --clobber
    # Update release notes
    gh release edit "$TAG_NAME" \
        --repo "$GITHUB_REPO" \
        --notes "$RELEASE_NOTES"
else
    log "Creating GitHub Release: $TAG_NAME"
    gh release create "$TAG_NAME" \
        "$APK_DEST" \
        "$CHECKSUM_FILE" \
        "$SOURCE_ARCHIVE" \
        --repo "$GITHUB_REPO" \
        --title "YAMP $TAG_NAME" \
        --notes "$RELEASE_NOTES"
fi

echo ""
log "========================================="
log "  Release $TAG_NAME published!"
log "========================================="
info "APK:     $APK_DEST"
info "SHA-256: $APK_SHA256"
info "Source:  $SOURCE_ARCHIVE"
info "GitHub:  https://github.com/$GITHUB_REPO/releases/tag/$TAG_NAME"
