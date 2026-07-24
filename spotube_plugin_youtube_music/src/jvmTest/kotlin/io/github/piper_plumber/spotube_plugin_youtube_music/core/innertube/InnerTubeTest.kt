package io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube

import io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.models.YouTubeLocale
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertNotNull

class InnerTubeTest {
    @Test
    fun testInnerTube() = runTest {
        val youtube = YouTube(YouTubeLocale("BD", "en"), OkHttp)

        val res = youtube.home().getOrNull()

        assertNotNull(res)
    }
}