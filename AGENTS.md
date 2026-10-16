# AGENTS.md — spotube-plugin-youtube-music

## Project

Kotlin Multiplatform plugin for [Spotube](https://github.com/KRTirtho/spotube) that provides YouTube Music metadata fetching via YouTube's InnerTube API.

## Build & Test

```bash
./gradlew :spotube_plugin_youtube_music:jsBrowserTest   # run tests (Karma + Chrome headless)
./gradlew :spotube_plugin_youtube_music:jsBrowserDistribution  # build JS bundle
./gradlew :spotube_plugin_youtube_music:jvmTest          # run JVM tests only
./gradlew :spotube_plugin_youtube_music:check            # lint + test all targets
```

Gradle config cache and parallel execution are enabled (`gradle.properties`). JVM args: `-Xmx4G`.

## Architecture

- **Single module**: `:spotube_plugin_youtube_music`
- **Targets**: `js` (browser, executable) + `jvm`
- **Entry point**: `src/jsMain/kotlin/.../js.kt` — `main()` function
  - Compiled to JS via Zipline, loaded by Spotube host at runtime
  - Zipline config: `io.github.piper_plumber.spotube_plugin_youtube_music.main`
- **Zipline services**: takes `WebViewAPI`, `PersistedStorageAPI`, `CryptoAPI`, `SystemInformationAPI` from host; binds `MetadataUserAPI` + `MetadataTrackAPI`
- **InnerTube implementation**: `src/commonMain/kotlin/.../core/innertube/` — ported from [Metrolist](https://github.com/metrolist/metrolist)
- **HTTP**: Ktor client (core + encoding + content negotiation + kotlinx-json serialization)

## Source Layout

```
spotube_plugin_youtube_music/src/
  commonMain/   # shared logic: InnerTube API, models, pages, RealMetadataUserAPI, RealMetadataTrackAPI
  commonTest/   # shared tests (run on JS via Karma, or JVM)
  jvmTest/      # JVM-specific integration tests (RealMetadataTrackAPITest, RealMetadataUserAPITest)
  jsMain/       # JS entry point (main()), Zipline bootstrap
```

## Key Dependencies

| Library | Version |
|---------|---------|
| Kotlin | 2.3.21 |
| Zipline | 1.27.0 |
| Ktor | 3.4.3 |
| kotlinx-coroutines | 1.10.2 |
| spotube-plugin-interfaces | 0.1.0 |

Version catalog: `gradle/libs.versions.toml`

## Gotchas

- **`spotube-plugin-interfaces`** is an external dependency (`org.spotube.spotube:plugin_interfaces`). Must be resolvable from Maven Central or `mavenLocal()`. If missing, publish it locally first.
- Tests require Chrome/Chromium installed (Karma uses `ChromeHeadless`).
- Kotlin incremental compilation for JS is explicitly disabled (`gradle.properties`) due to KT-82395.
- `plugin.json` declares capabilities: `PERSISTENT_STORAGE`, `NETWORK_REQUESTS`, `WEBVIEW` and ability: `METADATA`.
- Package namespace: `io.github.piper_plumber.spotube_plugin_youtube_music`

## Implemented APIs

- **`RealMetadataUserAPI`**: Fetches YouTube account info via `YouTube.accountInfo()` (InnerTube accountMenu endpoint)
- **`RealMetadataTrackAPI`**: All 6 methods implemented:
  - `getTrack(id)`: Uses `next()` endpoint; falls back to search if album metadata missing
  - `savedTracks(pagination)`: Uses `library("FEmusic_liked_videos")`
  - `isSavedTracks(ids)`: Checks `libraryRemoveToken` presence via `next()`
  - `saveTracks(ids)` / `removeSavedTracks(ids)`: Uses `addSongToLibrary` / `removeSongFromLibrary` with fresh tokens
  - `recommendationsBasedOnTracks(seedTrackIds, limit)`: Uses `next()` automix items (YouTube radio)
- **Album artists**: InnerTube `Album` model has no artists field; populated from track artists in `toMetadataAlbum()`
