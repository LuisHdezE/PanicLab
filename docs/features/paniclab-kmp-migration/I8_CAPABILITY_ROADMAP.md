# I8 Capability Roadmap — PanicLab KMP

**Baseline:** `main@1fa8d73a7e4fdfe9c56ba8cc067ce8d596be801e`
**Precondition:** I7 is DONE/CLOSED. TASK-KMP-070/071/072 are merged and documented.
**State:** ACTIVE — TASK-KMP-080 and TASK-KMP-081 DONE; remaining capability increments require separate authorization.
**Authorization boundary:** Luis authorized the I8 roadmap and separately authorized TASK-KMP-081. No later capability is authorized merely by appearing here.

## 1. Purpose

I8 closes platform capability gaps after the shared deterministic engine and the native iOS text/paste slice have already been proven through real Kotlin/Native and Xcode execution.

I8 is deliberately not a single parity mega-PR. Each capability is an independently reviewable increment with its own scope, native lifecycle/permission/error behavior, QA evidence and separate explicit approval gate.

The migration target remains **shared semantics where they add value, native platform implementation where platform APIs are the correct boundary**. Maximum shared-code percentage is not a goal.

## 2. Verified product baseline

### Android surfaces already present

The Android navigation/product currently exposes real surfaces for:

- file import;
- paste-log diagnosis;
- camera scanner;
- result/evidence/log views;
- history and case detail;
- PDF/report export;
- knowledge base and rule-pack management;
- settings;
- panic trends dashboard;
- AI-assisted repair suggestions from result/history flows.

The Android OCR acquisition path uses CameraX + ML Kit, while deterministic post-OCR cleanup has already moved to COMMON. Android persistence remains Room-based. Android settings remain platform-native. AI guidance remains outside the deterministic diagnosis core.

### iOS surface now present

The native iOS app contains the approved SwiftUI diagnostic slice plus the first I8 capability increment:

- text/paste input;
- native Files/document import for `.ips`, `.txt`, `.log` and `.json`;
- safe 5 MiB maximum import boundary;
- UTF-8 input plus BOM-gated UTF-16 LE/BE input;
- loading/error/non-conclusive/result states;
- canonical bundled Rule Pack loading;
- `NativeDiagnosticFacade` integration;
- real XCTest/XCUITest evidence;
- iOS deployment baseline 15.0.

The iOS app does **not** yet claim camera/OCR acquisition, persisted history, PDF/export/share, settings parity, knowledge-base/rule-pack management parity, AI guidance or trends parity.

## 3. Cross-cutting rules for every I8 capability

1. **Explicit approval per capability.** `adelante`, `seguimos` or approval of this roadmap does not authorize implementation of the next capability unless Luis explicitly authorizes that capability increment.
2. **No silent deterministic drift.** Platform work may feed or present the shared deterministic engine but must not alter diagnosis semantics without a separately approved behavior change.
3. **Android regression remains mandatory.** iOS capability work must not break the existing Android product.
4. **Real iOS evidence is mandatory.** Compilation alone is insufficient. Target-relevant XCTest/XCUITest or executable host evidence is required.
5. **iOS 15.0 remains the deployment floor** unless separately reconsidered with compatibility evidence.
6. **No combined fake coverage.** JVM/Kover, Kotlin/Native and Swift/Xcode evidence remain separately reported.
7. **No Room schema change is implied.** Android Room v3 and the I6 safety conclusions remain untouched unless a persistence-specific increment explicitly changes them.
8. **Room KMP remains ADOPT LATER.** Re-entry requires a dedicated decision checkpoint before implementation.
9. **Privacy/security by default.** No real customer panic logs, secrets, signing material or private workshop data may be added to fixtures/artifacts.
10. **Blueprint remains out of scope.** `SoftwareDevelopmentBlueprint` is not modified by I8 unless separately authorized.

## 4. Execution order and current status

