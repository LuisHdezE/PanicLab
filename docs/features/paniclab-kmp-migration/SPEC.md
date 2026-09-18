# SPEC: PanicLab KMP Migration

**State:** Approved
**Approved by:** Luis
**Approval date:** 2026-09-18

> This document defines WHAT must be true. It does not authorize implementation and does not define source-set layout, classes, framework wiring, adapters or file structure. Only Luis can approve this SPEC.

## Product goal

Evolve PanicLab from its current Android-only implementation into a Kotlin Multiplatform product that shares the diagnostic/business brain when behavior must be identical, while preserving native Android and iOS platform experiences.

The migration must preserve the trusted deterministic diagnostic behavior that exists today, avoid regression of the Android application, and establish evidence-based Android+iOS support rather than declaring portability from compilation alone.

## Verified current behavior

At baseline `main@c165ae4283a3b592eddb2b70a1c13a9ffeb51f01`:

- PanicLab is an Android application with a single `:app` Gradle module.
- The current diagnostic flow normalizes a raw Panic Full log, extracts metadata, resolves the device, classifies panic families, extracts sensors/evidence, evaluates diagnostic rules, ranks candidates, builds a diagnostic report and persists the result.
- The rule-pack subsystem supports parsing, validation, version comparison/diff and installation-related behavior.
- Diagnostic history is persisted with Room.
- The Android product includes native platform capabilities such as CameraX/ML Kit scanning, file import, Gemini-backed repair grounding, PDF/text export, clipboard/share actions and Compose navigation/UI.
- Existing automated tests already encode deterministic diagnostic scenarios, rule-pack behavior, OCR cleanup/redaction and Room storage behavior.
- The current Room database is version 3, has schema export disabled and uses destructive fallback for unhandled migrations.
- The repository is private and currently has no verified GitHub Actions/coverage gate equivalent to the KMP laboratory baseline.

## Target scope

| Target | In scope? | Expected role |
| --- | --- | --- |
| COMMON | Yes | Own behavior whose semantics must remain identical across Android and iOS, especially deterministic diagnosis and portable domain rules. |
| ANDROID | Yes | Preserve the existing native Android product and consume shared behavior without degrading current user-visible functionality or stored data. |
| IOS | Yes | Provide native SwiftUI product surfaces that execute the same approved shared diagnostic semantics and are validated with real iOS build/test evidence. |

## In scope

- **RF-01 — Shared diagnostic semantics:** The deterministic Panic Full diagnostic behavior must produce equivalent domain results for the same supported input and knowledge-base data on Android and iOS.
- **RF-02 — Android continuity:** The existing Android application must remain usable throughout the migration and its currently supported diagnostic workflow must not be replaced by an all-at-once rewrite.
- **RF-03 — Native iOS entry point:** iOS must gain a native SwiftUI surface capable of exercising the approved diagnostic workflow with supported text/log input and rendering a diagnostic result.
- **RF-04 — Knowledge-base integrity:** Rule-pack parsing/validation/diff semantics must remain deterministic and equivalent across supported targets.
- **RF-05 — Data preservation:** Migration work must not silently destroy existing Android diagnostic history or settings. Any persistence change that can affect installed data requires explicit migration evidence before adoption.
- **RF-06 — Native platform boundaries:** Camera/OCR acquisition, filesystem/import, share/clipboard, PDF generation, network/AI integration and other platform-facing capabilities must remain independently verifiable per platform rather than being treated as portable merely because the diagnostic core is shared.
- **RF-07 — QA baseline:** Changed/new deterministic shared behavior must be protected by automated tests and meaningful coverage gates; Android and iOS platform behavior must be proven with target-appropriate scenario evidence.
- **RF-08 — Evidence-based iOS support:** PanicLab must not be described as supporting iOS until an iOS build and target-relevant tests execute successfully and the resulting evidence is retained.
- **RF-09 — CI truthfulness:** CI documentation must distinguish what is proven for a public repository from what is actually available for the current repository visibility. No zero-cost private-repository iOS path may be claimed without validation.
- **RF-10 — Architectural boundary integrity:** Shared domain/business contracts must not expose Android persistence entities or require Android/JVM-only APIs as part of their observable contract.

## Out of scope

- Sharing the Android Compose UI with iOS by default.
- Replacing native SwiftUI with Compose Multiplatform UI as a migration objective.
- Rewriting the complete Android application in one pull request.
- Maximizing a percentage of shared code as an architectural goal.
- Inventing or applying Room schema migrations without verified schema history and an approved migration plan.
- Declaring a single global multiplatform coverage percentage.
- Promoting PanicLab migration rules into `SoftwareDevelopmentBlueprint` before PanicLab itself validates the process end to end.
- Implementing this SPEC before it is explicitly approved.

## User flow

### Core diagnostic flow

