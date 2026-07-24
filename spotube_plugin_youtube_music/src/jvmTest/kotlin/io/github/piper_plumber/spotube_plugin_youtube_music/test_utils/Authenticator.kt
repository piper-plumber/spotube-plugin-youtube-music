package io.github.piper_plumber.spotube_plugin_youtube_music.test_utils

import io.github.piper_plumber.spotube_plugin_youtube_music.BuildKonfig
import io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.YouTube
import io.github.piper_plumber.spotube_plugin_youtube_music.core.services.SingletonsRegistry

fun YouTube.authenticateTestUser() {
    SingletonsRegistry.crypto = JVMCryptoAPI()
    cookie = BuildKonfig.COOKIE
    dataSyncId = BuildKonfig.DATA_SYNC_ID
    visitorData = BuildKonfig.VISITOR_ID
}