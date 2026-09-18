# TASKS: PanicLab KMP Migration

**Reference PLAN:** `docs/features/paniclab-kmp-migration/PLAN.md`
**PLAN version:** `4d5c70f12c68c480a930be7ede1b5451b41d1415` / approved 2026-09-18
**Implementation authorization:** I0, I1, I2 and I3 explicitly authorized, completed and merged; I4 is not authorized.

> These tasks translate the approved PLAN into reviewable execution increments. No task may move to `IN PROGRESS` until Luis explicitly authorizes that implementation increment. Approval of this task list, merge of documentation PRs, repository visibility changes and application implementation remain separate actions.

## Authorization and checkpoint ledger

| Increment | Authorization | Execution status | Merge evidence |
| --- | --- | --- | --- |
| I0 | Explicitly authorized by Luis on 2026-09-18 | DONE | PR #2 merged as `a6a00ac153177dacfe7a18c229f3d48cbc16a976` |
| I1 | Explicitly authorized by Luis on 2026-09-18 | DONE | PR #3 merged as `8d8852d0693a8c7e099cbc9a23ff48d47cba266d` |
| I2 | Explicitly authorized by Luis on 2026-09-18 | DONE | PR #4 merged as `9b5b3ad98b8ae77016bedfa521fe6ad71c74035b` |
| I3 | Explicitly authorized by Luis on 2026-09-18 | DONE | PR #6 merged as `623aa9da0d4b6f1005d2a00039bd9e71a9c2f6a9` |
| I4 | Not authorized | NOT STARTED | PENDING explicit authorization |

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
- **Files/components expected:** `shared` test config, Kover/verification config, QA scripts/workflows as appropriate.
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

- **Status:** TODO
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
- **Actual result:** Not run
- **Evidence reference:** PENDING

### TASK-KMP-041 · Remove persistence entities from shared/domain contracts

- **Status:** TODO
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
- **Actual result:** Not run
- **Evidence reference:** PENDING

# I5 — Android shared-engine cutover

### TASK-KMP-050 · Wire Android diagnostic orchestration to shared engine

- **Status:** TODO
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
- **Actual result:** Not run
- **Evidence reference:** PENDING

### TASK-KMP-051 · Prove Android regression, lifecycle and resource behavior

- **Status:** TODO
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
- **Actual result:** Not run
- **Evidence reference:** PENDING

### TASK-KMP-052 · Remove superseded deterministic duplication

- **Status:** TODO
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
- **Actual result:** Not run
- **Evidence reference:** PENDING

# I6 — Persistence safety checkpoint

### TASK-KMP-060 · Recover and verify Room schema lineage

- **Status:** TODO
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
- **Actual result:** Not run
- **Evidence reference:** PENDING

### TASK-KMP-061 · Add non-destructive upgrade test harness

- **Status:** TODO
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
- **Actual result:** Not run
- **Evidence reference:** PENDING

### TASK-KMP-062 · Decide Room KMP adoption separately

- **Status:** TODO
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
- **Actual result:** Not run
- **Evidence reference:** PENDING

# I7 — Native iOS diagnostic slice

### TASK-KMP-070 · Resolve iOS deployment and USD0 CI preconditions

- **Status:** TODO
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
- **Actual result:** Not run
- **Evidence reference:** PENDING

### TASK-KMP-071 · Add native SwiftUI text/paste diagnostic slice

- **Status:** TODO
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
- **Actual result:** Not run
- **Evidence reference:** PENDING

### TASK-KMP-072 · Prove Kotlin/Native and iOS equivalence

- **Status:** TODO
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
- **Actual result:** Not run
- **Evidence reference:** PENDING

# I8 — Platform capability increments

### TASK-KMP-080 · Create capability parity backlog with separate approval gates

