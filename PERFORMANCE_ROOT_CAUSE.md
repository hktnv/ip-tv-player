# Performance Root Cause and Runtime Report

## 2026-06-25 Android TV Emulator Runtime Result

This run was performed with the release APK on an Android TV / Google TV emulator configured on drive `E:`:

- SDK: `E:\Codex\Android\sdk`
- AVD: `E:\Codex\Android\avd\IPTV_TVBox_API36.avd`
- Android image: `system-images;android-36;google-tv;x86_64`
- RAM: 2048 MB
- data partition: 32 GB
- resolution: 1920 x 1080
- APK: `app\build\outputs\apk\personal\release\app-personal-release.apk`
- M3U file served locally to emulator: `http://10.0.2.2:8765/yCEUbXB9_playlist.m3u`

No files were deleted, moved, or uninstalled from drive `C:`.

### Disk and Tooling Inventory

Drive space at the time of the emulator run:

- `C:` free: about 5.04 GB
- `E:` free: about 341.01 GB

Existing locations on `C:` were only inspected:

- `C:\Users\EVO-MRDM\AppData\Local\Android\Sdk`: about 4.12 GB
- `C:\Users\EVO-MRDM\.android\avd`: about 0.78 GB
- `C:\Users\EVO-MRDM\.gradle`: about 1.81 GB
- `C:\Program Files\Android\Android Studio`: about 2.59 GB

New/current test locations on `E:`:

- `E:\Codex\Android\sdk`: about 9.74 GB
- `E:\Codex\Android\avd`: about 2.11 GB
- `E:\Codex\Android\user-home`: about 0 GB
- `E:\Codex\GradleCache`: about 1.16 GB
- `E:\Codex\IPTVPlayerTest`: about 0.02 GB

### Runtime Metrics

Clean release install and import of the real 8,399 item M3U:

- cold start `am start -W TotalTime`: 380 ms
- home first draw: 388 ms
- playlist import total: 7,383 ms
- imported item count: 8,399
- press to first response: 0 ms
- URL normalize: 0 ms
- connection open: 15 ms
- download/read: 170 ms
- M3U line read: 20 ms
- M3U parse: 1,467 ms
- content cleaning: 1,314 ms
- live/movie/series split: 36 ms
- series extraction: 85 ms
- category extraction: 1 ms
- classification total: 122 ms
- loader total: 1,656 ms
- catalog normalization: 4,776 ms
- database total: 908 ms
- database item write: 850 ms
- database metadata save: 1 ms
- database index rebuild: 23 ms
- transaction end: 31 ms
- UI update: 38 ms
- image/logo work: 0 ms
- idle/wait time: 5 ms
- UI locked: no
- max main-thread gap during import: 252 ms
- hidden/unmeasured gap: 0 ms
- catalog screen ready after import: 17 ms
- RAM after import: about 84 MB PSS

Existing-data release start after force stop:

- `am start -W TotalTime`: 307 ms
- cold start onCreate: 4 ms
- restore state: 19 ms
- home first draw: 327 ms
- catalog ready: 24 ms
- search first result after restored query: 41 ms
- RAM with existing data: about 57 MB PSS
- crash / FATAL EXCEPTION in checked logcat slices: none

Menu and user-flow checks:

- Search menu transition: 40 ms
- Favorites menu transition: 56 ms
- Recent menu transition: 35 ms
- Catalog ready during menu transitions: 19-32 ms
- Search first result for `trt`: 14 ms on the first search run, 8 ms after favorite toggle
- One OK press on a focused content card opened the player.
- Back returned from player to catalog.
- Favorites showed the item added from search.
- Recent showed the item opened with OK.

### Result

The old 110 second TV Box import profile is not reproduced in the emulator after the parser/import/focus fixes. The hidden 60 second gap is now explicitly measured and reported as `0 ms`; the total import time is closed by measured phases plus explicit idle time.

## Current Root Cause

The TV Box failure was not explained by the old coarse metrics. The old report measured download, parser, classification, database and UI update, but it did not account for every app-side phase between pressing `Kaydet ve Yükle` and the first usable catalog state.

Two concrete problems were found in the latest pass:

- M3U parsing and classification were doing too much repeated work per item: repeated `SearchNormalizer` calls, repeated regex construction/use, duplicate series detection, and normalized hashing for stable IDs.
- Runtime telemetry wrote the diagnostics file on every individual metric update. During import this could add hidden I/O and recomposition pressure that was not attributed to any import phase.

## Fixes Applied

- `M3uPlaylistParser` now measures and reports:
  - line reading
  - total parse
  - parse other
  - content cleaning
  - live/movie/series split
  - category extraction
  - series/season/episode extraction
  - classification total
