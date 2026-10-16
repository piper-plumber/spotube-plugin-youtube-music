package io.github.piper_plumber.spotube_plugin_youtube_music.metadata

import org.spotube.plugin_interfaces.extras.logger.Logger
import org.spotube.plugin_interfaces.plugin_apis.metadata.browse.MetadataBrowseAPI
import org.spotube.plugin_interfaces.plugin_apis.metadata.browse.MetadataBrowseGenre
import org.spotube.plugin_interfaces.plugin_apis.metadata.browse.MetadataBrowseItem
import org.spotube.plugin_interfaces.plugin_apis.metadata.browse.MetadataBrowseSection
import org.spotube.plugin_interfaces.plugin_apis.metadata.common.PaginationResult
import org.spotube.plugin_interfaces.plugin_apis.metadata.common.PaginationStrategy
import io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.YouTube
import io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.models.AlbumItem
import io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.models.ArtistItem
import io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.models.PlaylistItem
import io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.models.SongItem
import io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.pages.BrowseResult
import io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.pages.HomePage
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class RealMetadataBrowseAPI(private val youTube: YouTube) : MetadataBrowseAPI {
    private val logger = Logger("RealMetadataBrowseAPI")

    private val cachedHomePage: MutableMap<String, HomePage> = mutableMapOf()
    private var homeMutex = Mutex()

    private suspend fun home(continuation: String? = null): HomePage {
        cachedHomePage[continuation ?: ""]?.let {
            logger.i { "[home] Returning cached home page" }
            return it
        }
        return homeMutex.withLock {
            cachedHomePage[continuation ?: ""]?.let { return@withLock it }

            logger.i { "[home] No cached home page, fetching..." }
            val result = youTube.home(continuation).getOrThrow()
            cachedHomePage[continuation ?: ""] = result
            logger.i { "[home] Fetched and cached home page" }
            result
        }
    }

    private suspend fun genre(genreId: String): List<MetadataBrowseSection> {
        return youTube.browse(browseId = "FEmusic_moods_and_genres_category", params = genreId)
            .getOrThrow().items.mapNotNull {
                if (!it.title.isNullOrBlank() && it.items.isNotEmpty()) {
                    MetadataBrowseSection(
                        title = it.title,
                        items = it.items.mapNotNull { item ->
                            when (item) {
                                is AlbumItem -> MetadataBrowseItem.Album(data = item.toMetadataAlbumBasic())
                                is ArtistItem -> MetadataBrowseItem.Artist(data = item.toMetadataArtistBasic())
                                is PlaylistItem -> MetadataBrowseItem.Playlist(data = item.toMetadataPlaylist())
                                is SongItem -> MetadataBrowseItem.Track(data = item.toMetadataTrack())
                                else -> null
                            }
                        }
                    )
                }
                null
            }
    }

    override suspend fun featured(): List<MetadataBrowseItem> {
        logger.i { "[featured] Fetching featured items" }
        val homePage = home()

        val firstSection = homePage.sections.firstOrNull()
            ?: return emptyList()
        val items = firstSection.items.mapNotNull { it.toMetadataBrowseItem() }
        logger.i { "[featured] Fetched featured items: $items" }
        return items
    }

    override suspend fun genres(): List<MetadataBrowseGenre> {
        val youtubeGenres = youTube.moodAndGenres().getOrThrow().flatMap { genre ->
            genre.items.mapNotNull { item ->
                item.endpoint.params?.let {
                    MetadataBrowseGenre(
                        id = it,
                        name = item.title,
                    )
                }
            }
        }

        return listOf(MetadataBrowseGenre(id = "home", name = "All")) + youtubeGenres
    }

    override suspend fun list(
        genreId: String,
        pagination: PaginationStrategy?
    ): PaginationResult<MetadataBrowseSection> {
        if (genreId == "home") {
            logger.i { "[list] Fetching sections" }
            val homePage =
                home(if (pagination is PaginationStrategy.Continuation) pagination.continuationToken else null)
            val sections = homePage.sections.drop(1).map { section ->
                MetadataBrowseSection(
                    title = section.title,
                    items = section.items.mapNotNull { it.toMetadataBrowseItem() },
                    moreLink = section.endpoint?.browseId,
                    description = section.label
                )
            }

            val res = PaginationResult(
                items = sections,
                totalCount = sections.size,
                nextPagination = homePage.continuation?.let { PaginationStrategy.Continuation(it) }
            )

            logger.i { "[list] Fetched sections: $res" }
            return res
        }

        val genreSections = genre(genreId)

        return PaginationResult(
            items = genreSections,
            totalCount = genreSections.size,
            nextPagination = null
        )
    }

    override suspend fun sublist(
        genreId: String,
        sectionId: String,
        pagination: PaginationStrategy?
    ): PaginationResult<MetadataBrowseItem> {
        if (genreId == "home") {
            val homePage = home()
            val targetSection = homePage.sections.find { it.endpoint?.browseId == sectionId }
                ?: throw IllegalArgumentException("Section not found: $sectionId")

            val items = targetSection.items.mapNotNull { it.toMetadataBrowseItem() }
            val hasMoreEndpoint = targetSection.endpoint != null

            return PaginationResult(
                items = items,
                totalCount = items.size,
                nextPagination = if (hasMoreEndpoint) pagination else null
            )
        }

        return PaginationResult(
            items = emptyList(),
            totalCount = 0,
            nextPagination = null
        )
    }

    private fun io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.models.YTItem.toMetadataBrowseItem(): MetadataBrowseItem? {
        return when (this) {
            is SongItem -> MetadataBrowseItem.Track(
                data = this.toMetadataTrack()
            )

            is ArtistItem -> MetadataBrowseItem.Artist(
                data = this.toMetadataArtistBasic()
            )

            is AlbumItem -> MetadataBrowseItem.Album(
                data = this.toMetadataAlbumBasic()
            )

            is PlaylistItem -> MetadataBrowseItem.Playlist(
                data = this.toMetadataPlaylist()
            )

            else -> null
        }
    }
}
