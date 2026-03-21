#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
# shellcheck source=scripts/ci/lib/version.sh
source "$SCRIPT_DIR/lib/version.sh"

usage() {
    cat <<'EOF'
Usage: ./scripts/ci/bump-version.sh [major|minor|patch|chore]

Prints shell-compatible key/value pairs describing the effective version.
EOF
}

BUMP_TYPE="${1:-}"
if [[ -z "$BUMP_TYPE" ]]; then
    usage
    exit 1
fi

version_bump "$BUMP_TYPE"

VERSION_NAME="$(version_name)"
cat <<EOF
VERSION_NAME=${VERSION_NAME}
VERSION_CODE=$(version_code)
VERSION_TAG=v${VERSION_NAME}
EOF