- **Status:** TODO
- **Target:** MULTI
- **RF/CA:** RF-06, RF-07 · CA-10, CA-12
- **Objective:** Split remaining iOS parity work into independently scoped, evidence-driven features rather than one giant parity PR.
- **Scope:** camera/OCR acquisition, persistence/history, file import/share, PDF/export, settings, AI repair guidance and any other native platform capability discovered during migration.
- **Dependencies:** TASK-KMP-072
- **Files/components expected:** separate feature scopes/SPECs or focused task groups as required by SDD.
- **Validation method:** every capability declares native target behavior, permissions/lifecycle/error cases and executable evidence before implementation.
- **Coverage expectation:** target-specific per capability.
- **Regression scope:** Android capability must not regress while iOS counterpart is introduced.
- **Evidence required:** approved capability roadmap with explicit supported/not-yet-supported states.
- **Actual result:** Not run
- **Evidence reference:** PENDING

## QA ledger

| QA dimension | Target | Planned threshold / scenarios | Actual result | Status |
| --- | --- | --- | --- | --- |
| Requirements / CA traceability | MULTI | 100% of CA-01..CA-15 mapped | I0-I3 evidence mapped; remaining increments pending | IN PROGRESS |
| Changed deterministic line coverage | COMMON/JVM-host | >= 90% | I3 deterministic scope reached 98.9011% in run `35404168248` | PASS THROUGH I3 |
| Changed deterministic branch coverage | COMMON/JVM-host | >= 85% | I3 deterministic scope reached 88.7776% in run `35404168248`; initial 73.5471% failure was corrected by adding branch tests without lowering the threshold | PASS THROUGH I3 |
| Critical deterministic scenario coverage | COMMON | 95–100% meaningful scenarios where practical for normalization, precedence, fallback, rule scope/ranking, validation/redaction | I3 covers exact/bitmask precedence, unknown fallback, scope/ranking, representative parsing and OCR branches; rule-pack validation/redaction shared implementation remains I4 | IN PROGRESS |
| Deterministic equivalence | COMMON/ANDROID/IOS | Approved fixture semantics match baseline | Shared I3 fixture scenarios passed while the original Android baseline suites also passed; Kotlin/Native/iOS execution equivalence remains pending | IN PROGRESS |
| Architecture/static | COMMON | No forbidden Android/JVM/platform leakage | COMMON guard and repository scan pass through I3 | PASS THROUGH I3 |
| Android regression | ANDROID | Existing diagnostic, rule-pack, OCR handoff, Room/history and touched UI scenarios pass | I0 deterministic baseline (`DeterministicDiagnosticEngineTest`, `RulePackManagementTest`, `OcrLogExtractorTest`) passed on I3 head in run `35404168170` | PASS THROUGH I3 |
| Android physical smoke | ANDROID | At least one physical-device smoke before final core cutover | PENDING | NOT RUN |
| Persistence upgrade safety | ANDROID | 100% of verified supported schema paths preserve representative history | PENDING | NOT RUN |
| Kotlin/Native execution | IOS/COMMON | Required common fixtures execute on Native path | Framework compilation with I3 engine code is proven; fixture execution on Native remains pending | IN PROGRESS |
| Native iOS build/test | IOS | Real Xcode build + target-relevant tests pass | `iosArm64` and `iosSimulatorArm64` frameworks link on macOS through I3; native app/XCTest slice remains pending | IN PROGRESS |
| iOS native UX/accessibility | IOS | text/paste diagnostic slice has native states and basic accessibility semantics | PENDING | NOT RUN |
| CI truthfulness | MULTI | runner/visibility claims match actual evidence; USD0 private iOS not claimed without proof | PanicLab is public after publication-readiness audit; standard GitHub-hosted Ubuntu/macOS execution validated through I3 | PASS THROUGH I3 |
| Security/privacy | MULTI | no secrets/raw customer logs in fixtures/artifacts; redaction scenarios pass | Publication audit and sanitized fixtures remain valid; I3 added no secrets or raw customer logs | PASS THROUGH I3 |
| Performance/resource regression | ANDROID | no gross representative analysis latency/memory regression at cutover | PENDING | NOT RUN |

