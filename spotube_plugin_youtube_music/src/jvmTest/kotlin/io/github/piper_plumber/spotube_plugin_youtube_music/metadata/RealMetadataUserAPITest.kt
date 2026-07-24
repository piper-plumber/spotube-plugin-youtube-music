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

class RealMetadataUserAPITest {
    val youtube = YouTube(YouTubeLocale("BD", "en"), RealHttpClientAPI()).apply {
        authenticateTestUser()
    }
    val client = RealMetadataUserAPI(youtube)


    @Test
    fun `test getUser`() = runTest {
        val user = client.getUser("lmao")

        assertNotNull(user)
        assertTrue(user.id.isNotBlank(), "User ID should not be blank")
        assertEquals(user.displayName?.isNotBlank(), true, "User name should not be blank")
        assertTrue(user.username.isNotBlank(), "Username should not be blank")
    }
}