# I5 Execution Evidence — Android shared-engine cutover

## Authorization and scope

I5 was explicitly authorized before implementation. This checkpoint covers only:

- TASK-KMP-050 — wire the Android product to the shared deterministic engine.
- TASK-KMP-051 — prove Android regression, repository/persistence integration and product semantics.
- TASK-KMP-052 — remove superseded Android deterministic implementations after regression evidence is green.

I6, I7, Blueprint changes, Room schema migration, DataStore migration, Compose redesign and native iOS application work are outside this checkpoint.

## Baseline and validated heads

- Base: `main@2c2c9025b969d8c76e5ec1fa0e6ea1dd2bae4ccc`.
- Final validated executable head: `1c58931acf9051de93914758dacd29270df51562`.
- Physical-smoke packaging head: `06a339808e861e4a737e62cf27a2ef1080d71820`.
- Physical-smoke evidence reconciliation head: `6bd6a03e7ed4eea2703142cb8ececb04c461a7f9`.
- Pull request: #10, `kmp/i5-android-shared-cutover`.
- Merge commit on `main`: `40cdc6006a0884465aa0cd92b89a3a1ae74638b2`.

Commits after the validated executable head are documentation, cleanup or CI-only changes and do not change product executable behavior.

## TASK-KMP-050 — Android cutover

Android `app` now depends on `:shared` for the deterministic diagnostic implementation. Android-native composition remains responsible for platform concerns.

`SharedEngineAndroidAdapters.kt` supplies the Android boundary for Room-backed device lookup with shared static-map fallback, UUID generation, wall-clock injection, shared evidence extraction and report building.

Android-only repair-grounding/presentation models remain native. Room, DataStore, CameraX/ML Kit, Gemini grounding, PDF/share, navigation and Compose remain Android-native.

During the cutover, the superseded Android deterministic files were temporarily retained byte-for-byte outside `src/main` as a safety net. After TASK-KMP-051 passed, TASK-KMP-052 removed those source copies completely.

## Cross-module compiler evidence

The first real Android cutover compilation reached `:shared:compileAndroidMain` successfully and then exposed eight Kotlin `SMARTCAST_IMPOSSIBLE` errors across module boundaries in `GeminiRepairGroundingService.kt`, `ResultScreen.kt`, `TechnicalEvidenceScreen.kt`, `CaseDetailScreen.kt` and `RuleDetailScreen.kt`.

Only those evidenced consumers were adapted using local snapshots/safe access. No shared model semantics, UI design, rule behavior or persistence contract was changed to suppress the compiler errors.

Android baseline run `35413521478` passed on executable head `1d67cb40a291b41b74636326beb85c4e7953e307` while consuming shared classes.

## TASK-KMP-051 — repository → shared → Room evidence

`AndroidSharedCutoverIntegrationTest` uses Robolectric API 36, real `Room.inMemoryDatabaseBuilder`, real `KnowledgeBaseRepositoryImpl` loading bundled `paniclab_rules_v1.json`, real `DiagnosticRepositoryImpl`, and production Android-to-shared adapters.

It proves two representative inputs through the complete Android product path:

1. iPhone 14 / `0x500000`: bundled rule `smc14base_0x500000_battery___battery_data_path`, label `Batería`, HIGH confidence; session/evidence/candidate/history round-trip through Room.
2. iPhone 14 / decimal `4194304`: normalization to `0x400000`, bundled rule `smc14base_0x400000_wireless_charge_coil`, label `Bobina de carga inalámbrica`, HIGH confidence; raw-log policy preserved through Room.

First green TASK-KMP-051 evidence:

- head `b6d51b9a42f9fd2659dca5a26fe9b53892e7bae4`;
- Android run `35414261710` — SUCCESS;
- artifact `kmp-i0-unit-test-reports`, ID `10574822860`;
- digest `sha256:5bf0e833d48a252b23cc979d38752dac14b084016e90d8f8437c747ff2b6f258`.

## TASK-KMP-052 — duplicate removal

