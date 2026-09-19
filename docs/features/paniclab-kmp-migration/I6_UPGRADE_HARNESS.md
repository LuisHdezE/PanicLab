# I6 — Non-destructive Room upgrade safety harness

## Authorization and scope

I6 was explicitly authorized by Luis on 2026-09-19.

This document records **TASK-KMP-061 — Add non-destructive upgrade test harness**.

Baseline for this task:

- `main@425c59017f0368833416f8b83925b4b099558073`
- TASK-KMP-060 evidence: `I6_SCHEMA_LINEAGE.md`
- current Room database version: **3**
- current production builder still uses `fallbackToDestructiveMigration()`

This task does **not** invent historical Room migrations, change the Room version, change entities/DAOs, change the product schema, remove destructive fallback from production, adopt Room KMP, add iOS persistence, or modify `SoftwareDevelopmentBlueprint`.

## Evidence classes

TASK-KMP-060 proved three source-declared Room versions: v1, v2 and v3. It did not recover original Room schema exports or historical user database files.

The harness therefore distinguishes:

1. **RECONSTRUCTED_SOURCE_FIXTURE** — a focused SQLite fixture derived from fields that are directly recoverable from the exact historical entity declarations. It is test input only and is not represented as an original Room export or recovered user database.
2. **CURRENT_ROOM_BASELINE** — a database created by the current Room v3 implementation itself.

## Harness implementation

Test class:

`app/src/test/java/com/example/RoomUpgradeSafetyTest.kt`

CI integration:

`.github/workflows/kmp-i0-baseline.yml`

The test suite contains three scenarios.

### 1. Reconstructed v1 fails closed without a migration

The harness creates a focused source-derived v1 SQLite fixture with representative:

- `diagnostic_sessions`
- `diagnostic_evidences`
- `diagnosis_candidates`

It sets `PRAGMA user_version = 1` and inserts sentinel session/evidence/candidate rows.

The fixture is then opened through a strict current-v3 Room builder that intentionally does **not** call `fallbackToDestructiveMigration()`.

Expected behavior:

- Room refuses to open because no v1→v3 migration path exists;
- the open attempt fails rather than destructively recreating the database;
- the fixture remains at `user_version = 1`;
- the sentinel session/evidence/candidate rows remain present afterward.

Result: **PASS**.

### 2. Reconstructed v2 fails closed without a migration

The same process is repeated for a v2 source-derived fixture, including the v2 session declaration additions recovered by TASK-KMP-060.

Expected behavior:

- Room refuses to open because no v2→v3 migration path exists;
- the fixture remains at `user_version = 2`;
- representative sentinel rows remain present.

Result: **PASS**.

### 3. Current v3 baseline survives strict close/reopen

The harness creates a v3 database through the current Room implementation, persists representative data, closes the database, then reopens it through the strict builder without destructive fallback.

Representative retained data includes:

- diagnostic session;
- diagnostic evidence;
- diagnosis candidate;
- `technicianNotes`;
- `customerName`;
- primary diagnosis/rule identity.

After reopen, all asserted data is unchanged.

Result: **PASS**.

## What this proves

The harness now provides an executable preservation gate with two important behaviors:

- historical source-derived fixtures are protected from accidental destructive opening when no migration exists;
- current v3 data has a verified preservation baseline that can be reused when a future schema version is introduced.

This is deliberately different from claiming v1→v2, v2→v3, v1→v3 or v2→v3 are supported migrations. They are **not** supported or retroactively proven.

## What this does not prove

The following remain unproven and are not claimed:

- that any historical user installation successfully preserved v1 data through v2 or v3;
- that an original v1/v2 Room export or installed database has been recovered;
- that v1→v2 or v2→v3 migration classes ever existed;
- that production is currently protected from destructive fallback when opening an unsupported old database;
- that a future v3→v4 migration works, because v4 does not exist.

No test-only `Migration(1,2)` or `Migration(2,3)` was fabricated to make the suite green.

## CI evidence

First green executable head before this evidence document:

`1b6867cd201eb9620211541163f1c4acc57ad7e4`

GitHub Actions:

- workflow: `KMP I0 Baseline Verification`
- run: `35423233503`
- result: **SUCCESS**
- I6 Room safety test step: **SUCCESS**
- architecture guard: **SUCCESS**
- existing I0/I5 deterministic/cutover tests: **SUCCESS**
- Android debug APK assembly: **SUCCESS**

Artifacts:

- `kmp-i0-unit-test-reports`
  - artifact ID: `10578726388`
  - SHA-256 digest: `076ddbae816ccd90316bcd41d2cb6d14f34039f96f18d43fa00130ea15dffcbe`
- `paniclab-i5-physical-smoke-apk`
  - artifact ID: `10578221739`
  - SHA-256 digest: `3a8843b0c755ce733b2d575ff5c850c83a7659c91cc433f0b1a114e1489e51b6`

The APK artifact is a regression/build by-product of the existing workflow; no new physical smoke is required for this test-only Room harness because production Room behavior and product schema were not changed.

## TASK-KMP-061 result

**DONE for harness establishment and current preservation baseline.**

Evidence status:

- current v3 strict reopen preservation — VERIFIED
- reconstructed v1 fail-closed preservation — VERIFIED TEST HARNESS
- reconstructed v2 fail-closed preservation — VERIFIED TEST HARNESS
- historical v1→v2 migration — UNSUPPORTED / NOT PROVEN
- historical v2→v3 migration — UNSUPPORTED / NOT PROVEN
- production destructive fallback behavior — unchanged
- future v3→next-version migration — PENDING a real future schema change

## Handoff to TASK-KMP-062

TASK-KMP-062 may now make the persistence architecture decision separately.

The decision must compare at least:

- keeping Android Room v3/native persistence and adding explicit future Android migrations only when needed;
- moving persistence to Room KMP later;
- data-migration risk;
- tooling/schema export implications;
- iOS persistence needs;
- testability and maintenance cost.

TASK-KMP-062 is a decision/ADR task. It does not authorize a Room KMP implementation by itself.
