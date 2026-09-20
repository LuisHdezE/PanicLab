# I8 / TASK-KMP-081 — iOS File Import

**Baseline:** `main@bd98d7c6bf727bc597d66972970b36ffbb773c69`
**Branch:** `kmp/i8-081-ios-file-import`
**State:** IMPLEMENTED / VALIDATION PENDING
**Authorization:** explicitly approved by Luis after merge of PR #24.

## Objective

Add native iOS file intake for Panic Full logs while reusing the exact existing text diagnostic pipeline. This increment does not add persistence, camera/OCR, PDF export, rule-pack import, AI guidance or deterministic-engine behavior changes.

## Product behavior

### Supported inputs

- `.ips`
- `.txt`
- `.log`
- `.json`

The SwiftUI surface uses the native iOS document importer. The selected file is read as text and placed into the same `DiagnosticViewModel.logText` input used by typed/pasted logs. Diagnosis continues through the existing `NativeDiagnosticFacade` and canonical bundled Rule Pack.

### Safety boundaries

- maximum imported file size: **5 MiB**;
- accepted encodings: **UTF-8 and UTF-16** (including little/big endian variants);
- empty or whitespace-only files are rejected;
- unsupported extensions are rejected;
- unreadable/missing/non-regular selections fail safely;
- user cancellation returns to idle without replacing existing input;
- successful import does not auto-persist or auto-analyze;
- imported file name is shown only as local UI state;
- no imported fixture contains real customer/workshop data.

These limits are intentionally local to TASK-KMP-081 and do not alter Android ingestion semantics.

## Changed surfaces

- `iosApp/PanicLabIOS/ContentView.swift`
  - native SwiftUI `fileImporter` entry point;
  - accessible Import button;
  - imported-file ready indicator;
  - explicit supported-format/encoding/size hint.
- `iosApp/PanicLabIOS/DiagnosticViewModel.swift`
  - security-scoped document access;
  - extension/size/encoding/content validation;
  - cancellation and read-error handling;
  - imported text feeds existing `logText` pipeline.
- `iosApp/PanicLabIOSTests/DiagnosticBridgeTests.swift`
  - import vs paste-path deterministic equivalence;
  - cancellation;
  - unsupported extension;
  - empty input;
  - oversized input;
  - undecodable input;
  - missing/unreadable input.
- `iosApp/PanicLabIOSUITests/PanicLabIOSUITests.swift`
  - accessibility smoke now requires the Import control.

## Acceptance mapping

| Roadmap evidence | Test/evidence |
| --- | --- |
| real simulator XCTest/UI integration for successful import | `testImportedFixtureMatchesPastePathDiagnosis` plus native Xcode simulator test workflow |
| cancellation path | `testImportCancellationPreservesExistingInputAndReturnsIdle` |
| unreadable/unsupported/empty input | dedicated XCTest cases for missing file, extension and empty text |
| imported fixture same diagnosis as paste input | `testImportedFixtureMatchesPastePathDiagnosis` compares canonical result fields |
| encoding/oversized behavior | dedicated undecodable and >5 MiB XCTest cases |
| Android regressions remain green | PR exact-head GitHub Actions evidence required before merge |

## Validation required before merge

1. `KMP I0 Baseline Verification` SUCCESS on exact PR HEAD.
2. `KMP I7 Native iOS Slice` SUCCESS on exact PR HEAD, including simulator/device compilation and XCTest/XCUITest.
3. Any additional exact-head workflows triggered by shared/ios paths must be green.
4. Review PR diff to confirm no Room/schema, deterministic engine, Rule Pack, Android behavior or Blueprint modification.

Until those gates pass, TASK-KMP-081 is implemented but not DONE.
