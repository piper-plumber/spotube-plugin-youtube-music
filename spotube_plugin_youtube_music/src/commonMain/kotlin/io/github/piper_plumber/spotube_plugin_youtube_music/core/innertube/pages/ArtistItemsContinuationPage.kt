package io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.pages

import io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.models.YTItem

data class ArtistItemsContinuationPage(
    val items: List<YTItem>,
    val continuation: String?,
)
