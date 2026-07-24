package io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class SubscriptionButton(
    val subscribeButtonRenderer: SubscribeButtonRenderer,
) {
    @Serializable
    data class SubscribeButtonRenderer(
        val subscribed: Boolean,
        val channelId: String,
        val longSubscriberCountText: Runs? = null,
        val shortSubscriberCountText: Runs? = null,
    )
}
