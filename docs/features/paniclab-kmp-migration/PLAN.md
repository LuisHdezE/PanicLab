# PLAN: PanicLab KMP Migration

**Reference SPEC:** `docs/features/paniclab-kmp-migration/SPEC.md`
**Reviewed SPEC version:** `dd3af741ce7c7813be665411ef84cefe285199b7` / approved 2026-09-18
**State:** Approved
**Approved by:** Luis on 2026-09-18

> This document defines HOW the approved SPEC will be implemented. PLAN approval authorizes generation of execution TASKS, but implementation remains a separate explicit authorization.

## Verified technical context

| Existing component | Verified path | Target today | Current responsibility |
| --- | --- | --- | --- |
| Android application | `app` | ANDROID | Single application module, Compose UI and platform composition root. |
| Domain models | `app/src/main/java/com/example/domain/model` | ANDROID/JVM | Diagnostic models, rule-pack models and some UI/network-oriented models currently mixed together. |
| Repository contracts | `app/src/main/java/com/example/domain/repository/Repositories.kt` | ANDROID/JVM | Diagnostic, knowledge-base and settings contracts; currently leaks `RulePackEntity`. |
| Diagnostic engine | `app/src/main/java/com/example/diagnostic` | ANDROID/JVM | Sensor extraction, deterministic rule evaluation, ranking and report building. |
| Parsing | `app/src/main/java/com/example/parser` | ANDROID/JVM | Log normalization, metadata, device resolution, classification and evidence extraction. |
| Rule-pack utilities | `app/src/main/java/com/example/util` | ANDROID/JVM | JSON parsing, validation, diff, hash, redaction and hex utilities. |
| Android persistence | `app/src/main/java/com/example/data/local` | ANDROID | Room v3 database, DAOs/entities and DataStore preferences. |
| Android orchestration | `app/src/main/java/com/example/data/repository` | ANDROID | Executes deterministic pipeline, loads rules and persists reports. |
| OCR acquisition | `app/src/main/java/com/example/ocr/PanicLogImageAnalyzer.kt` | ANDROID | CameraX/ML Kit image-to-text path. |
| OCR text cleanup | `app/src/main/java/com/example/ocr/OcrLogExtractor.kt` | ANDROID/JVM | Deterministic cleanup and extraction from OCR-produced text. |
| AI repair guidance | `app/src/main/java/com/example/data/remote/GeminiRepairGroundingService.kt` | ANDROID | Network/Firebase AI assisted guidance, outside deterministic diagnosis. |
| Android UI/navigation | `app/src/main/java/com/example/ui`, `navigation` | ANDROID | Compose/Material 3 screens, ViewModels and navigation. |
| Existing deterministic QA | `app/src/test/java/com/example/DeterministicDiagnosticEngineTest.kt` | ANDROID/JVM/Robolectric | End-to-end deterministic diagnosis scenarios. |
| Rule-pack QA | `app/src/test/java/com/example/RulePackManagementTest.kt` | ANDROID/JVM/Robolectric | Parse, validate, diff and redaction scenarios. |
| Persistence QA | `app/src/test/java/com/example/RoomDiagnosticStorageTest.kt` | ANDROID/JVM/Robolectric | In-memory Room persistence behavior. |

## Target extraction matrix

