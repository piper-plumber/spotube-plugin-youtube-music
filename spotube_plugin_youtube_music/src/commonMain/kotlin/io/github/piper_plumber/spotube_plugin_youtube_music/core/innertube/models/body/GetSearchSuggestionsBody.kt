package io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.models.body

import io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class GetSearchSuggestionsBody(
    val context: Context,
    val input: String,
)
