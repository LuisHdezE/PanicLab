# TASKS: PanicLab KMP Migration

**Reference PLAN:** `docs/features/paniclab-kmp-migration/PLAN.md`
**PLAN version:** `4d5c70f12c68c480a930be7ede1b5451b41d1415` / approved 2026-09-18
**Implementation authorization:** I0, I1, I2, I3, I4, I5, I6 and I7 are explicitly authorized, completed and merged. I8 is active only through separately approved capability increments: TASK-KMP-080 and TASK-KMP-081 are completed and merged; TASK-KMP-082 through TASK-KMP-090 are not authorized.

> These tasks translate the approved PLAN into reviewable execution increments. No task may move to `IN PROGRESS` until Luis explicitly authorizes that implementation increment. Approval of this task list, merge of documentation PRs, repository visibility changes and application implementation remain separate actions.

## Authorization and checkpoint ledger

| Increment | Authorization | Execution status | Merge evidence |
| --- | --- | --- | --- |
| I0 | Explicitly authorized by Luis on 2026-09-18 | DONE | PR #2 merged as `a6a00ac153177dacfe7a18c229f3d48cbc16a976` |
| I1 | Explicitly authorized by Luis on 2026-09-18 | DONE | PR #3 merged as `8d8852d0693a8c7e099cbc9a23ff48d47cba266d` |
| I2 | Explicitly authorized by Luis on 2026-09-18 | DONE | PR #4 merged as `9b5b3ad98b8ae77016bedfa521fe6ad71c74035b` |
| I3 | Explicitly authorized by Luis on 2026-09-18 | DONE | PR #6 merged as `623aa9da0d4b6f1005d2a00039bd9e71a9c2f6a9` |
| I4 | Explicitly authorized by Luis on 2026-09-18 | DONE | PR #8 merged as `b50e23cf879e191b531cf0c3ca7080b6300f5e41` |
| I5 | Explicitly authorized by Luis before implementation | DONE | PR #10 merged as `40cdc6006a0884465aa0cd92b89a3a1ae74638b2` |
| I6 | Explicitly authorized by Luis on 2026-09-19 | DONE | PR #13 merged as `425c59017f0368833416f8b83925b4b099558073`; PR #14 merged as `cd63f4f9a790ead3199bc1d1e063c5e905c5e82f`; PR #16 merged as `a97e4afdf25f8f5feb9b1900b6d9116a7d391e9f` |
| I7 | Explicitly authorized by Luis on 2026-09-19 | DONE | TASK-KMP-070 via PR #18 merged as `d64e106509f282b66e2cce0241d6c241ff9aaf14`; TASK-KMP-071 via PR #20 merged as `cb580ba0e5032fd96069b76e1706d979ed217335`; TASK-KMP-072 via PR #22 merged as `4b14b534364fe3b5703989eb5289a4dbf1653e19` |
| I8 | Roadmap preparation authorized after I7; TASK-KMP-081 separately authorized by Luis after PR #24 | IN PROGRESS — 080/081 DONE | TASK-KMP-080 via PR #24 merged as `bd98d7c6bf727bc597d66972970b36ffbb773c69`; TASK-KMP-081 via PR #25 merged as `1fa8d73a7e4fdfe9c56ba8cc067ce8d596be801e` |

## Status vocabulary

- TODO
- IN PROGRESS
- BLOCKED
- DONE

A task is DONE only when its required implementation and validation have both been completed and its evidence reference is recorded.

## Execution rules

1. Work in order unless a dependency-free QA/documentation task is intentionally advanced.
2. Each implementation boundary must remain reviewable and should normally land through its own PR or very small PR series.
3. The Android product must remain functional throughout the migration.
4. Existing deterministic behavior is the reference until an independently approved behavior change says otherwise.
5. No Room schema change, destructive migration, repository visibility change or Blueprint modification is implied by implementation authorization.
6. A failed deterministic equivalence, architecture, data-preservation or required coverage gate blocks the relevant increment.
7. iOS support is not declared from compilation alone. Real macOS/Xcode evidence is mandatory.
8. Coverage is reported per executable surface. JVM/Kover and Kotlin/Native/iOS results are never combined into a fabricated global percentage.

# I0 — Baseline freeze and fixture extraction

### TASK-KMP-001 · Freeze deterministic diagnostic fixtures

- **Status:** DONE
- **Target:** COMMON / ANDROID
- **RF/CA:** RF-01, RF-07 · CA-01, CA-02, CA-03, CA-11
- **Objective:** Convert the current deterministic Android expectations into reusable migration fixtures before moving production logic.
- **Scope:** Inventory and normalize representative cases from `DeterministicDiagnosticEngineTest`, including supported devices, exact sensor-code matches, decimal/hex equivalence, panic families, missing sensors, unknown SMC fallback, confidence, verification, primary diagnosis and alternatives.
- **Dependencies:** None
- **Files/components expected:** existing Android deterministic tests plus new fixture resources/expected-output representations suitable for later `commonTest` use.
- **Validation method:** Execute the existing baseline suite at `main@c165ae4283a3b592eddb2b70a1c13a9ffeb51f01` semantics and verify every extracted fixture reproduces the same approved outcome.
- **Coverage expectation:** 100% of CA-01/02/03 representative scenarios mapped; no new production-code threshold yet.
- **Regression scope:** Existing deterministic Android engine tests.
- **Evidence required:** fixture inventory, baseline test report, mapping from fixture to expected domain outcome.
- **Actual result:** Diagnostic fixture catalog frozen and validated; `DeterministicDiagnosticEngineTest` passed on GitHub Actions with the original Android implementation unchanged.
- **Evidence reference:** `I0_BASELINE_FREEZE.md`; workflow run `35385085058`; PR #2; merge `a6a00ac153177dacfe7a18c229f3d48cbc16a976`.

### TASK-KMP-002 · Freeze rule-pack and OCR-text fixtures

- **Status:** DONE
- **Target:** COMMON / ANDROID
- **RF/CA:** RF-04, RF-07 · CA-04, CA-11
- **Objective:** Preserve current rule-pack validation/diff and OCR text-cleanup semantics as portable fixtures.
- **Scope:** Extract valid/invalid SemVer, missing source, duplicate rule ID, diff cases, redaction cases, checksum expectations where stable, and representative OCR cleanup inputs/outputs.
- **Dependencies:** None
- **Files/components expected:** `RulePackManagementTest`, `OcrLogExtractorTest`, sanitized fixture resources.
- **Validation method:** Baseline Android/JVM tests plus explicit expected-output fixture review.
- **Coverage expectation:** Critical rule-pack and redaction scenario inventory approaches 95–100% of currently recognized branches where practical.
- **Regression scope:** Rule-pack management, OCR cleanup/redaction.
- **Evidence required:** fixture catalog + passing baseline tests.
- **Actual result:** Rule-pack/redaction and OCR-text fixture catalogs frozen; `RulePackManagementTest` and `OcrLogExtractorTest` passed in the I0 workflow.
- **Evidence reference:** `I0_BASELINE_FREEZE.md`; workflow run `35385085058`; PR #2; merge `a6a00ac153177dacfe7a18c229f3d48cbc16a976`.