| Order | Task | Capability | Android baseline | iOS target state | Risk | Dependencies | Implementation authorization |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | TASK-KMP-081 | File import / external document intake | Existing `ImportFileScreen` | **DONE** | Low–Medium | I7 | **AUTHORIZED / MERGED** |
| 2 | TASK-KMP-082 | Camera + OCR acquisition | CameraX + ML Kit + shared OCR cleanup | TODO | Medium | 081 optional, shared OCR cleanup already available | NOT AUTHORIZED |
| 3 | TASK-KMP-083 | Persistence strategy re-entry decision | Room v3 retained | Decision only | High | I6 ADR + I7 | NOT AUTHORIZED |
| 4 | TASK-KMP-084 | iOS persistence + history | Room-backed history/case detail | TODO | High | 083 | NOT AUTHORIZED |
| 5 | TASK-KMP-085 | Settings | DataStore/native settings | TODO | Low–Medium | I7; 084 only if settings affect persisted history | NOT AUTHORIZED |
| 6 | TASK-KMP-086 | PDF/report export + native share | Existing export surface | TODO | Medium | 084 for exporting historical sessions; otherwise I7 | NOT AUTHORIZED |
| 7 | TASK-KMP-087 | Knowledge base read-only UX | Existing KB/rule detail | TODO | Medium | I7 canonical Rule Pack | NOT AUTHORIZED |
| 8 | TASK-KMP-088 | Rule-pack import/management | Existing manage-rule-pack flow | TODO | Medium–High | 081 + persistence decision where persistence is required | NOT AUTHORIZED |
| 9 | TASK-KMP-089 | AI repair guidance | Existing Android Gemini-backed capability | TODO | High | stable result/history surfaces | NOT AUTHORIZED |
| 10 | TASK-KMP-090 | Trends dashboard | Existing Android trends dashboard | TODO | Medium–High | 084 persisted history | NOT AUTHORIZED |

The order above minimizes risk by taking stateless/native-input capabilities before data persistence, then layering stateful/export/management features, and leaving AI/trends until the product data surfaces they depend on are stable.

## 5. TASK-KMP-081 — iOS file import / external document intake

**Status:** DONE / MERGED via PR #25 as `1fa8d73a7e4fdfe9c56ba8cc067ce8d596be801e`.

**Objective:** Let iOS accept a panic log from Files or another supported external document source and feed the exact existing diagnostic pipeline.

### Implemented scope

- native SwiftUI `fileImporter` document flow;
- supported inputs `.ips`, `.txt`, `.log`, `.json`;
- 5 MiB maximum;
- UTF-8 accepted directly;
- UTF-16 LE/BE accepted only with explicit BOM and BOM consumed before decoding;
- arbitrary non-UTF-8 bytes without a valid BOM fail closed instead of receiving heuristic UTF-16 interpretation;
- explicit handling of unavailable/inaccessible, unsupported, empty and oversized files;
- cancellation preserves existing input and returns safely to idle;
- imported text passes through the same `DiagnosticViewModel -> NativeDiagnosticFacade -> shared deterministic engine` path as paste input;
- successful import does not auto-persist or auto-analyze;
- no history persistence, PDF generation or rule-pack import was introduced.

### Executed acceptance evidence

- exact implementation head: `dcae6855a9f290abc264a2978c9ff3bb3b7028ce`;
- `KMP I0 Baseline Verification` run `35489568505` SUCCESS;
- `KMP I7 Native Equivalence` run `35489568540` SUCCESS;
- `KMP I7 Native iOS Slice` run `35489568509` SUCCESS;
- simulator build SUCCESS;
- generic-device build without signing SUCCESS;
- native unit + accessibility UI smoke tests SUCCESS;
- import-versus-paste deterministic diagnosis equivalence XCTest SUCCESS;
- cancellation, unsupported extension, empty input, oversize, unreadable input, positive BOM-marked UTF-16 and fail-closed non-BOM binary cases covered by XCTest;
- iOS `.xcresult` artifact `10599226050`, digest `sha256:17653c50580ee0855638b1711298f6ac7b692cac8e663b5672fa22d17e376f76`;
- native-equivalence artifact `10598792265`, digest `sha256:37a159af8dcc938def6c4581f08d2c8ee7116c5614102e00376cde436960801f`;
- implementation evidence document: `I8_081_FILE_IMPORT.md`;
- PR #25 merged after explicit approval as `1fa8d73a7e4fdfe9c56ba8cc067ce8d596be801e`.

No Android behavior, Room/schema, deterministic engine semantics, canonical Rule Pack contents, camera/OCR, persistence/history, PDF/export, AI guidance or Blueprint content changed in TASK-KMP-081.

## 6. TASK-KMP-082 — iOS camera + OCR acquisition

