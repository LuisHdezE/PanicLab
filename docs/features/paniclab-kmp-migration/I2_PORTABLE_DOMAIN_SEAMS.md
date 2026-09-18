# I2 — Portable Domain and Nondeterminism Seams

**Branch:** `kmp/i2-portable-domain-seams`  
**Base:** `main@8d8852d0693a8c7e099cbc9a23ff48d47cba266d`  
**Authorization:** I2 explicitly authorized by Luis on 2026-09-18.  
**Authorized tasks:** TASK-KMP-020 and TASK-KMP-021 only.  
**Android product cutover:** Not authorized in I2.

## Objective

Create and validate the portable domain types and deterministic primitives needed by the future shared diagnostic engine while preserving the current Android execution path unchanged.

I2 follows the strangler sequence from the approved PLAN: shared types are established beside the Android implementation first. Android will consume the shared engine during the later cutover increment, not during this extraction step.

## Domain boundary

Added to `shared/commonMain` under the existing `com.example.domain.model` naming so semantics remain traceable to the Android baseline:

- panic families;
- confidence and verification enums;
- panic/sensor code model;
- diagnostic rule/device/evidence/candidate models;
- parsed metadata;
- diagnostic report;
- rule-pack domain/validation/diff models.

The shared `DiagnosticReport` requires `createdAt` explicitly and therefore cannot read wall-clock time implicitly.

Android retains its existing domain models in I2. This is deliberate. Moving Android consumers to the shared types would be a product cutover and would force cross-module consumer adaptations before the approved cutover phase.

The following concerns remain outside the deterministic shared domain:

- remote search grounding;
- repair-suggestion presentation state;
- Android UI state.

## Portable deterministic primitives

Added to `shared/commonMain`:

- `HexUtils`, with Java `Locale` removed;
- `LogNormalizer`;
- `Clock` contract;
- `IdGenerator` contract;
- `Sha256Hasher` contract.

`PanicCode` preserves the existing decimal/hex semantics through the portable shared `HexUtils`.

Android keeps its current production `HexUtils`, `LogNormalizer`, `HashUtils`, report builder and evidence extractor in I2. Those remain the behavioral baseline until the later Android cutover.

## Platform seams

Target-specific implementations are isolated outside COMMON:

- Android `SystemEpochClock` uses `System.currentTimeMillis()`;
- Android `UuidIdGenerator` uses `UUID.randomUUID()`;
- Android `AndroidSha256Hasher` uses `MessageDigest`;
- JVM `JvmSha256Hasher` exists for deterministic known-vector verification in CI.

No Java/JVM hashing, UUID or wall-clock API is imported by COMMON.

These implementations prove the boundary contracts without changing PanicLab's current Android production path.

## QA design

COMMON/JVM/Android-host validation includes:

- frozen decimal/hex equivalence values from I0;
- canonical hex, invalid/overflow parsing and bitmask branches;
- escaped text and line-ending normalization;
- fixed clock and ID providers;
- explicit report timestamp construction;
- SHA-256 known vectors;
- Android SHA-256 provider verification;
- Android runtime-provider smoke coverage;
- COMMON architecture guard;
- Kover >=90% line and >=85% branch for the deterministic I2 utility/hash scope;
- existing I0 Android deterministic regression workflow;
- iOS Arm64 + Simulator Arm64 framework compilation.

## Cross-module preflight finding

An initial I2 attempt wired `app -> :shared` immediately. Kotlin correctly rejected several existing Android smart-casts because nullable public properties from another module are not considered stable smart-cast targets.

That result exposed a sequencing issue rather than a domain defect. Adapting UI/services now would perform part of the Android cutover before its approved increment. I2 therefore keeps Android on its existing models while validating the portable parallel boundary. The smart-cast adaptations are deferred to the actual Android cutover, where they can be reviewed as one explicit migration step.

## Non-goals

I2 does not:

- switch Android production code to shared domain types;
- port metadata extraction, panic classification, sensor extraction or evidence extraction logic to COMMON;
- port the rules engine, ranking or report builder to COMMON;
- port rule-pack JSON parsing;
- alter Room schema or persistence;
- introduce SwiftUI;
- modify Blueprint;
- begin I3.

## Evidence status

Implementation prepared. Final GitHub Actions evidence is pending on the corrected I2 HEAD.
