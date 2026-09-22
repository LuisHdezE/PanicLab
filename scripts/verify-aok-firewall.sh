#!/usr/bin/env bash
set -euo pipefail

fail=0

report_matches() {
  local label="$1"
  local pattern="$2"
  shift 2

  local existing=()
  local path
  for path in "$@"; do
    [[ -e "$path" ]] && existing+=("$path")
  done

  [[ ${#existing[@]} -eq 0 ]] && return 0

  local matches
  matches=$(grep -RInE --include='*.kt' --include='*.java' --include='*.swift' "$pattern" "${existing[@]}" || true)
  if [[ -n "$matches" ]]; then
    echo "AOK FIREWALL FAIL: $label"
    echo "$matches"
    fail=1
  fi
}

# Deterministic parsing / diagnosis / Rule Pack code must never consume Apple Official Knowledge.
report_matches \
  "deterministic core imports Apple Official Knowledge" \
  '^import[[:space:]]+com\.example\.appleknowledge' \
  shared/src/commonMain/kotlin/com/example/diagnostic \
  shared/src/commonMain/kotlin/com/example/parser \
  shared/src/commonMain/kotlin/com/example/rulepack \
  app/src/main/java/com/example/data/repository/DiagnosticRepositoryImpl.kt \
  app/src/main/java/com/example/ui/analysis

# Apple Official Knowledge production code is contextual and must not call deterministic engines or Rule Pack internals.
report_matches \
  "Apple Official Knowledge imports deterministic diagnosis or Rule Pack internals" \
  '^import[[:space:]]+com\.example\.(diagnostic|rulepack|parser)' \
  shared/src/commonMain/kotlin/com/example/appleknowledge \
  app/src/main/java/com/example/data/appleknowledge \
  app/src/main/java/com/example/ui/appleknowledge

# Native AOK presentation must remain separate from the diagnostic facade.
report_matches \
  "Apple Official Knowledge Swift surface references NativeDiagnosticFacade" \
  'NativeDiagnosticFacade' \
  iosApp/PanicLabIOS/ContentView.swift

if [[ $fail -ne 0 ]]; then
  exit 1
fi

echo "AOK FIREWALL PASS: Apple Official Knowledge remains context-only and structurally separate from deterministic diagnosis."