### TASK-KMP-003 · Establish migration guard inventory

- **Status:** DONE
- **Target:** MULTI
- **RF/CA:** RF-07, RF-10 · CA-11, CA-13
- **Objective:** Define executable architecture/static guards before shared code begins moving.
- **Scope:** Guard forbidden imports/dependencies from `shared/commonMain`: Android SDK, Room entities, Compose UI, Firebase/Google platform APIs, `org.json`, `java.security`, `java.util.regex.Pattern` and app-specific platform classes.
- **Dependencies:** TASK-KMP-001, TASK-KMP-002
- **Files/components expected:** scripts/tests/Gradle verification hooks introduced only when implementation is authorized.
- **Validation method:** positive and negative guard checks, including one controlled failing example during development or equivalent fixture-based verification.
- **Coverage expectation:** Not applicable; architecture rule must be fully enforced.
- **Regression scope:** Build configuration and source dependency boundaries.
- **Evidence required:** guard command/output and documented forbidden dependency matrix.
- **Actual result:** Architecture guard implemented, self-tested with allowed/forbidden fixtures and executed successfully in GitHub Actions.
- **Evidence reference:** `scripts/verify-kmp-common-boundary.sh`; `I0_BASELINE_FREEZE.md`; workflow run `35385085058`; PR #2.

# I1 — KMP scaffold without product cutover

### TASK-KMP-010 · Compatibility preflight for KMP toolchain

- **Status:** DONE
- **Target:** MULTI
- **RF/CA:** RF-02, RF-08, RF-09 · CA-06, CA-09, CA-14
- **Objective:** Select a PanicLab-compatible KMP/Kotlin/AGP/Kover/serialization stack without blindly copying laboratory versions.
- **Scope:** Evaluate current PanicLab Gradle/Kotlin/AGP versions, minimum required changes, plugin compatibility and iOS target support. Record exact chosen versions before scaffold changes.
- **Dependencies:** TASK-KMP-003
- **Files/components expected:** version catalog/build configuration documentation; no product cutover.
- **Validation method:** Gradle configuration/compilation spike on migration branch; Android baseline must remain buildable.
- **Coverage expectation:** Not applicable.
- **Regression scope:** Android project sync/build.
- **Evidence required:** compatibility matrix, selected versions, build command/output.
- **Actual result:** Compatibility preflight selected Kotlin 2.4.20, AGP 9.1.1, Gradle 9.3.1, KSP 2.3.12 and Kover 0.9.8; Android baseline remained green.
- **Evidence reference:** PR #3; I1 workflow run `35389381798`; Android regression run `35389381864`; merge `8d8852d0693a8c7e099cbc9a23ff48d47cba266d`.

### TASK-KMP-011 · Add isolated `shared` KMP scaffold

- **Status:** DONE
- **Target:** MULTI
- **RF/CA:** RF-02, RF-08, RF-10 · CA-06, CA-09, CA-13
- **Objective:** Add KMP structure without moving production behavior or altering the Android execution path.
- **Scope:** `shared/commonMain`, `commonTest`, Android target, iOS Arm64 + Simulator Arm64 framework target, minimal Gradle wiring; Android `app` remains current host.
- **Dependencies:** TASK-KMP-010
- **Files/components expected:** root settings/build files, `shared` module, minimal target source sets.
- **Validation method:** Android build, common/JVM test plumbing, Kotlin/Native compilation where available, architecture guard.
- **Coverage expectation:** No deterministic threshold until production logic enters shared.
- **Regression scope:** Existing Android build and tests.
- **Evidence required:** build/test logs per target actually executed.
- **Actual result:** Isolated `:shared` KMP scaffold added; JVM/common and Android-host tests passed; `Shared.framework` linked for `iosArm64` and `iosSimulatorArm64`; Android `app` did not depend on `shared` in I1.
- **Evidence reference:** PR #3; workflow run `35389381798`; framework artifact recorded in PR #3; merge `8d8852d0693a8c7e099cbc9a23ff48d47cba266d`.

### TASK-KMP-012 · Establish truthful common QA gates

- **Status:** DONE
- **Target:** COMMON
- **RF/CA:** RF-07, RF-09 · CA-11, CA-13, CA-14
- **Objective:** Add common deterministic test and coverage plumbing before meaningful extraction.
- **Scope:** `kotlin-test`, test fixtures, Kover JVM/Android-host reporting, architecture guard integration, threshold configuration ready for changed deterministic scope.
- **Dependencies:** TASK-KMP-011
- **Files/components expected:** shared test config, Kover/verification config, QA scripts/workflows as appropriate.
- **Validation method:** intentionally small sample test passes; coverage report generated; architecture guard executes.
- **Coverage expectation:** gate configured at >=90% line and >=85% branch for defined changed/new deterministic production scope once such code exists.
- **Regression scope:** Build/test pipeline only.
- **Evidence required:** coverage report path, gate output, architecture check output.
- **Actual result:** GitHub Actions executes architecture guard, JVM/common tests, Android-host tests, Kover reports/verification and separate macOS framework compilation; 90/85 thresholds are enforced without merging Native coverage into JVM coverage.
- **Evidence reference:** PR #3; workflow run `35389381798`; Android regression run `35389381864`.

# I2 — Portable primitives and core domain

### TASK-KMP-020 · Extract portable core domain models

- **Status:** DONE
- **Target:** COMMON
- **RF/CA:** RF-01, RF-10 · CA-01, CA-13
- **Objective:** Move only the deterministic domain model required by diagnosis/rule evaluation into `shared/commonMain`.
- **Scope:** Split diagnostic/rule/evidence/candidate/confidence/verification models from presentation-only and Android/network-specific state. Remove Room entity exposure from any newly shared contract.
- **Dependencies:** TASK-KMP-012
- **Files/components expected:** shared domain packages, Android mapping/adaptation where compilation requires it.
- **Validation method:** common compilation, fixture model construction, architecture guard, Android regression build.
- **Coverage expectation:** >=90% line / >=85% branch for changed deterministic behavior if behavior is introduced; pure data models judged mainly by scenario use rather than accessor coverage.
- **Regression scope:** Android domain consumers and tests.
- **Evidence required:** dependency graph/guard output + passing builds/tests.
- **Actual result:** Portable diagnostic/rule domain established in `shared/commonMain` and validated in parallel. Android product models intentionally remain in place until the later cutover increment, preserving strangler sequencing.
- **Evidence reference:** PR #4; shared workflow run `35393319172`; Android regression run `35393319211`; merge `9b5b3ad98b8ae77016bedfa521fe6ad71c74035b`.

### TASK-KMP-021 · Port deterministic primitives and nondeterminism seams

