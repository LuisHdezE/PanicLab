# I7 iOS preflight — TASK-KMP-070

Base: `main@7543f2053f1aa5d626b918297816337ad34d8225`.

## Purpose

Establish an evidence-based iOS deployment baseline and a USD0 GitHub Actions path before creating the native SwiftUI product slice in TASK-KMP-071.

This task does not create `iosApp`, does not add product UI, does not add persistence, and does not introduce Apple signing credentials.

## Repository and CI facts

- Repository: `LuisHdezE/PanicLab`.
- Visibility verified through GitHub API: **public**.
- Current shared module already declares `iosArm64()` and `iosSimulatorArm64()` and produces a static `Shared.framework` for both targets.
- Existing `KMP I1 Scaffold Verification` already links both frameworks on the standard `macos-latest` GitHub-hosted runner.
- GitHub's current billing documentation states that standard GitHub-hosted runners are free for public repositories. Larger runners are excluded from that free-use rule and are not permitted for this USD0 path.

Official references:

- https://docs.github.com/en/billing/concepts/product-billing/github-actions
- https://docs.github.com/en/actions/reference/runners/github-hosted-runners

## Selected deployment baseline

**iOS 15.0** is the selected minimum deployment target for the I7 native diagnostic slice.

Rationale:

1. PanicLab currently uses Kotlin `2.4.20`.
2. Kotlin 2.4 raised the default minimum iOS target from 14.0 to **15.0**.
3. The project has no approved product requirement to support iOS 14 or older.
4. Supporting a lower target would require an explicit Kotlin/Native compiler override and would expand the compatibility surface without a demonstrated product need.
5. TASK-KMP-071 must therefore stay within APIs available on iOS 15 unless a later, separately reviewed requirement deliberately raises the application deployment target.

Official references:

- https://kotlinlang.org/docs/native-target-support.html
- https://kotlinlang.org/docs/whatsnew24.html
- https://kotlinlang.org/docs/whatsnew2420.html

## Executable verification added by TASK-KMP-070

`.github/workflows/kmp-i7-ios-preflight.yml` runs only on a standard `macos-latest` GitHub-hosted runner and:

1. records the actual runner architecture, macOS version, Xcode version and installed SDK list;
2. configures JDK 21 and Gradle 9.3.1, matching the existing KMP lane;
3. links `Shared.framework` for `iosArm64` and `iosSimulatorArm64`;
4. inspects each produced Mach-O binary with `xcrun vtool -show-build`;
5. fails if either effective `minos` is not exactly `15.0`;
6. records the environment and framework evidence in the GitHub Actions job summary;
7. uses no Apple certificate, provisioning profile, Team ID, App Store credential or signing secret;
8. uploads no custom artifact in this preflight lane, avoiding unnecessary Actions storage consumption.

A green PR run is mandatory before TASK-KMP-070 can be considered DONE.

## USD0 guardrail

The approved CI path for I7 is:

- repository remains public;
- standard GitHub-hosted `macos-latest` only;
- no larger runner;
- no private macOS runner marketplace service;
- no custom artifact upload from this preflight job;
- no Apple Developer credential required for framework compilation and compatibility checks.

If repository visibility changes to private, this USD0 conclusion is invalid and TASK-KMP-070 must be revisited before continuing iOS CI.

## Public-repository hygiene audit

A focused repository search was performed before adding the I7 lane:

- no committed `AIza` key was found;
- no `BEGIN PRIVATE KEY` material was found;
- `API_KEY` matches are limited to the documented placeholder in `.env.example` and runtime access through `BuildConfig.GEMINI_API_KEY`;
- this is a targeted publication-hygiene check, not a substitute for GitHub secret scanning or a full credential-history audit.

No new secret or signing material is introduced by TASK-KMP-070.

## Boundary for TASK-KMP-071

TASK-KMP-071 may start only after this preflight is green and merged. Its implementation should:

- create a native SwiftUI `iosApp`;
- use iOS 15.0 as the application deployment baseline;
- integrate the existing shared deterministic engine;
- add text/paste diagnostic input and native loading/error/non-conclusive/result states;
- avoid camera/OCR acquisition, AI repair guidance, PDF/export and persistence/history parity in this task;
- keep Room KMP deferred according to `I6_ROOM_KMP_DECISION.md`.

Physical-device signing belongs to TASK-KMP-071 validation and must remain separate from CI secrets unless explicitly required and approved.
