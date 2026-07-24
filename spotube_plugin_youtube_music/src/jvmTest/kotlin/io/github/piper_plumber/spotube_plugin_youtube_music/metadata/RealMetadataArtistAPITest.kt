package io.github.piper_plumber.spotube_plugin_youtube_music.metadata

import io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.YouTube
import io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.models.YouTubeLocale
import io.github.piper_plumber.spotube_plugin_youtube_music.test_utils.RealHttpClientAPI
import io.github.piper_plumber.spotube_plugin_youtube_music.test_utils.authenticateTestUser
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RealMetadataArtistAPITest {
    val youtube = YouTube(YouTubeLocale("BD", "en"), RealHttpClientAPI()).apply {
        authenticateTestUser()
    }
    val client = RealMetadataArtistAPI(youtube)


    @Test
    fun `test getArtistInfo`() = runTest {
        val artist = client.getArtist("UCBQZwaNPFfJ1gZ1fLZpAEGw")

        assertNotNull(artist)
        assertTrue(artist.id.isNotBlank(), "Artist id should not be blank")
        assertTrue(artist.name.isNotBlank(), "Artist name should not be blank")
        assertTrue(artist.thumbnails.isNotEmpty(), "Artist should have at least one thumbnail")
    }

    @Test
    fun `test getArtistTop10Tracks`() = runTest {
        val tracks = client.getArtistTop10Tracks("UCBQZwaNPFfJ1gZ1fLZpAEGw")

        assertTrue(tracks.isNotEmpty(), "Artist top tracks should not be empty")
        assertTrue(tracks.size <= 10, "Artist top tracks should not be more than 10")
        tracks.forEach {
            assertTrue(it.title.isNotBlank(), "Track title should not be blank")
            assertTrue(it.artists.isNotEmpty(), "Track artists should not be empty")
            it.artists.forEach { artist ->
                assertTrue(artist.name.isNotBlank(), "Artist name should not be blank")
                assertTrue(artist.id.isNotBlank(), "Artist ID should not be blank")
            }
        }
    }

    @Test
    fun `test getArtistAlbums`() = runTest {
        val albums = client.getArtistAlbums("UCBQZwaNPFfJ1gZ1fLZpAEGw", null)

        assertTrue(albums.items.isNotEmpty(), "Artist albums should not be empty")
        albums.items.forEach {
            assertTrue(it.title.isNotBlank(), "Album title should not be blank")
            assertTrue(it.thumbnails.isNotEmpty(), "Album should have at least one thumbnail")
            assertTrue(it.artists.isNotEmpty(), "Album artists should not be empty")
            it.artists.forEach { artist ->
                assertTrue(artist.name.isNotBlank(), "Artist name should not be blank")
                assertTrue(artist.id.isNotBlank(), "Artist ID should not be blank")
            }
        }
    }

    @Test
    fun `test savedArtists`() = runTest {
        val artists = client.savedArtists(null)

        assertTrue(artists.items.isNotEmpty(), "Saved artists should not be empty")
        artists.items.forEach {
            assertTrue(it.name.isNotBlank(), "Artist name should not be blank")
            assertTrue(it.thumbnails.isNotEmpty(), "Artist should have at least one thumbnail")
            assertTrue(it.id.isNotBlank(), "Artist ID should not be blank")
        }
    }

    @Test
    fun `test isSavedArtists`() = runTest {
        val isSaved = client.isSavedArtists(listOf("UCwppdrjsBPAZg5_cUwQjfMQ"))

        assertTrue(isSaved.isNotEmpty(), "isSavedAlbums result should not be empty")
    }
}