| Responsibility | Required semantics | Proposed target | Why shared or native | Existing behavior to preserve |
| --- | --- | --- | --- | --- |
| Core diagnostic models | Same meaning on Android/iOS | `shared/commonMain` | Cross-platform contract | Panic families, confidence, verification, rules, evidence, candidates, repair flow. |
| Hex/sensor-code normalization | Exact numeric equivalence | `shared/commonMain` | Pure deterministic logic | Decimal/hex equivalence and bitmask semantics. |
| Log normalization | Identical | `shared/commonMain` | Pure deterministic logic | Escaped/newline normalization. |
| Metadata extraction | Identical | `shared/commonMain` | Deterministic parser | Current JSON/regex fallback semantics. |
| Panic classification | Identical | `shared/commonMain` | Deterministic parser | Existing family detection and UNKNOWN behavior. |
| Sensor extraction | Identical | `shared/commonMain` | Deterministic parser | Missing sensors, SMC arrays, fallback scans. |
| Device static resolution | Identical | `shared/commonMain` | Portable domain lookup | Current verified static map semantics. |
| Device persisted resolution | Same contract, native storage | common contract + Android adapter | Storage is platform concern | DB lookup before static fallback on Android. |
| Evidence extraction | Identical except generated IDs | `shared/commonMain` with injected ID source | Logic is portable; ID generation is capability | Evidence type/value/excerpt/line semantics. |
| Rule evaluation | Identical | `shared/commonMain` | Core business brain | Exact matches, profile scope, fallback, ranking. |
| Candidate ranking | Identical | `shared/commonMain` | Pure transformation | Primary/alternative candidate semantics. |
| Diagnostic report construction | Identical except clock/ID | `shared/commonMain` with injected clock/ID | Domain output shared; nondeterminism injected | Report content and safe fallbacks. |
| Rule-pack parsing | Identical domain meaning | `shared/commonMain` | Required by Android+iOS | Current schema interpretation without Room entity creation. |
| Rule-pack validation | Identical | `shared/commonMain` | Business rules | Validation errors/warnings/checksum semantics. |
| Rule-pack diff | Identical | `shared/commonMain` | Pure deterministic logic | Added/modified/deactivated/removed semantics. |
| Rule-pack hashing | Identical SHA-256 bytes | common contract + platform implementation initially | Avoid JVM-only `MessageDigest` leak | 64-char SHA-256 checksum. |
| OCR text cleanup | Identical | `shared/commonMain` after Regex portability work | Post-OCR text logic is platform-independent | Hex repair, keyword/code extraction, normalized log text. |
| Camera/image OCR | Native | Android app / iOS app later | Camera and OCR frameworks differ | Android CameraX/ML Kit remains; iOS requires separate evidence. |
| Room persistence | Native first | Android app | Highest data-migration risk | Existing history and settings must survive. |
| Settings DataStore | Native first | Android app | Preserve behavior before optional abstraction | Current preferences. |
| Gemini/network guidance | Native/capability adapter | Android app initially | Not part of deterministic diagnosis | Must never redefine diagnosis. |
| PDF/share/clipboard/filesystem | Native | Android app / iOS app as separately implemented | Platform APIs differ | Existing Android behavior preserved. |
| Compose UI | Native Android | `app` | Existing product surface | No unnecessary rewrite. |
| SwiftUI UI | Native iOS | `iosApp` | Approved native UX | Text diagnostic flow first. |
| Trend dashboard | Deferred extraction | Android app initially | Not required for first shared-core cutover and currently coupled to `org.json`/UI export | Android feature preserved while boundary is cleaned later. |

## Proposed solution

Adopt an **incremental strangler migration** rather than converting the existing `app` module in place.

1. Add a new KMP library module named `shared` beside the existing Android `app`.
2. Keep `app` as the Android host/composition root during migration.
3. Move only verified deterministic responsibilities into `shared/commonMain`, protected first by migrated fixture tests in `shared/commonTest`.
4. Remove JVM/Android-only dependencies from shared code through explicit boundaries rather than broad rewrites.
5. Cut Android orchestration over to the shared deterministic engine only after baseline-vs-shared equivalence is demonstrated.
6. Keep Room v3/DataStore in Android until schema lineage and non-destructive upgrade behavior are proven.
7. Add a native `iosApp` SwiftUI shell only after the common engine is stable, then prove the same diagnostic fixtures through the iOS-linked shared framework.
8. Migrate or add camera/OCR, persistence, AI, PDF/share and other iOS capabilities independently; no capability is declared portable merely because `shared` compiles.

The migration target is **shared semantics, not maximum shared-code percentage**.

## Source sets, modules and boundaries

| Module / source set | Existing / proposed | Responsibility | May depend on |
| --- | --- | --- | --- |
| `app` | Existing | Android application, Compose UI, Android ViewModels, Room/DataStore, CameraX/ML Kit, Gemini, file/share/PDF adapters | `shared`, Android/Google/Firebase libraries |
| `shared/commonMain` | Proposed | Portable domain models, deterministic parsers, diagnostic engine, rule-pack business behavior and common contracts | Kotlin stdlib, coroutines where justified, approved KMP-safe libraries only |
| `shared/commonTest` | Proposed | Deterministic fixtures, rule-pack fixtures, architecture-facing tests and common regression suite | `kotlin-test`, coroutine test support if needed |
| `shared/androidMain` | Proposed minimal | Android/JVM implementations of genuinely platform-bound shared contracts such as hashing/clock/ID if direct app injection is not preferable | Android/JVM APIs only when contract requires target implementation |
| `shared/iosMain` | Proposed minimal | iOS implementations of genuinely platform-bound shared contracts if needed | Apple/Kotlin-Native APIs only |
| `iosApp` | Proposed | Native SwiftUI app, iOS composition root and platform adapters | Generated `Shared` framework, SwiftUI/Foundation and capability-specific Apple frameworks |

