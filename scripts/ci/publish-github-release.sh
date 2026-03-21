#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"

MANIFEST_FILE="${1:-$PROJECT_DIR/releases/release-manifest.env}"
if [[ ! -f "$MANIFEST_FILE" ]]; then
    echo "Missing release manifest: $MANIFEST_FILE" >&2
    exit 1
fi

if ! command -v gh >/dev/null 2>&1; then
    echo "GitHub CLI (gh) is required" >&2
    exit 1
fi

# shellcheck disable=SC1090
source "$MANIFEST_FILE"

REPO_URL="$(git -C "$PROJECT_DIR" remote get-url origin)"
GITHUB_REPO="$(echo "$REPO_URL" | sed -E 's|.*github\.com[:/]||' | sed 's/\.git$//')"

declare -a assets
assets=("$APK_DEST" "$APK_SHA_FILE" "$SOURCE_ARCHIVE" "$RELEASE_NOTES_FILE")
if [[ -n "${AAB_DEST:-}" ]]; then
    assets+=("$AAB_DEST")
fi
if [[ -n "${AAB_SHA_FILE:-}" ]]; then
    assets+=("$AAB_SHA_FILE")
fi
if [[ -n "${MAPPING_DEST:-}" ]]; then
    assets+=("$MAPPING_DEST")
fi

if gh release view "$TAG_NAME" --repo "$GITHUB_REPO" >/dev/null 2>&1; then
    gh release upload "$TAG_NAME" "${assets[@]}" --repo "$GITHUB_REPO" --clobber
    gh release edit "$TAG_NAME" --repo "$GITHUB_REPO" --title "YAMP $TAG_NAME" --notes-file "$RELEASE_NOTES_FILE"
else
    gh release create "$TAG_NAME" "${assets[@]}" --repo "$GITHUB_REPO" --title "YAMP $TAG_NAME" --notes-file "$RELEASE_NOTES_FILE"
fi