**Status:** TODO / NOT AUTHORIZED.

**Objective:** Add native image-to-text acquisition while preserving shared post-OCR cleanup and deterministic diagnosis.

### Scope

- native camera permission lifecycle;
- image capture / scanning UX;
- Apple-native OCR path compatible with iOS 15.0;
- OCR output handed to the existing shared OCR-text cleanup before diagnosis;
- loading/cancel/error/no-text states;
- no persistent photo storage by default;
- no change to Android CameraX/ML Kit implementation;
- no change to deterministic engine semantics.

### Acceptance evidence

- permission granted and denied paths;
- cancellation/background/foreground behavior where relevant;
- representative OCR fixture reaches shared cleanup and diagnosis;
- no-text/nonconclusive behavior is safe;
- real simulator/device-capable Xcode evidence as feasible for the camera boundary, with any simulator limitation documented instead of hidden;
- Android scanner regression remains green.

## 7. TASK-KMP-083 — persistence strategy re-entry decision

**Status:** TODO / NOT AUTHORIZED.

**Objective:** Re-enter the I6 `ADOPT LATER` decision only because native iOS history creates a concrete persistence need.

This task is a decision checkpoint, not a persistence implementation.

### Options to evaluate

- keep Android Room unchanged and use a native iOS persistence adapter;
- adopt Room KMP for new cross-platform persistence only if migration/data-safety prerequisites are satisfied;
- another native storage mechanism only if it satisfies the same domain contract and deployment constraints.

### Required decision criteria

- iOS 15.0 compatibility;
- Android existing-data safety;
- schema/versioning/migration tooling;
- ability to preserve sessions, evidence, candidates, notes and raw-log boundaries;
- testability on both platforms;
- maintenance cost and failure recovery;
- no invented historical Android migrations.

### Acceptance evidence

A focused ADR/decision note with one of:

- **KEEP PLATFORM-NATIVE**, or
- **ADOPT ROOM KMP NOW**, or
- **BLOCKED / NEED MORE EVIDENCE**.

No database code may be changed by this decision-only task.

## 8. TASK-KMP-084 — iOS persistence + history

**Status:** TODO / NOT AUTHORIZED.

**Objective:** Persist diagnostic sessions on iOS and expose native history/case-detail flows without weakening Android data safety.

### Scope

- persistence adapter selected by TASK-KMP-083;
- save successful/nonconclusive diagnostic sessions according to approved product semantics;
- list history;
- reopen case detail;
- evidence/log access from persisted session;
- technician notes where supported by current domain contracts;
- delete/retention behavior explicitly defined before implementation;
- corruption/open-failure handling;
- migration/versioning behavior for the selected iOS store.

### Acceptance evidence

- save → terminate/reopen → read round trip;
- representative report fields preserved;
- history ordering/filter semantics documented and tested;
- no partial session is presented as complete after interrupted persistence;
- Android Room safety tests remain green;
- if Android schema is touched, I6 safety requirements become blocking again.

## 9. TASK-KMP-085 — iOS settings

**Status:** TODO / NOT AUTHORIZED.

**Objective:** Add only settings that have real product meaning on iOS, using a native settings store unless a shared semantic contract is justified.

### Scope candidates

- presentation/preferences already meaningful in Android;
- privacy-related local preferences;
- feature toggles that actually exist in the product;
- no invented settings solely to mimic Android screen count.

### Acceptance evidence

- persistence across app relaunch;
- default values and reset behavior;
- invalid/legacy preference handling if applicable;
- no deterministic diagnosis behavior changes from presentation-only settings.

## 10. TASK-KMP-086 — PDF/report export + native share

**Status:** TODO / NOT AUTHORIZED.

**Objective:** Export a diagnostic report in a technician-usable form and share/save it through iOS-native mechanisms.

### Scope

- text/PDF report formatting boundary reviewed for what can remain shared vs native;
- native PDF generation compatible with iOS 15.0;
- share sheet / Files save behavior;
- deterministic diagnosis content is read-only input to export;
- failure/cancel states;
- sensitive/raw-log content policy explicitly defined.

### Acceptance evidence

- generated document opens successfully;
- required report fields are present and stable;
- share cancellation is harmless;
- export does not mutate session/diagnosis;
- historical-session export works only after TASK-KMP-084 if that path is included.