**Dependency direction:** `app` and `iosApp` depend inward on `shared`; `shared/commonMain` never imports Android SDK, Room entities, Compose UI, Firebase, CameraX, ML Kit, `org.json`, `java.security`, `java.util.regex.Pattern`, or application-specific platform classes.

## Contracts and platform adapters

| Capability / contract | COMMON contract needed? | Android implementation | iOS implementation | Decision state |
| --- | --- | --- | --- | --- |
| Clock | Yes | system epoch-millis provider | Foundation/Kotlin-Native epoch provider | PROPOSED |
| ID generation | Yes | UUID-backed provider | NSUUID/Kotlin-Native provider | PROPOSED |
| SHA-256 | Yes | `MessageDigest` behind contract initially | CryptoKit/CommonCrypto-compatible adapter or validated KMP implementation | PROPOSED |
| Device lookup | Yes | shared static resolver plus Room-backed adapter | shared static resolver; persistence adapter only when iOS persistence exists | PROPOSED |
| Knowledge-base source | Yes | current Android repository maps Room/assets to shared domain | bundled canonical rule-pack source for initial iOS diagnostic flow | PROPOSED |
| Diagnostic persistence | Yes at application boundary | existing Room repository adapter | deferred until iOS persistence scope is approved | PROPOSED |
| Settings | Not required for first common-core cutover | existing DataStore | native iOS settings later if feature parity is required | PROPOSED |
| OCR image acquisition | No common image API initially | CameraX + ML Kit | Vision/AVFoundation only in later capability increment | PROPOSED |
| OCR text cleanup | No platform adapter after port | shared implementation | shared implementation | PROPOSED |
| AI repair guidance | No dependency from deterministic core | existing Gemini adapter | separate iOS adapter only when scoped | PROPOSED |
| File import/share/PDF | No common platform API initially | existing Android implementations | native iOS implementations when scoped | PROPOSED |

**Mechanism choice:** prefer constructor/interface injection for clock, ID, hash and repositories because they are capabilities needed by use cases and are easy to fake in tests. Use `expect/actual` only when a tiny platform primitive cannot be cleanly composed from the app and does not create hidden global state. Do not use `expect/actual` as the default architecture.

## Data and persistence

- **Logical models and contracts:** move deterministic core models to `shared/commonMain`; split UI/network-only models from core instead of carrying the entire current `DomainModels.kt` wholesale.
- **Local persistence:** retain existing Room v3 in Android for the first migration stages.
- **Remote data:** Gemini/network guidance stays outside deterministic shared diagnosis.
- **Mapping boundaries:** Android Room entities map to/from shared domain at the Android data layer. Shared repository contracts must expose domain data only.
- **Existing data compatibility:** mandatory for Android. `fallbackToDestructiveMigration()` is not accepted as migration evidence.
- **Migration strategy:** before any Room schema/engine change, recover verifiable schema history from repository/releases/backups/build artifacts where available. If lineage cannot be proven, preserve the existing Room implementation rather than guessing a migration.
- **Upgrade evidence:** versioned installed-upgrade tests with representative pre-existing rows; verify sessions, evidence, candidates and notes remain readable.

**Room KMP decision:** the laboratory proves Room 3 can work in KMP, but PanicLab will **not** adopt Room 3 KMP during the first diagnostic-core extraction. It becomes a later decision only after Android data-preservation evidence exists. This separates portability risk from data-migration risk.

## State, lifecycle and errors

