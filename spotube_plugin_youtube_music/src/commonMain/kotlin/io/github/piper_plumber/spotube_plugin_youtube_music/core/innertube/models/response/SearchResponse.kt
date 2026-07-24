package io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.models.response

import io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.models.Continuation
import io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.models.MusicResponsiveListItemRenderer
import io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.models.Tabs
import kotlinx.serialization.Serializable

@Serializable
data class SearchResponse(
    val contents: Contents?,
    val continuationContents: ContinuationContents?,
) {
    @Serializable
    data class Contents(
        val tabbedSearchResultsRenderer: Tabs?,
    )

    @Serializable
    data class ContinuationContents(
        val musicShelfContinuation: MusicShelfContinuation,
    ) {
        @Serializable
        data class MusicShelfContinuation(
            val contents: List<Content>,
            val continuations: List<Continuation>?,
        ) {
            @Serializable
            data class Content(
                val musicResponsiveListItemRenderer: MusicResponsiveListItemRenderer,
            )
        }
    }
}