## 11. TASK-KMP-087 — iOS knowledge base read-only UX

**Status:** TODO / NOT AUTHORIZED.

**Objective:** Expose the canonical Rule Pack knowledge base natively without yet adding install/update management.

### Scope

- list/search rules where current product semantics support it;
- rule detail;
- current Rule Pack version/source metadata;
- use the same canonical parsed domain data already proven in COMMON;
- no remote update/install in this task.

### Acceptance evidence

- canonical bundled Rule Pack renders successfully;
- representative rule metadata/detail matches COMMON parsing semantics;
- unknown/missing rule states are safe;
- Android knowledge-base behavior is unchanged.

## 12. TASK-KMP-088 — iOS rule-pack import/management

**Status:** TODO / NOT AUTHORIZED.

**Objective:** Add native management only after read-only knowledge-base behavior and file intake are stable.

### Scope

- import candidate Rule Pack;
- shared validation/diff before activation;
- checksum/source/version display;
- rejection of invalid/unsafe packs;
- activation/rollback semantics defined explicitly;
- persistence location depends on TASK-KMP-083 decision if durable installation is required.

### Acceptance evidence

- valid pack install/activate path;
- invalid SemVer/source/duplicate/checksum cases rejected according to shared validation semantics;
- diff result matches COMMON tests;
- app restart preserves active pack only if durable install is in approved scope;
- canonical bundled fallback remains available.

## 13. TASK-KMP-089 — iOS AI repair guidance

**Status:** TODO / NOT AUTHORIZED.

**Objective:** Add optional AI-assisted repair guidance without allowing AI output to redefine deterministic diagnosis.

### Hard boundary

AI remains an **advisory capability**. The deterministic diagnosis, evidence, confidence and rule-derived result remain authoritative product data.

### Scope

- capability adapter chosen for iOS;
- explicit network/loading/offline/error states;
- sanitized/minimized prompt context;
- no secret API material committed to repository;
- no raw customer logs uploaded unless separately approved with privacy design;
- user-visible separation between deterministic diagnosis and AI suggestion.

### Acceptance evidence

- success/offline/provider-failure paths;
- no mutation of deterministic report fields;
- privacy fixture proves only approved context leaves the device;
- secrets provided through secure runtime configuration;
- Android AI behavior remains unchanged unless separately scoped.

## 14. TASK-KMP-090 — iOS trends dashboard

**Status:** TODO / NOT AUTHORIZED.

**Objective:** Add native trend analysis only after iOS persistence/history provides a real local dataset.

### Scope

- derive trends from persisted diagnostic history;
- clarify what aggregation semantics are portable and what UI/export pieces remain native;
- native SwiftUI dashboard;
- navigation back to supporting history/rule detail where available;
- no invented cross-device analytics backend.

### Acceptance evidence

- deterministic aggregation fixtures where applicable;
- empty/small/representative datasets;
- persisted history source is the only claimed data source unless a new source is separately approved;
- Android trend behavior remains unchanged.

## 15. Deferred/non-parity hardening queue

The following findings are intentionally **not** mixed into the I8 platform-parity tasks because they are deterministic/ingestion hardening concerns rather than missing iOS capabilities:

- character-spaced log ingestion;
- broad secondary panic-family false positives;
- fallback SMC-code false positives;
- cosmetic OS-version duplication.

They require their own behavior-change scope, fixtures and approval before engine/parser semantics change.

## 16. Approval flow

For every capability:

1. audit exact Android behavior and current shared contracts;
2. write capability-specific scope/acceptance note if needed;
3. obtain explicit user authorization for that capability;
4. implement on a dedicated branch/PR;
5. run target-specific QA plus Android/common regressions;
6. obtain explicit merge approval;
7. reconcile documentation;
8. move to the next capability only after the previous boundary is closed or an intentionally dependency-free task is separately approved.

TASK-KMP-081 followed this flow and is now closed. This does not authorize the next task automatically.

## 17. Immediate next decision

The next proposed implementation increment is:

**TASK-KMP-082 — iOS camera + OCR acquisition.**

It can reuse the COMMON post-OCR cleanup already proven while keeping camera and Apple OCR acquisition native. It must preserve Android CameraX/ML Kit behavior and deterministic semantics.

**TASK-KMP-082 is NOT AUTHORIZED.** It requires separate explicit authorization from Luis before implementation begins.