| Concern | COMMON semantics | Android mechanism | iOS mechanism | Related RF/CA |
| --- | --- | --- | --- | --- |
| State retention | Shared engine is stateless per analysis request; deterministic result is explicit data | Existing ViewModel/state flow patterns | SwiftUI observable state owned by native app | RF-01/02/03, CA-01/05/08 |
| Background / foreground | No hidden mutable engine state | Android lifecycle/ViewModel | SwiftUI scene lifecycle | RF-02/03 |
| Cancellation | Cancellable orchestration may abort before persistence; engine output is never partially persisted as complete | Coroutines/ViewModel scope | Swift concurrency/task cancellation around framework call | RF-02/03 |
| Error recovery | Parsing/validation failures are domain-safe results/errors; platform failures do not mutate diagnosis | Existing UI error handling refined around shared result | Native SwiftUI error states | RF-01/03/06 |

## Native UX implementation

- **Android:** preserve existing Compose/Material 3 screens and navigation. Initial migration should change dependencies behind existing screens, not redesign them.
- **iOS:** native SwiftUI diagnostic entry screen with text/paste input, progress/error/non-conclusive/result states and native navigation. Pixel parity is explicitly unnecessary.
- **Shared UI:** No.

## Dependencies and compatibility

| Dependency / SDK | Used by | Existing / new | Android verified | iOS/Kotlin-Native verified | Reason |
| --- | --- | --- | --- | --- | --- |
| Kotlin Multiplatform plugin | `shared` | New in PanicLab | Lab verified | Lab verified | Creates common + Android/iOS targets. |
| Kotlin test | `commonTest` | New surface | Lab verified | Lab verified | Portable deterministic tests. |
| kotlinx.coroutines core/test | shared use cases/tests when asynchronous contracts require it | Existing on Android, expanded to KMP | Existing/lab verified | Lab verified | Flow/suspend contracts and tests. |
| Kover | shared JVM/Android host test coverage | New | Lab verified | Not used to fabricate iOS coverage | Truthful deterministic coverage gate. |
| `kotlinx.serialization-json` | rule-pack DTO parsing | Proposed new | Compatibility check required before implementation | Compatibility check required before implementation | Replace `org.json` in portable parsing without coupling domain to Room. |
| Room 2.x current | Android app | Existing | Verified by baseline tests | N/A | Preserve data during first stages. |
| Room 3 KMP | later persistence option | Deferred | Lab verified | Lab verified | Not adopted until PanicLab schema lineage is safe. |
| CameraX / ML Kit | Android app | Existing | Existing behavior | N/A | Native Android OCR acquisition. |
| SwiftUI/Foundation | iosApp | New | N/A | Required real build evidence | Native iOS shell. |

Exact dependency versions introduced into PanicLab must be compatibility-checked together before the scaffold PR. The laboratory versions are evidence, not an instruction to blindly upgrade PanicLab's Kotlin/Android stack in the same increment.

## Zero-cost CI and repository visibility

- **USD 0 is an approved constraint?** Yes.
- **Repository visibility:** Private at plan baseline.
- **Linux/Android execution path:** local WSL/self-hosted execution may carry common/Android gates without consuming hosted minutes; any GitHub-hosted configuration must report its actual billing/visibility assumptions.
- **macOS/iOS execution path:** not considered solved while PanicLab remains private. The public-lab hosted-macOS result cannot be relabeled as private-repo evidence.
- **Standard hosted runners only?** Public-lab path: yes. PanicLab private path: pending explicit visibility/runner decision.
- **If private, validated alternative for iOS builds:** PENDING. No zero-cost private iOS alternative is claimed by this PLAN.
- **Publication-readiness review required?** Yes before making the repository public. Secrets, customer/log data, proprietary material and accidental credentials must be audited first.

CI introduction is therefore staged: common/Android quality gates can be established first; release-grade iOS CI remains blocked until a truthful USD0 execution path is selected and validated.

## QA strategy