- **Status:** DONE
- **Target:** COMMON / MULTI
- **RF/CA:** RF-01, RF-10 · CA-01, CA-02, CA-11, CA-13
- **Objective:** Make `HexUtils`, `LogNormalizer`, clock/ID/hash boundaries portable and deterministic under test.
- **Scope:** Remove Java Locale/JVM-only helpers, global `System.currentTimeMillis`, direct UUID generation and direct `MessageDigest` use from shared behavior. Inject/test fixed clock and ID; preserve SHA-256 semantics through a boundary.
- **Dependencies:** TASK-KMP-020
- **Files/components expected:** shared utilities/contracts plus minimal Android/iOS target implementations only when necessary.
- **Validation method:** common fixtures/property cases, fixed-provider tests, hash vector tests, architecture guard.
- **Coverage expectation:** >=90% line, >=85% branch; 95–100% meaningful scenario coverage for numeric normalization and critical utility branches where practical.
- **Regression scope:** existing hex/log normalization behavior.
- **Evidence required:** common test report + Kover report + hash vectors.
- **Actual result:** Portable `HexUtils`, `LogNormalizer`, `Clock`, `IdGenerator` and `Sha256Hasher` contracts validated; Android/JVM target providers kept outside COMMON; Kover 90/85 gate passed and both iOS frameworks continued to link.
- **Evidence reference:** PR #4; shared workflow run `35393319172`; Android regression run `35393319211`; coverage artifact `10567326148`; iOS artifact `10566611956`.

# I3 — Deterministic parsing and engine

### TASK-KMP-030 · Port deterministic parsing pipeline

- **Status:** DONE
- **Target:** COMMON
- **RF/CA:** RF-01, RF-07, RF-10 · CA-01, CA-02, CA-03, CA-11, CA-13
- **Objective:** Move metadata extraction, panic classification, sensor extraction, static device resolution and evidence extraction to portable code without semantic drift.
- **Scope:** Replace Java `Pattern` and `org.json` dependencies with verified KMP-safe equivalents; inject IDs for evidence; keep persisted device lookup outside static resolver.
- **Dependencies:** TASK-KMP-021
- **Files/components expected:** `shared/commonMain` parser/resolution/evidence components; corresponding `commonTest` fixtures.
- **Validation method:** run frozen baseline fixtures; compare metadata/device/family/sensor/evidence outputs exactly by domain meaning.
- **Coverage expectation:** >=90% line, >=85% branch; critical parsing fallback scenarios targeted at 95–100% meaningful coverage.
- **Regression scope:** deterministic parsing fixtures and existing Android tests.
- **Evidence required:** equivalence matrix + common/Kover reports.
- **Actual result:** Metadata extraction, panic classification, sensor extraction, static device resolution and evidence extraction were ported to COMMON with Kotlin Regex / `kotlinx.serialization.json` and injected IDs. Frozen I0 representative parsing cases passed in shared JVM/Android-host tests while the Android baseline remained green.
- **Evidence reference:** `I3_EXECUTION.md`; PR #6; shared/macOS run `35404168248`; Android regression run `35404168170`; merge `623aa9da0d4b6f1005d2a00039bd9e71a9c2f6a9`.

### TASK-KMP-031 · Port diagnostic rules engine, ranking and report construction

- **Status:** DONE
- **Target:** COMMON
- **RF/CA:** RF-01, RF-07 · CA-01, CA-02, CA-03, CA-11
- **Objective:** Move the central deterministic diagnostic brain into shared code.
- **Scope:** rule evaluation, exact/bitmask precedence, rule scope, unknown fallback, candidate ranking, report assembly, controlled clock/ID.
- **Dependencies:** TASK-KMP-030
- **Files/components expected:** shared diagnostic engine/ranker/report builder and common tests.
- **Validation method:** frozen fixture suite plus baseline-vs-shared differential comparison while both implementations coexist.
- **Coverage expectation:** >=90% line, >=85% branch; 95–100% meaningful scenario coverage for exact/bitmask precedence, unknown fallback, scope/ranking where practical.
- **Regression scope:** all deterministic diagnostic scenarios.
- **Evidence required:** differential result matrix, test report, Kover report.
- **Actual result:** Rule evaluation, exact-before-bitmask behavior, scope matching, unknown-SMC safe fallback, candidate ranking and report construction with controlled clock/IDs were ported to COMMON and validated. Final I3 deterministic coverage reached 98.9011% lines and 88.7776% branches without lowering the 90/85 gates or excluding I3 production classes.
- **Evidence reference:** `I3_EXECUTION.md`; PR #6; shared/macOS run `35404168248`; coverage artifact `10571499130` SHA-256 `9d1de44ec130f33387d54790836c5b3dc7176d8051ba6f8fdaabe4cd47ebfa8f`; merge `623aa9da0d4b6f1005d2a00039bd9e71a9c2f6a9`.

### TASK-KMP-032 · Port OCR text cleanup only

- **Status:** DONE
- **Target:** COMMON
- **RF/CA:** RF-06, RF-07 · CA-10, CA-11, CA-13
- **Objective:** Share post-OCR text normalization while leaving image acquisition native.
- **Scope:** `OcrLogExtractor` deterministic cleanup/signature/code extraction. Do not move CameraX/ML Kit image analysis into common.
- **Dependencies:** TASK-KMP-030
- **Files/components expected:** shared OCR-text utility + common tests; Android image analyzer remains native.
- **Validation method:** frozen OCR fixtures and Android integration regression.
- **Coverage expectation:** >=90% line / >=85% branch for changed deterministic cleanup code.
- **Regression scope:** Android OCR-to-text-to-diagnosis path.
- **Evidence required:** common tests + Android regression result.
- **Actual result:** Deterministic OCR text cleanup/signature/code extraction was ported to COMMON and passed frozen/common tests. CameraX and ML Kit image acquisition remain Android-native, and the existing Android `OcrLogExtractorTest` remained green.
- **Evidence reference:** `I3_EXECUTION.md`; PR #6; shared/macOS run `35404168248`; Android regression run `35404168170`; merge `623aa9da0d4b6f1005d2a00039bd9e71a9c2f6a9`.

# I4 — Rule-pack core and boundary cleanup

### TASK-KMP-040 · Port rule-pack DTO parsing, validation and diff

- **Status:** DONE
- **Target:** COMMON
- **RF/CA:** RF-04, RF-07, RF-10 · CA-04, CA-11, CA-13
- **Objective:** Move rule-pack business semantics to shared code without carrying Room entities or `org.json` across the boundary.
- **Scope:** KMP-safe DTO parsing, domain mapping, validation, SemVer checks, duplicate detection, diff semantics, redaction-compatible fixtures.
- **Dependencies:** TASK-KMP-031
- **Files/components expected:** shared rule-pack DTO/domain/parser/validator/diff components; common tests.
- **Validation method:** frozen rule-pack fixtures and expected diff/validation results.
- **Coverage expectation:** >=90% line, >=85% branch; critical validator/diff scenarios target 95–100% meaningful coverage where practical.
- **Regression scope:** current rule-pack management tests.
- **Evidence required:** common/Kover reports + fixture mapping.
- **Actual result:** Rule-pack JSON-to-domain parsing, validation and diff semantics were ported to COMMON using `kotlinx.serialization.json`, Kotlin `Regex` and injected `Sha256Hasher`. Shared JVM/Android-host tests and the 90/85 Kover gate passed; final I4 deterministic scope reached 99.2513% lines (928/935) and 87.6582% branches (554/632).
- **Evidence reference:** `I4_EXECUTION.md`; PR #8; shared/macOS run `35409648294`; coverage artifact `10573670700` SHA-256 `164395f9e2257c4dae6a0b0bbd7f7c1b86846efb25333b21fdf786f829b34b59`; merge `b50e23cf879e191b531cf0c3ca7080b6300f5e41`.

