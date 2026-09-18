# I0 — Baseline Freeze and Fixture Extraction

**Branch:** `kmp/i0-baseline-freeze`  
**Base:** `main@5a8ffffca3261b5f5aa3efd6cf2c17ba9b81e832`  
**Behavior baseline:** `c165ae4283a3b592eddb2b70a1c13a9ffeb51f01`  
**Implementation authorization:** I0 explicitly authorized by Luis on 2026-09-18.

## Purpose

Freeze the current deterministic Android behavior into portable fixtures and arm the architectural boundary guard before any production logic moves to Kotlin Multiplatform.

I0 does **not** add a `shared` module, change production diagnosis, alter Room, introduce iOS code, change repository visibility or modify `SoftwareDevelopmentBlueprint`.

## Frozen fixture inventory

### Diagnostic fixtures

File: `migration-fixtures/diagnostic/baseline-diagnostic-fixtures.json`

Source of truth: `app/src/test/java/com/example/DeterministicDiagnosticEngineTest.kt`.

Frozen scenarios:

| Fixture | Baseline behavior preserved | CA |
| --- | --- | --- |
| `iphone13-mini-0x1000-dock-mic` | Exact 0x1000 diagnosis, HIGH confidence, VERIFIED, dock/mic component | CA-01 |
| `iphone14-0x500000-battery` | Exact battery/gas-gauge diagnosis | CA-01 |
| `iphone16-pro-decimal-3145728` | Decimal 3145728 resolves as 0x300000 and preserves diagnosis | CA-01, CA-02 |
| `iphone-x-missing-prs0` | THERMAL missing-sensor PRS0 diagnosis | CA-01 |
| `unknown-smc-safe-fallback` | Unknown SMC code remains non-conclusive with no invented component | CA-03 |
| `iphone14-wireless-coil-decimal-regression` | Decimal 4194304 resolves to 0x400000 with evidence normalization | CA-01, CA-02 |

The same fixture also freezes seven decimal/hex equivalence pairs and sensor-array parsing for 0-5, 0-6, 0-7, 0-12 and mixed decimal/hex arrays.

### Rule-pack and redaction fixtures

File: `migration-fixtures/rulepack/rulepack-and-redaction-fixtures.json`

Source of truth: `app/src/test/java/com/example/RulePackManagementTest.kt`.

Frozen semantics:

- valid 1.1.0 rule pack validation counts and 64-character SHA-256 checksum shape;
- invalid SemVer and missing source rejection;
- duplicate rule ID rejection;
- 1.1.0 -> 1.2.0 diff with one ADDED and one MODIFIED rule;
- synthetic sensitive-data redaction while preserving diagnostic sensor evidence.

The canonical full rule-pack JSON intentionally remains in the existing Android test until I4, when parser semantics are migrated to common code.

### OCR text fixtures

File: `migration-fixtures/ocr/ocr-text-fixtures.json`

Source of truth: `app/src/test/java/com/example/OcrLogExtractorTest.kt`.

Frozen scenarios:

- panic signature + device/build/hex extraction;
- OCR repair of `Ox4OOOOO` -> `0x400000` and `0xl000` -> `0x1000`;
- decimal 524288 -> canonical 0x80000 detection;
- empty input returns a graceful empty/non-valid result.

These fixtures freeze **text post-processing only**. CameraX/ML Kit image acquisition remains an Android platform capability.

## Architecture guard inventory

Executable guard: `scripts/verify-kmp-common-boundary.sh`

The guard rejects imports from future `shared/src/commonMain` that cross the approved KMP boundary:

- `android.*`
- `androidx.room.*`
- `androidx.compose.*`
- `androidx.camera.*`
- `com.google.firebase.*`
- `com.google.mlkit.*`
- `org.json.*`
- `java.security.*`
- `java.util.regex.Pattern`
- Android app persistence/remote/UI/navigation packages under `com.example`

The guard intentionally passes before `shared/src/commonMain` exists and includes `--self-test`, which creates one allowed Kotlin fixture and one controlled forbidden `org.json` import. The self-test succeeds only if the allowed fixture passes and the forbidden fixture is rejected.

## Validation commands

Run from the repository root in the normal Linux/WSL development environment:

```bash
./gradlew :app:testDebugUnitTest \
  --tests 'com.example.DeterministicDiagnosticEngineTest' \
  --tests 'com.example.RulePackManagementTest' \
  --tests 'com.example.OcrLogExtractorTest'

bash scripts/verify-kmp-common-boundary.sh --self-test
bash scripts/verify-kmp-common-boundary.sh

python3 -m json.tool migration-fixtures/diagnostic/baseline-diagnostic-fixtures.json >/dev/null
python3 -m json.tool migration-fixtures/rulepack/rulepack-and-redaction-fixtures.json >/dev/null
python3 -m json.tool migration-fixtures/ocr/ocr-text-fixtures.json >/dev/null
```

## Evidence state

| Evidence | Status | Result |
| --- | --- | --- |
| Diagnostic fixture inventory | CREATED | Portable expected-output catalog created from baseline tests |
| Rule-pack/redaction fixture inventory | CREATED | Portable expected-output catalog created from baseline tests |
| OCR text fixture inventory | CREATED | Portable expected-output catalog created from baseline tests |
| Architecture guard | CREATED | Guard + self-test added |
| JSON syntax validation | NOT RUN | Requires command execution |
| Baseline Android/JVM tests | NOT RUN | Requires Gradle execution in development environment |
| Guard self-test | NOT RUN | Requires shell execution in development environment |
| Future commonMain guard | ARMED | No `shared` module exists in I0 |

## I0 completion gate

I0 can be marked complete only when:

1. all three JSON fixture files parse successfully;
2. `DeterministicDiagnosticEngineTest`, `RulePackManagementTest` and `OcrLogExtractorTest` pass against the unchanged Android implementation;
3. `scripts/verify-kmp-common-boundary.sh --self-test` passes;
4. the normal architecture guard command passes;
5. the executed evidence is recorded in `TASKS.md` or this evidence document.

Until those commands are actually executed, TASK-KMP-001/002/003 remain **IN PROGRESS**, not `DONE`.
