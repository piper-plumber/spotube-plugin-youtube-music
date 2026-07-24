package io.github.piper_plumber.spotube_plugin_youtube_music.metadata

import dev.krtirtho.plugin_interfaces.plugin_apis.metadata.common.PaginationResult
import dev.krtirtho.plugin_interfaces.plugin_apis.metadata.common.PaginationStrategy
import dev.krtirtho.plugin_interfaces.plugin_apis.metadata.common.Thumbnail
import dev.krtirtho.plugin_interfaces.plugin_apis.metadata.search.MetadataSearchAPI
import dev.krtirtho.plugin_interfaces.plugin_apis.metadata.search.MetadataSearchResult
import dev.krtirtho.plugin_interfaces.plugin_apis.metadata.search.MetadataSupportedSearchType
import dev.krtirtho.plugin_interfaces.plugin_apis.metadata.user.MetadataUser
import io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.YouTube
import io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.models.AlbumItem
import io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.models.ArtistItem
import io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.models.PlaylistItem
import io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.models.SongItem

internal fun PlaylistItem.toMetadataPlaylist(): dev.krtirtho.plugin_interfaces.plugin_apis.metadata.playlist.MetadataPlaylist {
    val songCount = songCountText?.let {
        Regex("\\d+").find(it)?.value?.toIntOrNull()
    } ?: 0

    val owner = author?.let { artist ->
        MetadataUser(
            id = artist.id ?: if (artist.name == "YouTube Music") "youtube_music" else "",
            username = artist.name,
            displayName = artist.name,
            thumbnails = emptyList(),
            externalUri = artist.id?.let { "https://music.youtube.com/channel/$it" }
        )
    }

    return dev.krtirtho.plugin_interfaces.plugin_apis.metadata.playlist.MetadataPlaylist(
        id = id,
        title = title,
        description = null,
        thumbnails = thumbnail?.let {
            listOf(Thumbnail(url = it, width = 540, height = 540))
        }.orEmpty(),
        trackCount = songCount,
        externalUri = "https://music.youtube.com/playlist?list=$id",
        owner = owner
    )
}

class RealMetadataSearchAPI(private val youTube: YouTube) : MetadataSearchAPI {
    override val supportedSearchTypes: List<MetadataSupportedSearchType>
        get() = listOf(
            MetadataSupportedSearchType.TRACK,
            MetadataSupportedSearchType.ARTIST,
            MetadataSupportedSearchType.ALBUM,
            MetadataSupportedSearchType.PLAYLIST,
            MetadataSupportedSearchType.USER,
            MetadataSupportedSearchType.ALL
        )

    override suspend fun search(query: String): List<MetadataSearchResult> {
        val summaryPage = youTube.searchSummary(query).getOrThrow()
        return summaryPage.summaries
            .flatMap { summary -> summary.items }
            .mapNotNull { item ->
                when (item) {
                    is SongItem -> MetadataSearchResult.Track(item.toMetadataTrack())
                    is ArtistItem -> MetadataSearchResult.Artist(item.toMetadataArtistBasic())
                    is AlbumItem -> MetadataSearchResult.Album(item.toMetadataAlbumBasic())
                    is PlaylistItem -> MetadataSearchResult.Playlist(item.toMetadataPlaylist())
                    else -> null
                }
            }
    }

    override suspend fun searchTracks(
        query: String,
        pagination: PaginationStrategy?
    ): PaginationResult<MetadataSearchResult.Track> {
        val continuationToken = when (pagination) {
            is PaginationStrategy.Continuation -> pagination.continuationToken
            else -> null
        }

        val searchResult = if (continuationToken != null) {
            youTube.searchContinuation(continuationToken).getOrThrow()
        } else {
            youTube.search(query, YouTube.SearchFilter.FILTER_SONG).getOrThrow()
        }

        val tracks = searchResult.items
            .filterIsInstance<SongItem>()
            .map { MetadataSearchResult.Track(it.toMetadataTrack()) }

        return PaginationResult(
            items = tracks,
            totalCount = -1,
            nextPagination = searchResult.continuation?.let { PaginationStrategy.Continuation(it) }
        )
    }

