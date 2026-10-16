package io.github.piper_plumber.spotube_plugin_youtube_music.metadata

import org.spotube.plugin_interfaces.plugin_apis.metadata.album.MetadataAlbum
import org.spotube.plugin_interfaces.plugin_apis.metadata.album.MetadataAlbumAPI
import org.spotube.plugin_interfaces.plugin_apis.metadata.album.MetadataAlbumType
import org.spotube.plugin_interfaces.plugin_apis.metadata.artist.MetadataArtist
import org.spotube.plugin_interfaces.plugin_apis.metadata.common.PaginationResult
import org.spotube.plugin_interfaces.plugin_apis.metadata.common.PaginationStrategy
import org.spotube.plugin_interfaces.plugin_apis.metadata.common.Thumbnail
import org.spotube.plugin_interfaces.plugin_apis.metadata.track.MetadataTrack
import io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.YouTube
import io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.models.AlbumItem
import io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.models.SongItem
import io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.models.WatchEndpoint
import io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.pages.AlbumPage
import io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.pages.ArtistPage

internal fun AlbumItem.toMetadataAlbumBasic(): MetadataAlbum.Basic {
    val albumType = MetadataAlbumType.Album
    return MetadataAlbum.Basic(
        id = browseId,
        title = title,
        description = null,
        thumbnails = listOf(Thumbnail(url = thumbnail, width = 300, height = 300)),
        albumType = albumType,
        artists = artists?.map { artist ->
            MetadataArtist.Basic(
                id = artist.id ?: "",
                name = artist.name,
                thumbnails = emptyList(),
                externalUri = artist.id?.let { "https://music.youtube.com/channel/$it" }
            )
        } ?: emptyList(),
        externalUri = shareLink
    )
}

internal fun AlbumPage.toMetadataAlbumDetailed(): MetadataAlbum.Detailed {
    val albumType = if (songs.size <= 2) MetadataAlbumType.Single else MetadataAlbumType.Album
    return MetadataAlbum.Detailed(
        id = album.browseId,
        title = album.title,
        description = null,
        thumbnails = listOf(Thumbnail(url = album.thumbnail, width = 300, height = 300)),
        albumType = albumType,
        artists = album.artists?.map { artist ->
            MetadataArtist.Basic(
                id = artist.id ?: "",
                name = artist.name,
                thumbnails = emptyList(),
                externalUri = artist.id?.let { "https://music.youtube.com/channel/$it" }
            )
        } ?: emptyList(),
        externalUri = album.shareLink,
        releaseDate = album.year?.toString(),
        genres = emptyList(),
        trackCount = songs.size
    )
}

internal fun AlbumItem.toMetadataAlbumDetailed(artist: ArtistPage? = null): MetadataAlbum.Detailed {
    return MetadataAlbum.Detailed(
        id = browseId,
        title = title,
        description = null,
        thumbnails = listOf(Thumbnail(url = thumbnail, width = 300, height = 300)),
        albumType = MetadataAlbumType.Album,
        artists = artists?.map { artist ->
            MetadataArtist.Basic(
                id = artist.id ?: "",
                name = artist.name,
                thumbnails = emptyList(),
                externalUri = artist.id?.let { "https://music.youtube.com/channel/$it" }
            )
        } ?: artist?.let { artist ->
            listOf(
                MetadataArtist.Basic(
                    id = artist.artist.id,
                    name = artist.artist.title,
                    thumbnails = artist.artist.thumbnail?.let {
                        listOf(
                            Thumbnail(
                                url = artist.artist.thumbnail,
                                width = 300,
                                height = 300
                            )
                        )
                    }.orEmpty(),
                    externalUri = artist.artist.id.let { "https://music.youtube.com/channel/$it" }
                )
            )
        }.orEmpty(),
        externalUri = shareLink,
        releaseDate = year?.toString(),
        genres = emptyList(),
        trackCount = 0
    )
}

