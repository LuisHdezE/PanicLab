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

| Fixture / scenario | COMMON test assertion |
| --- | --- |
| `iphone13-mini-0x1000-dock-mic` | `frozenI0Iphone13MiniExact0x1000RemainsEquivalent` |
| `iphone14-0x500000-battery` | `frozenI0Iphone14BatteryAndWirelessDecimalRemainEquivalent` |
| `iphone14-wireless-coil-decimal-regression` | `frozenI0Iphone14BatteryAndWirelessDecimalRemainEquivalent` |
| `iphone16-pro-decimal-3145728` | `frozenI0Iphone16DecimalAndPrs0RemainEquivalent` |
| `iphone-x-missing-prs0` | `frozenI0Iphone16DecimalAndPrs0RemainEquivalent` |
| `unknown-smc-safe-fallback` | `frozenI0UnknownSmcUsesSafeFallback` |
| Rule Pack full parse meaning | `fullParserPreservesRulePackMeaning` |
| Rule Pack valid validation | `validPackPassesValidator` |

The COMMON test layer deliberately protects the frozen migration semantics. It does not reinterpret product Rule Pack conflicts discovered later.

### Layer B: Swift/Xcode host with canonical product Rule Pack

`DiagnosticBridgeTests` executes the native Swift -> `Shared.framework` -> canonical bundled Rule Pack path and verifies:

1. iPhone 14 / `0x500000` canonical battery diagnosis already proven through Android repository -> shared engine -> Room in TASK-KMP-051;
2. iPhone 14 / decimal `4194304` normalization to the canonical wireless-charging-coil diagnosis already proven in TASK-KMP-051;
3. unknown SMC code remains non-conclusive with `UNKNOWN` confidence.

This separates frozen COMMON semantics from current product Rule Pack semantics instead of pretending they are identical where the canonical pack has evolved or contains broader rules.

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

Executable evidence is pending. TASK-KMP-072 must not be marked DONE until the exact final PR HEAD has green JVM, Kotlin/Native and Xcode evidence and the generated matrix is inspected.
