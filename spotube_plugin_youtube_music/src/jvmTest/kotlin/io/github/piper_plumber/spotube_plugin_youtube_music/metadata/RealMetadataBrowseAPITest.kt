package io.github.piper_plumber.spotube_plugin_youtube_music.metadata

import dev.krtirtho.plugin_interfaces.plugin_apis.metadata.browse.MetadataBrowseItem
import io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.YouTube
import io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.models.YouTubeLocale
import io.github.piper_plumber.spotube_plugin_youtube_music.test_utils.RealHttpClientAPI
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class RealMetadataBrowseAPITest {
    val youtube = YouTube(YouTubeLocale("US", "en"), RealHttpClientAPI())
    val client = RealMetadataBrowseAPI(youtube)

    @Test
    fun `test featured returns first row items`() = runTest {
        val items = client.featured()

        assertTrue(items.isNotEmpty(), "Featured should return items from first row")
        val hasTrack = items.any { it is MetadataBrowseItem.Track }
        val hasArtist = items.any { it is MetadataBrowseItem.Artist }
        val hasAlbum = items.any { it is MetadataBrowseItem.Album }
        val hasPlaylist = items.any { it is MetadataBrowseItem.Playlist }
        assertTrue(hasTrack || hasArtist || hasAlbum || hasPlaylist, "Featured should return at least one type of item")
    }

    @Test
    fun `test list returns sections from second row onwards`() = runTest {
        val result = client.list(null)

        assertTrue(result.items.isNotEmpty(), "Browse list should return sections")
        result.items.forEach { section ->
            assertTrue(section.title.isNotBlank(), "Section title should not be blank")
        }
    }

    @Test
    fun `test sublist returns items for valid section`() = runTest {
        val sections = client.list(null)
        val firstSection = sections.items.find { it.moreLink !=null }
        val result = client.sublist(firstSection!!.moreLink!!, null)

        assertTrue(result.items.isNotEmpty(), "Sublist should return items for valid section")
        result.items.forEach { item ->
            when (item) {
                is MetadataBrowseItem.Track -> assertTrue(item.data.title.isNotBlank())
                is MetadataBrowseItem.Artist -> assertTrue(item.data.name.isNotBlank())
                is MetadataBrowseItem.Album -> assertTrue(item.data.title.isNotBlank())
                is MetadataBrowseItem.Playlist -> assertTrue(item.data.title.isNotBlank())
                else -> {}
            }
        }
    }

    @Test
    fun `test sublist throws for invalid section`() = runTest {
        var threw = false
        try {
            client.sublist("nonexistent_section_xyz", null)
        } catch (e: IllegalArgumentException) {
            threw = true
        }
        assertTrue(threw, "Sublist should throw IllegalArgumentException for invalid section")
    }
}
