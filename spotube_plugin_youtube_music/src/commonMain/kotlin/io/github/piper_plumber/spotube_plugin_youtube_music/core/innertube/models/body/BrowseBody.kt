package io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.models.body

import io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.models.Context
import io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.models.Continuation
import kotlinx.serialization.Serializable

@Serializable
data class BrowseBody(
    val context: Context,
    val browseId: String?,
    val params: String?,
    val continuation: String?
)
