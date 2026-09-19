# I5 Execution Evidence — Android shared-engine cutover

## Authorization and scope

I5 was explicitly authorized before implementation. This checkpoint covers only:

- TASK-KMP-050 — wire the Android product to the shared deterministic engine.
- TASK-KMP-051 — prove Android regression, repository/persistence integration and product semantics.
- TASK-KMP-052 — remove superseded Android deterministic implementations after regression evidence is green.

I6, I7, Blueprint changes, Room schema migration, DataStore migration, Compose redesign and native iOS application work are outside this checkpoint.

## Baseline and executable head

- Base: `main@2c2c9025b969d8c76e5ec1fa0e6ea1dd2bae4ccc`.
- Final validated executable head: `1c58931acf9051de93914758dacd29270df51562`.
- Pull request: #10, `kmp/i5-android-shared-cutover`.
- At validation time the branch was 19 commits ahead and 0 behind the base.

Commits after the validated executable head are documentation/cleanup-only and do not change executable production/test behavior.

## TASK-KMP-050 — Android cutover

Android `app` now depends on `:shared` for the deterministic diagnostic implementation. Android-native composition remains responsible for platform concerns.

`SharedEngineAndroidAdapters.kt` supplies the Android boundary for:

- Room-backed device lookup with shared static-map fallback;
- UUID generation;
- wall-clock injection;
- shared evidence extraction and report building.

Android-only repair-grounding/presentation models remain native in `AppOnlyModels.kt`. Room, DataStore, CameraX/ML Kit, Gemini grounding, PDF/share, navigation and Compose remain Android-native.

During the cutover, the superseded Android deterministic files were temporarily retained byte-for-byte outside `src/main` as a safety net. This was necessary because AGP 9 built-in Kotlin does not support the attempted individual source-file exclusion approach. After TASK-KMP-051 passed, TASK-KMP-052 removed those source copies completely.

## Cross-module compiler evidence

The first real Android cutover compilation reached `:shared:compileAndroidMain` successfully and then exposed eight Kotlin `SMARTCAST_IMPOSSIBLE` errors across module boundaries in:

- `GeminiRepairGroundingService.kt`;
- `ResultScreen.kt`;
- `TechnicalEvidenceScreen.kt`;
- `CaseDetailScreen.kt`;
- `RuleDetailScreen.kt`.

Only those evidenced consumers were adapted, using local snapshots/safe access. No shared model semantics, UI design, rule behavior or persistence contract was changed to suppress the compiler errors.

After those adaptations, Android baseline run `35413521478` passed on executable head `1d67cb40a291b41b74636326beb85c4e7953e307` while consuming shared classes.

## TASK-KMP-051 — repository → shared → Room evidence

`AndroidSharedCutoverIntegrationTest` uses:

- Robolectric API 36;
- real `Room.inMemoryDatabaseBuilder`;
- real `KnowledgeBaseRepositoryImpl` loading the bundled `paniclab_rules_v1.json` asset;
- real `DiagnosticRepositoryImpl`;
- the production Android-to-shared adapters.

It proves two representative frozen diagnostic inputs through the complete Android product path:

1. iPhone 14 / `0x500000`: shared parsing/engine selects bundled rule `smc14base_0x500000_battery___battery_data_path`, label `Batería`, HIGH confidence; session, evidence, candidate, raw-log policy and history round-trip through Room.
2. iPhone 14 / decimal `4194304`: shared normalization produces `0x400000`, bundled rule `smc14base_0x400000_wireless_charge_coil`, label `Bobina de carga inalámbrica`, HIGH confidence; `saveRawLog=false` remains false after Room round-trip.

The first integration attempt intentionally failed because the test asserted descriptive labels from the unit-level frozen I0 rule fixtures instead of the labels used by the real bundled product rule pack. The pre-cutover base was checked before changing assertions: the bundled asset already used `Batería` and `Bobina de carga inalámbrica`. Production code was not changed. The integration test was corrected to protect the real product rule IDs/labels while the existing I0/shared unit fixtures continue protecting deterministic engine semantics independently.

TASK-KMP-051 first green evidence:

- head: `b6d51b9a42f9fd2659dca5a26fe9b53892e7bae4`;
- Android run: `35414261710` — SUCCESS;
- artifact: `kmp-i0-unit-test-reports`, ID `10574822860`;
- digest: `sha256:5bf0e833d48a252b23cc979d38752dac14b084016e90d8f8437c747ff2b6f258`.

