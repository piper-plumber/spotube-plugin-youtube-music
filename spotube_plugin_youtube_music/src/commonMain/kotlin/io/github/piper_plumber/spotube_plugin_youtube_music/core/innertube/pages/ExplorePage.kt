package io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.pages

import io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.models.AlbumItem

data class ExplorePage(
    val newReleaseAlbums: List<AlbumItem>,
    val moodAndGenres: List<MoodAndGenres.Item>,
)
