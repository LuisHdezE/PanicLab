# I8 TASK-KMP-082 — iOS Camera + OCR Acquisition Scope

## Status

**PRE-IMPLEMENTATION SCOPE / NOT AUTHORIZED FOR IMPLEMENTATION**

This note prepares the implementation boundary for TASK-KMP-082 after TASK-KMP-080 and TASK-KMP-081 were completed and reconciled. It does not authorize product-code changes.

Baseline for this scope: `main@74dc5d21afa3bb9f8341da8e4959b939149dc5ec`.

## Objective

Add native iOS camera/image OCR acquisition while preserving the existing shared post-OCR cleanup and deterministic diagnostic semantics.

The product boundary is:

`AVFoundation / PhotosUI image source -> Vision OCR -> shared OcrLogExtractor -> editable review -> DiagnosticViewModel -> NativeDiagnosticFacade -> shared deterministic engine`

Image acquisition and Apple framework integration remain native iOS responsibilities. Panic-log cleanup and diagnosis remain shared semantics.

## Verified existing behavior

### Android reference

Android already provides:

- rear-camera live preview;
- CameraX `ImageAnalysis` with ML Kit text recognition;
- frame throttling / keep-latest behavior;
- camera permission request and denied fallback;
- photo-gallery image intake;
- optional torch control;
- live OCR status derived from `OcrScanResult`;
- editable review of cleaned OCR text before diagnosis;
- explicit rescan and diagnose actions;
- diagnosis only after reviewed text is submitted.

Android OCR acquisition remains platform-native. `PanicLogImageAnalyzer` sends recognized text to COMMON `OcrLogExtractor.processScannedText(...)`.

### Shared contract

`shared/commonMain/com.example.ocr.OcrLogExtractor` already owns deterministic post-OCR behavior:

- OCR hex-artifact repair;
- decimal sensor-code normalization;
- device/build extraction;
- panic keyword detection;
- sanitized log construction;
- valid-signature classification;
- capture-confidence hint generation.

TASK-KMP-082 must reuse this implementation rather than reproduce the cleanup rules in Swift.

### Current iOS state

The native iOS app currently supports:

- typed/pasted log input;
- native file import;
- canonical Rule Pack loading;
- deterministic diagnosis through `NativeDiagnosticFacade`;
- real simulator/device build, XCTest and XCUITest evidence.

No camera/OCR product code, camera privacy key or Apple Vision capture path exists yet.

## Selected native approach

### Camera

Use AVFoundation with:

- `AVCaptureSession`;
- rear video capture device;
- `AVCaptureDeviceInput`;
- `AVCaptureVideoDataOutput` for OCR frames;
- native preview layer bridged into SwiftUI;
- a serial capture/OCR queue;
- drop/throttle semantics so OCR work cannot backlog indefinitely.

The initial parity target is approximately the Android 600 ms analysis cadence. The exact cadence is an implementation tuning value, not a diagnostic semantic contract.

### OCR

Use Vision `VNRecognizeTextRequest` / `VNImageRequestHandler`, compatible with the iOS 15 deployment floor.

Live-frame OCR should favor responsiveness while maintaining enough accuracy for Panic Full tokens. Still-image/gallery OCR may use the more accurate recognition level because latency is less critical.

Recognized raw text must then pass through COMMON `OcrLogExtractor` before any review or diagnosis.

### Gallery

Use PhotosUI `PHPickerViewController` for still-image selection so the user explicitly chooses the image exposed to PanicLab. Do not request broad photo-library access unless a later separately approved requirement proves it necessary.

### Swift/KMP interop boundary

Prefer a small Swift-friendly shared facade, for example `NativeOcrFacade`, only if direct Swift consumption of `OcrLogExtractor` / `OcrScanResult` is awkward or unstable. Such a facade may map existing COMMON OCR results but must not duplicate or alter cleanup semantics.

## Camera permission and privacy

Implementation must add a camera usage explanation through the generated Info.plist configuration (`NSCameraUsageDescription`).

No microphone capture is required and no microphone permission should be requested.

Default privacy behavior:

- no live frame is persisted;
- no captured photo is persisted by TASK-KMP-082;
- no OCR image or OCR text is uploaded to a network service;
- gallery access is limited to the item explicitly selected by the user;
- OCR output remains in-memory until the user reviews/submits it;
- TASK-KMP-082 does not introduce history persistence.

## Product flow

### Entry

Add an iOS camera/OCR action from the current diagnostic input surface.

### Permission states

The UI must represent at least:

1. `notDetermined` — explain why camera access is needed and request it deliberately;
2. `authorized` — start live preview/OCR;
3. `denied/restricted` — show a safe fallback with Settings guidance and gallery selection;
4. `unavailable` — no usable camera hardware/session; gallery remains available.

Permission denial must not block file import, paste or gallery-based OCR.

### Live scanner

When authorized:

- show rear-camera preview;
- analyze frames without unbounded queuing;
- update recognition status from the latest shared `OcrScanResult`;
- expose optional torch control only when the active device supports it;
- allow the user to open a review surface when recognized text is available;
- do not automatically diagnose merely because OCR detects a valid signature.

### Gallery path

The user may choose one still image through PhotosUI. The selected image is OCR-processed, passed through COMMON cleanup, and opened in the same review surface as live-camera OCR.

Canceling the picker is harmless and does not change the existing diagnostic input.

### Review before diagnosis

The review surface must:

