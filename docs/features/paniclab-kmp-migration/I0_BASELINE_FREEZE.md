# I0 — Baseline Freeze and Fixture Extraction

**Branch:** `kmp/i0-baseline-freeze`  
**Base:** `main@5a8ffffca3261b5f5aa3efd6cf2c17ba9b81e832`  
**Behavior baseline:** `c165ae4283a3b592eddb2b70a1c13a9ffeb51f01`  
**Implementation authorization:** I0 explicitly authorized by Luis on 2026-09-18.  
**Execution state:** COMPLETE, pending PR review/merge.

## Purpose

Freeze the current deterministic Android behavior into portable fixtures and arm the architectural boundary guard before any production logic moves to Kotlin Multiplatform.

I0 does **not** add a `shared` module, change production diagnosis, alter Room, introduce iOS code or modify `SoftwareDevelopmentBlueprint`.

The repository was intentionally changed to **public** on 2026-09-18 after a publication-readiness audit so standard GitHub-hosted CI can execute the zero-cost validation path established by the KMP laboratory.

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

## GitHub Actions execution

Workflow: `.github/workflows/kmp-i0-baseline.yml`

The final I0 validation executes on the standard public-repository GitHub-hosted Ubuntu runner with:

- Ubuntu 24.04 runner;
- Temurin JDK 21;
- Gradle 9.3.1;
- read-only `GITHUB_TOKEN` permissions;
- JSON fixture syntax validation;
- architecture guard self-test;
- repository architecture scan;
- selected deterministic Android/Robolectric baseline tests;
- test-report artifact upload.

JDK 21 is required because the existing Robolectric 4.16.1 tests explicitly target Android API 36. The test SDK was not lowered to make CI pass.

The workflow triggers on pull requests to `main` plus manual `workflow_dispatch`; the redundant branch-push trigger was removed so a PR update does not execute the same validation twice.

## Executed evidence

**Successful workflow run:** `35385085058`  
**Validated head:** `9e387b852c913f716267324e826e08e210ebf029`  
**Run URL:** `https://github.com/LuisHdezE/PanicLab/actions/runs/35385085058`  
**Result:** SUCCESS  
**Gradle result:** `BUILD SUCCESSFUL`  
**Artifact:** `kmp-i0-unit-test-reports`  
**Artifact ID:** `10563322749`  
**Artifact SHA-256:** `a3344fd9bae73034e1ad5cc8358ab11ceb8309c6f310edea2d9185b758b2c30c`

| Evidence | Status | Result |
| --- | --- | --- |
| Diagnostic fixture inventory | PASS | Portable expected-output catalog frozen from baseline tests |
| Rule-pack/redaction fixture inventory | PASS | Portable expected-output catalog frozen from baseline tests |
| OCR text fixture inventory | PASS | Portable expected-output catalog frozen from baseline tests |
| JSON syntax validation | PASS | All three fixture files parsed successfully in GitHub Actions |
| Baseline Android/JVM tests | PASS | `DeterministicDiagnosticEngineTest`, `RulePackManagementTest`, and `OcrLogExtractorTest` completed successfully |
| Guard self-test | PASS | Allowed fixture accepted and controlled forbidden `org.json` import rejected |
| Future commonMain guard | PASS / ARMED | Repository scan passes; `shared/src/commonMain` does not exist yet |
| Test report artifact | PASS | Uploaded by GitHub Actions with recorded digest |

## Baseline defects discovered and contained during I0

I0 exposed two infrastructure/test-harness defects before any KMP production migration:

1. `GreetingScreenshotTest` had not been updated after `HomeScreen` gained `onNavigateToTrends`; the test-only call site was corrected with an empty navigation callback. No production code changed.
2. The initial CI runner used JDK 17. Robolectric 4.16.1 supports API 36, but API 36 execution requires JDK 21. The workflow was corrected to JDK 21 rather than lowering the tested Android SDK or changing diagnostic behavior.

The architecture guard self-test also exposed a `set -u` cleanup bug in its first implementation. The guard was corrected and then proven by both positive and controlled-negative cases.

These findings are part of the methodology evidence: baseline CI must be executable and truthful before shared-code extraction begins.

## I0 completion gate

All I0 completion conditions are now satisfied:

1. all three JSON fixture files parse successfully;
2. `DeterministicDiagnosticEngineTest`, `RulePackManagementTest` and `OcrLogExtractorTest` pass against the unchanged Android diagnostic implementation;
3. `scripts/verify-kmp-common-boundary.sh --self-test` passes;
4. the normal architecture guard command passes;
5. executable evidence is recorded above.

Therefore TASK-KMP-001, TASK-KMP-002 and TASK-KMP-003 are considered **DONE for I0 execution**, subject only to PR #2 review and explicit merge approval.
