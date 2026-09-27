# Android Diagnostics · BugReport Expansion Scope

Date: 2026-09-27
Repository: `LuisHdezE/PanicLab`
Baseline: `main@541913c19eff95876bf55981be63c70da87c5459`
Status: SCOPE / BASELINE ONLY

## 1. Purpose

Extend PanicLab from Apple Panic Full analysis into a multi-platform mobile diagnostic-forensics product by adding a first Android ingestion lane centered on `adb bugreport` / Android bug report packages.

This scope intentionally preserves the existing deterministic Apple diagnostic engine and Apple Official Knowledge firewall. Android support starts as a separate, reviewable lane and must not alter existing Apple diagnosis semantics.

## 2. Product principle

PanicLab must identify the evidence type before attempting diagnosis.

Target flow:

```text
Diagnostic input
  -> Artifact detection
  -> Platform/family routing
  -> Artifact-specific parsing
  -> Normalized evidence
  -> Deterministic rules
  -> Ranked candidates
  -> Diagnostic report
```

The Android lane must reuse portable diagnostic primitives where they are genuinely common, but must not force Apple-specific concepts into Android artifacts.

## 3. First supported Android source

The first production source is a user-supplied Android Bug Report generated either from Developer Options or through:

```bash
adb bugreport report.zip
```

Initial supported containers:

- `.zip` Android bug report package;
- extracted `.txt` bugreport main report;
- standalone `.log` / `.txt` artifacts when confidently identifiable.

Direct privileged access to `/data/tombstones`, `/data/system/dropbox`, `/sys/fs/pstore`, or root-only paths is explicitly out of scope for the first increment.

## 4. Artifact taxonomy

Introduce a platform-neutral artifact classification contract. Exact naming may change during implementation review, but the behavior must distinguish at least:

```text
APPLE_PANIC_FULL
APPLE_IPS_CRASH
ANDROID_BUGREPORT
ANDROID_KERNEL_PANIC
ANDROID_TOMBSTONE
ANDROID_ANR
ANDROID_JAVA_CRASH
ANDROID_NATIVE_CRASH
ANDROID_LOGCAT
ANDROID_DUMPSYS
UNKNOWN
```

A detector must return both the detected type and evidence supporting the decision. Detection must fail closed to `UNKNOWN` when confidence is insufficient.

## 5. Android diagnostic session

A BugReport must not be treated as one giant text blob. Import must produce a diagnostic session containing normalized metadata and zero or more typed artifacts.

Minimum session metadata:

- manufacturer / brand when available;
- model / product / device identifiers;
- Android release / SDK level when available;
- build fingerprint / build ID when available;
- kernel version when available;
- security patch when available;
- report generation timestamp when available.

Initial artifact families to inventory from the package/report:

- kernel / reboot / watchdog evidence;
- tombstones / native crash evidence;
- ANR evidence;
- Java/Kotlin crash evidence;
- logcat excerpts relevant to fatal/reboot events;
- dumpsys evidence relevant to battery, thermal, storage and services;
- modem/radio evidence when present;
- camera-service / HAL evidence when present.

## 6. Initial deterministic diagnostic families

The first Android rule increment must be deliberately narrow. Initial families:

1. `KERNEL_OR_WATCHDOG`
2. `CAMERA_SUBSYSTEM`
3. `MODEM_OR_RADIO`
4. `THERMAL`
5. `STORAGE_OR_IO`

A rule may identify a subsystem failure pattern without claiming that a physical component is definitively defective.

Required candidate categories must preserve uncertainty, for example:

- software probable;
- driver/HAL probable;
- communication/interconnect probable;
- hardware probable;
- indeterminate.

## 7. Evidence and temporal correlation

Android diagnosis should support event correlation across multiple artifacts.

Where timestamps are available, the session should be able to construct a normalized incident timeline such as:

```text
camera HAL timeout
-> cameraserver crash
-> service restart
-> repeated HAL timeout
-> watchdog
-> reboot
```

The diagnostic engine may use repeated correlated evidence to raise confidence. A single crash or ANR must not automatically become a hardware diagnosis.

## 8. Rule packs

Android rules must not be hardcoded into Compose UI or Android platform code.

Planned rule-pack hierarchy:

```text
Android Generic
Android Samsung
Android Xiaomi
Android Motorola
Android Pixel
...
```

Vendor/model packs may refine generic rules but must not silently override evidence provenance.

Apple rule packs and Apple Official Knowledge remain isolated from Android deterministic rules.

## 9. AI boundary

AI must not own the base diagnosis.

The deterministic engine owns:

- detected artifact type;
- extracted evidence;
- matched rule IDs;
- ranked candidates;
- score/confidence;
- primary deterministic diagnosis.