### TASK-KMP-041 · Remove persistence entities from shared/domain contracts

- **Status:** DONE
- **Target:** COMMON / ANDROID
- **RF/CA:** RF-10, RF-05 · CA-13, CA-15
- **Objective:** Replace `RulePackEntity` leakage with domain-only contracts and Android mappings.
- **Scope:** knowledge-base repository contract cleanup, Room-to-domain mapping, static/persisted device lookup boundary, canonical rule-pack source definition.
- **Dependencies:** TASK-KMP-040
- **Files/components expected:** shared contracts and Android repository/mappers; no schema change.
- **Validation method:** architecture guard, Android repository tests, rule-pack install/read regression.
- **Coverage expectation:** shared changed deterministic mapping logic >=90% line / >=85% branch where applicable.
- **Regression scope:** Android rule-pack persistence/install/use behavior.
- **Evidence required:** guard output + repository regression tests.
- **Actual result:** Added domain-only `RulePackMetadata`, removed `RulePackEntity` from the knowledge-base repository contract, and introduced explicit Android Room-to-domain mapping. Room schema and DAOs remained unchanged. `RulePackBoundaryTest` plus the existing deterministic engine, rule-pack management and OCR regression suites passed on the final executable head.
- **Evidence reference:** `I4_EXECUTION.md`; PR #8; Android run `35409648288`; Android report artifact `10574175667` SHA-256 `c61efb03c6a12e038233ebe81f91501d98b3401025434ab72230968db9f75155`; merge `b50e23cf879e191b531cf0c3ca7080b6300f5e41`.

# I5 — Android shared-engine cutover

### TASK-KMP-050 · Wire Android diagnostic orchestration to shared engine

- **Status:** DONE
- **Target:** ANDROID / COMMON
- **RF/CA:** RF-01, RF-02, RF-06 · CA-01, CA-05, CA-06, CA-10
- **Objective:** Replace Android deterministic orchestration internals with the proven shared engine while preserving native UI/platform services.
- **Scope:** shared analyzer invocation, Android Room-backed lookup/persistence adapters, canonical rule-pack mapping; Compose, Room, DataStore, CameraX/ML Kit, Gemini, PDF/share and navigation remain native.
- **Dependencies:** TASK-KMP-041, TASK-KMP-032
- **Files/components expected:** Android data/repository composition and dependency wiring.
- **Validation method:** existing Android text diagnostic flow, baseline-vs-shared fixtures, repository tests, emulator/host smoke.
- **Coverage expectation:** common thresholds remain green; Android integration scenario evidence required.
- **Regression scope:** deterministic diagnosis, history write/read, rule-pack use, OCR text handoff, existing UI path.
- **Evidence required:** Android test matrix + equivalence evidence.
- **Actual result:** Android product now depends on `:shared` for deterministic parsing/domain/engine behavior through explicit Android adapters. Compose, Room, DataStore, CameraX/ML Kit, Gemini, PDF/share and navigation remained Android-native. Repository → shared → Room/history integration passed with the real bundled rule pack.
- **Evidence reference:** `I5_EXECUTION.md`; PR #10; TASK-051 first green run `35414261710`; final pre-merge I0/I5 run `35418051596`; merge `40cdc6006a0884465aa0cd92b89a3a1ae74638b2`.

### TASK-KMP-051 · Prove Android regression, lifecycle and resource behavior

- **Status:** DONE
- **Target:** ANDROID
- **RF/CA:** RF-02, RF-06, RF-07 · CA-05, CA-06, CA-10, CA-12
- **Objective:** Demonstrate that the cutover did not degrade the Android product outside the shared engine.
- **Scope:** text diagnosis, OCR handoff, loading/error/non-conclusive states, navigation/back/cancel where touched, history persistence, representative performance/memory comparison.
- **Dependencies:** TASK-KMP-050
- **Files/components expected:** Android tests/evidence only unless a regression fix is required.
- **Validation method:** Robolectric/host tests, emulator, and at least one physical Android smoke before final cutover acceptance.
- **Coverage expectation:** target-specific scenario evidence; not merged into common coverage.
- **Regression scope:** all Android surfaces touched by migration.
- **Evidence required:** test logs, physical smoke checklist, before/after representative latency/memory notes.
- **Actual result:** Automated Android regression and real repository → shared → Room/history coverage passed. A CI-built APK was installed on a physical Android device and the user reported `Smoke I5 PASS` after validating a real `iPhone12,8` / `Missing sensor(s): mic1` case, History/reopen, Evidence/Log navigation and lifecycle/background-resume behavior. No gross product regression was observed; dedicated latency/memory benchmarking was not added in I5.
- **Evidence reference:** `I5_EXECUTION.md`; Android run `35414571346`; packaging run `35416104717`; final pre-merge run `35418051596`; physical smoke PASS on 2026-09-19; raw APK SHA-256 `8ead753861eee499d27caa3bb1626a8073385f87555f4ff83da11da4e1473aaf`.

### TASK-KMP-052 · Remove superseded deterministic duplication

- **Status:** DONE
- **Target:** ANDROID / COMMON
- **RF/CA:** RF-01, RF-02, RF-10 · CA-05, CA-06, CA-13
- **Objective:** Delete old duplicated deterministic implementations only after shared equivalence and Android regression are proven.
- **Scope:** superseded parsers/engine/helpers only; retain Android-native adapters and capabilities.
- **Dependencies:** TASK-KMP-051
- **Files/components expected:** old Android deterministic implementation files plus import/wiring cleanup.
- **Validation method:** full common + Android regression and architecture guard after deletion.
- **Coverage expectation:** all existing gates remain green.
- **Regression scope:** complete I0–I5 suite.
- **Evidence required:** deletion diff review + full green gate matrix.
- **Actual result:** The 13 superseded Android deterministic source files were removed after TASK-KMP-051 became green, leaving `:shared` as the single implementation for migrated deterministic responsibilities. Full shared/Android regression and architecture gates remained green after deletion.
- **Evidence reference:** `I5_EXECUTION.md`; executable head `1c58931acf9051de93914758dacd29270df51562`; shared run `35414571367`; Android run `35414571346`; final pre-merge shared/iOS run `35418051651`; merge `40cdc6006a0884465aa0cd92b89a3a1ae74638b2`.

# I6 — Persistence safety checkpoint

### TASK-KMP-060 · Recover and verify Room schema lineage

