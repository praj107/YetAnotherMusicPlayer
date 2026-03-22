#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/../../.." && pwd)"
VERSION_FILE="$PROJECT_DIR/version.properties"

require_version_file() {
    if [[ ! -f "$VERSION_FILE" ]]; then
        echo "Missing version file: $VERSION_FILE" >&2
        exit 1
    fi
}

version_get() {
    local key="$1"
    require_version_file
    local value
    value="$(grep -E "^${key}=" "$VERSION_FILE" | cut -d'=' -f2- || true)"
    if [[ -z "$value" ]]; then
        echo "Missing version key ${key} in ${VERSION_FILE}" >&2
        exit 1
    fi
    printf '%s\n' "$value"
}

version_major() { version_get "VERSION_MAJOR"; }
version_minor() { version_get "VERSION_MINOR"; }
version_patch() { version_get "VERSION_PATCH"; }

version_name() {
    printf '%s.%s.%s\n' "$(version_major)" "$(version_minor)" "$(version_patch)"
}

version_write_name() {
    local version_name="$1"
    if [[ ! "$version_name" =~ ^([0-9]+)\.([0-9]+)\.([0-9]+)$ ]]; then
        echo "Invalid semantic version: $version_name" >&2
        exit 1
    fi
    version_write "${BASH_REMATCH[1]}" "${BASH_REMATCH[2]}" "${BASH_REMATCH[3]}"
}

version_code() {
    printf '%s\n' "$(( $(version_major) * 10000 + $(version_minor) * 100 + $(version_patch) ))"
}

version_write() {
    local major="$1"
    local minor="$2"
    local patch="$3"
    cat > "$VERSION_FILE" <<EOF
VERSION_MAJOR=${major}
VERSION_MINOR=${minor}
VERSION_PATCH=${patch}
EOF
}

version_bump() {
    local bump_type="$1"
    local major minor patch
    major="$(version_major)"
    minor="$(version_minor)"
    patch="$(version_patch)"

    case "$bump_type" in
        major)
            major=$((major + 1))
            minor=0
            patch=0
            ;;
        minor)
            minor=$((minor + 1))
            patch=0
            ;;
        patch)
            patch=$((patch + 1))
            ;;
        chore)
            ;;
        *)
            echo "Unsupported bump type: $bump_type" >&2
            exit 1
            ;;
    esac

    version_write "$major" "$minor" "$patch"
}
