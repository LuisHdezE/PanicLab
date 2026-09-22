# AOK-I3 Shared Query / Use Cases

## Status

AOK-I3 introduces the shared, read-only application/query layer for Apple Official Knowledge v1.

Baseline: `main@c0f7024ef0a32f656cec4a91ee7eeb2e01c5626f` (AOK-I2 merged).

## Why I3 uses a query projection

The strict AOK-I1 `AppleKnowledgeCard` domain contract requires source references to use normalized `AOK-*` ids. AOK-I2 proved that the frozen source/card dataset cannot be normalized losslessly under that rule:

- 776 card-source URL occurrences
- 117 unique card-reference URLs
- 68 unique URLs have one exact catalog match
- 4 unique URLs have multiple exact semantic matches
- 45 unique URLs have no exact catalog match
- 9 of those 45 have locale/path-equivalent candidates only
- 36 have no locale/path-equivalent catalog candidate

AOK-I3 therefore does **not** weaken the strict domain contract and does **not** fabricate source ids. Instead it adds a lossless read projection for product queries.

## Source-reference resolution states

Every query source reference is explicitly classified as one of:

- `EXACT_SINGLE`: exactly one catalog source has the same governed URL.
- `EXACT_AMBIGUOUS`: multiple semantic `AOK-*` rows intentionally share the exact URL.
- `LOCALE_PATH_CANDIDATE`: no exact URL match exists, but one or more catalog rows share the Apple support path after locale normalization.
- `UNMAPPED`: no exact or locale/path candidate exists in the frozen 91-source catalog.

Only `exactSourceIds` are treated as resolved source ids. `candidateSourceIds` are context only and are never silently promoted to exact mappings.

## Shared contracts

### `AppleOfficialKnowledgeQueryRepository`

Read-only boundary for shared application queries:

- get source by id
- get sources by ids
- get card projection by id
- get card projections for an exact model
- get model capability for an exact model

The boundary has no mutation methods and no deterministic-diagnosis methods.

### Query projections

`AppleKnowledgeCardView` preserves:

- governed model-scope text
- category/subcategory
- symptoms/checks/diagnostic guidance/inspection/action
- primary and secondary official URLs
- explicit URL-resolution state
- normalized source status plus contextual historical/ended status
- detail level
- PanicLab correlation text
- `rulePackEffect=NONE`
- card state
- verification date
- applicability notes

## Shared use cases

### `GetAppleOfficialKnowledgeForModelUseCase`

Returns model capability plus deterministic, sorted card projections for an exact-model query. An optional category filter is supported. Blank or unaudited models return `null`; I3 does not infer support outside the governed capability catalog.

### `GetAppleOfficialKnowledgeCardUseCase`

Returns a card projection plus only those `AppleKnowledgeSource` records that are backed by exact URL matches. Ambiguous exact references may legitimately resolve to multiple semantic source records. Locale/path candidates and unmapped URLs stay unresolved.

### `GetAppleOfficialModelCapabilityUseCase`

Returns the governed model capability record without inferring unsupported tools or procedures.

`hasUnresolvedSourceReferences()` provides an explicit presentation signal for cards containing ambiguous, candidate, or unmapped references.

## Mapping boundary

`AppleOfficialKnowledgeQueryMapper` converts governed AOK-I2 seed rows into the I3 read projection using the reproducible source-reference audit. It fails closed if an audit does not contain one of the card's governed URLs.

## QA

Common tests prove:

- exact ambiguity is preserved rather than collapsed
- locale/path candidates never become exact ids
- unmapped references remain unmapped
- Rule Pack effect remains `NONE`
- model/category queries are deterministic
- card-detail queries resolve only exact ids
- blank and unaudited models do not invent coverage

A JVM integration test loads the real frozen resources and requires:

- 241 query projections
- 241 unique card ids
- 776 source-reference occurrences
- `AOKF-12-001` primary URL `101969` remains `UNMAPPED`
- `101965` remains an exact `AOK-031` reference
- all four resolution states remain represented where applicable
- all projections keep `rulePackEffect=NONE`

## Governance firewall

AOK-I3 does not change:

- deterministic Panic Full Rule Pack
- primary cause
- score
- confidence
- current KMP migration
- TASK-KMP-082
- PR #28
- Android or iOS UI

## I4 handoff

AOK-I4 may bind these query contracts to platform/UI presentation. Resource loading and exact-model applicability indexing must remain separate from deterministic diagnosis. Any platform adapter must preserve the four source-reference resolution states and must not treat locale/path candidates as exact sources.
