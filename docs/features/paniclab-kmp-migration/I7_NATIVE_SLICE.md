# I7 Native iOS Diagnostic Slice

## Scope

TASK-KMP-071 introduces PanicLab's first native iOS product surface. The slice is intentionally narrow:

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

The dedicated `KMP I7 Native iOS Slice` workflow must prove on the exact PR HEAD:

- `iosArm64` and `iosSimulatorArm64` Shared frameworks link;
- SwiftUI app builds for iOS Simulator;
- SwiftUI app builds for generic iOS device with signing disabled;
- XCTest executes a frozen known diagnostic through Swift -> Shared -> canonical Rule Pack;
- XCTest executes the unknown-code non-conclusive path;
- XCUITest verifies accessible log input, analyze action and error-state surface.

The existing I0/I5/I6 baseline workflow remains an independent regression gate.

## Status

Implementation started after explicit user authorization on 2026-09-19. Initial implementation commit: `4dd950972d0ee44a9a6d75a4b2126bfd0a582e03`. Final executable evidence is pending the PR workflow and must be recorded before TASK-KMP-071 can be marked DONE.
