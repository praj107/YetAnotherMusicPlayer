#!/usr/bin/env bash
set -euo pipefail

# YAMP Release Build Script
# Usage: ./scripts/build-release.sh [major|minor|patch]
# Builds a release APK with version bumping

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
BUILD_GRADLE="$PROJECT_DIR/app/build.gradle.kts"
OUTPUT_DIR="$PROJECT_DIR/releases"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log() { echo -e "${GREEN}[YAMP]${NC} $1"; }
warn() { echo -e "${YELLOW}[YAMP]${NC} $1"; }
error() { echo -e "${RED}[YAMP]${NC} $1" >&2; }

# Extract current version
get_version_part() {
    grep "val version$1" "$BUILD_GRADLE" | grep -o '[0-9]*'
}

MAJOR=$(get_version_part "Major")
MINOR=$(get_version_part "Minor")
PATCH=$(get_version_part "Patch")
CURRENT_VERSION="$MAJOR.$MINOR.$PATCH"

log "Current version: $CURRENT_VERSION"

# Bump version if argument provided
BUMP_TYPE="${1:-}"
if [ -n "$BUMP_TYPE" ]; then
    case "$BUMP_TYPE" in
        major)
            MAJOR=$((MAJOR + 1))
            MINOR=0
            PATCH=0
            ;;
        minor)
            MINOR=$((MINOR + 1))
            PATCH=0
            ;;
        patch)
            PATCH=$((PATCH + 1))
            ;;
        *)
            error "Unknown bump type: $BUMP_TYPE (use major, minor, or patch)"
            exit 1
            ;;
    esac

    NEW_VERSION="$MAJOR.$MINOR.$PATCH"
    log "Bumping version to: $NEW_VERSION"

    sed -i "s/val versionMajor = [0-9]*/val versionMajor = $MAJOR/" "$BUILD_GRADLE"
    sed -i "s/val versionMinor = [0-9]*/val versionMinor = $MINOR/" "$BUILD_GRADLE"
    sed -i "s/val versionPatch = [0-9]*/val versionPatch = $PATCH/" "$BUILD_GRADLE"
else
    NEW_VERSION="$CURRENT_VERSION"
fi

# Clean and build
log "Cleaning project..."
cd "$PROJECT_DIR"
./gradlew clean

log "Running unit tests..."
./gradlew testDebugUnitTest

log "Building release APK..."
./gradlew assembleRelease

# Copy APK to releases directory
mkdir -p "$OUTPUT_DIR"
APK_SOURCE="$PROJECT_DIR/app/build/outputs/apk/release/app-release-unsigned.apk"
APK_DEST="$OUTPUT_DIR/yamp-v${NEW_VERSION}.apk"

if [ -f "$APK_SOURCE" ]; then
    cp "$APK_SOURCE" "$APK_DEST"
    log "Release APK: $APK_DEST"
    log "APK Size: $(du -h "$APK_DEST" | cut -f1)"
else
    error "APK not found at $APK_SOURCE"
    # Try alternate location
    APK_SOURCE=$(find "$PROJECT_DIR/app/build/outputs" -name "*.apk" -type f | head -1)
    if [ -n "$APK_SOURCE" ]; then
        cp "$APK_SOURCE" "$APK_DEST"
        log "Release APK (alt): $APK_DEST"
    else
        error "No APK found in build outputs"
        exit 1
    fi
fi

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

log "Source archive: $SOURCE_ARCHIVE"

echo ""
log "=== Release v${NEW_VERSION} Complete ==="
log "APK: $APK_DEST"
log "Source: $SOURCE_ARCHIVE"
