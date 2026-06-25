# ADR 0001: Initial Android Stack

## Status

Accepted

## Context

The blueprint targets Kotlin, Jetpack Compose, Compose for TV, Media3, Room/DataStore later, Gradle 9.4.1, AGP 9.2.x, Kotlin 2.4.0, Compose BOM 2026.06.00, Media3 1.10.1, minSdk 23, compileSdk 37, and targetSdk 37.

Official metadata confirms stable AGP 9.2.1, Kotlin 2.4.0, Compose BOM 2026.06.00, Compose TV Material 1.1.0, and Media3 1.10.1 are available. The local SDK initially had Android 34 and 35 installed. Android 36 was available through SDK Manager and installed during setup. The Android repository exposes build-tools 37, but `platforms;android-37` is not present in the local or remote package list checked during setup.

## Decision

Use the blueprint dependency line where possible:

- Gradle wrapper: 9.4.1
- AGP: 9.2.1
- Kotlin: 2.4.0
- Compose BOM: 2026.06.00
- Compose TV Material: 1.1.0
- Media3: 1.10.1
- minSdk: 23

Use `compileSdk = 36` and `targetSdk = 36` for the first executable local skeleton. Upgrade both to 37 as soon as the Android 37 platform package is installed and compatible dependencies require it.

## Consequences

The project can build on the current machine without inventing an unavailable SDK platform. The version catalog keeps SDK values centralized, so the later upgrade is a one-file change plus validation.
