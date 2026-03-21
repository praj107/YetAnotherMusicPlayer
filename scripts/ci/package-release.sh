#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
DEFAULT_OUTPUT_DIR="$PROJECT_DIR/releases"

# shellcheck source=scripts/ci/lib/version.sh
source "$SCRIPT_DIR/lib/version.sh"

VERSION_NAME=""
OUTPUT_DIR="$DEFAULT_OUTPUT_DIR"

while [[ $# -gt 0 ]]; do
    case "$1" in
        --version)
            VERSION_NAME="$2"
            shift 2
            ;;
        --output-dir)
            OUTPUT_DIR="$2"
            shift 2
            ;;
        *)
            echo "Unknown argument: $1" >&2
            exit 1
            ;;
    esac
done

if [[ -z "$VERSION_NAME" ]]; then
    VERSION_NAME="$(version_name)"
fi

TAG_NAME="v${VERSION_NAME}"
mkdir -p "$OUTPUT_DIR"

APK_SOURCE="$PROJECT_DIR/app/build/outputs/apk/release/app-release.apk"
if [[ ! -f "$APK_SOURCE" ]]; then
    APK_SOURCE="$(find "$PROJECT_DIR/app/build/outputs" -path '*/release/*.apk' ! -name '*unsigned*' -type f | head -1)"
fi
if [[ -z "$APK_SOURCE" || ! -f "$APK_SOURCE" ]]; then
    echo "Unable to find a signed release APK in app/build/outputs" >&2
    exit 1
fi

AAB_SOURCE="$PROJECT_DIR/app/build/outputs/bundle/release/app-release.aab"
if [[ ! -f "$AAB_SOURCE" ]]; then
    AAB_SOURCE="$(find "$PROJECT_DIR/app/build/outputs" -path '*/release/*.aab' -type f | head -1)"
fi

MAPPING_SOURCE="$PROJECT_DIR/app/build/outputs/mapping/release/mapping.txt"
if [[ ! -f "$MAPPING_SOURCE" ]]; then
    MAPPING_SOURCE=""
fi

APK_DEST="$OUTPUT_DIR/yamp-v${VERSION_NAME}.apk"
AAB_DEST="$OUTPUT_DIR/yamp-v${VERSION_NAME}.aab"
MAPPING_DEST="$OUTPUT_DIR/yamp-v${VERSION_NAME}-mapping.txt"
APK_SHA_FILE="$OUTPUT_DIR/yamp-v${VERSION_NAME}.apk.sha256"
AAB_SHA_FILE="$OUTPUT_DIR/yamp-v${VERSION_NAME}.aab.sha256"
SOURCE_ARCHIVE="$OUTPUT_DIR/yamp-v${VERSION_NAME}-source.tar.gz"
RELEASE_NOTES_FILE="$OUTPUT_DIR/yamp-v${VERSION_NAME}-release-notes.md"
MANIFEST_FILE="$OUTPUT_DIR/release-manifest.env"

cp "$APK_SOURCE" "$APK_DEST"
sha256sum "$APK_DEST" | awk '{print $1}' > "$APK_SHA_FILE"

if [[ -n "$AAB_SOURCE" && -f "$AAB_SOURCE" ]]; then
    cp "$AAB_SOURCE" "$AAB_DEST"
    sha256sum "$AAB_DEST" | awk '{print $1}' > "$AAB_SHA_FILE"
else
    AAB_DEST=""
    AAB_SHA_FILE=""
fi

if [[ -n "$MAPPING_SOURCE" ]]; then
    cp "$MAPPING_SOURCE" "$MAPPING_DEST"
else
    MAPPING_DEST=""
fi

if command -v apksigner >/dev/null 2>&1; then
    apksigner verify --print-certs "$APK_DEST" >/dev/null
elif [[ -x "${ANDROID_HOME:-}/build-tools/35.0.0/apksigner" ]]; then
    "${ANDROID_HOME}/build-tools/35.0.0/apksigner" verify --print-certs "$APK_DEST" >/dev/null
fi

(
    cd "$PROJECT_DIR"
    git archive --format=tar.gz --prefix="yamp-v${VERSION_NAME}/" -o "$SOURCE_ARCHIVE" HEAD
)

PREVIOUS_TAG="$(git -C "$PROJECT_DIR" tag --sort=-v:refname | grep -vx "$TAG_NAME" | head -1 || true)"
if [[ -n "$PREVIOUS_TAG" ]]; then
    CHANGELOG="$(git -C "$PROJECT_DIR" log --pretty=format:'- %s' "${PREVIOUS_TAG}"..HEAD)"
else
    CHANGELOG="$(git -C "$PROJECT_DIR" log --pretty=format:'- %s' -20)"
fi

APK_SHA="$(cat "$APK_SHA_FILE")"
AAB_SHA=""
if [[ -n "$AAB_SHA_FILE" && -f "$AAB_SHA_FILE" ]]; then
    AAB_SHA="$(cat "$AAB_SHA_FILE")"
fi

cat > "$RELEASE_NOTES_FILE" <<EOF
## YAMP ${TAG_NAME}

### Highlights
${CHANGELOG:-"- Release ${TAG_NAME}"}

### Assets
| File | SHA-256 |
|------|---------|
| $(basename "$APK_DEST") | \`${APK_SHA}\` |
EOF

if [[ -n "$AAB_DEST" && -n "$AAB_SHA" ]]; then
    cat >> "$RELEASE_NOTES_FILE" <<EOF
| $(basename "$AAB_DEST") | \`${AAB_SHA}\` |
EOF
fi

cat >> "$RELEASE_NOTES_FILE" <<EOF

### Install
- Download \`$(basename "$APK_DEST")\` for direct sideload installs and in-app update pickup.
- Use \`$(basename "$AAB_DEST")\` for store-style distribution if you need an Android App Bundle.
EOF

cat > "$MANIFEST_FILE" <<EOF
VERSION_NAME=${VERSION_NAME}
TAG_NAME=${TAG_NAME}
APK_DEST=${APK_DEST}
APK_SHA_FILE=${APK_SHA_FILE}
AAB_DEST=${AAB_DEST}
AAB_SHA_FILE=${AAB_SHA_FILE}
MAPPING_DEST=${MAPPING_DEST}
SOURCE_ARCHIVE=${SOURCE_ARCHIVE}
RELEASE_NOTES_FILE=${RELEASE_NOTES_FILE}
EOF

cat "$MANIFEST_FILE"
