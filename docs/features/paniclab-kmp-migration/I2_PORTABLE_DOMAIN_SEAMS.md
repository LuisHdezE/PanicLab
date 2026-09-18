# I2 — Portable Domain and Nondeterminism Seams

**Branch:** `kmp/i2-portable-domain-seams`  
**Base:** `main@8d8852d0693a8c7e099cbc9a23ff48d47cba266d`  
**Authorization:** I2 explicitly authorized by Luis on 2026-09-18.  
**Authorized tasks:** TASK-KMP-020 and TASK-KMP-021 only.  
**Product diagnostic-engine cutover:** Not authorized in I2.

## Objective

Extract the portable domain types and deterministic primitives needed by the future shared diagnostic engine while preserving the current Android execution path and behavior.

## Domain boundary

Moved to `shared/commonMain` under their existing `com.example.domain.model` package so Android consumers keep stable type names:

- panic families;
- confidence and verification enums;
- panic/sensor code model;
- diagnostic rule/device/evidence/candidate models;
- parsed metadata;
- diagnostic report;
- rule-pack domain/validation/diff models.

The shared `DiagnosticReport` no longer reads wall-clock time through a default constructor value. `createdAt` is explicit.

Intentionally retained in the Android `app` module:

- `SearchGroundingSource`;
- `GroundedRepairSuggestion`;
- `RepairSuggestionUiState`.

These models belong to remote grounding/presentation concerns rather than the deterministic diagnostic core.

## Portable deterministic primitives

Moved to `shared/commonMain`:

- `HexUtils`, with Java `Locale` removed;
- `LogNormalizer`;
- `Clock` contract;
- `IdGenerator` contract;
- `Sha256Hasher` contract.

`PanicCode` continues to preserve the existing decimal/hex semantics by consuming the shared `HexUtils`.

## Platform seams

Android-specific implementations live outside COMMON:

- `SystemEpochClock` uses `System.currentTimeMillis()`;
- `UuidIdGenerator` uses `UUID.randomUUID()`;
- `AndroidSha256Hasher` uses `MessageDigest`.

A JVM-only `JvmSha256Hasher` exists for deterministic known-vector verification in CI. No Java/JVM hashing API is imported by COMMON.

The current Android `HashUtils.sha256()` API remains available and delegates to `AndroidSha256Hasher`, so rule-pack parsing behavior is preserved without migrating the parser early.

## Android compatibility wiring

`app` now depends on `:shared` to consume the extracted domain/primitives. This is a type/utility extraction only. Android still owns and executes:

- diagnostic orchestration;
- metadata/panic/sensor/evidence parsing other than `LogNormalizer`;
- rules engine/ranking/report assembly implementation;
- Room/DataStore;
- CameraX/ML Kit;
- Firebase/Gemini;
- Compose/navigation;
- export/share/PDF.

`DiagnosticReportBuilder` accepts injectable `Clock` and `IdGenerator` with Android-compatible defaults.

`EvidenceExtractor` accepts an injectable `IdGenerator` with the same Android UUID default.

## QA design

COMMON/JVM/Android-host validation includes:

- frozen decimal/hex equivalence values from I0;
- canonical hex, invalid/overflow parsing and bitmask branches;
- escaped text and line-ending normalization;
- explicit/fixed clock and ID providers;
- explicit report timestamp construction;
- SHA-256 known vectors;
- Android SHA-256 parity;
- Android builder/evidence fixed-provider integration;
- COMMON architecture guard;
- Kover >=90% line and >=85% branch for the deterministic I2 utility/hash scope;
- existing I0 Android deterministic regression workflow;
- iOS Arm64 + Simulator Arm64 framework compilation.

## Non-goals

I2 does not:

- port metadata extraction, panic classification, sensor extraction or evidence extraction logic to COMMON;
- port the rules engine, ranking or report builder to COMMON;
- port rule-pack JSON parsing;
- alter Room schema or persistence;
- introduce SwiftUI;
- modify Blueprint;
- begin I3.

## Evidence status

Implementation prepared. GitHub Actions evidence is PENDING until the I2 PR executes on the final implementation HEAD.
