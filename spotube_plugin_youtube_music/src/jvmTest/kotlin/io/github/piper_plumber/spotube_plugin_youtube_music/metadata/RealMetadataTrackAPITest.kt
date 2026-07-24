package io.github.piper_plumber.spotube_plugin_youtube_music.metadata

import io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.YouTube
import io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.models.YouTubeLocale
import io.github.piper_plumber.spotube_plugin_youtube_music.test_utils.RealHttpClientAPI
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RealMetadataTrackAPITest {
    val youtube = YouTube(YouTubeLocale("BD", "en"), RealHttpClientAPI()).apply {
//        authenticateTestUser()
    }
    val client = RealMetadataTrackAPI(youtube)


    @Test
    fun `test getTrackInfo`() = runTest {
        val track = client.getTrack("dQw4w9WgXcQ")

        assertNotNull(track)
        assertTrue(track.title.isNotBlank(), "Track title should not be blank")
        assertTrue(track.durationMs > 0, "Track duration should be greater than 0")
        assertTrue(track.artists.isNotEmpty(), "Track should have at least one artist")
        track.artists.forEach {
            assertTrue(it.name.isNotBlank(), "Artist name should not be blank")
            assertTrue(it.id.isNotBlank(), "Artist ID should not be blank")
        }
    }
}