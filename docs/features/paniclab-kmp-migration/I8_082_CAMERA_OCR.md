# I8 TASK-KMP-082 — iOS Camera + OCR Acquisition

## Status

**IMPLEMENTED / AUTOMATED VALIDATION PENDING / PHYSICAL IPHONE SMOKE PENDING**

Implementation branch: `kmp/i8-082-ios-camera-ocr`.

Baseline: `main@33529ec86a2a72df314002f64262d235f33ca247`.

Authorization: Luis explicitly authorized TASK-KMP-082 after the scope in `I8_082_CAMERA_OCR_SCOPE.md` was merged through PR #27.

## Implemented product boundary

The iOS OCR acquisition path is:

`AVFoundation or PhotosUI -> Vision -> NativeOcrFacade -> COMMON OcrLogExtractor -> editable review -> DiagnosticViewModel -> NativeDiagnosticFacade -> shared deterministic engine`

No OCR cleanup rule is duplicated in Swift.

## Native camera

The implementation uses:

- rear `AVCaptureDevice`;
- `AVCaptureSession`;
- `AVCaptureDeviceInput`;
- `AVCaptureVideoDataOutput`;
- `alwaysDiscardsLateVideoFrames = true`;
- serial capture/OCR queues;
- approximately 600 ms OCR throttling;
- SwiftUI preview through `AVCaptureVideoPreviewLayer`;
- optional torch only when the active device reports support;
- explicit permission states for not-determined, authorized, denied/restricted and unavailable camera paths.

The generated Info.plist configuration now includes `NSCameraUsageDescription`.

No microphone permission is requested.

## Vision OCR

`VisionTextRecognizer` uses `VNRecognizeTextRequest`.

- live frames use `.fast` recognition;
- selected still images use `.accurate` recognition;
- language correction is disabled to avoid rewriting technical tokens such as hexadecimal panic/sensor codes;
- OCR output remains in memory;
- frames/images are not persisted by this task.

## Shared OCR bridge

`NativeOcrFacade` was added to `shared/commonMain` as a Swift-friendly projection only.

It delegates directly to `OcrLogExtractor.processScannedText(...)` and maps:

- raw text;
- cleaned text;
- detected device/build;
- panic codes;
- keywords;
- line count;
- valid-signature flag;
- confidence hint.

COMMON tests compare the facade result field-for-field with the direct `OcrLogExtractor` result.

## Gallery intake

The scanner uses `PHPickerViewController` with:

- image-only filter;
- one selected item;
- no broad photo-library authorization request;
- harmless cancellation;
- the same Vision -> COMMON cleanup -> review path as camera OCR.

## Review before diagnosis

Recognized text is never diagnosed automatically.

The review sheet:

- shows COMMON confidence/keyword/code metadata;
- exposes editable cleaned OCR text;
- supports cancellation;
- requires an explicit `Diagnosticar` action;
- sends the reviewed text to `DiagnosticViewModel.acceptReviewedOcrText(...)` and then the existing `analyze()` path.

Scanner cancellation does not mutate the previous diagnostic input.

## Lifecycle and resource behavior

The scanner:

- starts only for authorized camera state;
- stops when the scanner disappears;
- stops when the app backgrounds/inactivates;
- pauses for gallery and review surfaces;
- resumes only when active, authorized and no blocking modal is presented;
- drops late frames and avoids parallel Vision frame work;
- handles unavailable camera/session/torch/Vision errors as UI-safe states.

## Test seam

A DEBUG-only UI-test seam is enabled only when launch environment `PANICLAB_UI_TEST_SCANNER=1` is present.

It:

- never pretends simulator hardware is a real camera;
- exposes an unavailable/test fallback;
- injects controlled representative OCR text through the same COMMON `NativeOcrFacade` cleanup;
- exercises editable review and explicit diagnosis;
- is absent from normal release behavior.

## Automated QA added

### COMMON

`NativeOcrFacadeTest`:

- proves direct mapping from `OcrLogExtractor`;
- covers valid panic text;
- covers blank/no-text behavior.

### XCTest

Added coverage for:

- Swift-visible OCR bridge;
- authorization status mapping;
- reviewed OCR edits preserved without auto-diagnosis;
- gallery success using an injected recognizer;
- gallery recognizer failure;
- real `VNRecognizeTextRequest` against a high-contrast rendered Panic Full image;
- Vision output passed into COMMON cleanup.

### XCUITest

Added controlled simulator evidence for:

- scanner entry accessibility;
- honest camera-unavailable test fallback;
- controlled OCR sample -> review;
- review text editable;
- explicit diagnosis reaches existing diagnostic states;
- scanner cancellation preserves existing input.

## Required exact-head acceptance before merge

The implementation is not DONE until the final PR head has all applicable gates green:

1. `KMP I0 Baseline Verification`;
2. `KMP I7 Native Equivalence`;
3. `KMP I7 Native iOS Slice`;
4. shared JVM/Kotlin-Native OCR tests;
5. simulator build;
6. generic-device build without signing;
7. XCTest/XCUITest;
8. real Vision still-image integration evidence;
9. diff/scope audit.

Any code change after a green run invalidates that run for final approval.

## Physical iPhone smoke gate

Even if all automated gates are green, TASK-KMP-082 must remain **PHYSICAL SMOKE PENDING** until the dedicated iPhone executes the approved checklist:

1. first camera permission prompt;
2. live rear-camera preview;
3. OCR representative Panic Full text;
4. review/edit recognized text;
5. explicit diagnosis;
6. denied/recovery path where practical;
7. gallery OCR;
8. background/resume;
9. torch if available;
10. no unexpected photo persistence.

No customer/private image or panic log is to be committed as evidence.

## Scope protection

TASK-KMP-082 does not modify:

- Android CameraX/ML Kit behavior;
- Android Room/schema;
- Android DataStore;
- persistence/history;
- canonical Rule Pack contents;
- deterministic parser/engine semantics;
- PDF/export;
- AI guidance;
- rule-pack management;
- trends;
- cloud OCR;
- `SoftwareDevelopmentBlueprint`.

TASK-KMP-083 and later I8 capabilities remain separately gated and are not authorized by this implementation.