- **Status:** DONE
- **Target:** ANDROID
- **RF/CA:** RF-05 · CA-07, CA-15
- **Objective:** Determine what historical installed schemas can actually be proven before any database migration decision.
- **Scope:** repository history, releases/tags, prior DB definitions, exported schemas/build artifacts/backups if available, current v3 tables/columns/indexes.
- **Dependencies:** TASK-KMP-052
- **Files/components expected:** schema evidence documentation and recovered schema fixtures where verifiable.
- **Validation method:** compare evidence sources and reconstruct only proven versions.
- **Coverage expectation:** Not applicable.
- **Regression scope:** none, read-only investigation.
- **Evidence required:** lineage table with VERIFIED/UNKNOWN states and source references.
- **Actual result:** Room v1, v2 and current v3 declarations were recovered and source-verified. Historical declarations used `exportSchema = false`; no registered `addMigrations(...)` path, historical DB artifact or GitHub Release carrying a recoverable database was found. Historical non-destructive v1→v2 and v2→v3 upgrade behavior therefore remains unsupported/not proven and was not fabricated.
- **Evidence reference:** `I6_SCHEMA_LINEAGE.md`; PR #13; merge `425c59017f0368833416f8b83925b4b099558073`.

### TASK-KMP-061 · Add non-destructive upgrade test harness

- **Status:** DONE
- **Target:** ANDROID
- **RF/CA:** RF-05, RF-07 · CA-07, CA-15
- **Objective:** Prove supported installed data survives any future schema change.
- **Scope:** only schema versions verified by TASK-KMP-060; representative sessions/evidence/candidates/notes rows; start old DB, upgrade, read/compare.
- **Dependencies:** TASK-KMP-060
- **Files/components expected:** Room migration/instrumentation or Robolectric migration fixtures and tests. No migration implementation unless separately required by a focused change.
- **Validation method:** before/after row and schema assertions.
- **Coverage expectation:** scenario completeness for every supported verified upgrade path.
- **Regression scope:** Room storage/history tests.
- **Evidence required:** upgrade test logs and retained-row assertions.
- **Actual result:** `RoomUpgradeSafetyTest` proves reconstructed source-derived v1/v2 fixtures fail closed under a strict current-v3 Room opener when no migration exists and that their sentinel rows plus `user_version` remain intact. A current v3 database preserves representative session, evidence, candidate, technician notes and customer name across close/reopen. This is preservation evidence, not proof of historical v1→v2 or v2→v3 migrations. Production `fallbackToDestructiveMigration()` remains unchanged.
- **Evidence reference:** `I6_UPGRADE_HARNESS.md`; PR #14; final run `35423527938` SUCCESS; unit-test artifact `10578726388`; merge `cd63f4f9a790ead3199bc1d1e063c5e905c5e82f`.

### TASK-KMP-062 · Decide Room KMP adoption separately

- **Status:** DONE
- **Target:** MULTI
- **RF/CA:** RF-05, RF-10 · CA-07, CA-13, CA-15
- **Objective:** Decide from evidence whether moving persistence to Room KMP adds enough value to justify migration risk.
- **Scope:** compare keeping Android Room + future iOS native persistence versus Room KMP; data migration risk, schema tooling, testability and maintenance.
- **Dependencies:** TASK-KMP-061
- **Files/components expected:** focused ADR/decision note; implementation requires its own approved scope if chosen.
- **Validation method:** evidence-based decision review.
- **Coverage expectation:** Not applicable.
- **Regression scope:** Not applicable.
- **Evidence required:** explicit KEEP / ADOPT LATER decision with rationale.
- **Actual result:** Decision is **ADOPT LATER**. Room KMP is technically viable and officially supported, but PanicLab keeps Android Room unchanged through I7. Room KMP may be reconsidered only as a separately approved I8 persistence/history capability after a real native iOS persistence need exists and Android schema/data-preservation prerequisites are strengthened.
- **Evidence reference:** `I6_ROOM_KMP_DECISION.md`; PR #16; workflow run `35427718202` SUCCESS; merge `a97e4afdf25f8f5feb9b1900b6d9116a7d391e9f`.

# I7 — Native iOS diagnostic slice

### TASK-KMP-070 · Resolve iOS deployment and USD0 CI preconditions

- **Status:** DONE
- **Target:** IOS / CI
- **RF/CA:** RF-08, RF-09 · CA-09, CA-14
- **Objective:** Establish an honest path to execute iOS builds/tests without silently consuming paid minutes.
- **Scope:** select minimum iOS deployment target after compatibility preflight; audit publication readiness if public visibility is considered; validate actual runner/repository visibility assumptions.
- **Dependencies:** TASK-KMP-052
- **Files/components expected:** CI/compatibility documentation and, only after separate authorization where required, workflow configuration.
- **Validation method:** actual repository visibility + runner evidence. Public-lab evidence alone is insufficient for private PanicLab.
- **Coverage expectation:** Not applicable.
- **Regression scope:** none.
- **Evidence required:** selected deployment target and validated USD0 iOS execution path or explicit BLOCKED state.
- **Actual result:** PanicLab visibility was verified as public and the I7 preflight established iOS 15.0 as the deployment baseline. Standard GitHub-hosted `macos-latest` linked the static `Shared.framework` for `iosArm64` and `iosSimulatorArm64`; each archive exposed 286 `LC_BUILD_VERSION` entries and every observed `minos` value was `15.0`. Final executable evidence used macOS 26.6.2, ARM64 runner, Xcode 26.6 and iOS/iOS Simulator SDK 26.5. No Apple signing identity, provisioning profile, certificate, Team ID or App Store credential was required for this preflight. The Android/I0-I6 baseline remained green on the exact final PR head.
- **Evidence reference:** `I7_IOS_PREFLIGHT.md`; `.github/workflows/kmp-i7-ios-preflight.yml`; PR #18; I7 preflight run `35443960120` SUCCESS; baseline run `35443960074` SUCCESS; merge `d64e106509f282b66e2cce0241d6c241ff9aaf14`.

### TASK-KMP-071 · Add native SwiftUI text/paste diagnostic slice

- **Status:** DONE
- **Target:** IOS
- **RF/CA:** RF-03, RF-06, RF-08 · CA-08, CA-09, CA-12
- **Objective:** Create the first native iOS product surface that executes the shared deterministic diagnostic workflow.
- **Scope:** `iosApp`, SwiftUI text/paste input, progress/error/non-conclusive/result states, Shared framework integration, canonical bundled rule-pack adapter. No camera/AI/PDF parity in this task.
- **Dependencies:** TASK-KMP-070, TASK-KMP-052
- **Files/components expected:** native Xcode/SwiftUI project and iOS composition adapters.
- **Validation method:** build + executable fixture flow; native UI semantics/accessibility smoke.
- **Coverage expectation:** target-specific XCTest/integration evidence; no combined global percentage.
- **Regression scope:** shared fixture semantics on iOS.
- **Evidence required:** Xcode build/test logs and rendered domain-equivalent fixture result.
- **Actual result:** Native SwiftUI text/paste slice is integrated on iOS 15.0 and executes the shared deterministic engine through `NativeDiagnosticFacade` using the canonical bundled Rule Pack and native CryptoKit checksum boundary. The final exact PR head linked both Shared frameworks, built simulator and generic device targets without signing, booted an iPhone simulator, passed the known canonical product case, unknown-code non-conclusive XCTest path and accessibility-focused XCUITest smoke. The final QA correction changed only native tests and did not alter engine, Rule Pack, Android, Room/schema or SwiftUI product behavior.
- **Evidence reference:** `I7_NATIVE_SLICE.md`; PR #20; final head `9bede0c0f0cbb51f43292547a1ac29e5d5af5a00`; I0 run `35453724371` SUCCESS; I1 run `35453724330` SUCCESS; I7 preflight run `35453724342` SUCCESS; native run `35453724329`, job `105925223001` SUCCESS; `.xcresult` artifact `10587497480`, digest `sha256:dc665f944e1d685f95dc1ccc23be7f763f714f66cd6015b56ae9f283b4a54c52`; merge `cb580ba0e5032fd96069b76e1706d979ed217335`.