    override suspend fun searchArtists(
        query: String,
        pagination: PaginationStrategy?
    ): PaginationResult<MetadataSearchResult.Artist> {
        val continuationToken = when (pagination) {
            is PaginationStrategy.Continuation -> pagination.continuationToken
            else -> null
        }

        val searchResult = if (continuationToken != null) {
            youTube.searchContinuation(continuationToken).getOrThrow()
        } else {
            youTube.search(query, YouTube.SearchFilter.FILTER_ARTIST).getOrThrow()
        }

        val artists = searchResult.items
            .filterIsInstance<ArtistItem>()
            .map { MetadataSearchResult.Artist(it.toMetadataArtistBasic()) }

        return PaginationResult(
            items = artists,
            totalCount = -1,
            nextPagination = searchResult.continuation?.let { PaginationStrategy.Continuation(it) }
        )
    }

    override suspend fun searchAlbums(
        query: String,
        pagination: PaginationStrategy?
    ): PaginationResult<MetadataSearchResult.Album> {
        val continuationToken = when (pagination) {
            is PaginationStrategy.Continuation -> pagination.continuationToken
            else -> null
        }

        val searchResult = if (continuationToken != null) {
            youTube.searchContinuation(continuationToken).getOrThrow()
        } else {
            youTube.search(query, YouTube.SearchFilter.FILTER_ALBUM).getOrThrow()
        }

        val albums = searchResult.items
            .filterIsInstance<AlbumItem>()
            .map { MetadataSearchResult.Album(it.toMetadataAlbumBasic()) }

        return PaginationResult(
            items = albums,
            totalCount = -1,
            nextPagination = searchResult.continuation?.let { PaginationStrategy.Continuation(it) }
        )
    }

    override suspend fun searchPlaylists(
        query: String,
        pagination: PaginationStrategy?
    ): PaginationResult<MetadataSearchResult.Playlist> {
        val continuationToken = when (pagination) {
            is PaginationStrategy.Continuation -> pagination.continuationToken
            else -> null
        }

        val searchResult = if (continuationToken != null) {
            youTube.searchContinuation(continuationToken).getOrThrow()
        } else {
            youTube.search(query, YouTube.SearchFilter.FILTER_FEATURED_PLAYLIST).getOrThrow()
        }

        val playlists = searchResult.items
            .filterIsInstance<PlaylistItem>()
            .map { MetadataSearchResult.Playlist(it.toMetadataPlaylist()) }

        return PaginationResult(
            items = playlists,
            totalCount = -1,
            nextPagination = searchResult.continuation?.let { PaginationStrategy.Continuation(it) }
        )
    }

    override suspend fun searchUsers(
        query: String,
        pagination: PaginationStrategy?
    ): PaginationResult<MetadataSearchResult.User> {
        val continuationToken = when (pagination) {
            is PaginationStrategy.Continuation -> pagination.continuationToken
            else -> null
        }

        val searchResult = if (continuationToken != null) {
            youTube.searchContinuation(continuationToken).getOrThrow()
        } else {
            youTube.search(query, YouTube.SearchFilter.FILTER_PROFILE).getOrThrow()
        }

        val users = searchResult.items
            .filterIsInstance<ArtistItem>()
            .map { artistItem ->
                MetadataSearchResult.User(
                    MetadataUser(
                        id = artistItem.id,
                        username = artistItem.title,
                        displayName = artistItem.title,
                        thumbnails = artistItem.thumbnail?.let {
                            listOf(
                                Thumbnail(
                                    url = it,
                                    width = 540,
                                    height = 540
                                )
                            )
                        }.orEmpty(),
                        externalUri = "https://music.youtube.com/channel/${artistItem.id}"
                    )
                )
            }

        return PaginationResult(
            items = users,
            totalCount = -1,
            nextPagination = searchResult.continuation?.let { PaginationStrategy.Continuation(it) }
        )
    }
}