1. The user provides a supported Panic Full log through a platform-appropriate input surface.
2. PanicLab processes the log using the approved deterministic diagnostic semantics and local knowledge-base rules.
3. PanicLab presents the resolved device/context, diagnostic evidence, primary candidate, alternatives, confidence/verification state and repair flow when available.
4. Unknown or unsupported evidence must remain non-conclusive rather than being converted into an invented confident diagnosis.
5. When the platform supports history persistence for the current migration stage, the resulting report is stored without silently destroying previously compatible user data.

### Rule-pack flow

1. A rule pack is supplied through a platform-appropriate source.
2. PanicLab validates schema/business constraints before accepting it.
3. When comparing versions, the product exposes meaningful additions/modifications/removals without changing deterministic comparison semantics between platforms.
4. Invalid rule packs are rejected with actionable validation information.

## Data and business rules

- Exact diagnostic inputs and equivalent rule-pack data must lead to equivalent deterministic domain outcomes across targets.
- Exact sensor-code matches must retain their precedence and current scope rules unless a separately approved behavior change says otherwise.
- Unknown/unmapped SMC evidence must retain safe non-conclusive fallback behavior.
- Confidence and verification status are domain facts and must not be silently upgraded by platform UI or AI-assisted features.
- AI/network-assisted repair guidance must not redefine the deterministic diagnosis contract.
- User-controlled raw-log retention/redaction behavior must remain explicit.
- Existing persistent user history must be treated as data to preserve, not disposable cache.
- Platform-specific representations may differ as long as the underlying approved domain meaning remains equivalent.

## Mobile and KMP behavior

| Scenario | Target(s) | Expected behavior |
| --- | --- | --- |
| Loading / operation in progress | ANDROID / IOS | Native UI communicates ongoing work for operations that are not effectively immediate. |
| Empty state | ANDROID / IOS | No fabricated diagnosis is shown when no diagnostic input/history is available. |
| Invalid input | COMMON / ANDROID / IOS | Invalid or insufficient input produces a safe, understandable failure/non-conclusive outcome without crashing. |
| Error / timeout | ANDROID / IOS | Platform/network failures are surfaced without corrupting deterministic diagnostic state. |
| Offline / interrupted connection | COMMON / ANDROID / IOS | Deterministic diagnosis based on locally available rules remains usable without requiring Gemini/network access. |
| Cancel / back | ANDROID / IOS | Native navigation conventions are respected and cancellation does not create a false completed diagnosis. |
| Background -> foreground | ANDROID / IOS | Returning to the app must not silently substitute, duplicate or corrupt the active/resulting diagnostic state. |
| Screen recreation / scene restoration | ANDROID / IOS | Platform UI lifecycle events must not change deterministic diagnostic semantics. |
| Process termination -> relaunch | ANDROID / IOS | Persisted history that is within the supported schema remains recoverable according to the platform persistence contract. |
| Permission denied / capability unavailable | ANDROID / IOS | Camera/file/platform capability denial must not block text-based deterministic diagnosis when that input route is available. |
| Unsupported platform capability | ANDROID / IOS | The product must communicate unavailability; it must not pretend feature parity without an implemented and tested native capability. |

**Guideline items that do not apply to the shared diagnostic core:** native screen restoration, permission UI and platform navigation are platform concerns. They still apply to the Android/iOS product surfaces that host the core.

## Native UX expectations

- **ANDROID-UX:** Preserve native Jetpack Compose / Material 3 behavior and existing Android interaction patterns unless a later approved UI SPEC changes them.
- **IOS-UX:** Use native SwiftUI conventions. Functional/domain equivalence does not require pixel parity with Android.

## Explicit constraints

- `SoftwareDevelopmentBlueprint` is not modified during this migration stage.
- Android must continue working while shared behavior is introduced incrementally.
- iOS UI is native SwiftUI by default.
- No direct changes to `main`; changes use branches and pull requests.
- Merge requires explicit authorization in the form `Apruebo merge PR #N`.
- Current repository visibility is private; the validated public-repository USD0 iOS CI result must not be misrepresented as already solved here.
- Existing Room data must not be intentionally destroyed to simplify the KMP migration.
- QA thresholds are not lowered merely to make CI green.
- Coverage from different execution environments is not combined into a fabricated global percentage.
- Secrets remain outside committed runtime configuration.

## Acceptance criteria

