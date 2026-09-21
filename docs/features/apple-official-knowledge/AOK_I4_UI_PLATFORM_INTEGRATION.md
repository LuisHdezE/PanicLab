# AOK-I4 UI / Platform Integration

## Status

AOK-I4 integrates Apple Official Knowledge v1 into the native Android and iOS product surfaces without joining it to deterministic Panic Full diagnosis.

Baseline: `main@86e44d39f7f7cc6f96f34d11731f4437915025ec` (AOK-I3 merged).

## Product boundary

Apple Official Knowledge is presented as a separate contextual module named **Apple Oficial**.

It does not modify or participate in:

- deterministic Rule Pack selection or execution;
- primary cause;
- score;
- confidence;
- evidence extraction;
- Panic Full result construction;
- TASK-KMP-082;
- PR #28.

Both platform UIs state this boundary explicitly. AOK is intentionally not embedded inside the deterministic result card.

## Single source of truth

The only governed dataset remains:

`shared/src/commonMain/resources/appleknowledge/`

A build task generates Kotlin source under `shared/build/generated/appleknowledge/` from those governed JSON files. Generated source is build output only and is never committed as a second dataset.

This gives Android, JVM and Kotlin/Native/iOS the same validated runtime payload without copying the 19/91/241 dataset into platform resource folders.

## Runtime catalog

`AppleOfficialKnowledgeCatalog` parses and validates the complete frozen dataset before it becomes queryable.

Runtime gates include:

- capability manifest validation;
- 19 family records;
- 91 official source records;
- 241 knowledge cards;
- reproducible source-reference audit;
- lossless I3 query projection;
- exact-model applicability index;
- immutable read-only lookup surfaces.

`AppleOfficialKnowledgeRuntimeFactory.embedded()` creates the I3 repository and use cases from the build-generated payload.

`NativeEmbeddedAppleOfficialKnowledgeFacade` exposes a synchronous Swift-friendly read facade over the exact same catalog.

## Exact-model applicability

Raw card `modelScope` remains governed source text. I4 derives an exact-model index conservatively for presentation only.

Rules:

- exact named variants map only to those variants;
- slash/comma/plus lists do not broaden to the whole generation;
- `family` / `familia` scopes expand only within the corresponding audited capability family;
- storage-only `iPhone 5c 8 GB` inherits the same iPhone 5c hardware knowledge;
- historical special cases remain narrow.

Guardrail examples:

- `AOKF-12-013` applies to iPhone 12 and iPhone 12 Pro, not 12 mini or 12 Pro Max;
- `AOKF-X-012` applies to XR/XS/XS Max, not iPhone X;
- `AOKF-6-013` applies only to iPhone 6 Plus.

The union of exact-model query results must still cover all 241 governed cards.

## Android integration

Android adds a separate `apple_official_knowledge` route.

Home exposes an **Apple Oficial** contextual entry that navigates away from deterministic diagnosis.

The screen provides:

- exact-model selection;
- model capability summary;
- category filters;
- applicable AOK cards;
- expandable troubleshooting/detail content;
- current/historical context labels;
- official Apple URL links;
- explicit source-reference resolution state;
- visible `Rule Pack effect: NONE` context.

The Android ViewModel consumes `GetAppleOfficialKnowledgeForModelUseCase` from I3 rather than reimplementing query semantics in Compose.

## iOS integration

iOS exposes two top-level tabs:

1. **Diagnóstico**
2. **Apple Oficial**

The existing deterministic diagnostic surface remains the default first tab and is otherwise unchanged.

The Apple Oficial tab uses `NativeEmbeddedAppleOfficialKnowledgeFacade` from `Shared.framework` and provides the same conceptual information as Android.

No additional JSON bundle copy is added to the Xcode target.

## Source-reference presentation

I4 preserves the four I3 states:

- `EXACT_SINGLE`
- `EXACT_AMBIGUOUS`
- `LOCALE_PATH_CANDIDATE`
- `UNMAPPED`

Only exact mappings display normalized AOK ids as exact. Candidate ids remain candidates. Unmapped official URLs remain usable links without receiving a fabricated source id.

## QA

COMMON/JVM gates verify:

- governed file resources and generated embedded resources are equivalent;
- 19 capabilities / 48 exact models / 241 mapped cards;
- exact-model historical guardrails;
- `rulePackEffect=NONE` for every mapped card;
- native facade preserves unresolved source state.

Android host test verifies the Android composition layer can create the embedded runtime and query the frozen catalog.

iOS XCTest verifies the embedded Kotlin/Native facade from Swift and the iPhone 12 applicability guardrails.

iOS UI smoke verifies:

- the diagnostic tab remains available and unchanged as the default surface;
- Apple Oficial is a separate tab;
- contextual boundary text is visible;
- deterministic Analyze action is not present inside the Apple Oficial tab.

## I5 handoff

AOK-I5 should treat I4 as a presentation boundary and focus on acceptance/closure evidence: runtime counts, cross-platform parity, accessibility/smoke behavior, source-link semantics and a final firewall audit. It must not promote Apple Official Knowledge into deterministic scoring or Rule Pack behavior.
