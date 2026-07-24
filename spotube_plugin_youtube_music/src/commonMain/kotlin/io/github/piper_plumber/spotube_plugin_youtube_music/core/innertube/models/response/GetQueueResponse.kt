package io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.models.response

import io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.models.PlaylistPanelRenderer
import kotlinx.serialization.Serializable

@Serializable
data class GetQueueResponse(
    val queueDatas: List<QueueData>,
) {
    @Serializable
    data class QueueData(
        val content: PlaylistPanelRenderer.Content,
    )
}
