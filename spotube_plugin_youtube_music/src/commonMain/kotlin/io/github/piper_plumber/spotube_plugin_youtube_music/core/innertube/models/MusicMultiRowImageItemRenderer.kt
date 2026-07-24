package io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class MusicMultiRowImageItemRenderer(
    val title: Runs,
    val subtitle: Runs,
    val thumbnail: ThumbnailRenderer,
    val onTap: NavigationEndpoint,
)
