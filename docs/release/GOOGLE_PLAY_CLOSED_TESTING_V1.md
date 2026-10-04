# PanicLab — Google Play Closed Testing v1

## Release baseline

- Branch: `release/play-closed-testing-v1`
- Base: `main`
- Application ID: `uy.eliasworks.paniclab`
- Version code: `1`
- Version name: `1.0`
- Target: Google Play Closed Testing

## Signing model

PanicLab uses a dedicated Google Play upload key with alias:

`upload`

Signing material MUST remain outside Git. The repository ignores:

- `*.jks`
- `*.keystore`
- `keystore.properties`
- `/release-signing/`

The release build consumes the existing environment variables:

- `KEYSTORE_PATH`
- `STORE_PASSWORD`
- `KEY_PASSWORD`

## GitHub Actions secrets

Before running the Play release workflow, configure these repository secrets:

- `PLAY_UPLOAD_KEYSTORE_B64`
- `PLAY_UPLOAD_STORE_PASSWORD`
- `PLAY_UPLOAD_KEY_PASSWORD`

`PLAY_UPLOAD_KEYSTORE_B64` must contain the complete upload keystore encoded as a single-line Base64 string.

Never commit the keystore, its Base64 representation, or passwords.

## Build workflow

Workflow:

`.github/workflows/android-play-release.yml`

Trigger:

`workflow_dispatch`

The workflow:

1. checks out the selected release ref;
2. restores the upload keystore into the runner temporary directory;
3. verifies alias `upload`;
4. builds `:app:bundleRelease`;
5. verifies the AAB signature with `jarsigner -verify -strict`;
6. produces SHA-256;
7. uploads `paniclab-play-release-aab` as a GitHub Actions artifact.

Expected bundle:

`app/build/outputs/bundle/release/app-release.aab`

## Pre-upload gate

Before uploading to Google Play:

- Android physical smoke: PASS
- package ID equals `uy.eliasworks.paniclab`
- versionCode is unique and increasing
- versionName is correct
- AAB is signed with the dedicated upload key
- `jarsigner -verify -strict` passes
- SHA-256 is recorded
- no debug keystore is used for Play

## Closed Testing

Upload the signed AAB to the Closed Testing track in Play Console.

Keep the testing group enrolled continuously for the required testing period. Testers should install from Google Play, exercise real PanicLab flows, and provide genuine feedback. Do not condition rewards on positive ratings or reviews.

## Key custody

Keep at least two secure backups of the upload keystore and its credentials outside the repository. The upload key is operational signing material and must not be treated as a normal project file.
