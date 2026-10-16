package io.github.piper_plumber.spotube_plugin_youtube_music.metadata

import org.spotube.plugin_interfaces.plugin_apis.metadata.album.MetadataAlbum
import org.spotube.plugin_interfaces.plugin_apis.metadata.artist.MetadataArtist
import org.spotube.plugin_interfaces.plugin_apis.metadata.artist.MetadataArtistAPI
import org.spotube.plugin_interfaces.plugin_apis.metadata.artist.MetadataArtistOverview
import org.spotube.plugin_interfaces.plugin_apis.metadata.common.PaginationResult
import org.spotube.plugin_interfaces.plugin_apis.metadata.common.PaginationStrategy
import org.spotube.plugin_interfaces.plugin_apis.metadata.common.Thumbnail
import org.spotube.plugin_interfaces.plugin_apis.metadata.playlist.MetadataPlaylist
import org.spotube.plugin_interfaces.plugin_apis.metadata.track.MetadataTrack
import io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.YouTube
import io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.models.AlbumItem
import io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.models.ArtistItem
import io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.models.PlaylistItem
import io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.models.SongItem

internal fun ArtistItem.toMetadataArtistDetailed(
    description: String? = null,
    subscriberCount: Int? = null,
    monthlyListeners: Int? = null,
    genres: List<String> = emptyList()
): MetadataArtist.Detailed {
    return MetadataArtist.Detailed(
        id = id,
        name = title,
        thumbnails = thumbnail?.let { listOf(Thumbnail(url = it, width = 540, height = 540)) }
            .orEmpty(),
        externalUri = "https://music.youtube.com/channel/$id",
        genres = genres,
        biography = description,
        followersCount = subscriberCount ?: monthlyListeners
    )
}

internal fun ArtistItem.toMetadataArtistBasic(): MetadataArtist.Basic {
    return MetadataArtist.Basic(
        id = id,
        name = title,
        thumbnails = thumbnail?.let { listOf(Thumbnail(url = it, width = 540, height = 540)) }
            .orEmpty(),
        externalUri = "https://music.youtube.com/channel/$id"
    )
}


class RealMetadataArtistAPI(private val youTube: YouTube) : MetadataArtistAPI {
    override suspend fun getArtist(id: String): MetadataArtist.Detailed {
        val artistPage = youTube.artist(id).getOrThrow()
        val artist = artistPage.artist

        val subscriberCount = artistPage.subscriberCountText?.let { text ->
            parseCountText(text)
        }

        val monthlyListeners = artistPage.monthlyListenerCount?.let { text ->
            parseCountText(text)
        }

        return artist.toMetadataArtistDetailed(
            description = artistPage.description,
            subscriberCount = subscriberCount,
            monthlyListeners = monthlyListeners
        )
    }

