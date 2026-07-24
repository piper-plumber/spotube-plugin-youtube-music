package io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.models.response

import kotlinx.serialization.Serializable

@Serializable
data class ImageUploadResponse(
    val encryptedBlobId: String
)