# I4 Execution Evidence

## Scope

I4 implements only `TASK-KMP-040` and `TASK-KMP-041` from the approved PanicLab KMP migration plan.

Validated executable HEAD: `f5aa05e7315e7f73ee32b3054db35383bf5f807e`.
Base: `main@a924611b5b2eb0027605bb0a3548e12e929b9a3e`.

No I5 implementation is included or authorized by this checkpoint.

## TASK-KMP-040 — portable Rule Pack core

COMMON now contains KMP-safe Rule Pack parsing, validation and diff semantics using `kotlinx.serialization.json`, Kotlin `Regex` and the injected `Sha256Hasher` boundary.

The shared tests cover representative parsing/defaults, SemVer and referential validation, duplicate IDs, regex/code validation, warning branches, null-current diffs, added/modified/deactivated/removed rules and model/source/classifier differences.

Final shared/macOS workflow: `35409648294` — SUCCESS.

- architecture guard: PASS
- shared JVM tests: PASS
- shared Android-host tests: PASS
- Kover verification: PASS
- `iosArm64` framework link: PASS
- `iosSimulatorArm64` framework link: PASS
- line coverage: 928/935 = **99.2513%**
- branch coverage: 554/632 = **87.6582%**
- configured thresholds remain >=90% line / >=85% branch

Artifacts:

- coverage artifact `10573670700`, SHA-256 `164395f9e2257c4dae6a0b0bbd7f7c1b86846efb25333b21fdf786f829b34b59`
- iOS frameworks artifact `10574235454`, SHA-256 `24ded345c657862c5a0ea93938551ee0df6d43c9c9de45b6a021199ec1d3daec`

## TASK-KMP-041 — persistence boundary cleanup

`KnowledgeBaseRepository` no longer exposes `RulePackEntity`. It returns domain-only `RulePackMetadata` for active/history Rule Packs. Android Room entities remain in the data layer and map through `RulePackPersistenceMapper`.

The Room schema and DAOs were not changed. Existing installation/history behavior remains behind Android adapters. A temporary presentation adapter remains in `KnowledgeBaseViewModel` so this increment does not become a Compose redesign; this does not reintroduce a Room entity into the domain repository contract.

Final Android regression workflow: `35409648288` — SUCCESS.

The run executes:

- frozen JSON fixture validation: PASS
- architecture guard self-test: PASS
- architecture repository scan: PASS
- `DeterministicDiagnosticEngineTest`: PASS
- `RulePackManagementTest`: PASS
- `RulePackBoundaryTest`: PASS
- `OcrLogExtractorTest`: PASS

Android report artifact `10574175667`, SHA-256 `c61efb03c6a12e038233ebe81f91501d98b3401025434ab72230968db9f75155`.

## Corrective finding during I4

The first Android run `35409309335` had 23/24 tests passing. The only failure was the new `RulePackBoundaryTest.ruleAndDeviceEntitiesMapToDomainModels`: `org.json` parsing was exercised under a plain JVM test environment, where Android framework behavior is not available. Production code had compiled and the historical baseline suites were not the failing tests.

The fix was test-only: `RulePackBoundaryTest` was moved under `RobolectricTestRunner` with SDK 36. No mapper or product behavior was changed. The final Android run `35409648288` then passed all required suites.

## Delta and guardrails

Compared with the I4 base, the validated executable delta is 2 commits ahead / 0 behind and changes 11 files: shared Rule Pack core/tests/coverage scope, domain metadata, Android persistence mapping/repository boundary, a presentation compatibility adapter, the boundary test and the baseline workflow entry for that test.

Preserved guardrails:

- no Room schema change
- no DAO schema migration
- no destructive migration
- no Android diagnostic-engine cutover to shared code
- no Compose redesign
- no SwiftUI app
- no Blueprint modification
- no I5 work

Android shared-engine cutover remains reserved for I5 and requires separate explicit authorization after I4 merge and ledger reconciliation.
