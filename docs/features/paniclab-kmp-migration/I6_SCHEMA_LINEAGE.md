# I6 — Room schema lineage recovery

## Authorization and scope

I6 was explicitly authorized by Luis on 2026-09-19.

This document closes **TASK-KMP-060 — Recover and verify Room schema lineage**. It is an evidence-recovery checkpoint only. It does **not** add Room migrations, change the database version, change entities/DAOs, enable Room KMP, or modify the product schema.

Baseline for this investigation:

- `main@c6a269f7e137133fa010e83fe8a110ec958da018`
- database class: `app/src/main/java/com/example/data/local/AppDatabase.kt`
- entities: `app/src/main/java/com/example/data/local/entity/Entities.kt`
- database filename: `paniclab_database.db`

## Evidence vocabulary

- **VERIFIED_SOURCE** — exact schema declaration is recoverable from immutable repository history.
- **VERIFIED_CURRENT** — declaration is present on current `main` and unchanged from the identified historical commit.
- **NOT_EXPORTED** — Room schema JSON was not exported because `exportSchema = false`.
- **NO_ARTIFACT_FOUND** — no historical installed database / release artifact was found in repository evidence.
- **UNKNOWN** — cannot be proven from available evidence and must not be guessed.

A source-declared schema being verified does **not** prove that a historical user installation contains a preserved database with that schema.

## Current Room configuration

Current `AppDatabase` declares:

- Room version: **3**
- `exportSchema = false`
- six entities/tables
- builder uses `.fallbackToDestructiveMigration()`
- no registered `addMigrations(...)` path was found in current source

Current tables:

1. `device_models`
2. `diagnostic_rules`
3. `diagnostic_sessions`
4. `diagnostic_evidences`
5. `diagnosis_candidates`
6. `rule_packs`

`diagnostic_evidences.sessionId` and `diagnosis_candidates.sessionId` reference `diagnostic_sessions.id` with `ON DELETE CASCADE`; both child tables declare an index on `sessionId`.

## Verified lineage

| Room version | Evidence commit | Evidence status | What is proven |
| --- | --- | --- | --- |
| v1 | `8c297055af51d783923310ac50bea0a655166cda` | VERIFIED_SOURCE | First recoverable `AppDatabase` / `Entities.kt`; version 1; six tables; `exportSchema=false`; destructive fallback configured. |
| v2 | `63017f71ee359c38e8b99fa44862a1f4a2dc668b` | VERIFIED_SOURCE | Version increment to 2 and exact source-level entity changes are recoverable. |
| v3 | `c165ae4283a3b592eddb2b70a1c13a9ffeb51f01` | VERIFIED_SOURCE / VERIFIED_CURRENT | Version increment to 3 and exact source-level entity changes are recoverable; current `main` still uses this v3 declaration. |

The parent of the v1 introduction, `990c1357914477583a7f7b3eed1129854514cc14`, does not contain `AppDatabase.kt`; no earlier Room schema is recoverable from that path.

The commit history for both `AppDatabase.kt` and `Entities.kt` contains exactly the v1, v2 and v3 schema-defining commits above. No later schema declaration change was found after `c165ae4283a3b592eddb2b70a1c13a9ffeb51f01`.

## v1 source-declared schema

Commit: `8c297055af51d783923310ac50bea0a655166cda`

`AppDatabase` declares `version = 1` and the same six table names that exist today.

Important v1 `diagnostic_sessions` columns beyond its identity/device/diagnostic fields:

- `appliedRuleIdsJson`
- `repairFlowJson`
- `rawLog`
- `rawLogSaved`

It does **not** yet declare:

- `reanalyzedAt`
- `previousDiagnosis`
- `previousKnowledgeBaseVersion`
- `technicianNotes`
- `customerName`

The v1 `rule_packs` source declaration contains:

- `version`
- `title`
- `generatedAt`
- `schemaVersion`
- `rulesCount`
- `modelsCount`
- `isDefault`
- `importedAt`

## v1 → v2 verified delta

Commit: `63017f71ee359c38e8b99fa44862a1f4a2dc668b`

`AppDatabase` changes from Room version 1 to 2.

Verified `diagnostic_sessions` additions:

- `reanalyzedAt: Long?`
- `previousDiagnosis: String?`
- `previousKnowledgeBaseVersion: String?`

Verified `rule_packs` additions / expansion:

- `classifiersCount`
- `sourcesCount`
- `bitmaskCount`
- `origin`
- `sourceFilename`
- `checksum`
- `isActive`
- `previousVersion`
- `rawJson`

