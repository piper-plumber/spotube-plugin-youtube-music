package io.github.piper_plumber.spotube_plugin_youtube_music.metadata

import org.spotube.plugin_interfaces.plugin_apis.metadata.user.MetadataUser
import org.spotube.plugin_interfaces.plugin_apis.metadata.user.MetadataUserAPI
import org.spotube.plugin_interfaces.plugin_apis.metadata.common.Thumbnail
import io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.YouTube

class RealMetadataUserAPI(val youTube: YouTube) : MetadataUserAPI {
    override suspend fun getUser(id: String): MetadataUser {
        val accountInfo = youTube.accountInfo().getOrThrow()

        val thumbnails = accountInfo.thumbnailUrl?.let {
            listOf(Thumbnail(url = it, width = 300, height = 300))
        }.orEmpty()

        return MetadataUser(
            id = accountInfo.channelHandle ?: accountInfo.email ?: accountInfo.name,
            username = accountInfo.channelHandle ?: accountInfo.email ?: accountInfo.name,
            displayName = accountInfo.name,
            thumbnails = thumbnails,
            externalUri = accountInfo.channelHandle?.let { "https://music.youtube.com/channel/$it" }
        )
    }
}