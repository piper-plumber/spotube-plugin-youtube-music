package io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.models.response

import io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.models.NavigationEndpoint
import io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.models.PlaylistPanelRenderer
import io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.models.Tabs
import io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.models.YouTubeDataPage
import kotlinx.serialization.Serializable

@Serializable
data class NextResponse(
    val contents: Contents,
    val continuationContents: ContinuationContents?,
    val currentVideoEndpoint: NavigationEndpoint?,
) {
    @Serializable
    data class Contents(
        val singleColumnMusicWatchNextResultsRenderer: SingleColumnMusicWatchNextResultsRenderer?,
        val twoColumnWatchNextResults: YouTubeDataPage.Contents.TwoColumnWatchNextResults?,
    ) {
        @Serializable
        data class SingleColumnMusicWatchNextResultsRenderer(
            val tabbedRenderer: TabbedRenderer?,
        ) {
            @Serializable
            data class TabbedRenderer(
                val watchNextTabbedResultsRenderer: WatchNextTabbedResultsRenderer?,
            ) {
                @Serializable
                data class WatchNextTabbedResultsRenderer(
                    val tabs: List<Tabs.Tab>,
                )
            }
        }
    }

    @Serializable
    data class ContinuationContents(
        val playlistPanelContinuation: PlaylistPanelRenderer,
    )
}
