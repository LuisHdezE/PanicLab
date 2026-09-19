# I6 / TASK-KMP-062 — Room KMP Adoption Decision

**Date:** 2026-09-19  
**Base:** `main@c7f5c33277ccba13864499dacc325aea42b91422`  
**Decision:** **ADOPT LATER**  
**Scope:** Architecture decision only. No persistence implementation, schema change, DAO/entity move, destructive-fallback change, iOS implementation or Blueprint modification is authorized by this document.

## Question

Should PanicLab move persistence from the current Android-only Room database into the KMP `:shared` module now, or should that migration be deferred until an iOS persistence capability actually requires a shared store?

## Decision

**ADOPT LATER.**

Keep the existing Android Room persistence boundary unchanged through I7. Re-evaluate and, if still justified, implement Room KMP as a separately approved persistence capability in I8 when the native iOS product has a proven need for History/local persistence.

This is not a rejection of Room KMP. It is a sequencing decision driven by PanicLab's current data-safety evidence and product state.

## PanicLab evidence

### Current Android persistence

At the I6 decision base:

- `AppDatabase` is Android-only and uses `androidx.room`.
- database version is `3`;
- six entities are registered: device models, diagnostic rules, diagnostic sessions, diagnostic evidences, diagnosis candidates and rule packs;
- six DAO surfaces back those entities;
- `exportSchema = false`;
- the production builder still calls `fallbackToDestructiveMigration()`;
- the project pins Room `2.7.0` for runtime, KTX and compiler;
- `:shared` has no Room or SQLite dependency.

The DAO API is already broadly migration-friendly because persisted operations are predominantly `suspend` or `Flow`, but the storage model is still an Android implementation detail.

One current entity default, `RulePackEntity.importedAt`, calls `System.currentTimeMillis()`. Moving the entity directly into `commonMain` would therefore violate the migration's deterministic/nondeterminism boundary unless that default is removed or supplied through an explicit clock/caller value.

### I6 lineage evidence

TASK-KMP-060 established:

- Room v1, v2 and v3 declarations can be recovered from repository source;
- historical exported Room schema files were not found;
- historical database artifacts were not found;
- no registered non-destructive v1→v2 or v2→v3 migration path was found;
- recovered builders used destructive fallback;
- therefore historical non-destructive migration behavior must not be invented.

See `I6_SCHEMA_LINEAGE.md`.

### I6 executable preservation evidence

TASK-KMP-061 added `RoomUpgradeSafetyTest` and proved:

- current v3 data survives strict close/reopen;
- reconstructed v1/v2 sentinel fixtures fail closed when no migration path exists;
- those failed strict opens do not destroy the fixture database or sentinel rows;
- the test builder intentionally does not use destructive fallback;
- the production builder remains unchanged.

Final TASK-KMP-061 workflow: `35423527938` — SUCCESS.  
PR #14 merge: `cd63f4f9a790ead3199bc1d1e063c5e905c5e82f`.

See `I6_UPGRADE_HARNESS.md`.

## Current Room KMP platform evidence

As of 2026-09-19, Room KMP is a supported Jetpack path rather than an experimental concept:

- Android Developers lists Android, JVM and iOS as Tier 1 supported platforms for Jetpack KMP libraries.
- Room 3.0 is Kotlin-first and supports Kotlin Multiplatform.
- Room 3.0 uses the new `androidx.room3` package/artifacts, KSP, coroutine-first asynchronous APIs and SQLite Driver APIs instead of the previous SupportSQLite core path.
- Android's migration guidance recommends a staged migration: modernize on current Room 2.x first, then move to Room 3.0.
- the official Room KMP migration path can use `BundledSQLiteDriver`, but the official codelab explicitly notes that bundling SQLite increases binary size on Android and iOS.

External references reviewed for this decision:

- Android Developers — Migrate from Room 2.x to Room 3.0: https://developer.android.com/training/data-storage/room/migration-2-to-3
- Android Developers — Configure a Room database for KMP: https://developer.android.com/kotlin/multiplatform/room
- Android Developers — Migrate existing apps to Room KMP: https://developer.android.com/codelabs/kmp-migrate-room
- Android Developers — Room 3.0 release notes: https://developer.android.com/jetpack/androidx/releases/room3
- Android Developers — Kotlin Multiplatform / Jetpack supported platforms: https://developer.android.com/kotlin/multiplatform

