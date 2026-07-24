package io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.models.body

import io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class NextBody(
    val context: Context,
    val videoId: String?,
    val playlistId: String?,
    val playlistSetVideoId: String?,
    val index: Int?,
    val params: String?,
    val continuation: String?,
)