### TASK-KMP-072 · Prove Kotlin/Native and iOS equivalence

- **Status:** DONE
- **Target:** IOS / COMMON
- **RF/CA:** RF-01, RF-08 · CA-01, CA-08, CA-09, CA-12
- **Objective:** Prove the shared deterministic engine behaves equivalently when executed through the iOS/Kotlin-Native path.
- **Scope:** approved representative fixture subset plus unknown fallback and rule-pack fixture execution on real macOS/Xcode environment.
- **Dependencies:** TASK-KMP-071
- **Files/components expected:** Kotlin/Native tests and/or XCTest bridge fixtures.
- **Validation method:** compare domain output against approved common baseline; real macOS/Xcode execution mandatory.
- **Coverage expectation:** scenario evidence for required iOS slice; native coverage reported separately if tooling provides it.
- **Regression scope:** shared deterministic behavior + iOS host integration.
- **Evidence required:** per-fixture equivalence matrix + Xcode/Kotlin-Native test logs.
- **Actual result:** The approved COMMON deterministic scenarios execute successfully on both JVM and `iosSimulatorArm64` Kotlin/Native, with an automatically generated eight-scenario equivalence matrix reporting PASS/PASS and `equivalent: true` for every required fixture. The Swift/Xcode host separately executed the canonical bundled Rule Pack through `NativeDiagnosticFacade`; three XCTest cases passed for iPhone14 `0x500000` battery, decimal `4194304` wireless charging coil, and safe unknown-SMC non-conclusive fallback. No engine semantics, Rule Pack contents, Android behavior, Room/schema, persistence/history, camera/OCR, AI, PDF/export or SwiftUI product behavior changed in this task.
- **Evidence reference:** `I7_NATIVE_EQUIVALENCE.md`; PR #22; final head `ae67060540b7ff5032752cc6ade244fdbb22021c`; exact-head I0 run `35482186108` SUCCESS; native equivalence run `35482186113` SUCCESS; native iOS slice run `35482186094` SUCCESS; equivalence artifact from run `35481696591`, artifact `10595449813`, digest `sha256:21970459db3b0b233143426a04c5594870c5442f62d5d34a9f348e5a144747ee`; merge `4b14b534364fe3b5703989eb5289a4dbf1653e19`.

# I8 — Platform capability increments

### TASK-KMP-080 · Create capability parity backlog with separate approval gates

- **Status:** DONE
- **Target:** MULTI
- **RF/CA:** RF-06, RF-07 · CA-10, CA-12
- **Objective:** Split remaining iOS parity work into independently scoped, evidence-driven features rather than one giant parity PR.
- **Scope:** camera/OCR acquisition, persistence/history, file import/share, PDF/export, settings, knowledge base/rule-pack management, AI repair guidance, trends and other native platform capability gaps discovered during migration.
- **Dependencies:** TASK-KMP-072
- **Files/components expected:** separate feature scopes/SPECs or focused task groups as required by SDD.
- **Validation method:** every capability declares native target behavior, permissions/lifecycle/error cases and executable evidence before implementation.
- **Coverage expectation:** target-specific per capability.
- **Regression scope:** Android capability must not regress while iOS counterpart is introduced.
- **Evidence required:** approved capability roadmap with explicit supported/not-yet-supported states.
- **Actual result:** I8 capability roadmap created with TASK-KMP-081 through TASK-KMP-090 as independent increments, explicit authorization boundaries, Room KMP `ADOPT LATER` re-entry gating and Blueprint exclusion. The roadmap merge does not authorize later capabilities automatically.
- **Evidence reference:** `I8_CAPABILITY_ROADMAP.md`; PR #24; roadmap head `4d2f25554e0dd217664f075cf5a58ec027a22ded`; baseline workflow run `35486995683` SUCCESS; merge `bd98d7c6bf727bc597d66972970b36ffbb773c69`.

### TASK-KMP-081 · Add native iOS file import / external document intake

- **Status:** DONE
- **Target:** IOS
- **RF/CA:** RF-06, RF-07, RF-08 · CA-12
- **Objective:** Add safe native file intake to iOS while feeding imported text through the exact existing shared deterministic diagnostic pipeline.
- **Scope:** SwiftUI `fileImporter`; `.ips`, `.txt`, `.log`, `.json`; 5 MiB maximum; UTF-8 plus UTF-16 LE/BE only with explicit BOM; security-scoped access; cancellation/error/empty/oversize/unsupported/undecodable handling; imported text enters the same `DiagnosticViewModel -> NativeDiagnosticFacade` path as typed/pasted input. No persistence, camera/OCR acquisition, PDF/export, rule-pack import or deterministic semantic change.
- **Dependencies:** TASK-KMP-080, TASK-KMP-072
- **Files/components expected:** iOS SwiftUI/view-model surfaces, native tests and capability-specific evidence note only.
- **Validation method:** exact-head Android baseline + JVM/Kotlin-Native equivalence + real simulator/device build + XCTest/XCUITest; imported fixture must produce the same deterministic diagnosis as the paste path.
- **Coverage expectation:** target-specific XCTest/XCUITest scenario evidence; no fabricated cross-platform percentage.
- **Regression scope:** Android baseline, shared deterministic equivalence and existing native iOS diagnostic slice.
- **Evidence required:** import/paste equivalence; cancellation; unsupported, empty, oversized, unreadable and unsafe encoding paths; positive BOM-marked UTF-16 case; real Xcode evidence.
- **Actual result:** Native file import is integrated on iOS. The implementation accepts the four approved text-like formats, fails closed for arbitrary non-UTF-8 bytes without a valid UTF-16 BOM, preserves input on cancellation, does not auto-persist or auto-analyze, and reuses the existing deterministic diagnostic path. Simulator and generic-device builds passed; native XCTest/XCUITest and JVM/Kotlin-Native equivalence passed on the exact final implementation head.
- **Evidence reference:** `I8_081_FILE_IMPORT.md`; PR #25; final head `dcae6855a9f290abc264a2978c9ff3bb3b7028ce`; I0 run `35489568505` SUCCESS; Native Equivalence run `35489568540` SUCCESS; Native iOS Slice run `35489568509` SUCCESS; `.xcresult` artifact `10599226050`, digest `sha256:17653c50580ee0855638b1711298f6ac7b692cac8e663b5672fa22d17e376f76`; equivalence artifact `10598792265`, digest `sha256:37a159af8dcc938def6c4581f08d2c8ee7116c5614102e00376cde436960801f`; merge `1fa8d73a7e4fdfe9c56ba8cc067ce8d596be801e`.