AI may later explain findings, summarize long evidence, correlate documentation, or propose human-readable repair steps, but it must not silently replace deterministic output.

## 10. Security and privacy

Bug reports can contain sensitive device and application information. The first implementation must therefore:

- process locally by default;
- avoid uploading report contents unless a separately authorized feature explicitly requires it;
- avoid persisting the original report unless the user explicitly chooses to save it;
- redact obvious secrets/tokens where practical before any export/share path;
- impose explicit archive/file size limits;
- reject path traversal / zip-slip entries;
- reject decompression bombs using bounded entry count, total expanded bytes and compression-ratio guards;
- fail safely on malformed archives or unsupported encodings.

## 11. Architecture boundary

Target conceptual contracts:

```kotlin
enum class DiagnosticArtifactType

data class ArtifactDetection(
    val type: DiagnosticArtifactType,
    val confidence: Double,
    val reasons: List<String>
)

interface DiagnosticArtifactDetector {
    fun detect(input: DiagnosticInput): ArtifactDetection
}

interface DiagnosticArtifactParser<T> {
    fun parse(input: DiagnosticInput): T
}
```

These are conceptual only. Final implementation must follow the existing KMP/common architecture and naming conventions already present in PanicLab.

## 12. Proposed execution increments

### ADX-000 · Discovery and baseline

Status: IN PROGRESS

Goals:

- inventory the current shared parsing/diagnostic boundaries;
- inventory current Android file-import capabilities;
- prove that new Android routing can be added without changing Apple deterministic outputs;
- identify reusable common contracts versus Android-only archive/file intake.

No production behavior change.

### ADX-001 · Artifact detector contracts and fixtures

Goals:

- add platform-neutral artifact-type/domain contracts;
- add sanitized fixtures for Apple Panic Full, Android BugReport text, tombstone, ANR, logcat and unknown input;
- implement deterministic type detection;
- prove existing Apple inputs still route identically.

No Android diagnosis yet.

### ADX-002 · Android BugReport safe importer

Goals:

- accept `.zip` and extracted text reports;
- bounded safe archive extraction;
- locate/report primary text plus known diagnostic artifacts;
- create `AndroidDiagnosticSession` metadata and artifact inventory.

No subsystem diagnosis yet.

### ADX-003 · Android evidence extraction

Goals:

- extract normalized reboot/watchdog, crash, ANR, thermal, storage/I/O, camera and modem evidence;
- preserve source artifact and line/section provenance;
- build optional timestamp correlation.

### ADX-004 · Android deterministic rules v1

Goals:

- implement only the five approved initial families;
- introduce Android Generic rule pack;
- rank candidates conservatively;
- keep hardware claims probabilistic unless evidence is explicit.

### ADX-005 · Android native UX

Goals:

- import/share entry point;
- artifact/session summary;
- evidence timeline;
- diagnosis with confidence, matched evidence and next test;
- clear distinction between deterministic result and explanatory AI text.

## 13. QA gates

Every implementation increment must preserve existing Apple behavior and add its own regression evidence.

Minimum gates:

- KMP common architecture guard green;
- shared deterministic tests green;
- existing Apple fixture equivalence green;
- Android-host tests green where applicable;
- archive-security tests for malformed ZIP, zip-slip, excessive expansion and unsupported files;
- deterministic fixture tests for each supported Android artifact family;
- no existing Apple Rule Pack / AOK firewall regression;
- exact-head CI evidence before merge.

Coverage expectations for changed deterministic common code remain aligned with the existing project gates: at least 90% line and 85% branch where the current KMP coverage policy applies.

## 14. Explicit exclusions from the first implementation

- root acquisition;
- privileged direct reading of Android system paths;
- bootloader / fastboot automation;
- automatic repair actions;
- cloud upload of raw bug reports;
- AI-generated base diagnoses;
- new Room schema unless separately approved;
- changes to existing Apple diagnostic semantics;
- changes to Apple Official Knowledge behavior;
- merge of PR #28 or modification of its pending physical acceptance gate;
- SoftwareDevelopmentBlueprint changes.

## 15. Governance

- inspect before modify;
- incremental, reviewable changes only;
- no big-bang parser rewrite;
- Apple behavior remains the regression baseline;
- every new behavior requires sanitized fixtures and deterministic QA;
- merge requires explicit approval from Luis;
- this scope document does not itself authorize merge of any implementation PR.

## 16. Immediate next checkpoint

Complete ADX-000 by auditing the exact `main@541913c19eff95876bf55981be63c70da87c5459` source tree and produce a concrete file-level implementation map for ADX-001, including:

- new files;
- existing files to touch;
- dependency direction;
- test fixtures;
- CI/QA gates;
- explicit list of files that must remain unchanged.
