package io.github.piper_plumber.spotube_plugin_youtube_music.metadata

import org.spotube.plugin_interfaces.plugin_apis.metadata.common.PaginationResult
import org.spotube.plugin_interfaces.plugin_apis.metadata.common.PaginationStrategy
import org.spotube.plugin_interfaces.plugin_apis.metadata.common.Thumbnail
import org.spotube.plugin_interfaces.plugin_apis.metadata.track.MetadataTrack
import org.spotube.plugin_interfaces.plugin_apis.metadata.track.MetadataTrackAPI
import org.spotube.plugin_interfaces.plugin_apis.metadata.artist.MetadataArtist
import org.spotube.plugin_interfaces.plugin_apis.metadata.album.MetadataAlbum
import org.spotube.plugin_interfaces.plugin_apis.metadata.album.MetadataAlbumType
import io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.YouTube
import io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.models.SongItem
import io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.models.WatchEndpoint
import io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.models.Artist

internal fun SongItem.toMetadataTrack(): MetadataTrack {


    return MetadataTrack(
        id = id,
        title = title,
        artists = artists.map { artist ->
            MetadataArtist.Basic(
                id = artist.id ?: "",
                name = artist.name,
                thumbnails = emptyList(),
                externalUri = artist.id?.let { "https://music.youtube.com/channel/$it" }
            )
        },
        album = null,
        thumbnails = listOf(
            Thumbnail(
                url = thumbnail,
                width = 300,
                height = 300
            )
        ),
        durationMs = duration?.toLong()?.times(1000) ?: 0L,
        trackNumber = 0,
        discNumber = 0,
        popularity = 0,
        isrcCode = "",
        externalUri = shareLink,
        explicit = explicit
    )
}

internal fun io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.models.Album.toMetadataAlbum(
    trackArtists: List<Artist>
): MetadataAlbum.Detailed {
    return MetadataAlbum.Detailed(
        id = id,
        title = name,
        releaseDate = null,
        genres = emptyList(),
        trackCount = 0,
        description = "",
        thumbnails = emptyList(),
        albumType = MetadataAlbumType.Album,
        artists = trackArtists.map { artist ->
            MetadataArtist.Basic(
                id = artist.id ?: "",
                name = artist.name,
                thumbnails = emptyList(),
                externalUri = artist.id?.let { "https://music.youtube.com/channel/$it" }
            )
        },
        externalUri = "https://music.youtube.com/playlist?list=$id"
    )
}

class RealMetadataTrackAPI(val youTube: YouTube) : MetadataTrackAPI {
    override suspend fun getTrack(id: String): MetadataTrack {
        val nextResult = youTube.next(WatchEndpoint(videoId = id)).getOrThrow()
        val song = nextResult.items.find { it.id == id }
            ?: throw Exception("Track not found: $id")

        val enrichedSong = if (song.album == null && song.artists.isNotEmpty()) {
            val cleanTitle = song.title.replace(Regex("\\s*\\([^)]*\\)\\s*"), " ").trim()
            val artistName = song.artists.first().name
            val queries = listOf(
                "$cleanTitle $artistName",
                cleanTitle,
                artistName
            )
            var searchedSong: SongItem? = null
            for (query in queries) {
                val searchResult =
                    youTube.search(query, YouTube.SearchFilter.FILTER_SONG).getOrNull()
                val songs = searchResult?.items?.filterIsInstance<SongItem>() ?: emptyList()
                searchedSong = songs.firstOrNull { it.id == id }
                    ?: songs.firstOrNull {
                        it.album != null && it.artists.any { a ->
                            a.name.contains(
                                artistName,
                                ignoreCase = true
                            ) || artistName.contains(a.name, ignoreCase = true)
                        }
                    }
                            ?: songs.firstOrNull { it.album != null }
                if (searchedSong?.album != null) break
            }
            if (searchedSong?.album != null) {
                song.copy(album = searchedSong.album, artists = searchedSong.artists)
            } else {
                song
            }
        } else {
            song
        }

        return enrichedSong.toMetadataTrack()
    }

    override suspend fun savedTracks(pagination: PaginationStrategy?): PaginationResult<MetadataTrack> {
        val continuationToken = when (pagination) {
            is PaginationStrategy.Continuation -> pagination.continuationToken
            else -> null
        }

        val (items, nextContinuation) = if (continuationToken != null) {
            val page = youTube.libraryContinuation(continuationToken).getOrThrow()
            page.items to page.continuation
        } else {
            val page = youTube.library("FEmusic_liked_videos").getOrThrow()
            page.items to page.continuation
        }

        val tracks = items
            .filterIsInstance<SongItem>()
            .map { it.toMetadataTrack() }

        return PaginationResult(
            items = tracks,
            totalCount = -1,
            nextPagination = nextContinuation?.let { PaginationStrategy.Continuation(it) }
        )
    }

    override suspend fun isSavedTracks(ids: List<String>): List<Boolean> {
        return ids.map { id ->
            runCatching {
                val nextResult = youTube.next(WatchEndpoint(videoId = id)).getOrThrow()
                val song = nextResult.items.find { it.id == id }
                song?.libraryRemoveToken != null
            }.getOrDefault(false)
        }
    }

    override suspend fun saveTracks(ids: List<String>) {
        ids.forEach { id ->
            youTube.addSongToLibrary(id).getOrThrow()
        }
    }

    override suspend fun removeSavedTracks(ids: List<String>) {
        ids.forEach { id ->
            youTube.removeSongFromLibrary(id).getOrThrow()
        }
    }

    override suspend fun recommendationsBasedOnTracks(
        seedTrackIds: List<String>,
        limit: Int
    ): List<MetadataTrack> {
        if (seedTrackIds.isEmpty()) return emptyList()

        val seedId = seedTrackIds.last()
        val nextResult = youTube.next(WatchEndpoint(videoId = seedId)).getOrThrow()

        return nextResult.items
            .filter { it.id != seedId }
            .take(limit)
            .map { it.toMetadataTrack() }
    }
}