- **Feature criticality:** Critical, because wrong deterministic diagnosis can direct hardware repair decisions and because persistence contains user/workshop history.
- **Requirements coverage target:** 100% of approved CA mapped to evidence.
- **Changed deterministic code line coverage target:** >=90%.
- **Changed deterministic branch/decision coverage target:** >=85%.
- **Critical algorithm/rule scenario coverage:** target 95–100% scenario coverage for sensor-code normalization, exact-vs-bitmask precedence, fallback diagnosis, rule scope/ranking, rule-pack validation and redaction fixtures where practical.
- **Regression scope:** existing deterministic engine fixtures, rule-pack management, OCR text cleanup, Android repository orchestration, Room storage and current text diagnostic UI path.
- **Architecture checks:** fail when `shared/commonMain` imports Android SDK, Room entities, Compose UI, Firebase/Google platform APIs, `org.json`, or JVM-only APIs not explicitly allowed.
- **Static quality checks:** compilation with warnings reviewed, source-set dependency guard, duplicate/unused migration adapters reviewed per PR.
- **Android automated QA:** common tests plus Android host/JVM tests, existing Robolectric regressions, Room integration and selected Compose/navigation smoke scenarios.
- **iOS automated QA:** Kotlin/Native shared tests plus native Swift/XCTest or executable SwiftUI-host integration scenarios as appropriate; real Xcode build required.
- **Integration QA:** Android shared-engine cutover, canonical rule-pack loading, repository mapping and iOS framework invocation.
- **Accessibility QA:** native UI semantics for new iOS flow and regression checks on Android screens touched by migration.
- **Security/privacy QA:** redaction fixtures, no raw panic logs or customer data in test artifacts, no secrets committed, AI/network separated from deterministic diagnosis.
- **Performance/resource QA:** compare representative log-analysis latency and memory before/after Android cutover; detect gross regressions rather than optimize prematurely.
- **Device/OS matrix:** Android minSdk remains current 24 unless separately approved; Android emulator/host-test baseline plus at least one physical Android smoke before major cutover. iOS deployment target is selected during iOS scaffold compatibility preflight and recorded before implementation of native UI.
- **Release-blocking defect policy:** any deterministic semantic regression, data-loss path, architecture leak into common, failed iOS evidence claim, or coverage threshold failure blocks the relevant migration increment.
- **Coverage tooling and limitations:** Kover measures JVM/Android-host executable common code; Kotlin/Native/iOS evidence is reported separately. No global merged percentage.

## Validation and evidence plan

| CA | Target | Method | Environment | Evidence to record |
| --- | --- | --- | --- | --- |
| CA-01 | COMMON | Port baseline fixtures to `commonTest`; compare expected domain result | JVM/common host first, Kotlin/Native where executable | Test report + fixture mapping |
| CA-02 | COMMON | Decimal/hex property/fixture cases | common tests | Passing assertions + coverage |
| CA-03 | COMMON | Unknown SMC fixture | common tests | Safe fallback assertion |
| CA-04 | COMMON | Rule-pack parse/validate/diff fixtures | common tests | Validation/diff reports |
| CA-05 | ANDROID | Existing Android text diagnostic flow using shared engine | Android host/emulator | Test/log/screenshot evidence as appropriate |
| CA-06 | ANDROID | Regression suite each increment | WSL + Android test environment | Passing regression matrix |
| CA-07 | ANDROID | Installed-upgrade DB fixture when schema change is actually proposed | Android/Robolectric or instrumentation as appropriate | Before/after row assertions; schema evidence |
| CA-08 | IOS | SwiftUI host invokes shared engine on approved fixture | macOS/Xcode | XCTest/run evidence and domain-equivalent result |
| CA-09 | IOS | Build shared framework + iOS app + target tests | real macOS runner | Xcode build/test logs/artifacts |
| CA-10 | ANDROID/IOS | Capability-specific native tests | target-specific | Separate evidence per capability |
| CA-11 | COMMON | Kover on JVM-executable deterministic scope | WSL/Linux | line/branch XML/HTML reports |
| CA-12 | ANDROID/IOS | target scenario suites | each platform | per-surface test evidence |
| CA-13 | COMMON | source/dependency architecture guard | Linux/WSL CI | guard output |
| CA-14 | CI | inspect actual jobs/runners/visibility | GitHub/local runner evidence | workflow/run metadata and visibility statement |
| CA-15 | ANDROID | migration review gate | PR/QA review | explicit non-destructive evidence or block |

### Deterministic equivalence

- **Baseline behavior:** current deterministic Android implementation at `main@c165ae4283a3b592eddb2b70a1c13a9ffeb51f01`.
- **Representative inputs:** existing deterministic test logs, rule-pack fixtures, OCR cleanup fixtures, unknown-code cases and canonical bundled rule pack.
- **Relevant outputs to compare:** parsed metadata, device model, panic families, sensor codes, evidence meaning, matched/applied rule IDs, primary/alternatives, confidence, verification and repair flow.
- **Excluded nondeterminism and treatment:** generated IDs and timestamps are controlled through fixed injected test providers; persistence IDs are compared by meaning rather than random value when appropriate; AI/network output is excluded from deterministic equivalence.
- **Regression evidence:** during cutover, preserve expected-output fixtures and, where practical, run baseline and shared implementations against the same inputs before deleting the old implementation.

