package io.github.piper_plumber.spotube_plugin_youtube_music.metadata

import io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.YouTube
import io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.models.YouTubeLocale
import io.github.piper_plumber.spotube_plugin_youtube_music.test_utils.RealHttpClientAPI
import io.github.piper_plumber.spotube_plugin_youtube_music.test_utils.authenticateTestUser
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RealMetadataPlaylistAPITest {
    val youtube = YouTube(YouTubeLocale("BD", "en"), RealHttpClientAPI()).apply {
        authenticateTestUser()
    }
    val client = RealMetadataPlaylistAPI(youtube)


    @Test
    fun `test getPlaylistInfo`() = runTest {
        val playlist = client.getPlaylist("PLGBuKfnErZlAq0H6P6doTx1EiUP5L_Q3-")

        assertNotNull(playlist)
        assertTrue(playlist.title.isNotBlank(), "Playlist title should not be blank")
        assertTrue(playlist.thumbnails.isNotEmpty(), "Playlist should have at least one thumbnail")
        assertNotNull(playlist.owner, "Playlist owner should not be null")
        assertEquals(
            playlist.owner?.displayName?.isNotBlank(),
            true,
            "Playlist owner name should not be blank"
        )
        assertTrue(playlist.trackCount > 0, "Playlist should have at least one track")
    }

    @Test
    fun `test savedPlaylist`() = runTest {
        val playlists = client.savedPlaylists(null)

        assertTrue(playlists.items.isNotEmpty(), "Saved playlists should not be empty")
        playlists.items.forEach {
            assertTrue(it.title.isNotBlank(), "Playlist title should not be blank")
            assertTrue(it.thumbnails.isNotEmpty(), "Playlist should have at least one thumbnail")
            assertNotNull(it.owner, "Playlist owner should not be null")
            assertEquals(
                it.owner?.displayName?.isNotBlank(),
                true,
                "Playlist owner name should not be blank"
            )
        }
    }

    @Test
    fun `test getPlaylistTracks`() = runTest {
        val tracks =
            client.getPlaylistTracks("PLGBuKfnErZlAq0H6P6doTx1EiUP5L_Q3-", null)

        assertTrue(tracks.items.isNotEmpty(), "Playlist tracks should not be empty")
        tracks.items.forEach {
            assertTrue(it.title.isNotBlank(), "Track title should not be blank")
            assertTrue(
                it.album?.thumbnails?.isNotEmpty() == true || it.thumbnails?.isNotEmpty() == true,
                "Track should have at least one thumbnail either from album or track"
            )
            assertNotNull(it.artists, "Track artists should not be null")
            assertTrue(it.artists.isNotEmpty(), "Track should have at least one artist")
            it.artists.forEach { artist ->
                assertTrue(artist.name.isNotBlank(), "Artist name should not be blank")
            }
        }
    }

    @Test
    fun `test isSavedPlaylists`() = runTest {
        val isSaved = client.isSavedPlaylists(listOf("RDCLAK5uy_mvPJO7smbmUuPjfFs5gEW0e0sIAJ1ngDo"))

        assertTrue(isSaved.isNotEmpty(), "isSavedPlaylists result should not be empty")
    }
}