The original `isDefault` field remains present.

No explicit Room `Migration(1, 2)` or `addMigrations(...)` registration was found. The database builder still uses `.fallbackToDestructiveMigration()`.

Therefore the repository proves the schema declaration change, but it does **not** prove a data-preserving v1 → v2 upgrade path. Under this configuration, Room is allowed to destructively recreate the database when no migration path exists.

## v2 → v3 verified delta

Commit: `c165ae4283a3b592eddb2b70a1c13a9ffeb51f01`

`AppDatabase` changes from Room version 2 to 3.

Verified `diagnostic_sessions` additions:

- `technicianNotes: String?`
- `customerName: String?`

No other Room entity declaration changes were found in the v2 → v3 commit.

Again, no explicit Room `Migration(2, 3)` or `addMigrations(...)` registration was found. `.fallbackToDestructiveMigration()` remains configured.

Therefore the repository proves the schema declaration change, but it does **not** prove a data-preserving v2 → v3 upgrade path.

## v3 current schema snapshot

Current `main` remains Room v3 with the same six entities and the same v3 entity declarations first seen at `c165ae4283a3b592eddb2b70a1c13a9ffeb51f01`.

`diagnostic_sessions` currently contains the historical/reanalysis/customer fields introduced through v2/v3:

- `reanalyzedAt`
- `previousDiagnosis`
- `previousKnowledgeBaseVersion`
- `technicianNotes`
- `customerName`

The existing `RoomDiagnosticStorageTest` proves current in-memory v3 persistence semantics for representative session/evidence/candidate data, including technician notes, but it is **not** an installed-version upgrade test.

## Missing evidence / hard limits

### Room schema exports

All recovered database declarations use `exportSchema = false`.

Status: **NOT_EXPORTED**.

No canonical historical Room schema JSON should be fabricated by hand and presented as an original Room export.

### Registered migrations

No current `addMigrations(...)` registration was found, and each recovered `AppDatabase` snapshot uses `.fallbackToDestructiveMigration()`.

Status: **no data-preserving migration implementation proven**.

### Historical installed databases

No historical `.db` artifact, user backup, or equivalent installed-database fixture was found in repository evidence during TASK-KMP-060.

Status: **NO_ARTIFACT_FOUND**.

### GitHub releases

The repository currently has no GitHub Releases from which an old application/database artifact can be recovered.

Status: **NO_ARTIFACT_FOUND**.

### Actual past user upgrade outcomes

The repository does not prove whether any specific device installed v1, then v2, then v3, nor which rows existed before those updates.

Status: **UNKNOWN**.

## Preservation conclusion

Three Room source schemas are recoverable with high confidence: **v1, v2 and v3**.

What is **not** recoverable is evidence of a historical non-destructive migration path. The source instead consistently proves destructive fallback was enabled while the Room version changed.

Consequences for I6:

1. Do not invent historical migration classes or pretend legacy data was preserved.
2. Do not change Room version 3 or entity shape until an upgrade harness exists.
3. Treat current v3 as the minimum preservation baseline for future releases.
4. Historical v1/v2 declarations may be used as evidence inputs only when TASK-KMP-061 creates fixtures from their exact source declarations through a reproducible process; do not hand-author “official Room exports” and call them historical artifacts.
5. Any future schema change must have an explicit migration and installed-upgrade test before destructive fallback can be considered removable for that path.

## TASK-KMP-060 result

**DONE**.

Recovered and classified:

- v1 source declaration — VERIFIED_SOURCE
- v2 source declaration — VERIFIED_SOURCE
- v3 source declaration — VERIFIED_SOURCE / VERIFIED_CURRENT
- Room exports — NOT_EXPORTED
- historical database artifacts — NO_ARTIFACT_FOUND
- data-preserving v1→v2 migration — NOT PROVEN
- data-preserving v2→v3 migration — NOT PROVEN
- current v3 in-memory storage semantics — already covered by existing test, not an upgrade proof

## Handoff to TASK-KMP-061

TASK-KMP-061 may now build a **non-destructive upgrade test harness**, but it must distinguish two evidence classes:

- **current v3 preservation baseline**, which can be generated directly from current source;
- **historical v1/v2 source-derived fixtures**, which must be generated reproducibly from the exact historical declarations/commits and clearly labeled as reconstructed test fixtures, not original exported Room schemas or recovered user databases.

No production migration is authorized merely by this lineage recovery.