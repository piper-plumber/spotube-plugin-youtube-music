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
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeMark
import kotlin.time.TimeSource

class RealMetadataBrowseAPI(private val youTube: YouTube) : MetadataBrowseAPI {
    private companion object {
        const val MIN_HOME_SECTIONS = 6
        val HOME_CACHE_TTL = 5.minutes
    }

    private val logger = Logger("RealMetadataBrowseAPI")

    private class CachedHome(val page: HomePage, val fetchedAt: TimeMark)

    private val cachedHomePage: MutableMap<String, CachedHome> = mutableMapOf()
    private var cachedSession: String? = null
    private var homeMutex = Mutex()

    /** Identifies the current login session, so a login/logout never serves the other session's feed */
    private fun sessionKey(): String = "${youTube.cookie}|${youTube.dataSyncId}"

    private fun cachedHome(continuation: String?): HomePage? {
        if (cachedSession != sessionKey()) {
            cachedHomePage.clear()
            return null
        }
        val entry = cachedHomePage[continuation ?: ""] ?: return null
        if (entry.fetchedAt.elapsedNow() > HOME_CACHE_TTL) {
            cachedHomePage.remove(continuation ?: "")
            return null
        }
        return entry.page
    }

    private suspend fun home(continuation: String? = null): HomePage {
        cachedHome(continuation)?.let {
            logger.i { "[home] Returning cached home page" }
            return it
        }
        return homeMutex.withLock {
            cachedHome(continuation)?.let { return@withLock it }

            logger.i { "[home] No cached home page, fetching..." }
            val session = sessionKey()
            val result = youTube.home(continuation).getOrThrow()
            if (cachedSession != session) cachedHomePage.clear()
            cachedSession = session
            cachedHomePage[continuation ?: ""] = CachedHome(result, TimeSource.Monotonic.markNow())
            logger.i { "[home] Fetched and cached home page" }
            result
        }
    }

    private suspend fun genre(genreId: String): List<MetadataBrowseSection> {
        return youTube.browse(browseId = "FEmusic_moods_and_genres_category", params = genreId)
            .getOrThrow().items.mapNotNull {
                if (it.title.isNullOrBlank() || it.items.isEmpty()) return@mapNotNull null
                val items = it.items.mapNotNull { item -> item.toMetadataBrowseItem() }
                if (items.isEmpty()) return@mapNotNull null
                MetadataBrowseSection(title = it.title, items = items)
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
            val isFirstPage = pagination !is PaginationStrategy.Continuation
            var homePage =
                home(if (pagination is PaginationStrategy.Continuation) pagination.continuationToken else null)
            // The first section is exposed via featured(); only the first page contains it
            val allSections = (if (isFirstPage) homePage.sections.drop(1) else homePage.sections).toMutableList()

            // A single home response carries only a few shelves, which leaves the "All" tab nearly empty.
            // Eagerly follow continuations until there is enough content to fill the screen.
            var extraRequests = 0
            while (allSections.size < MIN_HOME_SECTIONS && homePage.continuation != null && extraRequests < 5) {
                homePage = home(homePage.continuation)
                allSections.addAll(homePage.sections)
                extraRequests++
            }

            val sections = allSections.map { section ->
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
            val targetSection = cachedHomePage.values.asSequence()
                .flatMap { it.page.sections }
                .find { it.endpoint?.browseId == sectionId }
                ?: home().sections.find { it.endpoint?.browseId == sectionId }
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