- **CA-01 · RF-01 · COMMON:** Given each approved deterministic diagnostic fixture, when it is processed by the shared diagnostic behavior, then device/context extraction, panic-family classification, extracted codes/evidence, primary/alternative diagnosis, confidence and verification semantics match the approved baseline expectations.
- **CA-02 · RF-01 · COMMON:** Given equivalent hexadecimal and decimal representations of the same supported sensor code, when diagnosis is evaluated, then they resolve equivalently where the current baseline defines them as equivalent.
- **CA-03 · RF-01 · COMMON:** Given an unmapped SMC code, when diagnosis is evaluated, then PanicLab returns the approved safe/non-conclusive fallback rather than inventing a confident component diagnosis.
- **CA-04 · RF-04 · COMMON:** Given valid and invalid rule-pack fixtures, when validation and diff behavior execute, then valid packs pass, invalid business/schema conditions are reported, and version differences match approved baseline expectations.
- **CA-05 · RF-02 · ANDROID:** Given the migrated Android application, when the existing supported text/log diagnostic path is exercised, then it completes successfully using the approved shared semantics and presents the expected native Android result.
- **CA-06 · RF-02 · ANDROID:** Given the Android migration occurs incrementally, when each migration increment is integrated, then unrelated existing Android capabilities are not intentionally removed or replaced without their own approved scope.
- **CA-07 · RF-05 · ANDROID:** Given an installed database state covered by an approved migration path, when the updated application starts, then supported diagnostic history survives and remains readable; destructive fallback is not accepted as evidence of preservation.
- **CA-08 · RF-03 · IOS:** Given a supported diagnostic fixture on iOS, when the native SwiftUI flow invokes the approved shared diagnostic behavior, then it renders a domain-equivalent result to the approved baseline.
- **CA-09 · RF-08 · IOS:** Given the iOS target, when release-readiness evidence is produced, then a real iOS build plus target-relevant automated tests succeed before iOS support is declared.
- **CA-10 · RF-06 · ANDROID/IOS:** Given a platform capability such as camera/OCR, import/share, PDF or network/AI guidance, when it is claimed as supported on a target, then target-specific executable evidence exists for that capability.
- **CA-11 · RF-07 · COMMON:** Given changed/new deterministic shared production logic, when the QA gate runs, then line coverage is at least 90% and branch coverage at least 85% for the defined deterministic changed/new scope; critical deterministic scenarios target meaningful 95–100% scenario coverage where practical.
- **CA-12 · RF-07 · ANDROID/IOS:** Given platform UI/capability behavior, when QA runs, then it is evaluated with scenario/evidence-driven tests appropriate to that target rather than forced into a single cross-platform coverage number.
- **CA-13 · RF-10 · COMMON:** Given the shared domain/business contract, when its dependency boundary is inspected, then no Android persistence entity is exposed through that contract and no Android SDK dependency is required to use it.
- **CA-14 · RF-09 · CI:** Given CI documentation/results, when repository visibility and runner type are reviewed, then the evidence clearly states which Android/iOS jobs actually ran and does not claim an unvalidated private-repository USD0 path.
- **CA-15 · RF-05 · ANDROID:** Given a proposed persistence/schema change, when no verified non-destructive migration evidence exists, then that change cannot be treated as migration-complete.

## Behavior verification map

| Criterion | Target | Conditions / steps | Expected result |
| --- | --- | --- | --- |
| CA-01 | COMMON | Run approved diagnostic fixture suite | Shared diagnostic outputs preserve approved semantics. |
| CA-02 | COMMON | Run equivalent decimal/hex fixtures | Equivalent codes resolve equivalently. |
| CA-03 | COMMON | Run unknown SMC fixture | Safe non-conclusive fallback. |
| CA-04 | COMMON | Run rule-pack parse/validation/diff fixtures | Valid/invalid/diff semantics preserved. |
| CA-05 | ANDROID | Exercise Android diagnostic flow | Native Android path completes with shared result. |
| CA-06 | ANDROID | Regression suite per increment | No unrelated intentional feature loss. |
| CA-07 | ANDROID | Installed-upgrade migration test for verified schema path | Supported history remains readable. |
| CA-08 | IOS | Exercise native SwiftUI diagnostic flow with fixture | Domain-equivalent result rendered. |
| CA-09 | IOS | Execute iOS build and target tests | Real build/tests succeed before support claim. |
| CA-10 | ANDROID/IOS | Exercise each claimed native capability | Target-specific evidence exists. |
| CA-11 | COMMON | Run deterministic coverage gate | >=90% line and >=85% branch on defined changed/new deterministic scope. |
| CA-12 | ANDROID/IOS | Run target scenario suites | Platform evidence stays target-specific. |
| CA-13 | COMMON | Architecture/dependency guard | Shared contract has no Android persistence/SDK leak. |
| CA-14 | CI | Inspect jobs, runner evidence and repo visibility | CI claims match executed reality. |
| CA-15 | ANDROID | Review proposed persistence change | No destructive shortcut accepted as preservation. |

## Pending decisions

These decisions are intentionally not made by this SPEC and require PLAN-level or later feature-specific resolution:

- whether `PanicLab` becomes public to use the already validated GitHub-hosted USD0 iOS CI model
- exact shared/persisted-data architecture and the timing of any Room KMP adoption
- verified recoverable Room schema lineage and concrete non-destructive migrations from existing installed versions
- exact multiplatform JSON implementation
- exact hashing implementation/boundary
- exact clock and ID mechanism
- exact iOS minimum deployment target
- sequencing and parity targets for Camera/OCR, Gemini, PDF, file import/share and other platform-specific capabilities

## Approval gate

**Approved by Luis on 2026-09-18.**

The next SDD artifact is the technical PLAN. Approval of this SPEC authorizes PLAN design only; it does not authorize implementation or merge.