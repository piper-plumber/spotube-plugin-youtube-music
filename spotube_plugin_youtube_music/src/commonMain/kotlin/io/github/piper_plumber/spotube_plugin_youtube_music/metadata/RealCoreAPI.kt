package io.github.piper_plumber.spotube_plugin_youtube_music.metadata

import dev.krtirtho.plugin_interfaces.host_apis.Cookie
import dev.krtirtho.plugin_interfaces.host_apis.PersistedStorageAPI
import dev.krtirtho.plugin_interfaces.host_apis.WebViewAPI
import dev.krtirtho.plugin_interfaces.plugin_apis.core.CoreAPI
import dev.krtirtho.plugin_interfaces.plugin_apis.core.PluginUpdateInfo
import dev.krtirtho.plugin_interfaces.extras.logger.Logger
import io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.YouTube
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.swiftzer.semver.SemVer

@Serializable
data class SessionData(
    val cookies: String,
    val dataSyncId: String,
    val visitorData: String
)

@OptIn(ExperimentalCoroutinesApi::class)
class RealCoreAPI(
    val youTube: YouTube,
    val webViewAPI: WebViewAPI,
    val storage: PersistedStorageAPI,
) : CoreAPI {
    private val scope = CoroutineScope(Dispatchers.Main)
    private val logger = Logger("RealCoreAPI")
    private val loggedInStateFlow = MutableStateFlow(false)

    init {
        scope.launch {
            restoreSession()
        }
    }

    private suspend fun storeSession(
        cookies: List<Cookie>,
        dataSyncId: String,
        visitorData: String
    ) {
        logger.d { "Storing session data..." }
        val cookieHeaderStr = cookies.joinToString("; ") { "${it.name}=${it.value}" }
        val sessionData = SessionData(cookieHeaderStr, dataSyncId, visitorData)
        val sessionDataStr = Json.encodeToString(sessionData)

        logger.d { "Session data to store: $sessionDataStr" }
        storage.putString("youtube_music_session_data", sessionDataStr)

        youTube.cookie = sessionData.cookies
        youTube.dataSyncId = sessionData.dataSyncId
        youTube.visitorData = sessionData.visitorData
        youTube.useLoginForBrowse = true
    }

    private suspend fun restoreSession() {
        val sessionDataStr = storage.getString("youtube_music_session_data")
        if (sessionDataStr == null) {
            logger.d { "No session data found in storage." }
            return
        }
        val sessionData = Json.decodeFromString<SessionData>(sessionDataStr)

        logger.d { "Restoring session data: $sessionData" }

        youTube.cookie = sessionData.cookies
        youTube.dataSyncId = sessionData.dataSyncId
        youTube.visitorData = sessionData.visitorData
        youTube.useLoginForBrowse = true
        loggedInStateFlow.value = true

        logger.d { "Session restored successfully." }
    }

    override val requiresAuthentication = true

    override suspend fun checkPluginUpdates(currentVersion: SemVer): PluginUpdateInfo? {
        return null
    }

    override fun supportMarkdownText(currentVersion: SemVer): String {
        return "PLS SUPPORT THIS PLUGIN"
    }

    override val loggedInFlow = loggedInStateFlow.asStateFlow()

    override suspend fun login() {
        logger.d { "Starting login process..." }

        webViewAPI.webviewCreatedFlow()
            .take(1)
            .flatMapLatest {
                logger.d { "Webview created, waiting for URL change..." }
                webViewAPI.urlChangeFlow()
            }.onEach {
                logger.d { "URL changed: $it" }
                if (it.startsWith("https://music.youtube.com")) {
                    logger.d { "Login page loaded, waiting for cookies..." }
                    val cookies = webViewAPI.getCookies("https://music.youtube.com")
                    if (cookies.isEmpty()) {
                        logger.d { "No cookies found, waiting for next URL change..." }
                        return@onEach
                    }
                    logger.d { "Cookies found, waiting for dataSyncId and visitorData..." }

                    val dataSyncId = webViewAPI.evaluateJavaScript("window.yt.config_.DATASYNC_ID")
                    val visitorData =
                        webViewAPI.evaluateJavaScript("window.yt.config_.VISITOR_DATA")
                    if (dataSyncId == null || visitorData == null) {
                        logger.d { "dataSyncId=$dataSyncId or visitorData=$visitorData. But one is null, waiting for next URL change..." }
                        return@onEach
                    }
                    logger.d { "dataSyncId=$dataSyncId, visitorData=$visitorData. Storing session data..." }

                    webViewAPI.exitWebView()
                    storeSession(cookies, dataSyncId, visitorData)
                    logger.d { "Session data stored, marking as logged in..." }
                    loggedInStateFlow.value = true
                    logger.d { "Login process completed successfully." }
                }
            }.launchIn(scope)

        logger.d { "Navigating to login page..." }
        webViewAPI.navigateTo(
            "https://accounts.google.com/ServiceLogin?continue=https%3A%2F%2Fmusic.youtube.com"
        )
        logger.d { "Login page navigated to, waiting for login to complete..." }
    }

    override suspend fun logout() {
        storage.remove("youtube_music_session_data")
        youTube.cookie = null
        youTube.dataSyncId = null
        youTube.visitorData = null
        youTube.useLoginForBrowse = false
        loggedInStateFlow.value = false
    }

}