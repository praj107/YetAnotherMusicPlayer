#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
# shellcheck source=scripts/ci/lib/version.sh
source "$SCRIPT_DIR/lib/version.sh"

usage() {
    cat <<'EOF'
Usage: ./scripts/ci/set-version.sh <major.minor.patch>

Sets version.properties to the provided semantic version and prints shell-compatible
key/value pairs describing the effective version.
EOF
}

VERSION_NAME="${1:-}"
if [[ -z "$VERSION_NAME" ]]; then
    usage
    exit 1
fi

version_write_name "$VERSION_NAME"

cat <<EOF
VERSION_NAME=$(version_name)
VERSION_CODE=$(version_code)
VERSION_TAG=v$(version_name)
EOF