### Physical-device validation

- **Android:** Required before final Android core cutover because camera/file/lifecycle integration cannot be fully represented by common/JVM tests, even though the deterministic engine itself is host-testable.
- **iOS:** Required before claiming user-facing iOS support for native capabilities; initial shared-engine correctness still requires real macOS/Xcode build/test evidence even before every hardware capability exists.

## Implementation order

### I0 — Baseline freeze and fixture extraction
Capture the approved Android deterministic outputs as reusable fixtures. Add no KMP behavior yet. Establish regression inventory and architecture guard expectations.

### I1 — KMP scaffold without product cutover
Add `shared` with COMMON + Android + iOS targets and common test infrastructure. Keep Android behavior running from existing code. Prove compilation and test plumbing before moving business logic.

### I2 — Portable primitives and core domain
Extract core models, `HexUtils`, `LogNormalizer`, clock/ID/hash contracts and remove JVM-only defaults. Move tests first, then implementation. Keep Android adapters compiling against the new domain.

### I3 — Deterministic parsing and engine
Port metadata/classifier/sensor/evidence parsing, static device resolution, rule evaluation, ranking and report construction. Replace `Pattern`, `org.json`, UUID/time globals and other JVM-only seams. Achieve common coverage gates before Android cutover.

### I4 — Rule-pack core and boundary cleanup
Port rule-pack DTO parsing, validation and diff. Stop returning Room entities from shared/domain contracts. Define canonical rule-pack data source and mappings. Preserve Android installation/history behavior behind adapters.

### I5 — Android shared-engine cutover
Wire current Android orchestration to the shared analyzer while leaving Compose UI, Room, DataStore, CameraX/ML Kit, Gemini, PDF/share and navigation native. Run full Android regression and equivalence evidence. Remove old duplicated deterministic code only after the shared path proves equivalent.

### I6 — Persistence safety checkpoint
Recover/verify Room schema lineage. Add upgrade tests. Only if a persistence change is actually beneficial and non-destructive evidence exists, propose it in a focused increment. Room KMP adoption remains optional, not automatic.

### I7 — Native iOS diagnostic slice
Add `iosApp` SwiftUI text/paste diagnostic flow linked to `Shared`. Bundle/load the canonical rule pack through an iOS adapter, run the same approved fixture semantics, and collect real Xcode build/test evidence. This establishes the first honest iOS product slice without pretending camera/AI/PDF parity.

### I8 — Platform capability increments
Independently scope iOS camera/OCR, persistence/history, file import/share, PDF/export, settings and AI guidance. Each capability gets its own target evidence and can remain asymmetric until implemented.

Each increment should be a reviewable PR or very small PR series with a green gate before the next boundary is crossed.

## Risks and pending decisions

### Risks

- Semantic drift while replacing Java/JVM regex/JSON behavior with multiplatform implementations.
- Hidden coupling caused by the current large `DomainModels.kt` and repository contracts leaking Room entities.
- Data loss if Room migration is combined prematurely with KMP extraction.
- Version-stack churn if PanicLab upgrades Kotlin/AGP/Room and introduces KMP in a single jump.
- Rule-pack resource drift between Android and iOS if a single canonical source is not enforced.
- False confidence from JVM coverage if Kotlin/Native/iOS execution evidence is omitted.
- Private-repository macOS CI cost/availability remaining unresolved.

### Pending decisions before their relevant increment

- Repository visibility for iOS hosted CI. No visibility change is authorized by this PLAN alone.
- Exact compatible versions of Kotlin Multiplatform, serialization and Kover after preflight against PanicLab's current AGP/Kotlin stack.
- Exact iOS minimum deployment target after the iOS scaffold preflight.
- SHA-256 iOS adapter implementation choice after a tiny compatibility spike; checksum semantics themselves are already fixed.
- Whether Room KMP provides enough value to justify a later persistence migration after schema lineage is proven.
- Capability-by-capability iOS parity order after the first native diagnostic slice.

## Approval gate

This PLAN is **Approved** by Luis on 2026-09-18.

PLAN approval authorizes generation of execution TASKS. It does not authorize implementation, repository visibility changes, destructive data operations, Blueprint changes or merge.