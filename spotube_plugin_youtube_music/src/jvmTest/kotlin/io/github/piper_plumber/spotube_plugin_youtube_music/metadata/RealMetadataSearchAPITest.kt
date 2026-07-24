package io.github.piper_plumber.spotube_plugin_youtube_music.metadata

import dev.krtirtho.plugin_interfaces.plugin_apis.metadata.search.MetadataSearchResult
import dev.krtirtho.plugin_interfaces.plugin_apis.metadata.search.MetadataSupportedSearchType
import io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.YouTube
import io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.models.YouTubeLocale
import io.github.piper_plumber.spotube_plugin_youtube_music.test_utils.RealHttpClientAPI
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RealMetadataSearchAPITest {
    val youtube = YouTube(YouTubeLocale("US", "en"), RealHttpClientAPI())
    val client = RealMetadataSearchAPI(youtube)

    @Test
    fun `test supportedSearchTypes contains all expected types`() {
        val supportedTypes = client.supportedSearchTypes

        assertTrue(supportedTypes.contains(MetadataSupportedSearchType.TRACK))
        assertTrue(supportedTypes.contains(MetadataSupportedSearchType.ARTIST))
        assertTrue(supportedTypes.contains(MetadataSupportedSearchType.ALBUM))
        assertTrue(supportedTypes.contains(MetadataSupportedSearchType.PLAYLIST))
        assertTrue(supportedTypes.contains(MetadataSupportedSearchType.USER))
        assertTrue(supportedTypes.contains(MetadataSupportedSearchType.ALL))
        assertEquals(6, supportedTypes.size)
    }

    private fun testTrack(track: MetadataSearchResult.Track) {
        assertTrue(track.data.title.isNotBlank(), "Track title should not be blank")
        assertTrue(track.data.artists.isNotEmpty(), "Track should have at least one artist")
        assertTrue(
            track.data.thumbnails?.isNotEmpty() == true || track.data.album?.thumbnails?.isNotEmpty() == true,
            "Track should have at least one thumbnail either directly or through its album"
        )
    }

    private fun testArtist(artist: MetadataSearchResult.Artist) {
        assertTrue(artist.data.name.isNotBlank(), "Artist name should not be blank")
        assertTrue(artist.data.id.isNotBlank(), "Artist ID should not be blank")
        assertTrue(artist.data.thumbnails.isNotEmpty(), "Artist should have at least one thumbnail")
    }

    private fun testAlbum(album: MetadataSearchResult.Album) {
        assertTrue(album.data.title.isNotBlank(), "Album title should not be blank")
        assertTrue(album.data.thumbnails.isNotEmpty(), "Album should have at least one thumbnail")
        assertTrue(album.data.artists.isNotEmpty(), "Album should have at least one artist")
        album.data.artists.forEach { artist ->
            assertTrue(artist.name.isNotBlank(), "Artist name should not be blank")
        }
    }

    private fun testPlaylist(playlist: MetadataSearchResult.Playlist) {
        assertTrue(playlist.data.title.isNotBlank(), "Playlist title should not be blank")
        assertTrue(
            playlist.data.thumbnails.isNotEmpty(),
            "Playlist should have at least one thumbnail"
        )
        assertNotNull(playlist.data.owner) { owner ->
            assertEquals(
                owner.displayName?.isNotBlank(),
                true,
                "Playlist owner display name should not be blank"
            )
            assertTrue(owner.id.isNotBlank(), "Playlist owner ID should not be blank")
        }
    }

    private fun testUser(user: MetadataSearchResult.User) {
        assertEquals(
            user.data.displayName?.isNotBlank(),
            true,
            "User display name should not be blank"
        )
        assertTrue(user.data.id.isNotBlank(), "User ID should not be blank")
        assertTrue(user.data.thumbnails.isNotEmpty(), "User should have at least one thumbnail")
    }

    @Test
    fun `test search returns mixed results`() = runTest {
        val results = client.search("Taylor Swift")

        assertTrue(results.isNotEmpty(), "Search should return results")
        val hasTrack = results.any { it is MetadataSearchResult.Track }
        val hasArtist = results.any { it is MetadataSearchResult.Artist }
        val hasAlbum = results.any { it is MetadataSearchResult.Album }
        val hasPlaylist = results.any { it is MetadataSearchResult.Playlist }
        assertTrue(
            hasTrack || hasArtist || hasAlbum || hasPlaylist,
            "Search should return at least one type of result"
        )

        results.forEach {
            when (it) {
                is MetadataSearchResult.Track -> {
                    testTrack(it)
                }

                is MetadataSearchResult.Artist -> {
                    testArtist(it)
                }

                is MetadataSearchResult.Album -> {
                    testAlbum(it)
                }

                is MetadataSearchResult.Playlist -> {
                    testPlaylist(it)
                }

                is MetadataSearchResult.User -> {
                    testUser(it)
                }
            }
        }
    }

    @Test
    fun `test searchTracks returns tracks`() = runTest {
        val result = client.searchTracks("Bohemian Rhapsody", null)

        assertTrue(result.items.isNotEmpty(), "Search tracks should return results")
        result.items.forEach { trackResult ->
            testTrack(trackResult)
        }
    }

    @Test
    fun `test searchArtists returns artists`() = runTest {
        val result = client.searchArtists("Drake", null)

        assertTrue(result.items.isNotEmpty(), "Search artists should return results")
        result.items.forEach { artistResult ->
            testArtist(artistResult)
        }
    }

    @Test
    fun `test searchAlbums returns albums`() = runTest {
        val result = client.searchAlbums("Thriller", null)

        assertTrue(result.items.isNotEmpty(), "Search albums should return results")
        result.items.forEach { albumResult ->
            testAlbum(albumResult)
        }
    }

    @Test
    fun `test searchPlaylists returns playlists`() = runTest {
        val result = client.searchPlaylists("Chill Hits", null)

        assertTrue(result.items.isNotEmpty(), "Search playlists should return results")
        result.items.forEach { playlistResult ->
            testPlaylist(playlistResult)
        }
    }

    @Test
    fun `test searchUsers returns users`() = runTest {
        val result = client.searchUsers("PewDiePie", null)

        assertTrue(result.items.isNotEmpty(), "Search users should return results")
        result.items.forEach { userResult ->
            testUser(userResult)
        }
    }
}
