package io.github.piper_plumber.spotube_plugin_youtube_music.metadata

import dev.krtirtho.plugin_interfaces.plugin_apis.metadata.common.PaginationResult
import dev.krtirtho.plugin_interfaces.plugin_apis.metadata.common.PaginationStrategy
import dev.krtirtho.plugin_interfaces.plugin_apis.metadata.common.Thumbnail
import dev.krtirtho.plugin_interfaces.plugin_apis.metadata.playlist.MetadataPlaylist
import dev.krtirtho.plugin_interfaces.plugin_apis.metadata.playlist.MetadataPlaylistAPI
import dev.krtirtho.plugin_interfaces.plugin_apis.metadata.track.MetadataTrack
import dev.krtirtho.plugin_interfaces.plugin_apis.metadata.user.MetadataUser
import io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.YouTube
import kotlin.io.encoding.Base64

class RealMetadataPlaylistAPI(val youTube: YouTube) : MetadataPlaylistAPI {
    override suspend fun getPlaylist(id: String): MetadataPlaylist {
        val playlistPage = youTube.playlist(id).getOrThrow()
        val playlist = playlistPage.playlist

        val songCount = playlist.songCountText?.let {
            Regex("\\d+").find(it)?.value?.toIntOrNull()
        } ?: 0

        val thumbnails = playlist.thumbnail?.let {
            listOf(
                Thumbnail(
                    url = it,
                    width = 540,
                    height = 540
                )
            )
        }.orEmpty()

        val owner = playlist.author?.let { artist ->
            MetadataUser(
                id = artist.id ?: "",
                username = artist.name,
                displayName = artist.name,
                thumbnails = emptyList(),
                externalUri = artist.id?.let { "https://music.youtube.com/channel/$it" }
            )
        }

        return MetadataPlaylist(
            id = playlist.id,
            title = playlist.title,
            description = null,
            thumbnails = thumbnails,
            trackCount = songCount,
            externalUri = "https://music.youtube.com/playlist?list=${playlist.id}",
            owner = owner
        )
    }

    override suspend fun getPlaylistTracks(
        id: String,
        pagination: PaginationStrategy?
    ): PaginationResult<MetadataTrack> {
        val continuationToken = when (pagination) {
            is PaginationStrategy.Continuation -> pagination.continuationToken
            else -> null
        }

        val (songs, nextContinuation) = if (continuationToken != null) {
            val page = youTube.playlistContinuation(continuationToken).getOrThrow()
            page.songs to page.continuation
        } else {
            val page = youTube.playlist(id).getOrThrow()
            page.songs to page.songsContinuation
        }

        val tracks = songs.map { it.toMetadataTrack() }

        return PaginationResult(
            items = tracks,
            totalCount = -1,
            nextPagination = nextContinuation?.let { PaginationStrategy.Continuation(it) }
        )
    }

    override suspend fun savedPlaylists(pagination: PaginationStrategy?): PaginationResult<MetadataPlaylist> {
        val continuationToken = when (pagination) {
            is PaginationStrategy.Continuation -> pagination.continuationToken
            else -> null
        }

        val (items, nextContinuation) = if (continuationToken != null) {
            val page = youTube.libraryContinuation(continuationToken).getOrThrow()
            page.items to page.continuation
        } else {
            val page = youTube.library("FEmusic_liked_playlists").getOrThrow()
            page.items to page.continuation
        }

        val playlists = items
            .filterIsInstance<io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.models.PlaylistItem>()
            .map { it.toMetadataPlaylist() }

        return PaginationResult(
            items = playlists,
            totalCount = -1,
            nextPagination = nextContinuation?.let { PaginationStrategy.Continuation(it) }
        )
    }

    override suspend fun isSavedPlaylists(ids: List<String>): List<Boolean> {
        val savedIds = mutableSetOf<String>()
        val libraryPage = youTube.library("FEmusic_liked_playlists").getOrThrow()

        libraryPage.items
            .filterIsInstance<io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.models.PlaylistItem>()
            .forEach { savedIds.add(it.id) }

        var continuation = libraryPage.continuation
        var requestCount = 0
        while (continuation != null && requestCount < 10) {
            val nextPage = youTube.libraryContinuation(continuation).getOrThrow()
            nextPage.items
                .filterIsInstance<io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.models.PlaylistItem>()
                .forEach { savedIds.add(it.id) }
            continuation = nextPage.continuation
            requestCount++
        }

        return ids.map { it in savedIds }
    }

    override suspend fun savePlaylists(ids: List<String>) {
        ids.forEach { id ->
            youTube.likePlaylist(id, true).getOrThrow()
        }
    }