## Options considered

### A. KEEP Android Room indefinitely and use separate native iOS persistence

**Benefits**

- zero migration pressure on the current Android database;
- Android persistence remains isolated and already proven by existing product tests;
- iOS can choose a platform-native implementation independently.

**Costs**

- persistence schemas and repository behavior can diverge across platforms;
- History/persistence features would need duplicate platform implementations and parity tests;
- future cross-platform maintenance cost increases.

**Assessment:** safe in the short term, but unnecessarily commits PanicLab to duplicated persistence before the iOS product surface exists.

### B. ADOPT Room KMP now

**Benefits**

- one shared persistence model for Android and future iOS;
- one DAO/schema surface and one place for persistence behavior;
- aligns with the broader KMP architecture and official Jetpack direction.

**Costs / risks now**

- there is no native iOS persistence consumer yet;
- Android would absorb migration risk solely for a future capability;
- PanicLab is still on Room 2.7.0 while current official guidance recommends staged modernization before Room 3.0;
- the existing DB has no exported historical schema baseline and no proven historical migrations;
- moving entities/DAOs/build configuration and database creation would enlarge the blast radius immediately after I5/I6 stabilization;
- `System.currentTimeMillis()` in a Room entity must be removed from any direct common migration;
- a bundled SQLite driver may increase application binary size;
- introducing Room into `:shared` would expand the COMMON dependency surface and require new architecture/QA gates.

**Assessment:** technically feasible, but poorly timed.

### C. ADOPT LATER when iOS persistence becomes a real capability

**Benefits**

- preserves the now-green Android data boundary;
- avoids speculative platform work;
- allows I7 to prove the native iOS product slice first;
- creates a concrete consumer and acceptance criteria before persistence is shared;
- gives PanicLab time to establish proper schema export and migration evidence before changing the database technology boundary.

**Cost**

- Android remains on its current platform persistence boundary for at least one more increment.

**Assessment:** best fit for the approved strangler sequence and current evidence.

## Re-entry conditions for Room KMP adoption

Room KMP should be reconsidered during the I8 persistence/history capability only after all of the following are satisfied or explicitly dispositioned:

1. TASK-KMP-071/072 have produced a real native iOS diagnostic slice and executable iOS/Kotlin-Native equivalence evidence.
2. iOS History/local persistence is explicitly approved as a required capability.
3. The Android database is modernized in a focused, independently tested step before any cross-platform move. Follow the then-current official Room migration guidance rather than jumping versions opportunistically.
4. Schema export is enabled and a trustworthy current schema baseline is committed for future migration testing.
5. Existing Android v3 user data has an explicit preservation plan. No historical migration path may be invented from source declarations alone.
6. Production destructive fallback is removed only when supported upgrade paths have real migrations and executable preservation evidence.
7. A Room KMP spike proves Android and iOS database creation, DAO operation, close/reopen, foreign-key behavior and representative History reads/writes on actual target runners.
8. The selected SQLite driver and its binary-size/resource consequences are measured and accepted.
9. The `RulePackEntity.importedAt` wall-clock default and any similar platform/nondeterministic persistence behavior are converted to explicit caller/provider values before entering COMMON.
10. Architecture guards and target-specific QA are updated without weakening existing deterministic coverage thresholds.

## Consequences of this decision

Until the re-entry gate is approved:

- Android Room remains in `app`;
- `AppDatabase` remains version 3;
- entities and DAOs remain Android-native;
- `:shared` remains free of Room/SQLite persistence dependencies;
- no schema/version change occurs;
- no migration class is fabricated;
- `fallbackToDestructiveMigration()` is not changed by TASK-KMP-062;
- I7 can proceed without coupling the first iOS product slice to persistence migration;
- the I8 persistence/history capability must revisit this ADR rather than assuming either native duplication or Room KMP adoption.

## TASK-KMP-062 result

**ADOPT LATER** — Room KMP is technically viable and officially supported for Android/iOS, but adoption is deferred until iOS persistence is an approved product need and Android data-preservation prerequisites are stronger.