    override suspend fun artistOverview(id: String): MetadataArtistOverview {
        val artistPage = youTube.artist(id).getOrThrow()

        val subscriberCount = artistPage.subscriberCountText?.let { text ->
            parseCountText(text)
        }

        val monthlyListeners = artistPage.monthlyListenerCount?.let { text ->
            parseCountText(text)
        }

        val detailedArtist = artistPage.artist.toMetadataArtistDetailed(
            description = artistPage.description,
            subscriberCount = subscriberCount,
            monthlyListeners = monthlyListeners
        )

        val top10Tracks = artistPage.sections
            .flatMap { section -> section.items.filterIsInstance<SongItem>() }
            .distinctBy { it.id }
            .take(10)
            .map { it.toMetadataTrack() }

        val albumsSection = artistPage.sections.find { section ->
            section.title.contains("album", ignoreCase = true) ||
                    section.title.contains("singles", ignoreCase = true) ||
                    section.title.contains("releases", ignoreCase = true) ||
                    section.title.contains("compilation", ignoreCase = true)
        }

        val albums = albumsSection?.items?.filterIsInstance<AlbumItem>()
            ?.map { it.toMetadataAlbumDetailed(artist = artistPage) }
            ?: emptyList()

        val relatedSection = artistPage.sections.find { section ->
            section.items.any { it is ArtistItem } &&
                    (section.title.contains("related", ignoreCase = true) ||
                            section.title.contains("similar", ignoreCase = true) ||
                            section.title.contains("fans", ignoreCase = true) ||
                            section.title.contains("you might like", ignoreCase = true))
        }

        val related = relatedSection?.items?.filterIsInstance<ArtistItem>()
            ?.map { it.toMetadataArtistBasic() }
            ?: emptyList()

        val playlistSection = artistPage.sections.find { section ->
            section.items.any { it is PlaylistItem } &&
                    (section.title.contains("featuring", ignoreCase = true) ||
                            section.title.contains("appears on", ignoreCase = true) ||
                            section.title.contains("featured on", ignoreCase = true) ||
                            section.title.contains("in playlists", ignoreCase = true) ||
                            section.title.contains("artist playlists", ignoreCase = true))
        }

        val featured = playlistSection?.items?.filterIsInstance<PlaylistItem>()
            ?.map { it.toMetadataPlaylist() }
            ?: emptyList()

        return MetadataArtistOverview(
            artist = detailedArtist,
            top10Tracks = top10Tracks,
            albums = PaginationResult(items = albums, totalCount = albums.size, nextPagination = null),
            relatedArtists = PaginationResult(items = related, totalCount = related.size, nextPagination = null),
            featuredPlaylists = PaginationResult(items = featured, totalCount = featured.size, nextPagination = null)
        )
    }

    override suspend fun getArtistTop10Tracks(id: String): List<MetadataTrack> {
        val artistPage = youTube.artist(id).getOrThrow()
        return artistPage.sections
            .flatMap { section ->
                section.items.filterIsInstance<SongItem>()
            }
            .distinctBy { it.id }
            .take(10)
            .map { it.toMetadataTrack() }
    }

    override suspend fun relatedArtists(
        id: String,
        pagination: PaginationStrategy?
    ): PaginationResult<MetadataArtist.Basic> {
        val artistPage = youTube.artist(id).getOrThrow()

        val relatedSection = artistPage.sections.find { section ->
            section.items.any { it is ArtistItem } &&
                    (section.title.contains("related", ignoreCase = true) ||
                            section.title.contains("similar", ignoreCase = true) ||
                            section.title.contains("fans", ignoreCase = true) ||
                            section.title.contains("fans might also like", ignoreCase = true) ||
                            section.title.contains("you might like", ignoreCase = true))
        }

        val related = relatedSection?.items?.filterIsInstance<ArtistItem>()
            ?.map { it.toMetadataArtistBasic() }
            ?: emptyList()

        return PaginationResult(
            items = related,
            totalCount = related.size,
            nextPagination = null
        )
    }

    override suspend fun featuredPlaylists(
        id: String,
        pagination: PaginationStrategy?
    ): PaginationResult<MetadataPlaylist> {
        val artistPage = youTube.artist(id).getOrThrow()

        val playlistSection = artistPage.sections.find { section ->
            section.items.any { it is PlaylistItem } &&
                    (section.title.contains("featuring", ignoreCase = true) ||
                            section.title.contains("appears on", ignoreCase = true) ||
                            section.title.contains("featured on", ignoreCase = true) ||
                            section.title.contains("in playlists", ignoreCase = true) ||
                            section.title.contains("artist playlists", ignoreCase = true) ||
                            section.title.contains("playlists by", ignoreCase = true))
        }

        val playlists = playlistSection?.items?.filterIsInstance<PlaylistItem>()
            ?.map { it.toMetadataPlaylist() }
            ?: emptyList()

        return PaginationResult(
            items = playlists,
            totalCount = playlists.size,
            nextPagination = null
        )
    }

