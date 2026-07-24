package io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.models.response

import io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.models.SearchSuggestionsSectionRenderer
import kotlinx.serialization.Serializable

@Serializable
data class GetSearchSuggestionsResponse(
    val contents: List<Content>?,
) {
    @Serializable
    data class Content(
        val searchSuggestionsSectionRenderer: SearchSuggestionsSectionRenderer,
    )
}
