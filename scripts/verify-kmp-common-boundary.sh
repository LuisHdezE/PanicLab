#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
COMMON_DIR="${KMP_COMMON_DIR:-$ROOT_DIR/shared/src/commonMain}"

FORBIDDEN_IMPORTS=(
  'android\.'
  'androidx\.room\.'
  'androidx\.compose\.'
  'androidx\.camera\.'
  'com\.google\.firebase\.'
  'com\.google\.mlkit\.'
  'org\.json\.'
  'java\.security\.'
  'java\.util\.regex\.Pattern'
  'com\.example\.data\.local\.'
  'com\.example\.data\.remote\.'
  'com\.example\.ui\.'
  'com\.example\.navigation\.'
)

build_pattern() {
  local joined=""
  local entry
  for entry in "${FORBIDDEN_IMPORTS[@]}"; do
    if [[ -z "$joined" ]]; then
      joined="$entry"
    else
      joined="$joined|$entry"
    fi
  done
  printf '%s' "$joined"
}

check_dir() {
  local dir="$1"
  if [[ ! -d "$dir" ]]; then
    echo "PASS: commonMain boundary guard is armed; directory does not exist yet: $dir"
    return 0
  fi

  local pattern
  pattern="$(build_pattern)"

  local violations
  violations="$(grep -RInE --include='*.kt' "^[[:space:]]*import[[:space:]]+(${pattern})" "$dir" || true)"

  if [[ -n "$violations" ]]; then
    echo "FAIL: forbidden platform/JVM imports detected in shared/commonMain:" >&2
    echo "$violations" >&2
    return 1
  fi

  echo "PASS: no forbidden platform/JVM imports found in $dir"
}

self_test() {
  local tmp
  tmp="$(mktemp -d)"

  mkdir -p "$tmp/safe" "$tmp/bad"
  cat > "$tmp/safe/Safe.kt" <<'EOF'
package fixture
import kotlin.collections.List
class Safe
EOF
  cat > "$tmp/bad/Bad.kt" <<'EOF'
package fixture
import org.json.JSONObject
class Bad
EOF

  check_dir "$tmp/safe"

  if check_dir "$tmp/bad" >/dev/null 2>&1; then
    rm -rf -- "$tmp"
    echo "FAIL: guard self-test expected forbidden import rejection" >&2
    return 1
  fi

  rm -rf -- "$tmp"
  echo "PASS: guard self-test rejected a controlled forbidden import"
}

case "${1:-}" in
  --self-test)
    self_test
    ;;
  --help|-h)
    cat <<'EOF'
Usage:
  scripts/verify-kmp-common-boundary.sh
  scripts/verify-kmp-common-boundary.sh --self-test

Environment:
  KMP_COMMON_DIR=/path/to/commonMain  Override the directory to inspect.
EOF
    ;;
  *)
    check_dir "$COMMON_DIR"
    ;;
esac