    override suspend fun getArtistAlbums(
        id: String,
        pagination: PaginationStrategy?
    ): PaginationResult<MetadataAlbum.Detailed> {
        val artistPage = youTube.artist(id).getOrThrow()
        val albumsSection = artistPage.sections.find { section ->
            section.title.contains("album", ignoreCase = true) ||
                    section.title.contains("singles", ignoreCase = true) ||
                    section.title.contains("releases", ignoreCase = true)
        }

        val albums = albumsSection?.items?.filterIsInstance<AlbumItem>()
            ?.map { it.toMetadataAlbumDetailed(artist = artistPage) }
            ?: emptyList()


        return PaginationResult(
            items = albums,
            totalCount = albums.size,
            nextPagination = null
        )
    }

    override suspend fun savedArtists(pagination: PaginationStrategy?): PaginationResult<MetadataArtist.Detailed> {
        val continuationToken = when (pagination) {
            is PaginationStrategy.Continuation -> pagination.continuationToken
            else -> null
        }

        val (items, nextContinuation) = if (continuationToken != null) {
            val page = youTube.libraryContinuation(continuationToken).getOrThrow()
            page.items to page.continuation
        } else {
            val page = youTube.library("FEmusic_library_corpus_track_artists").getOrThrow()
            page.items to page.continuation
        }

        val artists = items
            .filterIsInstance<ArtistItem>()
            .map { artistItem ->
                artistItem.toMetadataArtistDetailed()
            }

        return PaginationResult(
            items = artists,
            totalCount = -1,
            nextPagination = nextContinuation?.let { PaginationStrategy.Continuation(it) }
        )
    }

    override suspend fun isSavedArtists(ids: List<String>): List<Boolean> {
        val libraryPage = youTube.library("FEmusic_library_corpus_track_artists").getOrThrow()
        val savedIds = libraryPage.items
            .filterIsInstance<ArtistItem>()
            .map { it.id }
            .toSet()

        val allSavedIds = savedIds.toMutableSet()
        var continuation = libraryPage.continuation
        val seenContinuations = mutableSetOf<String>()

        while (continuation != null && continuation !in seenContinuations && seenContinuations.size < 20) {
            seenContinuations.add(continuation)
            val continuationPage = youTube.libraryContinuation(continuation).getOrThrow()
            val newIds = continuationPage.items
                .filterIsInstance<ArtistItem>()
                .map { it.id }
            allSavedIds.addAll(newIds)
            continuation = continuationPage.continuation
        }

        return ids.map { it in allSavedIds }
    }

    override suspend fun saveArtists(ids: List<String>) {
        ids.forEach { id ->
            val artistPage = youTube.artist(id).getOrThrow()
            val channelId = artistPage.artist.channelId
            if (channelId != null) {
                youTube.subscribeChannel(channelId, subscribe = true).getOrThrow()
            }
        }
    }

    override suspend fun removeSavedArtists(ids: List<String>) {
        ids.forEach { id ->
            val artistPage = youTube.artist(id).getOrThrow()
            val channelId = artistPage.artist.channelId
            if (channelId != null) {
                youTube.subscribeChannel(channelId, subscribe = false).getOrThrow()
            }
        }
    }

    private fun parseCountText(text: String): Int? {
        return runCatching {
            val cleaned = text.replace(Regex("[^0-9.KMBkmb]"), "")
            when {
                cleaned.endsWith('K', ignoreCase = true) ->
                    (cleaned.dropLast(1).toDoubleOrNull() ?: 0.0) * 1_000

                cleaned.endsWith('M', ignoreCase = true) ->
                    (cleaned.dropLast(1).toDoubleOrNull() ?: 0.0) * 1_000_000

                cleaned.endsWith('B', ignoreCase = true) ->
                    (cleaned.dropLast(1).toDoubleOrNull() ?: 0.0) * 1_000_000_000

                else -> cleaned.toDoubleOrNull()
            }?.toInt()
        }.getOrNull()
    }
}
