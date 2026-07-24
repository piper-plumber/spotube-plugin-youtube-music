package io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.models

data class SearchSuggestions(
    val queries: List<String>,
    val recommendedItems: List<YTItem>,
)