- show the cleaned OCR text;
- allow technician edits;
- expose detected device/build/codes/keywords where useful;
- allow rescan/cancel;
- require explicit user action before diagnosis;
- feed the final reviewed text into the same existing `DiagnosticViewModel` / `NativeDiagnosticFacade` path used by paste/file import.

## Lifecycle/resource rules

- Start the capture session only while the scanner is visible and authorized.
- Stop or pause capture when the scanner disappears, the app backgrounds, a blocking picker/review flow is presented, or an unrecoverable capture error occurs.
- Resume safely when returning to the scanner if permission and hardware remain valid.
- Do not retain camera frames longer than required for the current Vision request.
- Serialize Vision work or drop frames while an OCR request is already executing.
- Camera/torch configuration errors must fail safely rather than crash the app.

## Error and safe-state requirements

The implementation must handle:

- permission denied/restricted;
- no camera device;
- capture-session configuration failure;
- Vision request failure;
- gallery cancellation;
- unreadable/unsupported selected image;
- OCR result with no text;
- OCR text with no valid panic signature;
- user cancellation from review;
- app background/foreground transitions;
- torch unavailable/configuration failure.

No failure may silently mutate deterministic diagnosis state.

## QA plan

### Shared/JVM + Kotlin/Native regression

Required exact-head gates:

- `KMP I0 Baseline Verification` SUCCESS;
- `KMP I7 Native Equivalence` SUCCESS;
- COMMON OCR cleanup tests remain green on JVM and Kotlin/Native;
- no deterministic coverage threshold reduction.

If a Swift-friendly `NativeOcrFacade` is introduced, add COMMON tests proving its output maps directly from `OcrLogExtractor` semantics.

### iOS unit tests

Use injectable/native test seams for deterministic tests of:

- authorization state transitions;
- capture unavailable/error states;
- OCR service success/failure/no-text behavior;
- gallery cancel/error/success orchestration;
- raw Vision text -> COMMON cleanup bridge;
- review edits preserved into diagnosis input;
- no auto-diagnosis before explicit user action;
- lifecycle start/stop/restart logic;
- torch availability/failure handling where the adapter exposes it.

### Vision integration test

Add at least one real Vision still-image integration test or equivalent executable evidence demonstrating that representative high-contrast Panic Full text can be recognized and then accepted by COMMON cleanup.

The assertion should focus on stable diagnostic tokens rather than pixel-perfect OCR text if the Vision model can vary across OS/toolchain revisions.

### XCUITest / simulator smoke

Because the simulator does not provide an honest physical camera validation, UI automation must use a test seam rather than pretending camera hardware exists.

Required simulator UI evidence:

- camera/OCR entry control is accessible;
- denied/unavailable fallback is usable;
- gallery/review path can be exercised with controlled test input;
- cleaned OCR text is editable;
- explicit diagnose action reaches the existing diagnostic flow;
- cancellation leaves prior diagnostic input/state safe.

Any test-only scanner injection must be compile-time/debug/test gated and unreachable in normal release behavior.

### Xcode build evidence

Required before merge:

- `Shared.framework` link SUCCESS;
- iOS simulator build SUCCESS;
- generic-device build without signing SUCCESS;
- XCTest/XCUITest SUCCESS;
- exact-head evidence artifact archived where the current workflow supports it.

### Physical iPhone smoke — required for camera acceptance

TASK-KMP-082 must not be considered fully accepted from simulator evidence alone.

Before final merge approval, perform a physical-device smoke on the dedicated iPhone covering:

1. first camera permission prompt;
2. live rear-camera preview;
3. OCR of representative Panic Full text from another screen or printed sample;
4. review/edit of recognized text;
5. explicit diagnosis from reviewed OCR text;
6. permission-denied recovery path where practical;
7. gallery image OCR;
8. background/resume without stuck session;
9. torch toggle when the device reports torch support;
10. no unexpected photo persistence.

Record the device/OS, observed result and PASS/FAIL checklist without committing personal/customer images or logs.

## Android regression boundary

TASK-KMP-082 must not modify:

- Android CameraX/ML Kit scanner behavior;
- Android Room/schema;
- Android DataStore;
- Android navigation except if a shared source move is separately justified;
- existing deterministic engine semantics;
- canonical Rule Pack contents.

The Android scanner remains the product reference for capability intent, not a requirement to share platform capture code.

## Explicitly out of scope

- persistence/history;
- Room KMP adoption;
- PDF/report export;
- AI repair guidance;
- rule-pack import/management;
- trends;
- remote/cloud OCR;
- storing captured photos;
- changing Panic Full parser/engine semantics;
- the separate Panic Full hardening findings tracked outside I8;
- `SoftwareDevelopmentBlueprint` changes.

## Proposed implementation slices after explicit authorization

If TASK-KMP-082 is explicitly authorized, keep implementation reviewable in this order:

1. Swift/KMP OCR bridge and deterministic test seam, if needed;
2. Vision still-image OCR service + gallery path;
3. AVFoundation live camera + permission/lifecycle handling;
4. review/edit UI + handoff to existing diagnosis flow;
5. Xcode/Native/Android regression closure;
6. physical iPhone smoke;
7. documentation closeout only after implementation merge.

These slices may live in one focused PR if the final diff remains reviewable, or a very small PR series if camera lifecycle complexity justifies separation.

## Authorization boundary

This document is planning evidence only.

**TASK-KMP-082 remains NOT AUTHORIZED FOR IMPLEMENTATION until Luis explicitly authorizes TASK-KMP-082 after reviewing this scope.**
