# ADR 0003: Personal Release Signing and Hardening

## Status

Accepted

## Context

Debug APKs are not suitable for sideload testing on Android TV devices. They are signed with the shared debug key, are debuggable, are larger than needed, and can trigger stronger device protection warnings.

The goal is not to bypass Play Protect. The goal is to produce a normal release-quality personal APK with a stable local signing identity and predictable security posture.

## Decision

Add a `personalRelease` signing path backed by local-only properties or environment variables:

- `IPTVBOX_PERSONAL_STORE_FILE`
- `IPTVBOX_PERSONAL_STORE_PASSWORD`
- `IPTVBOX_PERSONAL_KEY_ALIAS`
- `IPTVBOX_PERSONAL_KEY_PASSWORD`

Use a local RSA 4096-bit JKS key at `C:\Users\EVO-MRDM\.android\iptvbox-personal-release.jks`. Do not commit the key or passwords.

Release builds:

- are non-debuggable
- enable R8 minification
- enable resource shrinking
- sign with V1, V2, and V3 schemes because the app still supports `minSdk = 23`
- disable app backup
- allow cleartext traffic in the personal flavor because user-provided IPTV lists and streams are often HTTP-only; the UI must show an explicit warning when an HTTP URL is entered

## Consequences

The installable personal artifact is `app/build/outputs/apk/personal/release/app-personal-release.apk`, not the debug APK. Play Protect may still warn about sideloaded apps from a new local signing certificate, but the APK now follows normal Android release packaging and signing expectations.

The Play flavor should revisit cleartext policy with either provider allowlists or a stricter product decision before store submission.