    override suspend fun removeSavedPlaylists(ids: List<String>) {
        ids.forEach { id ->
            youTube.likePlaylist(id, false).getOrThrow()
        }
    }

    override suspend fun createPlaylist(
        name: String,
        description: String?,
        isPublic: Boolean,
        isCollaborating: Boolean,
        imageBase64: String,
        trackIds: List<String>
    ): MetadataPlaylist {
        val createdId = youTube.createPlaylist(name)

        if (trackIds.isNotEmpty()) {
            trackIds.forEach { trackId ->
                youTube.addToPlaylist(createdId, trackId).getOrThrow()
            }
        }

        if (imageBase64.isNotBlank()) {
            val imageBytes = Base64.decode(imageBase64)
            youTube.uploadCustomThumbnailLink(createdId, imageBytes).getOrNull()
        }

        return MetadataPlaylist(
            id = createdId,
            title = name,
            description = description,
            thumbnails = emptyList(),
            trackCount = trackIds.size,
            externalUri = "https://music.youtube.com/playlist?list=$createdId",
            owner = null
        )
    }

    override suspend fun updatePlaylist(
        id: String,
        name: String?,
        description: String?,
        isPublic: Boolean?,
        isCollaborating: Boolean?,
        imageBase64: String?,
        trackIds: List<String>?
    ): MetadataPlaylist {
        if (name != null) {
            youTube.renamePlaylist(id, name).getOrThrow()
        }

        if (!imageBase64.isNullOrBlank()) {
            val imageBytes = Base64.decode(imageBase64)
            youTube.uploadCustomThumbnailLink(id, imageBytes).getOrNull()
        }

        val playlistPage = youTube.playlist(id).getOrThrow()
        val playlist = playlistPage.playlist

        val songCount = playlist.songCountText?.let {
            Regex("\\d+").find(it)?.value?.toIntOrNull()
        } ?: 0

        return MetadataPlaylist(
            id = playlist.id,
            title = name ?: playlist.title,
            description = description,
            thumbnails = playlist.thumbnail?.let {
                listOf(
                    Thumbnail(
                        url = it,
                        width = 540,
                        height = 540
                    )
                )
            }.orEmpty(),
            trackCount = songCount,
            externalUri = "https://music.youtube.com/playlist?list=${playlist.id}",
            owner = null
        )
    }

    override suspend fun deletePlaylist(id: String) {
        youTube.deletePlaylist(id).getOrThrow()
    }

    override suspend fun addTracksToPlaylist(
        playlistId: String,
        trackIds: List<String>
    ) {
        trackIds.forEach { trackId ->
            youTube.addToPlaylist(playlistId, trackId).getOrThrow()
        }
    }

    override suspend fun removeTracksFromPlaylist(
        playlistId: String,
        trackIds: List<String>
    ) {
        val playlistPage = youTube.playlist(playlistId).getOrThrow()
        val setVideoIdMap = playlistPage.songs.associate { it.id to it.setVideoId }.toMutableMap()

        var continuation = playlistPage.songsContinuation
        var requestCount = 0
        while (continuation != null && requestCount < 10) {
            val nextPage = youTube.playlistContinuation(continuation).getOrThrow()
            nextPage.songs.forEach { song ->
                if (song.id !in setVideoIdMap) {
                    setVideoIdMap[song.id] = song.setVideoId
                }
            }
            continuation = nextPage.continuation
            requestCount++
        }

        trackIds.forEach { trackId ->
            val setVideoId = setVideoIdMap[trackId]
            if (setVideoId != null) {
                youTube.removeFromPlaylist(playlistId, trackId, setVideoId).getOrThrow()
            }
        }
    }

    internal fun io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.models.PlaylistItem.toMetadataPlaylist(): MetadataPlaylist {
        val songCount = songCountText?.let {
            Regex("\\d+").find(it)?.value?.toIntOrNull()
        } ?: 0

        val owner = author?.let { artist ->
            MetadataUser(
                id = artist.id ?: "",
                username = artist.name,
                displayName = artist.name,
                thumbnails = emptyList(),
                externalUri = artist.id?.let { "https://music.youtube.com/channel/$it" }
            )
        }

        return MetadataPlaylist(
            id = id,
            title = title,
            description = null,
            thumbnails = thumbnail?.let {
                listOf(
                    Thumbnail(
                        url = it,
                        width = 540,
                        height = 540
                    )
                )
            }.orEmpty(),
            trackCount = songCount,
            externalUri = "https://music.youtube.com/playlist?list=$id",
            owner = owner
        )
    }
}
