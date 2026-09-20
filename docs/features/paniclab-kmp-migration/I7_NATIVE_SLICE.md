# I7 Native iOS Diagnostic Slice

## Scope

TASK-KMP-071 introduced PanicLab's first native iOS product surface. The slice is intentionally narrow:

- native SwiftUI app target under `iosApp/`;
- text entry and clipboard paste only;
- loading, error, non-conclusive and result states;
- direct execution of the KMP deterministic engine through a Swift-friendly COMMON facade;
- canonical `app/src/main/assets/paniclab_rules_v1.json` bundled into the iOS target as the single rule-pack source of truth;
- CryptoKit SHA-256 supplied by the iOS composition layer;
- native unit tests plus an accessibility-focused UI smoke test.

Explicitly out of scope for TASK-KMP-071:

- camera / OCR acquisition;
- file import/share extensions;
- AI/Gemini guidance;
- PDF/export;
- persistence/history/Room KMP;
- App Store signing/release configuration;
- parity claims beyond the text/paste diagnostic slice.

## Architecture

`NativeDiagnosticFacade` lives in `shared/commonMain`. It does not introduce UIKit, SwiftUI, Room, Foundation, CryptoKit or other Apple dependencies into COMMON.

The facade receives:

1. raw Panic Full text;
2. canonical rule-pack JSON;
3. the SHA-256 digest computed by the native iOS layer.

It then executes the existing portable sequence:

`LogNormalizer -> MetadataExtractor -> PanicClassifier -> SensorExtractor -> EvidenceExtractor -> DiagnosticRulesEngine -> CandidateRanker`

The result is flattened into `NativeDiagnosticResult`, a Swift-friendly scalar DTO. I7 does not persist reports, so IDs used internally for evidence are deterministic sequential IDs scoped to one analysis call.

## iOS composition

The SwiftUI app uses:

- `CryptoKit` for the canonical Rule Pack digest;
- `UIPasteboard` for native paste behavior;
- `NativeDiagnosticFacade` from `Shared.framework`;
- the same `paniclab_rules_v1.json` file already used by Android, referenced directly as an Xcode resource rather than copied.

The Xcode project supports iOS 15.0 and links the target-specific static `Shared.framework` produced by Gradle for device or Apple-silicon simulator.

## QA contract

The dedicated `KMP I7 Native iOS Slice` workflow proves on the exact PR HEAD:

- `iosArm64` and `iosSimulatorArm64` Shared frameworks link;
- SwiftUI app builds for iOS Simulator;
- SwiftUI app builds for generic iOS device with signing disabled;
- XCTest executes a known canonical product Rule Pack diagnosis through Swift -> Shared -> canonical Rule Pack;
- XCTest executes the unknown-code non-conclusive path;
- XCUITest verifies accessible log input, analyze action and error-state surface.

The existing I0/I5/I6 baseline workflow remains an independent regression gate.

## Executable evidence

Final TASK-KMP-071 PR head: `9bede0c0f0cbb51f43292547a1ac29e5d5af5a00`.

All independent gates were green on that exact head:

- `KMP I0 Baseline Verification` run `35453724371` — SUCCESS;
- `KMP I1 Scaffold Verification` run `35453724330` — SUCCESS;
- `KMP I7 iOS Preflight` run `35453724342` — SUCCESS;
- `KMP I7 Native iOS Slice` run `35453724329`, job `105925223001` — SUCCESS.

The native lane linked Shared frameworks, built the SwiftUI app for simulator, built the generic iOS device target with signing disabled, booted an iPhone simulator, and passed native unit plus accessibility UI smoke tests.

The final `.xcresult` was uploaded as artifact `paniclab-i7-ios-xcresult`, artifact id `10587497480`, digest `sha256:dc665f944e1d685f95dc1ccc23be7f763f714f66cd6015b56ae9f283b4a54c52`.

## QA correction discovered during execution

The first executable attempts exposed two test-harness assumptions rather than a product-engine regression:

1. the original iPhone 13 mini `0x1000` XCTest expectation came from the synthetic frozen migration baseline, while the native slice intentionally executes the full canonical product Rule Pack, where an overlapping `SMC_13_MINI` rule has different product semantics;
2. the first XCUITest queried the SwiftUI error-state accessibility identifier only as `XCUIElementTypeOther`, which was too type-specific for SwiftUI's accessibility tree.

Final correction commit `9bede0c0f0cbb51f43292547a1ac29e5d5af5a00` changed only the two native test files. The known diagnosis fixture was aligned with the canonical iPhone 14 / `0x500000` product case already proven through the Android repository -> shared engine -> Room path in TASK-KMP-051, and the UI smoke now resolves the error-state identifier through `.any` while also asserting the visible error title.

No engine, Rule Pack, Android product, Room/schema, SwiftUI product behavior, Gradle, workflow semantics or Blueprint content changed as part of that correction.

## Merge evidence and status

TASK-KMP-071 is **DONE**.

PR #20 was explicitly approved and merged as `cb580ba0e5032fd96069b76e1706d979ed217335`. The GitHub merge commit is validly signed and has parents `8e5b6de79bd1c90987dd8b02aaaa28c5869a4240` and the approved TASK-KMP-071 head `9bede0c0f0cbb51f43292547a1ac29e5d5af5a00`.

TASK-KMP-072 remains a separate increment and is not proven by the two TASK-KMP-071 fixture smokes. It must establish a broader per-fixture Kotlin/Native/iOS equivalence matrix on real macOS/Xcode evidence before I7 can close.