class RealMetadataAlbumAPI(private val youTube: YouTube) : MetadataAlbumAPI {
    override suspend fun getAlbum(id: String): MetadataAlbum.Detailed {
        val albumPage = youTube.album(id).getOrThrow()
        return albumPage.toMetadataAlbumDetailed()
    }

    override suspend fun getTrackAlbum(track: MetadataTrack): MetadataAlbum.Detailed {
        val nextResult = youTube.next(WatchEndpoint(videoId = track.id), loadAutomix = false).getOrThrow()
        val song = nextResult.items.find { it.id == track.id }

        val albumInfo = song?.album
        if (albumInfo != null) {
            return getAlbum(albumInfo.id)
        }

        val cleanTitle = song?.title?.replace(Regex("\\s*\\([^)]*\\)\\s*"), " ")?.trim() ?: ""
        val artistName = song?.artists?.firstOrNull()?.name ?: ""

        val searchResult =
            youTube.search("$cleanTitle $artistName", YouTube.SearchFilter.FILTER_SONG).getOrThrow()
        val songs = searchResult.items.filterIsInstance<SongItem>()
        val foundSong = songs.firstOrNull { it.id == song?.id && it.album != null }

        if (foundSong?.album != null) {
            return getAlbum(foundSong.album.id)
        }

        throw Exception("Album not found for track: ${track.title} by ${track.artists.joinToString(", ") { it.name }}")
    }

    override suspend fun getAlbumTracks(
        id: String,
        pagination: PaginationStrategy?
    ): PaginationResult<MetadataTrack> {
        val albumPage = youTube.album(id, withSongs = true).getOrThrow()
        val allTracks = albumPage.songs.map { it.toMetadataTrack() }

        return PaginationResult(
            items = allTracks,
            totalCount = allTracks.size,
            nextPagination = null
        )
    }

    override suspend fun savedAlbums(pagination: PaginationStrategy?): PaginationResult<MetadataAlbum.Detailed> {
        val continuationToken = when (pagination) {
            is PaginationStrategy.Continuation -> pagination.continuationToken
            else -> null
        }

        val (items, nextContinuation) = if (continuationToken != null) {
            val page = youTube.libraryContinuation(continuationToken).getOrThrow()
            page.items to page.continuation
        } else {
            val page = youTube.library("FEmusic_liked_albums").getOrThrow()
            page.items to page.continuation
        }

        val albumItems = items.filterIsInstance<AlbumItem>()
        val albums = albumItems.map { it.toMetadataAlbumDetailed() }

        return PaginationResult(
            items = albums,
            totalCount = -1,
            nextPagination = nextContinuation?.let { PaginationStrategy.Continuation(it) }
        )
    }

    override suspend fun isSavedAlbums(ids: List<String>): List<Boolean> {
        val libraryPage = youTube.library("FEmusic_liked_albums").getOrThrow()
        val savedIds = libraryPage.items
            .filterIsInstance<AlbumItem>()
            .map { it.browseId }
            .toMutableSet()

        var continuation = libraryPage.continuation
        var requestCount = 0
        while (continuation != null && requestCount < 10) {
            val nextPage = youTube.libraryContinuation(continuation).getOrThrow()
            nextPage.items.filterIsInstance<AlbumItem>().forEach { savedIds.add(it.browseId) }
            continuation = nextPage.continuation
            requestCount++
        }

        return ids.map { it in savedIds }
    }

    override suspend fun saveAlbums(ids: List<String>) {
        ids.forEach { id ->
            val albumPage = youTube.album(id).getOrThrow()
            val playlistId = albumPage.album.playlistId
            youTube.likePlaylist(playlistId, true).getOrThrow()
        }
    }

    override suspend fun removeSavedAlbums(ids: List<String>) {
        ids.forEach { id ->
            val albumPage = youTube.album(id).getOrThrow()
            val playlistId = albumPage.album.playlistId
            youTube.likePlaylist(playlistId, false).getOrThrow()
        }
    }
}
