# I7 Kotlin/Native and iOS Equivalence

## Scope

TASK-KMP-072 proves that the deterministic shared engine behaves equivalently when the same COMMON tests execute on JVM and Kotlin/Native for an iOS Simulator target, and that the Swift/Xcode host reaches the same canonical product semantics already proven on Android.

This task does not add product capability. It does not change the deterministic engine, canonical Rule Pack, Android product behavior, Room/schema, persistence/history, camera/OCR acquisition, AI guidance, PDF/export or App Store signing.

## Evidence model

The proof has two independent layers.

### Layer A: same COMMON assertions on JVM and Kotlin/Native

The workflow runs:

- `:shared:jvmTest` as the reference execution;
- `:shared:iosSimulatorArm64Test` as the Kotlin/Native execution on a real GitHub-hosted macOS/Xcode environment.

A CI parser reads both JUnit result sets and fails unless every selected fixture-backed test exists exactly once and passes on both targets. It emits `i7-native-equivalence-matrix.json` as machine-readable evidence.

Selected fixture matrix:

| Fixture / scenario | COMMON test assertion | JVM | iosSimulatorArm64 | Equivalent |
| --- | --- | --- | --- | --- |
| `iphone13-mini-0x1000-dock-mic` | `frozenI0Iphone13MiniExact0x1000RemainsEquivalent` | PASS | PASS | yes |
| `iphone14-0x500000-battery` | `frozenI0Iphone14BatteryAndWirelessDecimalRemainEquivalent` | PASS | PASS | yes |
| `iphone14-wireless-coil-decimal-regression` | `frozenI0Iphone14BatteryAndWirelessDecimalRemainEquivalent` | PASS | PASS | yes |
| `iphone16-pro-decimal-3145728` | `frozenI0Iphone16DecimalAndPrs0RemainEquivalent` | PASS | PASS | yes |
| `iphone-x-missing-prs0` | `frozenI0Iphone16DecimalAndPrs0RemainEquivalent` | PASS | PASS | yes |
| `unknown-smc-safe-fallback` | `frozenI0UnknownSmcUsesSafeFallback` | PASS | PASS | yes |
| Rule Pack full parse meaning | `fullParserPreservesRulePackMeaning` | PASS | PASS | yes |
| Rule Pack valid validation | `validPackPassesValidator` | PASS | PASS | yes |

All eight selected rows were generated from JUnit evidence and reported `jvm=PASS`, `iosSimulatorArm64=PASS`, `equivalent=true`.

The COMMON test layer deliberately protects the frozen migration semantics. It does not reinterpret product Rule Pack conflicts discovered later.

### Layer B: Swift/Xcode host with canonical product Rule Pack

`DiagnosticBridgeTests` executes the native Swift -> `Shared.framework` -> canonical bundled Rule Pack path and verifies:

1. iPhone 14 / `0x500000` canonical battery diagnosis already proven through Android repository -> shared engine -> Room in TASK-KMP-051;
2. iPhone 14 / decimal `4194304` normalization to the canonical wireless-charging-coil diagnosis already proven in TASK-KMP-051;
3. unknown SMC code remains non-conclusive with `UNKNOWN` confidence.

On the successful equivalence run, Xcode executed all three bridge tests with zero failures:

- `testSharedFacadeMatchesAndroidProvenCanonicalDecimalWirelessDiagnosis` PASS;
- `testSharedFacadeMatchesAndroidProvenCanonicalDiagnosis` PASS;
- `testSharedFacadeReturnsNonConclusiveUnknownCode` PASS.

This separates frozen COMMON semantics from current product Rule Pack semantics instead of pretending they are identical where the canonical pack has evolved or contains broader rules.

## Executable evidence

Successful implementation/evidence head before this documentation-only reconciliation commit:

`9e158e0d6c5b12cf3aa87c334c8b4afdde72b776`

`KMP I7 Native Equivalence` run `35481696591`, job `106000559578`: **SUCCESS**.

Observed real runner/toolchain:

- macOS 26.6.2;
- GitHub image `macos-26-arm64`;
- Xcode 26.6, build `17F113`;
- iPhoneOS SDK 26.5;
- iPhone Simulator SDK 26.5;
- selected simulator: iPhone Air, iOS 26.5.

Executed gates:

- `:shared:jvmTest` -> BUILD SUCCESSFUL;
- `:shared:iosSimulatorArm64Test` -> BUILD SUCCESSFUL, proving actual COMMON test execution through Kotlin/Native on iOS Simulator rather than framework compilation alone;
- generated 8-row JVM/Kotlin-Native equivalence matrix -> SUCCESS;
- `:shared:linkDebugFrameworkIosSimulatorArm64` -> SUCCESS;
- Xcode `PanicLabIOSTests` against the canonical Rule Pack -> 3 tests, 0 failures, `TEST SUCCEEDED`;
- independent `KMP I0 Baseline Verification` run `35481696603` -> SUCCESS on the same implementation/evidence head.

Evidence artifact:

- name: `paniclab-i7-native-equivalence`;
- artifact ID: `10595449813`;
- size: 60,513 bytes;
- digest: `sha256:21970459db3b0b233143426a04c5594870c5442f62d5d34a9f348e5a144747ee`;
- contains `i7-native-equivalence-matrix.json`, JVM JUnit results, Kotlin/Native iOS Simulator JUnit results and `PanicLabIOS-Equivalence.xcresult.zip`.

The first workflow attempt failed only in the CI matrix collector because Gradle appends target suffixes such as `[jvm]` and `[iosSimulatorArm64]` to JUnit testcase names. Both JVM and Kotlin/Native test tasks had already passed. Commit `9e158e0d6c5b12cf3aa87c334c8b4afdde72b776` normalizes those suffixes before comparison; no test, fixture, engine or product semantic was weakened.

## CI contract

`.github/workflows/kmp-i7-native-equivalence.yml` must, on the exact PR HEAD:

- record the real Apple toolchain;
- boot an available iPhone Simulator;
- pass `:shared:jvmTest`;
- pass `:shared:iosSimulatorArm64Test`;
- generate a per-fixture JVM/Kotlin-Native PASS matrix from JUnit evidence;
- link `Shared.framework` for `iosSimulatorArm64`;
- pass `PanicLabIOSTests` under Xcode against the canonical Rule Pack;
- upload the matrix, JVM/Native JUnit results and `.xcresult` bundle as one evidence artifact.

The existing I0, I1, I7 preflight and native-slice workflows remain independent regression gates and are not replaced by this workflow.

## Status

Implementation started after explicit authorization to continue on 2026-09-19, from `main@bdaa79c8f564ff9f2e1d28df7bd529ce1515595d`.

Executable equivalence evidence is complete on implementation/evidence head `9e158e0d6c5b12cf3aa87c334c8b4afdde72b776`. This documentation update intentionally triggers the equivalence workflow again so the final exact PR HEAD must also prove the same JVM, Kotlin/Native and Xcode gates.

TASK-KMP-072 remains **IN PROGRESS / merge pending**. It must not be marked DONE until PR #22 is explicitly approved and merged. I8 remains unimplemented and unauthorized by this task.
