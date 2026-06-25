# IP TV Player

Modern Android IPTV player scaffold for Android TV / Google TV, Xiaomi TV Box S 3rd Gen, phones, and tablets.

The project starts from `00_MASTER_BLUEPRINT.md` and currently includes:

- multi-module Kotlin/Compose project structure
- personal/play product flavors
- source management UI for JSON directory, M3U URL, and Xtream inputs
- M3U and JSON directory parser foundations
- secret redaction utilities
- Media3 player factory and capability model
- unit tests for parser, search normalization, and redaction

## Build

```powershell
.\gradlew.bat test
.\gradlew.bat :app:assemblePersonalDebug
```

## Release APK

Personal release builds are signed from local-only values in `local.properties` or matching environment variables:

- `IPTVBOX_PERSONAL_STORE_FILE`
- `IPTVBOX_PERSONAL_STORE_PASSWORD`
- `IPTVBOX_PERSONAL_KEY_ALIAS`
- `IPTVBOX_PERSONAL_KEY_PASSWORD`

The generated local signing key is stored outside the repository at `C:\Users\EVO-MRDM\.android\iptvbox-personal-release.jks`.

```powershell
.\gradlew.bat :app:assemblePersonalRelease
```

Signed APK:

```text
app/build/outputs/apk/personal/release/app-personal-release.apk
```

Verify the signature with the Android SDK tool:

```powershell
$apk = "app\build\outputs\apk\personal\release\app-personal-release.apk"
& "$env:LOCALAPPDATA\Android\Sdk\build-tools\36.0.0\apksigner.bat" verify --verbose --print-certs --min-sdk-version 23 $apk
```