## TASK-KMP-052 — duplicate removal

After green TASK-KMP-051 evidence, the 13 superseded deterministic Android sources were removed. The final PR diff therefore shows those original `app/src/main` deterministic files as deleted, leaving `:shared` as the single implementation for the migrated deterministic responsibilities.

No Room schema/DAO migration was performed.

## Final automated validation after duplicate removal

### Shared common / JVM / Android-host / coverage

Workflow: `KMP I1 Scaffold Verification`, run `35414571367` — SUCCESS on executable head `1c58931acf9051de93914758dacd29270df51562`.

Passed:

- architecture boundary guard;
- `:shared:jvmTest`;
- `:shared:testAndroidHostTest`;
- Kover XML/HTML generation;
- Kover verification gate.

Exact Kover XML counters:

- LINE: 928 covered / 7 missed = **99.2513368984%**;
- BRANCH: 554 covered / 78 missed = **87.6582278481%**;
- INSTRUCTION: 5860 covered / 68 missed = **98.8529014845%**;
- METHOD: 56 covered / 3 missed = **94.9152542373%**;
- CLASS: 16 covered / 0 missed = **100%**.

Required gates remain satisfied: line >=90%, branch >=85%.

Coverage artifact:

- `kmp-i1-shared-coverage`;
- artifact ID `10575332892`;
- digest `sha256:c5adb990af4a737080e926c79f1f3411fb833903a9bd5c7c0a8386ebdf575a64`.

### Kotlin/Native iOS framework linking

The same workflow run `35414571367` passed:

- `linkDebugFrameworkIosArm64`;
- `linkDebugFrameworkIosSimulatorArm64`.

Artifact:

- `kmp-i1-ios-frameworks`;
- artifact ID `10575766136`;
- digest `sha256:bdd8b6061201d6f1487941e1a864a71d49230d9e239c13c22f4787e5bb8f5cd4`.

This evidence proves Kotlin/Native framework compile/link compatibility only. It does **not** claim an iOS native application build, XCTest execution or iOS behavioral equivalence.

### Final Android regression after duplicate removal

Workflow: `KMP I0 Baseline Verification`, run `35414571346` — SUCCESS on executable head `1c58931acf9051de93914758dacd29270df51562`.

Passed:

- frozen JSON fixtures;
- architecture guard self-test and repository scan;
- deterministic I0 baseline tests;
- rule-pack boundary/management tests;
- OCR deterministic baseline tests;
- `AndroidSharedCutoverIntegrationTest` repository → shared → Room/history integration.

Artifact:

- `kmp-i0-unit-test-reports`;
- artifact ID `10575586401`;
- digest `sha256:75389d053f8eaeb15f55f0236d89c3ec738d79e12cbae6ad944a790503b00827`.

## CI truthfulness improvement

The shared verification workflow now also triggers when the Android cutover boundary changes:

- `app/build.gradle.kts`;
- `SharedEngineAndroidAdapters.kt`;
- `AndroidSharedCutoverIntegrationTest.kt`.

This prevents future Android-to-shared boundary changes from bypassing shared JVM/Android-host/Kover/iOS-link validation merely because `shared/**` itself was not edited.

## Remaining acceptance gate — physical Android smoke

Automated I5 evidence is complete. A physical Android smoke remains intentionally pending and must not be fabricated. Before I5 can be considered accepted, a real device should verify at minimum:

- launch/current build;
- one known `0x500000` or decimal `4194304` diagnostic;
- expected product/rule/verdict;
- persisted case visible in History and reopenable;
- Evidence/Log navigation;
- back navigation plus one background/resume or rotation lifecycle check;
- no obvious regression in the Android-native acquisition path used for the smoke.

Until that smoke is reported PASS, PR #10 remains Draft.

## Checkpoint state

- TASK-KMP-050: automated implementation/validation **DONE**.
- TASK-KMP-051: automated implementation/validation **DONE**.
- TASK-KMP-052: duplicate removal and full automated post-removal matrix **DONE**.
- Physical Android smoke: **PENDING**.
- PR #10: **Draft / not merged**.
- I6: **not authorized**.
- Blueprint: **untouched**.
