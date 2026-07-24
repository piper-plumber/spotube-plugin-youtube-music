package io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class MusicResponsiveHeaderRenderer(
    val thumbnail: ThumbnailRenderer?,
    val buttons: List<Button>,
    val title: Runs,
    val subtitle: Runs,
    val secondSubtitle: Runs?,
    val straplineTextOne: Runs?,
    val facepile: Facepile?,
) {
    @Serializable
    data class Facepile(
        val avatarStackViewModel: AvatarStackViewModel?,
    ) {
        @Serializable
        data class AvatarStackViewModel(
            val avatars: List<Item>?,
            val text: Text?,
            val rendererContext: RendererContext?,
        ) {
            @Serializable
            data class RendererContext(
                val accessibilityContext: AccessibilityContext?,
                val commandContext: CommandContext?,
            ) {
                @Serializable
                data class AccessibilityContext(
                    val label: String,
                )

                @Serializable
                data class CommandContext(
                    val onTap: OnTap?,
                ) {
                    @Serializable
                    data class OnTap(
                        val innertubeCommand: InnertubeCommand?,
                    ) {
                        @Serializable
                        data class InnertubeCommand(
                            val clickTrackingParams: String?,
                            val browseEndpoint: BrowseEndpoint?,
                        )
                    }
                }
            }

            @Serializable
            data class Text(
                val content: String
            )

            @Serializable
            data class Item(
                val avatarViewModel: AvatarViewModel,
            ) {
                @Serializable
                data class AvatarViewModel(
                    val image: Image,
                    val avatarImageSize: String?,
                ) {
                    @Serializable
                    data class Image(
                        val sources: List<Source>,
                    ) {
                        @Serializable
                        data class Source(
                            val url: String,
                        )
                    }
                }
            }
        }
    }

    @Serializable
    data class Button(
        val musicPlayButtonRenderer: MusicPlayButtonRenderer?,
        val menuRenderer: Menu.MenuRenderer?,
        val toggleButtonRenderer: ToggleButtonRenderer?,
    ) {
        @Serializable
        data class MusicPlayButtonRenderer(
            val playNavigationEndpoint: NavigationEndpoint?,
        )

        @Serializable
        data class ToggleButtonRenderer(
            val defaultIcon: Icon?,
            val defaultServiceEndpoint: DefaultServiceEndpoint?,
            val toggledServiceEndpoint: ToggledServiceEndpoint?,
        )
    }
}
