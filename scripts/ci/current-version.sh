#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
# shellcheck source=scripts/ci/lib/version.sh
source "$SCRIPT_DIR/lib/version.sh"

cat <<EOF
VERSION_NAME=$(version_name)
VERSION_CODE=$(version_code)
VERSION_TAG=v$(version_name)
EOF