After green TASK-KMP-051 evidence, the 13 superseded deterministic Android sources were removed. The final PR diff shows those original `app/src/main` deterministic files as deleted, leaving `:shared` as the single implementation for the migrated deterministic responsibilities.

No Room schema/DAO migration was performed.

## Final automated validation after duplicate removal

### Shared common / JVM / Android-host / coverage

Workflow `KMP I1 Scaffold Verification`, run `35414571367` — SUCCESS on executable head `1c58931acf9051de93914758dacd29270df51562`.

Passed architecture boundary guard, `:shared:jvmTest`, `:shared:testAndroidHostTest`, Kover XML/HTML generation and Kover verification.

Exact Kover XML counters:

- LINE: 928 covered / 7 missed = **99.2513368984%**;
- BRANCH: 554 covered / 78 missed = **87.6582278481%**;
- INSTRUCTION: 5860 covered / 68 missed = **98.8529014845%**;
- METHOD: 56 covered / 3 missed = **94.9152542373%**;
- CLASS: 16 covered / 0 missed = **100%**.

Required gates remain satisfied: line >=90%, branch >=85%.

Coverage artifact `kmp-i1-shared-coverage`, ID `10575332892`, digest `sha256:c5adb990af4a737080e926c79f1f3411fb833903a9bd5c7c0a8386ebdf575a64`.

### Kotlin/Native iOS framework linking

The same run `35414571367` passed `linkDebugFrameworkIosArm64` and `linkDebugFrameworkIosSimulatorArm64`.

Artifact `kmp-i1-ios-frameworks`, ID `10575766136`, digest `sha256:bdd8b6061201d6f1487941e1a864a71d49230d9e239c13c22f4787e5bb8f5cd4`.

This proves Kotlin/Native framework compile/link compatibility only. It does **not** claim a native iOS application build, XCTest execution or iOS behavioral equivalence.

### Final Android regression after duplicate removal

Workflow `KMP I0 Baseline Verification`, run `35414571346` — SUCCESS on executable head `1c58931acf9051de93914758dacd29270df51562`.

Passed frozen JSON fixtures, architecture guard, deterministic I0 baseline, rule-pack boundary/management, OCR baseline and `AndroidSharedCutoverIntegrationTest` repository → shared → Room/history integration.

Artifact `kmp-i0-unit-test-reports`, ID `10575586401`, digest `sha256:75389d053f8eaeb15f55f0236d89c3ec738d79e12cbae6ad944a790503b00827`.

## Physical-smoke APK packaging evidence

A CI-produced debug APK was added so the physical smoke used the exact I5 branch build rather than an independently compiled local binary.

The first packaging run `35415807909` passed all I0/I5 tests but failed only at `:app:validateSigningDebug` because CI did not contain the root `debug.keystore` expected by the existing debug signing config.

The CI fix did not modify release signing or add a persistent private key. The workflow generates an ephemeral Android debug keystore using standard non-secret debug credentials and assembles the debug APK.

Final packaging evidence on head `06a339808e861e4a737e62cf27a2ef1080d71820`:

- Android run `35416104717` — SUCCESS;
- frozen fixtures / architecture guard / I0 + I5 tests — PASS;
- ephemeral debug keystore generation — PASS;
- `:app:assembleDebug` — PASS;
- APK artifact upload — PASS;
- artifact `paniclab-i5-physical-smoke-apk`, ID `10576026106`;
- artifact ZIP digest `sha256:c39b103c490ba433a99e0dcec4ea575e84a7fd53f5840cdae2104b621a8c3205`;
- raw APK SHA-256 `8ead753861eee499d27caa3bb1626a8073385f87555f4ff83da11da4e1473aaf`.

On the same packaging head, shared/iOS run `35416104730` also completed SUCCESS.

## Physical Android smoke — PASS

Physical smoke was executed on a real Android device using the CI-produced I5 APK and reported **`Smoke I5 PASS`** on 2026-09-19.

The app installed successfully after removal of the previously installed build whose signing certificate differed from the CI ephemeral debug key. The visible Android product remained consistent with the pre-cutover application, which is expected because I5 is an internal engine cutover rather than a UI redesign.