### TASK-KMP-082 · Add native iOS camera + OCR acquisition

- **Status:** TODO / NOT AUTHORIZED
- **Target:** IOS / COMMON
- **RF/CA:** RF-06, RF-07 · CA-10, CA-12
- **Objective:** Add native image-to-text acquisition while preserving COMMON post-OCR cleanup and deterministic diagnosis semantics.
- **Scope:** native camera permission lifecycle, image capture/scanning UX, Apple-native OCR compatible with iOS 15.0, OCR output handed to existing shared cleanup, cancel/error/no-text states, no persistent photo storage by default. Android CameraX/ML Kit remains unchanged.
- **Dependencies:** TASK-KMP-081 optional; TASK-KMP-032 shared OCR cleanup already available.
- **Validation method:** requires separate explicit authorization before implementation.
- **Evidence required:** permission granted/denied, cancellation/lifecycle, representative OCR fixture through shared cleanup, no-text safe behavior, target-relevant Xcode evidence and Android scanner regression.

### TASK-KMP-083 through TASK-KMP-090

- **Status:** TODO / NOT AUTHORIZED
- **Source of scope:** `I8_CAPABILITY_ROADMAP.md`
- **Authorization:** Each remaining persistence decision/history, settings, PDF/share, knowledge-base, rule-pack management, AI guidance and trends capability requires a separate explicit implementation authorization and its own QA gate.

## QA ledger

| QA dimension | Target | Planned threshold / scenarios | Actual result | Status |
| --- | --- | --- | --- | --- |
| Requirements / CA traceability | MULTI | 100% of CA-01..CA-15 mapped | I0-I7 evidence is mapped through TASK-KMP-072; I8 roadmap and TASK-KMP-081 are now recorded, while TASK-KMP-082..090 remain pending separate authorization | IN PROGRESS |
| Changed deterministic line coverage | COMMON/JVM-host | >= 90% | I5 deterministic scope remains 99.2513% (928/935); TASK-KMP-081 did not change deterministic production code and exact-head regressions passed | PASS THROUGH 081 |
| Changed deterministic branch coverage | COMMON/JVM-host | >= 85% | I5 deterministic scope remains 87.6582% (554/632); TASK-KMP-081 did not lower or redefine the threshold | PASS THROUGH 081 |
| Critical deterministic scenario coverage | COMMON | 95–100% meaningful scenarios where practical for normalization, precedence, fallback, rule scope/ranking, validation/redaction | I0-I5 protect normalization, exact/bitmask precedence, unknown fallback, scope/ranking, rule-pack validation/diff and Android repository→shared→Room scenarios; TASK-KMP-072 and TASK-KMP-081 exact-head equivalence runs preserve the approved representative JVM/Native semantics | PASS THROUGH 081 |
| Deterministic equivalence | COMMON/ANDROID/IOS | Approved fixture semantics match baseline | TASK-KMP-081 Native Equivalence run `35489568540` passed on the exact final head, including JVM common tests, Kotlin/Native simulator common tests, equivalence matrix and Xcode canonical Rule Pack bridge tests | PASS THROUGH 081 |
| Architecture/static | COMMON | No forbidden Android/JVM/platform leakage | COMMON guard and repository scan remain green; TASK-KMP-081 changed only native iOS/documentation/tests and introduced no COMMON platform leakage | PASS THROUGH 081 |
| Android regression | ANDROID | Existing diagnostic, rule-pack, OCR handoff, Room/history and touched UI scenarios pass | TASK-KMP-081 exact-head I0 run `35489568505` passed, including deterministic I0/I5 cutover and I6 Room safety tests plus APK assembly | PASS THROUGH 081 |
| Android physical smoke | ANDROID | At least one physical-device smoke before final core cutover | Real-device CI APK smoke reported `Smoke I5 PASS` on 2026-09-19, including diagnosis, History/reopen, Evidence/Log and lifecycle checks | PASS |
| Persistence upgrade safety | ANDROID | 100% of verified supported schema paths preserve representative history | Source lineage v1/v2/v3 recovered; reconstructed v1/v2 fixtures fail closed without mutation when migrations are absent; current v3 preserves representative history across strict reopen. Historical v1→v2 and v2→v3 migrations remain unsupported/not proven. Room KMP remains ADOPT LATER and TASK-KMP-081 made no persistence change | PASS THROUGH I6 / UNCHANGED IN 081 |
| Kotlin/Native execution | IOS/COMMON | Required common fixtures execute on Native path | TASK-KMP-081 exact-head Native Equivalence run `35489568540` passed JVM and real `iosSimulatorArm64Test` execution before bridge tests | PASS THROUGH 081 |
| Native iOS build/test | IOS | Real Xcode build + target-relevant tests pass | TASK-KMP-081 exact-head Native iOS Slice run `35489568509` passed Shared framework link, simulator build, generic-device build without signing, XCTest/XCUITest and `.xcresult` archive/upload | PASS THROUGH 081 |
| iOS native UX/accessibility | IOS | approved native capabilities expose accessible controls and safe states | Text/paste slice remains green; TASK-KMP-081 added accessible Import control and exercised native unit/accessibility UI smoke with safe cancellation/error behavior | PASS THROUGH 081 |
| CI truthfulness | MULTI | runner/visibility claims match actual evidence; USD0 private iOS not claimed without proof | Repository remains public; TASK-KMP-081 ran exact-head Linux/Android and standard GitHub-hosted macOS/Xcode evidence without signing credentials | PASS THROUGH 081 |
| Security/privacy | MULTI | no secrets/raw customer logs in fixtures/artifacts; redaction scenarios pass | TASK-KMP-081 uses controlled fixtures, security-scoped file access, bounded input size and fail-closed encoding rules; no signing secrets or customer/workshop logs were added | PASS THROUGH 081 |
| Performance/resource regression | ANDROID | no gross representative analysis latency/memory regression at cutover | Physical smoke showed no gross lifecycle/product regression, but no dedicated latency/memory benchmark was captured | IN PROGRESS |

## Acceptance evidence ledger

