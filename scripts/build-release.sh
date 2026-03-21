#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

usage() {
    cat <<'EOF'
Usage: ./scripts/build-release.sh [major|minor|patch|chore]

This wrapper now uses the same shared release primitives as the Jenkins pipeline.
Jenkins is the source-of-truth release path; this script is retained for parity
when you need to run the exact same workflow locally.
EOF
}

BUMP_TYPE="${1:-}"
if [[ -z "$BUMP_TYPE" ]]; then
    usage
    exit 1
fi

cd "$PROJECT_DIR"

eval "$("$PROJECT_DIR/scripts/ci/bump-version.sh" "$BUMP_TYPE")"
echo "[YAMP] Releasing $VERSION_TAG"

./gradlew clean testDebugUnitTest lintRelease assembleRelease bundleRelease
"$PROJECT_DIR/scripts/ci/package-release.sh" --version "$VERSION_NAME"

if git diff --quiet -- version.properties; then
    echo "[YAMP] version.properties unchanged for $VERSION_TAG"
else
    git add version.properties
    git commit -m "chore: bump version to $VERSION_NAME"
fi

if ! git rev-parse "$VERSION_TAG" >/dev/null 2>&1; then
    git tag -a "$VERSION_TAG" -m "Release $VERSION_TAG"
fi

git push origin "$(git branch --show-current)" --tags
"$PROJECT_DIR/scripts/ci/publish-github-release.sh"
