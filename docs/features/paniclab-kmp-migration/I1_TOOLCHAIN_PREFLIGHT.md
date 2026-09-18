# I1 — KMP Toolchain Compatibility Preflight

**Branch:** `kmp/i1-toolchain-scaffold`  
**Base:** `main@a6a00ac153177dacfe7a18c229f3d48cbc16a976`  
**Authorization:** I1 explicitly authorized by Luis on 2026-09-18.  
**Product cutover:** Not authorized in I1.

## Decision

PanicLab cannot safely introduce Kotlin Multiplatform while keeping its previous Kotlin `2.2.10` together with AGP `9.1.1`. The official Kotlin Multiplatform compatibility table supports Kotlin `2.2.0–2.2.10` only through AGP `8.10.0`, while Kotlin `2.4.20` supports AGP `8.5.2–9.3.1` and Gradle through `9.7.0`.

The I1 scaffold therefore performs the minimum compatibility move needed for the existing AGP/Gradle baseline instead of downgrading Android tooling or copying laboratory versions without analysis.

## Selected stack

| Component | Before I1 | I1 selection | Decision |
| --- | --- | --- | --- |
| Android Gradle Plugin | 9.1.1 | 9.1.1 | KEEP |
| Gradle wrapper | 9.3.1 | 9.3.1 | KEEP |
| Kotlin / KGP | 2.2.10 | 2.4.20 | UPGRADE — required for supported KMP + AGP 9.1.1 combination |
| KSP | 2.3.5 | 2.3.12 | UPGRADE — current stable; includes Kotlin 2.4/AGP 9 and Android-KMP fixes from the 2.3.10+ line |
| Android-KMP plugin | none | com.android.kotlin.multiplatform.library 9.1.1 | ADD — official Android KMP library plugin |
| Kover | none | 0.9.8 | ADD — stable documented KMP/JVM coverage plugin; configuration-cache compatible |
| kotlinx.serialization plugin | none | 2.4.20 | SELECTED, not applied in I1 |
| kotlinx.serialization JSON runtime | none | 1.11.0 | SELECTED for later rule-pack work, not consumed in I1 |
| CI JVM | 21 | 21 | KEEP — required by the existing API 36 Robolectric baseline |
| Android bytecode target | JVM 11 | JVM 11 | KEEP |

The `1.12.0-RC` serialization runtime is intentionally not selected because I1 uses stable dependencies only and serialization is not required by the scaffold itself.

## Android target strategy

I1 uses `com.android.kotlin.multiplatform.library`, not legacy `com.android.library`. With AGP 9.x this is the officially supported Android target plugin for KMP libraries.

`app` remains an Android application and is **not** converted to KMP. It is not given a dependency on `shared` in I1. The migration remains a strangler-style extraction rather than a rewrite.

Shared Android configuration:

- namespace `com.aistudio.paniclab.shared`
- compileSdk `36`
- minSdk `24`
- JVM target `11`
- host-test support enabled for common/JVM-host validation

## Apple target strategy

The isolated `shared` module declares:

- `iosArm64`
- `iosSimulatorArm64`
- static framework with base name `Shared`

I1 proves framework compilation only. It does not claim an iOS application, device installation, UI parity, persistence parity or production iOS readiness.

## Coverage truthfulness

Kover is used only for JVM/Android-host executable coverage. Native/iOS execution is **not** folded into a fabricated global percentage.

The configured verification floor is:

- line coverage >= 90%
- branch coverage >= 85%

In I1 these thresholds exercise only the non-product scaffold probe. They become a meaningful product gate only when deterministic production code starts entering `shared` in later increments.

## I1 validation matrix

| Evidence | Runner | Command / intent |
| --- | --- | --- |
| Android product regression | ubuntu-latest | existing I0 deterministic suites remain green after Kotlin/KSP upgrade |
| shared Android/common host tests | ubuntu-latest | execute shared host/common test plumbing |
| Kover XML/HTML + verification | ubuntu-latest | generate JVM/Android-host report and enforce 90/85 gate |
| common boundary guard | ubuntu-latest | `scripts/verify-kmp-common-boundary.sh` |
| iOS device framework compile | macos-latest | link `Shared` for `iosArm64` |
| iOS simulator framework compile | macos-latest | link `Shared` for `iosSimulatorArm64` |

## Non-goals of I1

- no diagnostic classes moved from `app`;
- no Android runtime dependency on `shared`;
- no Room/DataStore migration;
- no CameraX/ML Kit/Firebase migration;
- no SwiftUI app;
- no rule-pack parser migration;
- no Blueprint change.

## Sources used for the preflight

- Kotlin Multiplatform compatibility guide: https://kotlinlang.org/docs/multiplatform/multiplatform-compatibility-guide.html
- Kotlin Multiplatform Gradle DSL reference: https://kotlinlang.org/docs/multiplatform/multiplatform-dsl-reference.html
- Android KMP library plugin guide: https://developer.android.com/kotlin/multiplatform/plugin
- Kotlin/Native Apple framework documentation: https://kotlinlang.org/docs/apple-framework.html
- Kover Gradle plugin documentation: https://kotlin.github.io/kotlinx-kover/gradle-plugin/
- KSP releases: https://github.com/google/ksp/releases
- kotlinx.serialization releases: https://github.com/Kotlin/kotlinx.serialization/releases
