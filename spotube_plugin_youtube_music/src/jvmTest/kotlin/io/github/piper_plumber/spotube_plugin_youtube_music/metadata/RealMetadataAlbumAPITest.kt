package io.github.piper_plumber.spotube_plugin_youtube_music.metadata

import org.spotube.plugin_interfaces.plugin_apis.metadata.artist.MetadataArtist
import org.spotube.plugin_interfaces.plugin_apis.metadata.track.MetadataTrack
import io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.YouTube
import io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.models.YouTubeLocale
import io.github.piper_plumber.spotube_plugin_youtube_music.test_utils.RealHttpClientAPI
import io.github.piper_plumber.spotube_plugin_youtube_music.test_utils.authenticateTestUser
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RealMetadataAlbumAPITest {
    val youtube = YouTube(YouTubeLocale("BD", "en"), RealHttpClientAPI()).apply {
        authenticateTestUser()
    }
    val client = RealMetadataAlbumAPI(youtube)


    @Test
    fun `test getAlbumInfo`() = runTest {
        val album = client.getAlbum("MPREb_2XTBJUK3vdW")

        assertNotNull(album)
        assertTrue(album.title.isNotBlank(), "Album title should not be blank")
        assertTrue(album.thumbnails.isNotEmpty(), "Album should have at least one thumbnail")
        assertTrue(album.artists.isNotEmpty(), "Album artists should not be empty")
        album.artists.forEach {
            assertTrue(it.name.isNotBlank(), "Artist name should not be blank")
            assertTrue(it.id.isNotBlank(), "Artist ID should not be blank")
        }
    }

    @Test
    fun `test savedAlbums`() = runTest {
        val albums = client.savedAlbums(null)

        assertTrue(albums.items.isNotEmpty(), "Saved albums should not be empty")
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
    fun `test getAlbumTracks`() = runTest {
        val tracks =
            client.getAlbumTracks("MPREb_2XTBJUK3vdW", null)

        assertTrue(tracks.items.isNotEmpty(), "Album tracks should not be empty")
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
    fun `test getTrackAlbum`() = runTest {
        val album =
            client.getTrackAlbum(
                MetadataTrack(
                    id = "LZy5vrzeKE4",
                    title = "City Walls",
                    durationMs = 0L,
                    trackNumber = null,
                    discNumber = null,
                    artists = listOf(
                        MetadataArtist.Basic(
                            id = "UCBQZwaNPFfJ1gZ1fLZpAEGw",
                            name = "Twenty One Pilots",
                            thumbnails = emptyList(),
                            externalUri = "https://music.youtube.com/channel/UCBQZwaNPFfJ1gZ1fLZpAEGw"
                        )
                    ),
                    album = null,
                    thumbnails = null,
                    explicit = true,
                    popularity = null,
                    isrcCode = null,
                    externalUri = null
                )
            )

        assertNotNull(album)
        assertTrue(album.title.isNotBlank(), "Album title should not be blank")
        assertTrue(album.thumbnails.isNotEmpty(), "Album should have at least one thumbnail")
        assertTrue(album.artists.isNotEmpty(), "Album artists should not be empty")
        album.artists.forEach {
            assertTrue(it.name.isNotBlank(), "Artist name should not be blank")
            assertTrue(it.id.isNotBlank(), "Artist ID should not be blank")
        }
    }

    @Test
    fun `test isSavedAlbums`() = runTest {
        val isSaved = client.isSavedAlbums(listOf("MPREb_2XTBJUK3vdW"))

        assertTrue(isSaved.isNotEmpty(), "isSavedAlbums result should not be empty")
    }
}