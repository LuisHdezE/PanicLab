# PanicLab KMP Migration — Discovery / Baseline Audit

**State:** VERIFIED baseline audit
**Audit date:** 2026-09-18
**Repository:** `LuisHdezE/PanicLab`
**Baseline branch:** `main`
**Baseline SHA:** `c165ae4283a3b592eddb2b70a1c13a9ffeb51f01`

> This document records verified current facts and migration seams. It does not authorize implementation and it is not a PLAN.

## 1. Repository baseline

- `main` still points to `c165ae4283a3b592eddb2b70a1c13a9ffeb51f01`.
- The repository is currently private.
- The project is Android-only today.
- Gradle includes only `:app`.
- There is no Kotlin Multiplatform target or shared source set in the current baseline.
- No `.github` directory is present in this baseline and no GitHub Actions run is associated with the audited SHA.
- No Kover configuration was found.

## 2. Current Android stack

Verified dependencies and platform facilities include:

- Jetpack Compose / Material 3
- Android Navigation Compose
- Room
- DataStore Preferences
- CameraX
- Google ML Kit text recognition
- Firebase AI / App Check
- Retrofit / OkHttp / Moshi
- Kotlin coroutines
- Robolectric
- Roborazzi
- Android instrumentation test dependencies

The existing Android application must remain functional while KMP is introduced incrementally.

## 3. Verified diagnostic pipeline

`DiagnosticRepositoryImpl.analyzeLog()` currently orchestrates:

1. initialize bundled/default knowledge base if needed
2. `LogNormalizer`
3. `MetadataExtractor`
4. `DeviceResolver`
5. `PanicClassifier`
6. `SensorExtractor`
7. `EvidenceExtractor`
8. `DiagnosticRulesEngine`
9. `CandidateRanker`
10. `DiagnosticReportBuilder`
11. Room persistence of session, evidence and candidates

This observable sequence is the primary behavior contract to preserve during extraction.

## 4. Portability matrix

Classification meanings:

- **COMMON:** behavior is already platform-independent or requires only trivial cleanup.
- **ADAPTABLE:** business behavior should be shared, but current implementation contains JVM/Android/data-layer coupling that must be removed or isolated.
- **PLATFORM:** capability is inherently platform-facing or currently bound to Android APIs and must remain behind a platform boundary unless separately proven portable.

| Area / component | Classification | Verified reason |
| --- | --- | --- |
| `LogNormalizer` | COMMON | Pure deterministic string transformation. |
| `CandidateRanker` | COMMON | Depends only on domain models; deterministic mapping. |
| `RulePackDiffCalculator` | COMMON | Pure deterministic comparison of domain rule-pack models. |
| `DiagnosticRulesEngine` | COMMON after dependency cleanup | Core logic is deterministic; current dependency on `HexUtils` must be portable. |
| `HexUtils` | ADAPTABLE (small) | Logic is portable; current formatting imports `java.util.Locale`. |
| `PanicClassifier` | ADAPTABLE (small) | Deterministic behavior; current regex implementation uses `java.util.regex.Pattern`. |
| `SensorExtractor` | ADAPTABLE (small) | Deterministic parsing; current regex implementation uses `java.util.regex.Pattern`. |
| `MetadataExtractor` | ADAPTABLE | Deterministic extraction, but uses `org.json.JSONObject` and Java regex. |
| `EvidenceExtractor` | ADAPTABLE | Core extraction is deterministic; IDs use `java.util.UUID`. |
| `DiagnosticReportBuilder` | ADAPTABLE | Core assembly is deterministic; IDs/time use JVM APIs. |
| `RulePackValidator` | ADAPTABLE (small) | Validation is deterministic; current regex uses Java `Pattern`. |
| `RulePackJsonParser` | ADAPTABLE / split required | Combines JSON parsing, domain construction and Room entity construction. |
| `DeviceResolver` | ADAPTABLE / split required | Static device resolution is portable; database lookup depends on `DeviceDao` and Room entity mapping. |
| `OcrLogExtractor` | ADAPTABLE | OCR text cleanup and signature extraction are deterministic; current code uses Java regex/Locale. |
| `HashUtils` | PLATFORM seam or portable replacement required | Current SHA-256 implementation uses `java.security.MessageDigest`. |
| Trend domain models | ADAPTABLE / split required | Models are portable but contain `org.json` serialization methods inside Domain. |
| `UnknownCaseRedactor` | ADAPTABLE | Sanitization is potentially shareable; current implementation mixes JSON and JVM date/time APIs. |
| Text report formatting | ADAPTABLE / split required | Formatting can be shared; current generator also performs Android clipboard/share/toast effects. |
| Room database / DAOs / entities | PLATFORM/INFRASTRUCTURE boundary today | Current implementation uses Android `Context` and Room Android builder. Persistence strategy requires separate design. |
| DataStore settings implementation | PLATFORM boundary today | Current implementation depends on Android `Context`. |
| Camera capture | PLATFORM | CameraX and Android camera lifecycle. |
| ML Kit image recognition | PLATFORM | Current image analyzer uses Android image types, CameraX and ML Kit. |
| Gemini grounding implementation | PLATFORM | Current implementation uses Android logging/application config and Firebase AI. |
| PDF generation | PLATFORM | Current implementation uses Android `PdfDocument`, graphics and filesystem APIs. |
| File import / Android URI handling | PLATFORM | Android URI/content APIs. |
| Share / clipboard / toast effects | PLATFORM | Android framework effects. |
| Navigation and Compose screens | PLATFORM | Existing Android native UI. Future iOS UI is expected to be native SwiftUI. |