- Parser hot path was optimized:
  - regex constants are compiled once
  - stable item IDs no longer normalize the full URL/title/source string
  - series episode regexes run only when the title looks like it can contain episode data
  - series extraction runs once and is reused for kind selection
  - category derivation avoids recleaning already cleaned group titles
- `SearchNormalizer` now uses static regexes and a cached Turkish locale.
- `RemotePlaylistLoader` now splits URL work into URL normalization, connection open, and body download/read.
- `CatalogStore.replacePlaylistMeasured()` now splits database work into counter calculation, transaction begin, metadata save, item write, transaction end, and total DB time.
- Import telemetry now publishes import metrics in one batch instead of writing the diagnostics report after every metric.
- Import completion now records `playlist_import_idle_wait_ms`, `playlist_import_known_phase_sum_ms`, `playlist_import_overaccounted_ms`, and `playlist_import_unmeasured_gap_ms`.
- The app-side diagnostics screen now shows the detailed phase list, so TV Box testing can reveal where time is actually spent without ADB.
- TV remote selection now uses a shared `tvClickable` handler for custom cards/navigation surfaces, so OK/Enter triggers the focused item with one press.
- Wide/TV layout now groups side navigation and right content separately with Compose focus groups, reducing random focus escape between regions.
- Settings/diagnostics panels are focusable so long reports can be traversed naturally with a remote.

## Runtime Metrics Implemented In APK

These are measured in real app code and shown under `Ayarlar > Performans / Tanılama`:

- `playlist_import_press_to_response_ms`
- `playlist_import_url_normalize_ms`
- `playlist_import_connection_open_ms`
- `playlist_import_download_ms`
- `playlist_import_line_read_ms`
- `playlist_import_parse_ms`
- `playlist_import_parse_other_ms`
- `playlist_import_content_cleaning_ms`
- `playlist_import_kind_split_ms`
- `playlist_import_category_extract_ms`
- `playlist_import_series_extract_ms`
- `playlist_import_db_transaction_begin_ms`
- `playlist_import_db_counter_calc_ms`
- `playlist_import_db_metadata_save_ms`
- `playlist_import_db_write_ms`
- `playlist_import_db_transaction_end_ms`
- `playlist_import_db_total_ms`
- `playlist_import_normalize_ms`
- `playlist_import_image_ms`
- `playlist_import_ui_update_ms`
- `playlist_import_idle_wait_ms`
- `playlist_import_gc_memory_suspect_ms`
- `playlist_import_known_phase_sum_ms`
- `playlist_import_unmeasured_gap_ms`
- `playlist_import_total_ms`
- `playlist_import_item_count`
- `playlist_import_ui_locked`
- `playlist_import_max_main_thread_gap_ms`

The total import time is now closed by measured phases plus explicit idle/wait time. The unmeasured gap is recorded as its own field and is expected to stay at `0 ms`.

## Local Measurements

Environment: Windows desktop JVM/unit tests, not physical TV Box hardware.

Real file: `C:/Users/EVO-MRDM/Desktop/yCEUbXB9_playlist.m3u`

`M3uPlaylistParserTest.parsesRealLargePlaylistWhenProvided`:

- file bytes: 2,515,464
- items: 8,399
- live: 264
- movies: 6,712
- episodes: 1,423
- parse: 557 ms

`RealM3uPerformanceTest`:

- file bytes: 2,515,464
- items: 8,399
- live: 435
- movies: 6,580
- series: 71
- parse: 793 ms
- normalize: 350 ms
- in-memory UI index: 93 ms
- search: 3 ms
- JVM RAM after test: 61 MB

## File Size / SOLID Guard

- Total Kotlin source lines: 7,231
- 10% threshold: 723 lines
- Largest file: `MainActivity.kt`, 638 lines

The largest source file remains under the requested 10% threshold.

## Verification Commands

- `./gradlew.bat :app:compilePersonalDebugKotlin --no-daemon` passed.
- `./gradlew.bat test --no-daemon` passed.
- `REAL_M3U_PATH=C:/Users/EVO-MRDM/Desktop/yCEUbXB9_playlist.m3u ./gradlew.bat :data:playlist:test --tests com.evomrdm.iptvbox.data.playlist.M3uPlaylistParserTest.parsesRealLargePlaylistWhenProvided --no-daemon --rerun-tasks` passed.
- `./gradlew.bat :app:testPersonalDebugUnitTest --tests com.evomrdm.iptvbox.RealM3uPerformanceTest --no-daemon --rerun-tasks` passed.
- `./gradlew.bat :app:assemblePersonalRelease --no-daemon` passed.
- `apksigner verify --verbose app/build/outputs/apk/personal/release/app-personal-release.apk` passed with v1/v2/v3 signatures.

## Release Output

`C:/Users/EVO-MRDM/Documents/IP TV PLAYER FOR TV BOX/app/build/outputs/apk/personal/release/app-personal-release.apk`