A real iPhone panic log was then used as the diagnostic specimen. The source log identified `iPhone12,8` and contained a userspace watchdog timeout from `thermalmonitord` with `Missing sensor(s): mic1`. The original exported file had an unusual preexisting character-spacing format that neither the pre-I5 Android normalizer nor the shared I5 normalizer supports. A semantically identical normalized copy was therefore used for the engine smoke; this ingestion-format gap is not an I5 regression.

Before running the normalized log through PanicLab, the expected deterministic outcome was frozen from the bundled rule pack:

- product: `iPhone12,8` → iPhone SE (2nd generation);
- family: `THERMAL_MISSING_SENSOR`;
- missing sensor: `Mic1`;
- rule: `classic_mic1_charge_port`;
- verdict: `Ruta de micrófono inferior / conjunto del puerto de carga`;
- primary component: `Charge Port Assembly / bottom microphone path`;
- secondary component: `Logic-board connector / communication lines`;
- confidence: HIGH / well documented.

The physical Android result matched that primary diagnosis exactly. History persistence/reopen, Evidence/Log navigation and lifecycle/background-resume checks were also reported PASS as part of the final `Smoke I5 PASS` acceptance.

### Non-blocking preexisting findings discovered by the real smoke

The smoke exposed three product-hardening items that predate I5 and are intentionally **not** folded into this cutover PR:

1. **Character-spaced log ingestion** — `LogNormalizer` does not reconstruct exports where every character is separated by spaces. Pre-I5 Android had the same limitation.
2. **Secondary panic-family false positives** — classifier scans the whole crash log for broad tokens such as `ANS2`, `AppleSocHot`, DCP/iomfb and Baseband, so loaded-driver/kext names can add unrelated secondary families even when the primary thermal diagnosis is correct. Pre-I5 Android used the same behavior.
3. **Fallback SMC-code false positives** — when no sensor-array values are found and `SMC` occurs somewhere in the combined log, the legacy/shared fallback can collect unrelated hexadecimal values (for example epoch/kernel values) as apparent SMC codes. This behavior also existed before I5.

A cosmetic duplicate OS-version presentation was also observed in the generated report. It does not affect diagnosis and is outside the I5 cutover scope.

These findings should be handled as a separate diagnostic-hardening increment with dedicated fixtures/regression tests, not as silent scope expansion of I5.

## CI truthfulness improvement

The shared verification workflow now also triggers when the Android cutover boundary changes (`app/build.gradle.kts`, `SharedEngineAndroidAdapters.kt`, `AndroidSharedCutoverIntegrationTest.kt`).

The Android baseline workflow also builds and publishes the signed debug APK used for physical I5 smoke with only an ephemeral CI debug key. Release signing remains untouched.

## Final-head verification before merge

After the physical-smoke evidence reconciliation commit `6bd6a03e7ed4eea2703142cb8ececb04c461a7f9`, both required workflows were rerun on that exact PR head and completed successfully:

- `KMP I0 Baseline Verification` run `35418051596` — SUCCESS;
- `KMP I1 Scaffold Verification` run `35418051651` — SUCCESS, including shared JVM/Android-host/Kover and both iOS framework links.

## Merge evidence

PR #10 was explicitly approved for merge and merged on 2026-09-19.

- PR #10 final head: `6bd6a03e7ed4eea2703142cb8ececb04c461a7f9`;
- merge commit: `40cdc6006a0884465aa0cd92b89a3a1ae74638b2`;
- PR state after merge: closed / merged;
- `main` immediately after merge: `40cdc6006a0884465aa0cd92b89a3a1ae74638b2`;
- merge signature: verified / valid.

## Checkpoint state

- TASK-KMP-050: **DONE**.
- TASK-KMP-051: **DONE**.
- TASK-KMP-052: **DONE**.
- Physical-smoke APK packaging: **DONE**.
- Physical Android smoke: **PASS / DONE**.
- I5 acceptance gate: **CLOSED**.
- PR #10: **MERGED** as `40cdc6006a0884465aa0cd92b89a3a1ae74638b2`.
- I6: **not authorized**.
- Blueprint: **untouched**.