## 5. Domain and layering findings

### VERIFIED — data entity leak in Domain contract

`KnowledgeBaseRepository` exposes Room data-layer types directly:

- `Flow<RulePackEntity?>`
- `Flow<List<RulePackEntity>>`

A shared Domain contract must not depend on `com.example.data.local.entity`.

### VERIFIED — serialization leak in Domain

`TrendDashboardModels.kt` imports `org.json.JSONArray` / `JSONObject` and domain models implement JSON serialization directly.

The statistical/domain data can be shared, but serialization is a separate concern.

### VERIFIED — presentation state located in Domain package

`DomainModels.kt` currently contains `RepairSuggestionUiState`.

This is presentation-facing state and should not be treated automatically as a Domain primitive during KMP extraction.

### VERIFIED — time and ID creation inside Domain-facing models/builders

Current code uses JVM/system facilities such as:

- `System.currentTimeMillis()`
- `java.util.UUID`

Shared deterministic tests will need controllable time/ID behavior where these values affect observable output.

## 6. Room and data-preservation baseline

Current `AppDatabase` is:

- Room database version **3**
- `exportSchema = false`
- built with `.fallbackToDestructiveMigration()`

This creates a real preservation risk for installed user history when schema versions change.

Important correction to historical notes: the current audited database is already **v3**. Therefore any future migration test must start from the actual schema lineage that can be recovered/verified; a hypothetical v1 → v2 path must not be presented as the current migration requirement without evidence.

No persistence rewrite or migration is authorized by this discovery.

## 7. Existing QA assets

Verified test assets include:

- `DeterministicDiagnosticEngineTest.kt`
- `OcrLogExtractorTest.kt`
- `RepairGroundingServiceTest.kt`
- `RoomDiagnosticStorageTest.kt`
- `RulePackManagementTest.kt`
- `TextSummaryReportGeneratorTest.kt`
- Robolectric smoke/example tests
- Roborazzi screenshot test
- one Android instrumentation example test

### Strong reusable behavior contracts

`DeterministicDiagnosticEngineTest` already exercises the diagnostic chain for representative cases including:

- hex/decimal code equivalence
- iPhone 13 mini `0x1000`
- iPhone 14 `0x500000`
- iPhone 16 Pro decimal `3145728`
- missing sensor `PRS0`
- unknown SMC fallback behavior

`RulePackManagementTest` already exercises:

- rule-pack parsing
- rule-pack validation
- invalid SemVer/source references
- duplicate IDs
- rule-pack diff behavior
- unknown-case redaction

`RoomDiagnosticStorageTest` verifies Room insert/retrieve/search/update behavior in memory under Robolectric.

### QA gaps

- deterministic tests currently run through Robolectric rather than a true KMP `commonTest` host
- no verified Kotlin/Native tests
- no verified iOS XCTest/XCUITest
- no coverage gate was found
- no checked-in GitHub Actions workflow was found
- current instrumentation coverage is only a minimal example baseline
- no verified installed-upgrade data preservation test exists

## 8. CI / zero-cost constraint

The repository is private at this baseline.

The validated KMP laboratory proved the zero-cost GitHub-hosted Android+iOS CI model for a **public** repository using standard runners. That proof must not be silently generalized to this private repository.

Until repository visibility or another validated path is decided, private-repository zero-cost iOS CI remains **PENDING**.

## 9. Migration invariants

The following invariants are carried forward from the validated KMP laboratory:

- share behavior/business logic when semantics should be identical
- keep Android UI native Compose
- keep iOS UI native SwiftUI
- do not optimize for a vanity percentage of shared code
- do not allow Android/JVM-only dependencies to leak into shared behavior
- do not rewrite the application in one change
- preserve Android behavior while shared code is extracted incrementally
- do not claim iOS support until real iOS evidence exists
- keep coverage metrics separated by execution target
- do not reduce QA thresholds merely to make CI green

## 10. Pending decisions for SPEC / PLAN

These are not resolved by discovery:

1. whether `PanicLab` will become public for the validated USD0 GitHub-hosted iOS CI path
2. exact persistence strategy and timing for Room/KMP persistence
3. recoverable Room schema lineage needed for non-destructive upgrade tests
4. whether SHA-256 uses a multiplatform library, expect/actual boundary or another verified approach
5. exact JSON serialization approach for rule packs and trend payloads
6. exact ID/clock abstraction strategy
7. sequencing of Camera/OCR, Gemini, PDF, import/share and other native capabilities on iOS
8. exact iOS minimum deployment target

These belong to an approved PLAN or later feature-specific SPECs, not to discovery assumptions.

## 11. Discovery conclusion

PanicLab is a strong candidate for incremental KMP extraction because its central diagnostic engine is already largely deterministic and its existing tests encode valuable behavior.

The safest migration boundary is not “Android app to shared UI”. It is the diagnostic/domain brain and its deterministic transformations, with Android and iOS retaining native platform surfaces.

No application code was changed as part of this discovery.