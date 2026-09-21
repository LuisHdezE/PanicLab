# AOK-I2 — Curated Dataset Import / Seed

Status: DATA IMPORT COMPLETE / VALIDATION IN PROGRESS

Baseline main: `49e1203f158cf44b108897f8a43eb7421757f9c4`

Issue: #30

## Frozen source dataset

AOK-I2 is sourced from the governed Google Sheet `PanicLab Findings Inbox` and preserves the approved v1 scope:

- 19 audited model families / coverage rows
- 91 Apple source records
- 241 knowledge cards
- lower bound: iPhone 5
- all 241 cards: `PILOT_READY`
- all 241 cards: `DETAILED`
- all 241 cards: `rulePackEffect=NONE`

The scope is frozen. iPhone 4S/4/3GS/original are outside v1 unless separately approved.

## Import stages

### I2A — Model capabilities: COMPLETE

Implemented:

- versioned resource: `shared/src/commonMain/resources/appleknowledge/apple_official_knowledge_capabilities_v1.json`
- common JSON parser
- validator for frozen counts, scope, IDs, Apple-controlled URLs, exact-model uniqueness and dates
- JVM resource test that parses and validates the real seed
- common contract tests for parser/validator failure modes

The capability resource contains all 19 audited families and keeps these concepts independent:

- public Repair Manual
- Apple Diagnostics SSR
- Recovery Assistant Diagnostics Mode
- Repair Assistant
- Parts & Service History capabilities
- troubleshooting availability
- historical-program references

### I2B — Apple source catalog: COMPLETE

The 91 governed `Apple Oficial` rows are versioned in three resources containing 30 / 31 / 30 records.

The source import preserves every governed field and validates:

- unique `AOK-###` IDs
- source type/status/authority enums
- HTTPS Apple-controlled URLs
- verification date
- frozen source count = 91

Important: 91 source records correspond to 82 unique official URLs because some governed semantic source records intentionally share the same Apple URL.

### I2C — Knowledge cards raw seed: COMPLETE

All 241 governed `Apple Fichas` rows are versioned without fabricating source IDs.

The raw card representation preserves all 18 sheet columns:

1. ID
2. model scope
3. category
4. subcategory
5. symptoms / input
6. Apple quick checks
7. Apple diagnosis
8. inspection / discard
9. Apple action / service
10. primary source URL
11. secondary source raw references
12. source status
13. detail level
14. PanicLab correlation
15. Rule Pack effect
16. card state
17. verified date
18. applicability notes

The logical dataset remains five parts. For transport safety in repository writes, logical parts 2–5 are physically fragmented as `a/b/c/d`; this is a storage detail only. Validation groups fragments by logical `part` and still requires exactly logical parts `{1,2,3,4,5}`, 241 cards and 241 unique card IDs.

The parser requires exactly 18 columns per raw row so a truncated export cannot silently validate.

Composite sheet statuses are normalized explicitly only at the status level:

- `CURRENT + HISTORICAL` → primary `CURRENT` + contextual `HISTORICAL`
- `CURRENT + ENDED_CONTEXT` → primary `CURRENT` + contextual `ENDED`
- `CURRENT`, `HISTORICAL`, `ENDED` remain direct statuses

## Source-reference audit

See `AOK_I2_SOURCE_REFERENCE_AUDIT.md`.

The governed sheet stores card source references as URLs, while the I1 normalized domain contract stores `primarySourceId` and `secondarySourceIds`. The audit proves that a lossless global URL → ID conversion is not currently possible:

- 776 URL occurrences across card primary/secondary references
- 117 unique card-reference URLs
- 68 unique URLs map exactly to one catalog ID
- 4 unique URLs match catalog URLs that belong to multiple semantic `AOK-*` IDs
- 45 unique URLs have no exact catalog match
- 9 of those 45 have a locale/path-equivalent catalog candidate, but candidate equivalence is not treated as identity
- 36 have no locale/path-equivalent source row in the 91-source catalog

Therefore AOK-I2 does not fabricate mappings. Exact single matches may be resolved deterministically by a later mapping layer; ambiguous, locale-candidate and unmapped references must preserve their raw Apple URL.

## Validation gates

AOK-I2 is complete only when repository CI proves the real resources satisfy all of the following:

- capabilities = 19
- sources = 91
- cards = 241
- card IDs unique = 241
- logical parts = 1 through 5
- every card `DETAILED`
- every card `PILOT_READY`
- every card `rulePackEffect=NONE`
- every primary card source is an Apple-controlled HTTPS URL
- historical/composite statuses remain explicitly modeled

## Governance firewall

AOK-I2 does not:

- modify the deterministic Rule Pack
- alter diagnostic score, confidence or primary cause
- touch TASK-KMP-082 / PR #28 evidence
- add UI
- promote secondary historical details to Apple official authority
- invent `AOK-*` source IDs to make normalization appear complete

Full conversion into normalized `AppleKnowledgeCard` objects remains gated on an explicit source-reference resolution layer. The frozen raw v1 dataset is the lossless source of truth for that future step.
