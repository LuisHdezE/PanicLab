# AOK-I2 — Curated Dataset Import / Seed

Status: IN PROGRESS

Baseline main: `49e1203f158cf44b108897f8a43eb7421757f9c4`

Issue: #30

## Frozen source dataset

AOK-I2 is sourced from the governed Google Sheet `PanicLab Findings Inbox` and preserves the approved v1 scope:

- 19 audited model families / coverage rows
- 91 Apple source records
- 241 knowledge cards
- lower bound: iPhone 5
- all 241 cards: `PILOT_READY`
- all 241 cards: `rulePackEffect=NONE`

The scope is frozen. iPhone 4S/4/3GS/original are outside v1 unless separately approved.

## Import stages

### I2A — Model capabilities

Implemented in this branch:

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

### I2B — Apple source catalog

Next stage: import the 91 `Apple Oficial` rows as a separate versioned resource and validate:

- unique `AOK-###` IDs
- source type/status/authority enums
- HTTPS Apple-controlled URLs
- verification date
- frozen source count = 91

### I2C — Knowledge cards

Only after I2B is stable, import the 241 `Apple Fichas` rows.

The governed sheet stores card source references as URLs, while the I1 normalized domain contract stores `primarySourceId` and `secondarySourceIds`. AOK-I2 must not fabricate ID mappings.

Required normalization gate:

1. build URL → `AOK-*` lookup from the imported source catalog;
2. audit every primary and secondary card URL;
3. preserve Apple-controlled URLs that are not represented by a catalog ID in a raw seed representation;
4. normalize to `AppleKnowledgeCard` only when references are explicit and lossless;
5. report unmapped URLs as governed import findings instead of silently dropping or inventing source IDs.

Composite sheet statuses are also normalized explicitly:

- `CURRENT + HISTORICAL` → primary `CURRENT` + contextual `HISTORICAL`
- `CURRENT + ENDED_CONTEXT` → primary `CURRENT` + contextual `ENDED`
- `CURRENT`, `HISTORICAL`, `ENDED` remain direct statuses

## Governance firewall

AOK-I2 does not:

- modify the deterministic Rule Pack
- alter diagnostic score, confidence or primary cause
- touch TASK-KMP-082 / PR #28 evidence
- add UI
- promote secondary historical details to Apple official authority

The full 241-card import is not complete until all records validate with `rulePackEffect=NONE` and the frozen counts match exactly.