## Acceptance evidence ledger

| CA | Target | Task(s) | Status | Executed evidence | Actual result |
| --- | --- | --- | --- | --- | --- |
| CA-01 | COMMON | 001, 020, 021, 030, 031, 050, 072 | IN PROGRESS | I0 fixture baseline + I2 portable models/utilities + I3 shared parser/engine fixtures | I0-I3 portions pass; Android cutover and iOS execution equivalence remain pending |
| CA-02 | COMMON | 001, 021, 030, 031 | DONE | I0 decimal/hex fixtures + I2 HexUtils + I3 parser/engine exact/decimal cases | Required COMMON decimal/hex deterministic semantics pass through I3 |
| CA-03 | COMMON | 001, 030, 031, 072 | IN PROGRESS | I0 deterministic fixtures + I3 shared parsing/engine scenarios | Shared JVM/Android-host semantics pass; Native/iOS equivalence remains pending |
| CA-04 | COMMON | 002, 040 | IN PROGRESS | I0 rule-pack fixtures | Shared rule-pack implementation pending I4 |
| CA-05 | ANDROID | 050, 051, 052 | NOT RUN | PENDING | PENDING |
| CA-06 | ANDROID | 010, 011, 050, 051, 052 | IN PROGRESS | I1 scaffold + repeated Android baseline regression through I3 | Cutover/regression/removal pending I5 |
| CA-07 | ANDROID | 060, 061, 062 | NOT RUN | PENDING | PENDING |
| CA-08 | IOS | 071, 072 | NOT RUN | PENDING | PENDING |
| CA-09 | IOS | 010, 011, 070, 071, 072 | IN PROGRESS | I1-I3 macOS framework links | Native iOS slice/equivalence pending |
| CA-10 | ANDROID/IOS | 032, 050, 051, 080 | IN PROGRESS | I3 COMMON OCR text cleanup + Android OCR baseline regression | Android shared-engine handoff and iOS capability backlog remain pending |
| CA-11 | COMMON | 001, 002, 003, 012, 021, 030, 031, 032, 040 | IN PROGRESS | I0 fixtures/guard + I1 QA gates + I2 portable core + I3 Kover 98.9011/88.7776 | I3 scope passes; I4 rule-pack deterministic scope remains pending |
| CA-12 | ANDROID/IOS | 051, 071, 072, 080 | NOT RUN | PENDING | PENDING |
| CA-13 | COMMON | 003, 011, 012, 020, 021, 030, 040, 041, 052, 062 | IN PROGRESS | Architecture guard + isolated shared + I2 portable boundary + I3 parser boundary | Rule-pack boundary cleanup and later duplication removal/persistence decision remain pending |
| CA-14 | CI | 010, 012, 070 | IN PROGRESS | Public-repo Ubuntu/macOS workflow evidence through I3 | Final iOS deployment/CI decision remains I7 |
| CA-15 | ANDROID | 041, 060, 061, 062 | NOT RUN | PENDING | PENDING |

## Outstanding checks before next implementation authorization

- I0, I1, I2 and I3 are merged and have executable evidence recorded above.
- This reconciliation checkpoint updates the task ledger only; it does not authorize or begin I4.
- I4 requires a new explicit authorization from Luis before TASK-KMP-040 may move to `IN PROGRESS`.
- PanicLab is intentionally public following the publication-readiness audit; no additional repository visibility change is implied by later implementation authorization.
- Do not modify `SoftwareDevelopmentBlueprint` during PanicLab validation.
- Preserve `main@c165ae4283a3b592eddb2b70a1c13a9ffeb51f01` as the verified behavioral reference for migration fixtures.
- Preserve the approved strangler sequence: Android production cutover to shared code remains deferred to I5.

## Completion condition

PanicLab KMP migration is not complete until every required CA has executed evidence for its target, Android data-preservation obligations are satisfied, the claimed iOS slice has real macOS/Xcode evidence, documentation matches implemented reality, and no unresolved release-blocking regression remains.
