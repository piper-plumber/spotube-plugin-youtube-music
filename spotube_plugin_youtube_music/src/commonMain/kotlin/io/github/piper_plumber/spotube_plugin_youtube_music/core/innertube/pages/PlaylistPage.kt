package io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.pages

import io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.models.Album
import io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.models.Artist
import io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.models.MusicResponsiveListItemRenderer
import io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.models.PlaylistItem
import io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.models.SongItem
import io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.models.oddElements
import io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.models.splitBySeparator
import io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.utils.parseTime

data class PlaylistPage(
    val playlist: PlaylistItem,
    val songs: List<SongItem>,
    val songsContinuation: String?,
    val continuation: String?,
) {
    companion object {
        fun fromMusicResponsiveListItemRenderer(renderer: MusicResponsiveListItemRenderer): SongItem? {
            // Extract library tokens using the new method that properly handles multiple toggle items
            val libraryTokens = PageHelper.extractLibraryTokensFromMenuItems(renderer.menu?.menuRenderer?.items)

            // Split the secondary line by bullet separator to separate artists from other metadata (like views)
            val secondaryLineRuns = renderer.flexColumns
                .getOrNull(1)
                ?.musicResponsiveListItemFlexColumnRenderer
                ?.text
                ?.runs
                ?.splitBySeparator()

            return SongItem(
                id = renderer.playlistItemData?.videoId ?: return null,
                title = renderer.flexColumns.firstOrNull()
                    ?.musicResponsiveListItemFlexColumnRenderer?.text
                    ?.runs?.firstOrNull()?.text ?: return null,
                artists = secondaryLineRuns?.firstOrNull()?.oddElements()?.map {
                    Artist(
                        name = it.text,
                        id = it.navigationEndpoint?.browseEndpoint?.browseId,
                    )
                }.orEmpty(),
                album = renderer.flexColumns.getOrNull(2)?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.firstOrNull()?.let {
                    Album(
                        name = it.text,
                        id = it.navigationEndpoint?.browseEndpoint?.browseId ?: return@let null
                    )
                },
                duration = renderer.fixedColumns?.firstOrNull()?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.firstOrNull()?.text?.parseTime(),
                musicVideoType = renderer.musicVideoType,
                thumbnail = renderer.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                explicit = renderer.badges?.find {
                    it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                } != null,
                endpoint = renderer.overlay?.musicItemThumbnailOverlayRenderer?.content?.musicPlayButtonRenderer?.playNavigationEndpoint?.watchEndpoint,
                setVideoId = renderer.playlistItemData.playlistSetVideoId ?: return null,
                libraryAddToken = libraryTokens.addToken,
                libraryRemoveToken = libraryTokens.removeToken,
                isEpisode = renderer.isEpisode
            )
        }
    }
}
