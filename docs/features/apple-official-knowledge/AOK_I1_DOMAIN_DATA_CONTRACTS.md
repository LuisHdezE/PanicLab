# AOK-I1 — Domain/Data Contracts

Status: IMPLEMENTED / PR PENDING REVIEW

Baseline main: `33529ec86a2a72df314002f64262d235f33ca247`

Issue: #30

## Scope

AOK-I1 introduces only the shared domain and data boundary for Apple Official Knowledge.

This increment intentionally does not import the curated dataset, add UI, alter the deterministic engine, modify Rule Pack data, change diagnostic score/confidence, or touch TASK-KMP-082 / PR #28.

## Contracts added

### Domain models

`shared/src/commonMain/kotlin/com/example/appleknowledge/model/AppleOfficialKnowledgeModels.kt`

Defines:

- `AppleKnowledgeSource`
- `AppleKnowledgeCard`
- `AppleModelCapability`
- `AppleModelScope`
- `AppleCapability`
- governed enums for source status, authority, source type, card category, capability availability, detail/state and Rule Pack effect

### Read-only repository

`shared/src/commonMain/kotlin/com/example/appleknowledge/repository/AppleOfficialKnowledgeRepository.kt`

The repository exposes read operations only:

- source by ID
- sources by IDs
- card by ID
- cards for an exact model
- model capability for an exact model

No mutation operation exists in the contract.

## Governance invariants

1. `AppleKnowledgeRulePackEffect` has exactly one possible value: `NONE`.
2. Apple Official Knowledge remains an informational layer and cannot represent a deterministic Rule Pack effect.
3. Exact model resolution is explicit through `AppleModelScope.exactModels` and `appliesToExactModel`.
4. Family/display labels are not treated as exact models.
5. `PART_SUPPORTED`, public repair procedure availability, Repair Assistant, Diagnostics SSR and Recovery Diagnostics Mode remain separately representable capabilities.
6. Official source URLs must be HTTPS.
7. Historical-program references remain explicit source IDs and do not imply current eligibility.
8. `NOT_FOUND_PUBLICLY` is distinct from `NOT_SUPPORTED`.

## Tests

`shared/src/commonTest/kotlin/com/example/appleknowledge/AppleOfficialKnowledgeContractsTest.kt`

The tests lock:

- the Rule Pack firewall (`NONE` only)
- strict exact-model matching
- HTTPS source validation
- default governed card state
- independence of Apple repair/diagnostic capability concepts

## Deferred to AOK-I2

AOK-I2 will transform the frozen Google Sheet dataset into a versioned curated resource and add validators for IDs, URLs, statuses, exact models, source references and `rulePackEffect=NONE`.

No dataset rows are bundled in AOK-I1.
