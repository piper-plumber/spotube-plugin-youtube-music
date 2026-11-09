# Changelog

## 0.2.0

### Highlights
- **Requires Spotube with plugin API 0.1.0.** Spotube now only lets a plugin read WebView cookies, and run scripts in the WebView, on domains it declares. This version declares `music.youtube.com`, where the login reads the session cookies and `yt.config_`. Spotube shows it when installing or updating the plugin.
- **Update checks.** The plugin now checks GitHub releases for new versions.

### Features
- Declare `cookieDomains` for the WebView login, target plugin API 0.1.0, and update `spotube-gradle` to 0.2.0 (`cc2f2da`).
- Implement the plugin update check with the GitHub API (`c8df05b`).

### Chore
- Bump plugin version to 0.2.0 (`23963d4`).

## 0.1.0

### Highlights
- **First release as a Kotlin Multiplatform (Kotlin/JS) plugin** for YouTube Music metadata, configured through the `spotubePlugin {}` Gradle DSL.

### Fixes
- Return every section in "All" search results, and include community playlists in playlist search (`1aa4f67`).

### Chore
- Improve the metadata APIs and their caching (`6f4bad5`).
- Use the `org.spotube` namespace for plugin interface imports (`e74beea`).
- Add contact, repository, bugs and license information to the plugin metadata (`2246734`).
- Read the release version from `build.gradle.kts` instead of `plugin.json` (`43d6b44`).
- Update `spotube-gradle` to 0.1.2 (`28dee1e`).
