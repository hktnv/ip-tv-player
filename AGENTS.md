# Project Agent Notes

This workspace follows the project master blueprint when it is provided in the local working environment.

## Build Direction

- Kotlin, Jetpack Compose, Compose for TV-ready focus behavior, and Media3 are the default stack.
- Keep UI -> domain -> data dependencies one-way.
- Do not add IPTV provider discovery, DRM bypass, or unauthorized source scraping.
- Treat Xtream credentials, cookies, bearer tokens, and URL query secrets as sensitive.
- Prefer streaming parsers for M3U/XMLTV and staged database writes once persistence is introduced.

## Local Commands

- `.\gradlew.bat test`
- `.\gradlew.bat :app:assemblePersonalDebug`