| CA | Target | Task(s) | Status | Executed evidence | Actual result |
| --- | --- | --- | --- | --- | --- |
| CA-01 | COMMON | 001, 020, 021, 030, 031, 050, 072 | DONE | I0 fixture baseline + I2 portable models/utilities + I3 shared parser/engine + I5 Android shared-engine cutover + TASK-KMP-072 JVM/Kotlin-Native matrix | Required COMMON semantics pass across the approved JVM/Native representative set |
| CA-02 | COMMON | 001, 021, 030, 031 | DONE | I0 decimal/hex fixtures + I2 HexUtils + I3 parser/engine exact/decimal cases | Required COMMON decimal/hex deterministic semantics pass through I3 |
| CA-03 | COMMON | 001, 030, 031, 072 | DONE | I0 deterministic fixtures + I3 shared parsing/engine scenarios + TASK-KMP-072 Native equivalence matrix | Shared JVM/Android-host semantics and the approved Native/iOS representative equivalence scenarios pass |
| CA-04 | COMMON | 002, 040 | DONE | I0 rule-pack fixtures + I4 shared parser/validator/diff tests and Kover evidence | Required COMMON rule-pack parse/validate/diff semantics pass through I4 |
| CA-05 | ANDROID | 050, 051, 052 | DONE | I5 repository→shared→Room integration, duplicate removal, final Android regression and physical smoke | Android cutover completed with single shared deterministic implementation and physical acceptance PASS |
| CA-06 | ANDROID | 010, 011, 050, 051, 052 | DONE | I1 scaffold + I5 Android cutover/regression/duplicate removal | Android remains functional after shared-engine cutover; I5 physical smoke PASS |
| CA-07 | ANDROID | 060, 061, 062 | DONE | I6 schema lineage (`I6_SCHEMA_LINEAGE.md`) + strict upgrade safety harness (`I6_UPGRADE_HARNESS.md`, run `35423527938`) + Room KMP decision (`I6_ROOM_KMP_DECISION.md`, run `35427718202`) | Source lineage and current-v3/fail-closed preservation are evidenced; historical migration support is not invented; persistence strategy is ADOPT LATER |
| CA-08 | IOS | 071, 072 | DONE | TASK-KMP-071 native SwiftUI/XCTest/XCUITest + TASK-KMP-072 exact-head native equivalence/iOS runs `35482186113` and `35482186094` | Native product slice, canonical host semantics and approved representative Native equivalence pass |
| CA-09 | IOS | 010, 011, 070, 071, 072 | DONE | I1-I5 framework links + TASK-KMP-070 preflight + TASK-KMP-071 simulator/device builds + TASK-KMP-072 Kotlin/Native/Xcode equivalence | iOS deployment, native build/test and required equivalence closure pass through I7 |
| CA-10 | ANDROID/IOS | 032, 050, 051, 080, 082 | IN PROGRESS | I3 COMMON OCR cleanup + I5 Android shared-engine handoff/regression + I8 capability roadmap | Android OCR portion passes; iOS camera/OCR acquisition remains TASK-KMP-082 and is not authorized |
| CA-11 | COMMON | 001, 002, 003, 012, 021, 030, 031, 032, 040 | DONE | I0 fixtures/guard + I1 QA gates + I2 portable core + I3 parser/engine/OCR + I4 rule-pack Kover 99.2513/87.6582 | Required COMMON changed deterministic coverage gates remain green through I5 and were not altered by TASK-KMP-081 |
| CA-12 | ANDROID/IOS | 051, 071, 072, 080, 081 | IN PROGRESS | I5 Android physical lifecycle/navigation smoke + TASK-KMP-071 native SwiftUI accessibility/error-state smoke + TASK-KMP-072 equivalence closure + TASK-KMP-081 native file import/XCTest/XCUITest | Android physical behavior, native iOS diagnosis, equivalence and file intake pass; later I8 platform capabilities remain pending |
| CA-13 | COMMON | 003, 011, 012, 020, 021, 030, 040, 041, 052, 062 | DONE | Architecture guard + isolated shared + I2/I3/I4 boundary work + I5 duplicate removal + I6 Room KMP ADR | Single shared deterministic implementation is proven; persistence remains outside COMMON by explicit ADOPT LATER decision |
| CA-14 | CI | 010, 012, 070 | DONE | I1 truthful common QA + TASK-KMP-070 public-repo standard macOS runner, real Xcode/SDK capture, dual-framework link and minos 15.0 verification | Required CI truthfulness/deployment precondition is executable and green; later I8 exact-head macOS runs preserve that evidence model |
| CA-15 | ANDROID | 041, 060, 061, 062 | DONE | I4 boundary cleanup + I6 source lineage + strict fail-closed/current-v3 preservation harness + Room KMP ADR | Schema evidence and safety harness are complete; historical migration support is not claimed; persistence migration is deferred by explicit ADOPT LATER decision |

## Outstanding checks before next implementation authorization

- I0, I1, I2, I3, I4, I5, I6 and I7 are merged with their evidence recorded above.
- I7 is DONE/CLOSED after TASK-KMP-070, TASK-KMP-071 and TASK-KMP-072 merged successfully.
- TASK-KMP-080 is DONE via `I8_CAPABILITY_ROADMAP.md`, PR #24 and merge `bd98d7c6bf727bc597d66972970b36ffbb773c69`.
- TASK-KMP-081 was separately authorized, is DONE via `I8_081_FILE_IMPORT.md`, exact head `dcae6855a9f290abc264a2978c9ff3bb3b7028ce`, exact-head runs `35489568505`, `35489568540` and `35489568509`, PR #25 and merge `1fa8d73a7e4fdfe9c56ba8cc067ce8d596be801e`.
- I8 remains capability-gated. TASK-KMP-082 through TASK-KMP-090 are **not authorized** by completion of TASK-KMP-081 or by this documentation reconciliation.
- TASK-KMP-082 is the next proposed capability, but no camera/OCR implementation may start without separate explicit authorization.
- Room KMP implementation remains **ADOPT LATER**. TASK-KMP-083 is a decision-only re-entry checkpoint and TASK-KMP-084 persistence/history cannot begin without its required prior decision and explicit authorization.
- No historical `Migration(1,2)` or `Migration(2,3)` has been invented; historical non-destructive upgrade paths remain unsupported/not proven.
- Production `fallbackToDestructiveMigration()` remains unchanged; no Room version/entity/DAO/schema change was made in I6, I7 or TASK-KMP-081.
- PanicLab remains intentionally public; TASK-KMP-070 verified that fact for the USD0 standard-runner path, but no future visibility change is implied.
- Do not modify `SoftwareDevelopmentBlueprint` during PanicLab validation.
- Preserve `main@c165ae4283a3b592eddb2b70a1c13a9ffeb51f01` as the verified behavioral reference for migration fixtures.
- Preserve the approved strangler sequence and Android-native Compose/Room/DataStore/CameraX/ML Kit/Gemini/PDF/share/navigation boundaries unless separately approved.
- Track the real-smoke hardening findings separately: character-spaced log ingestion, broad secondary panic-family false positives, fallback SMC-code false positives and cosmetic OS-version duplication.
- Issue #11 tracks the separate future `Panic Full Findings Inbox`; it does not alter completed I0-I7/TASK-KMP-081 evidence or authorize implementation.

## Completion condition

PanicLab KMP migration is not complete until every required CA has executed evidence for its target, Android data-preservation obligations are satisfied, every claimed iOS capability has real target-relevant evidence, documentation matches implemented reality, and no unresolved release-blocking regression remains.
