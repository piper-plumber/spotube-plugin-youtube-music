package io.github.piper_plumber.spotube_plugin_youtube_music

import app.cash.zipline.Zipline
import org.spotube.plugin_interfaces.core.runPluginInitialized
import org.spotube.plugin_interfaces.host_apis.CryptoAPI
import org.spotube.plugin_interfaces.host_apis.CryptoAPI_SERVICE_NAME
import org.spotube.plugin_interfaces.host_apis.HttpClientAPI
import org.spotube.plugin_interfaces.host_apis.HttpClientAPI_SERVICE_NAME
import org.spotube.plugin_interfaces.host_apis.PersistedStorageAPI
import org.spotube.plugin_interfaces.host_apis.PersistedStorageAPI_SERVICE_NAME
import org.spotube.plugin_interfaces.host_apis.SystemInformationAPI
import org.spotube.plugin_interfaces.host_apis.SystemInformationAPI_SERVICE_NAME
import org.spotube.plugin_interfaces.host_apis.WebViewAPI
import org.spotube.plugin_interfaces.host_apis.WebViewAPI_SERVICE_NAME
import io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.YouTube
import io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.models.YouTubeLocale
import io.github.piper_plumber.spotube_plugin_youtube_music.core.services.SingletonsRegistry
import org.spotube.plugin_interfaces.plugin_apis.core.CoreAPI
import org.spotube.plugin_interfaces.plugin_apis.core.CoreAPI_SERVICE_NAME
import io.github.piper_plumber.spotube_plugin_youtube_music.metadata.RealCoreAPI
import org.spotube.plugin_interfaces.plugin_apis.metadata.album.MetadataAlbumAPI
import org.spotube.plugin_interfaces.plugin_apis.metadata.album.MetadataAlbumAPI_SERVICE_NAME
import org.spotube.plugin_interfaces.plugin_apis.metadata.artist.MetadataArtistAPI
import org.spotube.plugin_interfaces.plugin_apis.metadata.artist.MetadataArtistAPI_SERVICE_NAME
import org.spotube.plugin_interfaces.plugin_apis.metadata.browse.MetadataBrowseAPI
import org.spotube.plugin_interfaces.plugin_apis.metadata.browse.MetadataBrowseAPI_SERVICE_NAME
import org.spotube.plugin_interfaces.plugin_apis.metadata.playlist.MetadataPlaylistAPI
import org.spotube.plugin_interfaces.plugin_apis.metadata.playlist.MetadataPlaylistAPI_SERVICE_NAME
import org.spotube.plugin_interfaces.plugin_apis.metadata.search.MetadataSearchAPI
import org.spotube.plugin_interfaces.plugin_apis.metadata.search.MetadataSearchAPI_SERVICE_NAME
import org.spotube.plugin_interfaces.plugin_apis.metadata.track.MetadataTrackAPI
import org.spotube.plugin_interfaces.plugin_apis.metadata.track.MetadataTrackAPI_SERVICE_NAME
import org.spotube.plugin_interfaces.plugin_apis.metadata.user.MetadataUserAPI
import org.spotube.plugin_interfaces.plugin_apis.metadata.user.MetadataUserAPI_SERVICE_NAME
import io.github.piper_plumber.spotube_plugin_youtube_music.metadata.RealMetadataBrowseAPI
import io.github.piper_plumber.spotube_plugin_youtube_music.metadata.RealMetadataPlaylistAPI
import io.github.piper_plumber.spotube_plugin_youtube_music.metadata.RealMetadataTrackAPI
import io.github.piper_plumber.spotube_plugin_youtube_music.metadata.RealMetadataUserAPI
import io.github.piper_plumber.spotube_plugin_youtube_music.metadata.RealMetadataAlbumAPI
import io.github.piper_plumber.spotube_plugin_youtube_music.metadata.RealMetadataArtistAPI
import io.github.piper_plumber.spotube_plugin_youtube_music.metadata.RealMetadataSearchAPI

private val zipline by lazy { Zipline.get() }

@OptIn(ExperimentalJsExport::class)
@JsExport
fun main() {
    runPluginInitialized {
        SingletonsRegistry.crypto = zipline.take<CryptoAPI>(CryptoAPI_SERVICE_NAME)

        val httpClient = zipline.take<HttpClientAPI>(HttpClientAPI_SERVICE_NAME)
        val webview = zipline.take<WebViewAPI>(WebViewAPI_SERVICE_NAME)
        val storage = zipline.take<PersistedStorageAPI>(PersistedStorageAPI_SERVICE_NAME)
        val systemInfo = zipline.take<SystemInformationAPI>(SystemInformationAPI_SERVICE_NAME)

        val locale = systemInfo.getLocale()
        val parts = locale.split("-")
        val language = parts.getOrNull(0) ?: "en"
        val region = parts.getOrNull(1) ?: "US"
        val youtube = YouTube(YouTubeLocale(hl = language, gl = region), httpClient)

        zipline.bind<CoreAPI>(CoreAPI_SERVICE_NAME, RealCoreAPI(youtube, webview, storage))

        zipline.bind<MetadataAlbumAPI>(MetadataAlbumAPI_SERVICE_NAME, RealMetadataAlbumAPI(youtube))
        zipline.bind<MetadataArtistAPI>(
            MetadataArtistAPI_SERVICE_NAME,
            RealMetadataArtistAPI(youtube)
        )
        zipline.bind<MetadataBrowseAPI>(
            MetadataBrowseAPI_SERVICE_NAME,
            RealMetadataBrowseAPI(youtube)
        )
        zipline.bind<MetadataPlaylistAPI>(
            MetadataPlaylistAPI_SERVICE_NAME,
            RealMetadataPlaylistAPI(youtube)
        )
        zipline.bind<MetadataSearchAPI>(
            MetadataSearchAPI_SERVICE_NAME,
            RealMetadataSearchAPI(youtube)
        )
        zipline.bind<MetadataTrackAPI>(MetadataTrackAPI_SERVICE_NAME, RealMetadataTrackAPI(youtube))
        zipline.bind<MetadataUserAPI>(MetadataUserAPI_SERVICE_NAME, RealMetadataUserAPI(youtube))
    }
}
