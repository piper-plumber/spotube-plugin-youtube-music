package io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube

import dev.krtirtho.plugin_interfaces.extras.logger.Logger
import dev.krtirtho.plugin_interfaces.extras.spotor.HttpRequestBuilder
import dev.krtirtho.plugin_interfaces.extras.spotor.JsonContentSerializer
import dev.krtirtho.plugin_interfaces.extras.spotor.SpotrClient
import dev.krtirtho.plugin_interfaces.extras.spotor.SpotrHttpResponse
import dev.krtirtho.plugin_interfaces.host_apis.HttpClientAPI
import io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.models.Context
import io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.models.MediaInfo
import io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.models.ReturnYouTubeDislikeResponse
import io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.models.YouTubeClient
import io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.models.YouTubeLocale
import io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.models.body.AccountMenuBody
import io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.models.body.Action
import io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.models.body.BrowseBody
import io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.models.body.CreatePlaylistBody
import io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.models.body.EditPlaylistBody
import io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.models.body.FeedbackBody
import io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.models.body.GetQueueBody
import io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.models.body.GetSearchSuggestionsBody
import io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.models.body.GetTranscriptBody
import io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.models.body.LikeBody
import io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.models.body.NextBody
import io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.models.body.PlayerBody
import io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.models.body.PlaylistDeleteBody
import io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.models.body.SearchBody
import io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.models.body.SubscribeBody
import io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.models.response.NextResponse
import io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.utils.parseCookieString
import io.github.piper_plumber.spotube_plugin_youtube_music.core.innertube.utils.sha1
import kotlinx.coroutines.delay
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.time.Clock

/**
 * Provide access to InnerTube endpoints.
 * For making HTTP requests, not parsing response.
 */
@OptIn(ExperimentalEncodingApi::class)
class InnerTube(
    var locale: YouTubeLocale,
    var httpClientAPI: HttpClientAPI,
) {
    private val logger = Logger("InnerTube")
    private var httpClient = createClient()

    var visitorData: String? = null
    var dataSyncId: String? = null
    var cookie: String? = null
        set(value) {
            field = value
            cookieMap = if (value == null) emptyMap() else parseCookieString(value)
        }
    private var cookieMap = emptyMap<String, String>()

    var useLoginForBrowse: Boolean = false

    @OptIn(ExperimentalSerializationApi::class)
    private fun createClient() = SpotrClient(httpClientAPI) {
        serializer = JsonContentSerializer(Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            encodeDefaults = true
        })

        defaultUrl = YouTubeClient.API_URL_YOUTUBE_MUSIC
        defaultHeaders.apply {
            put("Accept", "application/json")
            put("Accept-Language", "en-US,en;q=0.9")
            put("Cache-Control", "no-cache")
        }
    }

    private suspend fun authorizationHeader(): String? {
        if ("SAPISID" !in cookieMap) {
            logger.i { "[sapisidHash] SAPISID not found in cookie" }
            return null
        }
        val currentTime = Clock.System.now().toEpochMilliseconds() / 1000
        val hash =
            sha1("$currentTime ${cookieMap["SAPISID"]} ${YouTubeClient.ORIGIN_YOUTUBE_MUSIC}")
        return "SAPISIDHASH ${currentTime}_$hash"
    }

    private fun HttpRequestBuilder.ytClient(
        client: YouTubeClient,
        setLogin: Boolean = false,
        sapisidHash: String? = null
    ) {
        logger.i { "[ytClient] Setting up content-type" }
        contentType("application/json")
        logger.i { "[ytClient] Setting up request headers" }
        headers {
            logger.i { "[ytClient] Appending X-Goog-Api-Format-Version header" }
            append("X-Goog-Api-Format-Version", "1")
            logger.i { "[ytClient] Appending X-YouTube-Client-Name header with value ${client.clientId}" }
            append(
                "X-YouTube-Client-Name",
                client.clientId /* Not a typo. The Client-Name header does contain the client id. */
            )
            logger.i { "[ytClient] Appending X-YouTube-Client-Version header with value ${client.clientVersion}" }
            append("X-YouTube-Client-Version", client.clientVersion)
            logger.i { "[ytClient] Appending X-Origin header with value ${YouTubeClient.ORIGIN_YOUTUBE_MUSIC}" }
            append("X-Origin", YouTubeClient.ORIGIN_YOUTUBE_MUSIC)
            logger.i { "[ytClient] Appending Referer header with value ${YouTubeClient.REFERER_YOUTUBE_MUSIC}" }
            append("Referer", YouTubeClient.REFERER_YOUTUBE_MUSIC)
            logger.i { "[ytClient] Appending X-Goog-Visitor-Id header with value $visitorData" }
            visitorData?.let { append("X-Goog-Visitor-Id", it) }
            logger.i { "[ytClient] is cookie set: ${setLogin && client.loginSupported}" }
            if (setLogin && client.loginSupported) {
                logger.i { "[ytClient] Appending cookie header with value $cookie" }
                cookie?.let { cookie ->
                    append("cookie", cookie)
                    logger.i { "[ytClient] Appending Authorization header" }

                    sapisidHash?.let {
                        logger.i { "[ytClient] Using provided SAPISID hash" }
                        append("Authorization", it)
                    }
                }
            }
            append("User-Agent", client.userAgent)
        }
        logger.i { "[ytClient] Appending prettyPrint parameter with value false" }
        parameter("prettyPrint", "false")
        logger.i { "[ytClient] Finished setting up request headers" }
    }


    suspend fun search(
        client: YouTubeClient,
        query: String? = null,
        params: String? = null,
        continuation: String? = null,
    ): SpotrHttpResponse {
        val authHeader = authorizationHeader()
        return httpClient.post {
            url("search")
            ytClient(client, setLogin = useLoginForBrowse, authHeader)
            jsonBody(
                SearchBody(
                    context = client.toContext(
                        locale,
                        visitorData,
                        if (useLoginForBrowse) dataSyncId else null
                    ),
                    query = query,
                    params = params
                )
            )
            continuation?.let {
                parameter("continuation", continuation)
                parameter("ctoken", continuation)
            }
        }
    }

    suspend fun player(
        client: YouTubeClient,
        videoId: String,
        playlistId: String?,
        signatureTimestamp: Int?,
        poToken: String? = null,
    ): SpotrHttpResponse {
        val authHeader = authorizationHeader()
        return httpClient.post {
            url("player")
            ytClient(client, setLogin = true, authHeader)
            jsonBody(
                PlayerBody(
                    context = client.toContext(locale, visitorData, dataSyncId).let {
                        if (client.isEmbedded) {
                            it.copy(
                                thirdParty = Context.ThirdParty(
                                    embedUrl = "https://www.youtube.com/watch?v=${videoId}"
                                )
                            )
                        } else it
                    },
                    videoId = videoId,
                    playlistId = playlistId,
                    playbackContext = if (client.useSignatureTimestamp && signatureTimestamp != null) {
                        PlayerBody.PlaybackContext(
                            PlayerBody.PlaybackContext.ContentPlaybackContext(
                                signatureTimestamp
                            )
                        )
                    } else null,
                    serviceIntegrityDimensions = if (client.useWebPoTokens && poToken != null) {
                        PlayerBody.ServiceIntegrityDimensions(poToken)
                    } else null,
                )
            )
        }
    }

    suspend fun registerPlayback(
        url: String,
        cpn: String,
        playlistId: String?,
        client: YouTubeClient = YouTubeClient.WEB_REMIX,
    ): SpotrHttpResponse {
        val authHeader = authorizationHeader()
        return httpClient.get {
            url(url)
            ytClient(client, true, authHeader)
            parameter("ver", "2")
            parameter("c", client.clientName)
            parameter("cpn", cpn)

            playlistId?.let {
                parameter("list", playlistId)
                parameter("referrer", "https://music.youtube.com/playlist?list=$playlistId")
            }

        }
    }

    suspend fun browse(
        client: YouTubeClient,
        browseId: String? = null,
        params: String? = null,
        continuation: String? = null,
        setLogin: Boolean = false,
    ): SpotrHttpResponse {
        val authHeader = authorizationHeader()
        logger.i { "[browse] Fetching browse page" }
        val body = BrowseBody(
            context = client.toContext(
                locale,
                visitorData,
                if (setLogin || useLoginForBrowse) dataSyncId else null
            ),
            browseId = browseId,
            params = params,
            continuation = continuation
        )

        val res = httpClient.post {
            logger.i { "[browse] Calling browse with browseId=$browseId, params=$params, continuation=$continuation" }
            url("browse")
            logger.i { "[browse] Set login: ${setLogin || useLoginForBrowse}" }
            ytClient(client, setLogin = setLogin || useLoginForBrowse, authHeader)
            logger.i { "[browse] continuation: $continuation" }
            continuation?.let { parameter("continuation", it) }
            logger.i { "[browse] body: $body" }
            jsonBody(body)
            logger.i { "[browse] Request sent, awaiting response..." }
        }
        logger.i { "[browse] Fetched browse page: $res" }
        return res
    }

    suspend fun next(
        client: YouTubeClient,
        videoId: String?,
        playlistId: String?,
        playlistSetVideoId: String?,
        index: Int?,
        params: String?,
        continuation: String? = null,
    ): SpotrHttpResponse {
        val authHeader = authorizationHeader()
        return httpClient.post {
            url("next")
            ytClient(client, setLogin = true, authHeader)
            jsonBody(
                NextBody(
                    context = client.toContext(locale, visitorData, dataSyncId),
                    videoId = videoId,
                    playlistId = playlistId,
                    playlistSetVideoId = playlistSetVideoId,
                    index = index,
                    params = params,
                    continuation = continuation
                )
            )
        }
    }

    suspend fun feedback(
        client: YouTubeClient,
        tokens: List<String>
    ): SpotrHttpResponse {
        val authHeader = authorizationHeader()
        return httpClient.post {
            url("feedback")
            ytClient(client, setLogin = true, authHeader)
            jsonBody(
                FeedbackBody(
                    context = client.toContext(locale, visitorData, dataSyncId),
                    feedbackTokens = tokens
                )
            )
        }
    }

    suspend fun getSearchSuggestions(
        client: YouTubeClient,
        input: String,
    ): SpotrHttpResponse {
        return httpClient.post {
            url("music/get_search_suggestions")
            ytClient(client)
            jsonBody(
                GetSearchSuggestionsBody(
                    context = client.toContext(locale, visitorData, null),
                    input = input
                )
            )
        }
    }

    suspend fun getQueue(
        client: YouTubeClient,
        videoIds: List<String>?,
        playlistId: String?,
    ) = run {
        httpClient.post {
            url("music/get_queue")
            ytClient(client)
            jsonBody(
                GetQueueBody(
                    context = client.toContext(locale, visitorData, null),
                    videoIds = videoIds,
                    playlistId = playlistId
                )
            )
        }
    }

    suspend fun getTranscript(
        client: YouTubeClient,
        videoId: String,
    ) = run {
        httpClient.post {
            url("https://music.youtube.com/youtubei/v1/get_transcript")
            parameter("key", "AIzaSyC9XL3ZjWddXya6X74dJoCTL-WEYFDNX3")
            headers {
                append("Content-Type", "application/json")
            }
            jsonBody(
                GetTranscriptBody(
                    context = client.toContext(locale, null, null),
                    params = Base64.Default.encode(
                        "\n${11.toChar()}$videoId".encodeToByteArray()
                    )
                )
            )
        }
    }

    suspend fun getSwJsData() = run {
        httpClient.get {
            url("https://music.youtube.com/sw.js_data")
        }
    }

    suspend fun accountMenu(client: YouTubeClient): SpotrHttpResponse {
        val authHeader = authorizationHeader()
        return httpClient.post {
            url("account/account_menu")
            ytClient(client, setLogin = true, authHeader)
            jsonBody(AccountMenuBody(client.toContext(locale, visitorData, dataSyncId)))
        }
    }

    suspend fun likeVideo(
        client: YouTubeClient,
        videoId: String,
    ) = run {
        val authHeader = authorizationHeader()
        httpClient.post {
            url("like/like")
            ytClient(client, setLogin = true, authHeader)
            jsonBody(
                LikeBody(
                    context = client.toContext(locale, visitorData, dataSyncId),
                    target = LikeBody.Target.video(videoId)
                )
            )
        }
    }

    suspend fun unlikeVideo(
        client: YouTubeClient,
        videoId: String,
    ) = run {
        val authHeader = authorizationHeader()
        httpClient.post {
            url("like/removelike")
            ytClient(client, setLogin = true, authHeader)
            jsonBody(
                LikeBody(
                    context = client.toContext(locale, visitorData, dataSyncId),
                    target = LikeBody.Target.video(videoId)
                )
            )
        }
    }

    suspend fun subscribeChannel(
        client: YouTubeClient,
        channelId: String,
        params: String? = null,
    ) = run {
        val authHeader = authorizationHeader()
        httpClient.post {
            url("subscription/subscribe")
            ytClient(client, setLogin = true, authHeader)
            jsonBody(
                SubscribeBody(
                    context = client.toContext(locale, visitorData, dataSyncId),
                    channelIds = listOf(channelId),
                    params = params
                )
            )
        }
    }

    suspend fun unsubscribeChannel(
        client: YouTubeClient,
        channelId: String,
        params: String? = null,
    ) = run {
        val authHeader = authorizationHeader()
        httpClient.post {
            url("subscription/unsubscribe")
            ytClient(client, setLogin = true, authHeader)
            jsonBody(
                SubscribeBody(
                    context = client.toContext(locale, visitorData, dataSyncId),
                    channelIds = listOf(channelId),
                    params = params
                )
            )
        }
    }

    suspend fun likePlaylist(
        client: YouTubeClient,
        playlistId: String,
    ) = run {
        val authHeader = authorizationHeader()
        httpClient.post {
            url("like/like")
            ytClient(client, setLogin = true, authHeader)
            jsonBody(
                LikeBody(
                    context = client.toContext(locale, visitorData, dataSyncId),
                    target = LikeBody.Target.playlist(playlistId)
                )
            )
        }
    }

    suspend fun unlikePlaylist(
        client: YouTubeClient,
        playlistId: String,
    ) = run {
        val authHeader = authorizationHeader()
        httpClient.post {
            url("like/removelike")
            ytClient(client, setLogin = true, authHeader)
            jsonBody(
                LikeBody(
                    context = client.toContext(locale, visitorData, dataSyncId),
                    target = LikeBody.Target.playlist(playlistId)
                )
            )
        }
    }

    suspend fun addToPlaylist(
        client: YouTubeClient,
        playlistId: String,
        videoId: String,
    ) = run {
        val authHeader = authorizationHeader()

        httpClient.post {
            url("browse/edit_playlist")
            ytClient(client, setLogin = true, authHeader)
            jsonBody(
                EditPlaylistBody(
                    context = client.toContext(locale, visitorData, dataSyncId),
                    playlistId = playlistId.removePrefix("VL"),
                    actions = listOf(
                        Action.AddVideoAction(addedVideoId = videoId)
                    )
                )
            )
        }
    }

    suspend fun addPlaylistToPlaylist(
        client: YouTubeClient,
        playlistId: String,
        addPlaylistId: String,
    ) = run {
        val authHeader = authorizationHeader()

        httpClient.post {
            url("browse/edit_playlist")
            ytClient(client, setLogin = true, authHeader)
            jsonBody(
                EditPlaylistBody(
                    context = client.toContext(locale, visitorData, dataSyncId),
                    playlistId = playlistId.removePrefix("VL"),
                    actions = listOf(
                        Action.AddPlaylistAction(addedFullListId = addPlaylistId)
                    )
                )
            )
        }
    }

    suspend fun removeFromPlaylist(
        client: YouTubeClient,
        playlistId: String,
        videoId: String,
        setVideoId: String,
    ) = run {
        val authHeader = authorizationHeader()

        httpClient.post {
            url("browse/edit_playlist")
            ytClient(client, setLogin = true, authHeader)
            jsonBody(
                EditPlaylistBody(
                    context = client.toContext(locale, visitorData, dataSyncId),
                    playlistId = playlistId.removePrefix("VL"),
                    actions = listOf(
                        Action.RemoveVideoAction(
                            removedVideoId = videoId,
                            setVideoId = setVideoId,
                        )
                    )
                )
            )
        }
    }

    suspend fun moveSongPlaylist(
        client: YouTubeClient,
        playlistId: String,
        setVideoId: String,
        successorSetVideoId: String?,
    ) = run {
        val authHeader = authorizationHeader()

        httpClient.post {
            url("browse/edit_playlist")
            ytClient(client, setLogin = true, authHeader)
            jsonBody(
                EditPlaylistBody(
                    context = client.toContext(locale, visitorData, dataSyncId),
                    playlistId = playlistId,
                    actions = listOf(
                        Action.MoveVideoAction(
                            movedSetVideoIdSuccessor = successorSetVideoId,
                            setVideoId = setVideoId,
                        )
                    )
                )
            )
        }
    }

    suspend fun createPlaylist(
        client: YouTubeClient,
        title: String,
    ) = run {
        val authHeader = authorizationHeader()

        httpClient.post {
            url("playlist/create")
            ytClient(client, true, authHeader)
            jsonBody(
                CreatePlaylistBody(
                    context = client.toContext(locale, visitorData, dataSyncId),
                    title = title
                )
            )
        }
    }

    suspend fun renamePlaylist(
        client: YouTubeClient,
        playlistId: String,
        name: String,
    ) = run {
        val authHeader = authorizationHeader()

        httpClient.post {
            url("browse/edit_playlist")
            ytClient(client, setLogin = true, authHeader)
            jsonBody(
                EditPlaylistBody(
                    context = client.toContext(locale, visitorData, dataSyncId),
                    playlistId = playlistId,
                    actions = listOf(
                        Action.RenamePlaylistAction(
                            playlistName = name
                        )
                    )
                )
            )
        }
    }

    suspend fun getUploadCustomThumbnailLink(
        client: YouTubeClient,
        contentLength: Int
    ) = run {
        val authHeader = authorizationHeader()
        httpClient.post {
            url("https://music.youtube.com/playlist_image_upload/playlist_custom_thumbnail")
            ytClient(client, setLogin = true, authHeader)
            headers {
                append("X-Goog-Upload-Command", "start")
                append("X-Goog-Upload-Protocol", "resumable")
                append("X-Goog-Upload-Header-Content-Length", contentLength.toString())
            }
        }
    }

    suspend fun uploadCustomThumbnail(
        client: YouTubeClient,
        uploadId: String,
        image: ByteArray,
    ) = run {
        val authHeader = authorizationHeader()

        httpClient.post {
            url("https://music.youtube.com/playlist_image_upload/playlist_custom_thumbnail")
            ytClient(client, setLogin = true, authHeader)
            parameter("upload_id", uploadId)
            parameter("upload_protocol", "resumable")
            headers {
                append("X-Goog-Upload-Command", "upload, finalize")
                append("X-Goog-Upload-Offset", "0")
            }
            jsonBody(image)
        }
    }

    suspend fun setThumbnailPlaylist(
        client: YouTubeClient,
        playlistId: String,
        blobId: String,
    ) = run {
        val authHeader = authorizationHeader()

        httpClient.post {
            url("browse/edit_playlist")
            ytClient(client, setLogin = true, authHeader)
            jsonBody(
                EditPlaylistBody(
                    context = client.toContext(locale, visitorData, dataSyncId),
                    playlistId = playlistId,
                    actions = listOf(
                        Action.SetCustomThumbnailAction(
                            addedCustomThumbnail = Action.SetCustomThumbnailAction.AddedCustomThumbnail(
                                playlistScottyEncryptedBlobId = blobId
                            )
                        )
                    )
                )
            )
        }
    }

    suspend fun removeThumbnailPlaylist(
        client: YouTubeClient,
        playlistId: String
    ) = run {
        val authHeader = authorizationHeader()

        httpClient.post {
            url("browse/edit_playlist")
            ytClient(client, setLogin = true, authHeader)
            jsonBody(
                EditPlaylistBody(
                    context = client.toContext(locale, visitorData, dataSyncId),
                    playlistId = playlistId,
                    actions = listOf(
                        Action.RemoveCustomThumbnailAction()
                    )
                )
            )
        }
    }

    suspend fun deletePlaylist(
        client: YouTubeClient,
        playlistId: String,
    ) = run {
        val authHeader = authorizationHeader()
        httpClient.post {
            url("playlist/delete")
            println("deleting $playlistId")
            ytClient(client, setLogin = true, authHeader)
            jsonBody(
                PlaylistDeleteBody(
                    context = client.toContext(locale, visitorData, dataSyncId),
                    playlistId = playlistId
                )
            )
        }
    }

    private suspend fun returnYouTubeDislike(videoId: String) = run {
        httpClient.get {
            url("https://returnyoutubedislikeapi.com/Votes?videoId=$videoId")
            contentType("application/json")
        }
    }


    /**
     * Initialize a song upload to YouTube Music.
     * Returns the upload URL in the X-Goog-Upload-URL header.
     */
    suspend fun initSongUpload(
        filename: String,
        contentLength: Long
    ): SpotrHttpResponse {
        val authUser = "0"
        val authHeader = authorizationHeader()
        return httpClient.post {
            url("https://upload.youtube.com/upload/usermusic/http?authuser=$authUser")
            headers {
                append("X-Goog-Upload-Command", "start")
                append("X-Goog-Upload-Protocol", "resumable")
                append("X-Goog-Upload-Header-Content-Length", contentLength.toString())
                append("X-Goog-AuthUser", authUser)
                append("Origin", YouTubeClient.ORIGIN_YOUTUBE_MUSIC)
                cookie?.let { cookie ->
                    append("cookie", cookie)
                    authHeader?.let {
                        append("Authorization", it)
                    }
                }
                contentType("application/x-www-form-urlencoded")
                jsonBody("filename=$filename")
            }
        }
    }

    /**
     * Upload song data to the provided upload URL.
     */
    suspend fun uploadSongData(
        uploadUrl: String,
        data: ByteArray,
        onProgress: ((Float) -> Unit)? = null
    ): SpotrHttpResponse {
        val authHeader = authorizationHeader()
        return httpClient.post {
            url(uploadUrl)
            headers {
                append("X-Goog-Upload-Command", "upload, finalize")
                append("X-Goog-Upload-Offset", "0")
                append("X-Goog-AuthUser", "0")
                append("Origin", YouTubeClient.ORIGIN_YOUTUBE_MUSIC)
                cookie?.let { cookie ->
                    append("cookie", cookie)
                    authHeader?.let {
                        append("Authorization", it)
                    }
                }
                contentType("application/octet-stream")
                jsonBody(data)
//            TODO: Implement proper resumable upload with progress tracking.
//            onUpload { bytesSentTotal, contentLength ->
//                contentLength?.let {
//                    onProgress?.invoke(bytesSentTotal.toFloat() / it.toFloat())
//                }
//            }
            }
        }
    }

    /**
     * Delete a privately owned (uploaded) song from YouTube Music.
     */
    suspend fun deletePrivatelyOwnedEntity(entityId: String) {
        val authHeader = authorizationHeader()
        val context = YouTubeClient.WEB_REMIX.toContext(locale, visitorData, null)
        val requestBody =
            """{"context":${Json.encodeToString(context)},"entityId":"$entityId"}"""
        httpClient.post {
            url("https://music.youtube.com/youtubei/v1/music/delete_privately_owned_entity")
            contentType("application/json")
            headers {
                append("Referer", YouTubeClient.REFERER_YOUTUBE_MUSIC)
                append("Origin", YouTubeClient.ORIGIN_YOUTUBE_MUSIC)
                cookie?.let { cookie ->
                    append("cookie", cookie)
                    authHeader?.let {
                        append("Authorization", it)
                    }
                }
            }
            parameter("key", "AIzaSyC9XL3ZjWddXya6X74dJoCTL-WEYFDNX3")
            parameter("prettyPrint", "false")
            jsonBody(requestBody)
        }
    }

    suspend fun getMediaInfo(videoId: String): Result<MediaInfo> =
        runCatching {
            val response = next(
                client = YouTubeClient.WEB,
                videoId,
                null,
                null,
                null,
                null,
                null
            ).body<NextResponse>()

            val baseForInfo =
                response.contents.twoColumnWatchNextResults
                    ?.results
                    ?.results
                    ?.content
                    ?.find {
                        it?.videoSecondaryInfoRenderer != null
                    }?.videoSecondaryInfoRenderer

            val baseForTitle =
                response.contents.twoColumnWatchNextResults
                    ?.results
                    ?.results
                    ?.content
                    ?.find {
                        it?.videoPrimaryInfoRenderer != null
                    }?.videoPrimaryInfoRenderer

            val returnYouTubeDislikeResponse =
                returnYouTubeDislike(videoId).body<ReturnYouTubeDislikeResponse>()

            return@runCatching MediaInfo(
                videoId = videoId,
                title = baseForTitle
                    ?.title
                    ?.runs
                    ?.firstOrNull()
                    ?.text,
                author = baseForInfo
                    ?.owner
                    ?.videoOwnerRenderer
                    ?.title
                    ?.runs
                    ?.firstOrNull()
                    ?.text,
                authorId =
                    baseForInfo
                        ?.owner
                        ?.videoOwnerRenderer
                        ?.navigationEndpoint
                        ?.browseEndpoint
                        ?.browseId,
                authorThumbnail =
                    baseForInfo
                        ?.owner
                        ?.videoOwnerRenderer
                        ?.thumbnail
                        ?.thumbnails
                        ?.find {
                            it.height == 48
                        }?.url
                        ?.replace("s48", "s960"),
                description = baseForInfo?.attributedDescription?.content,
                subscribers =
                    baseForInfo
                        ?.owner
                        ?.videoOwnerRenderer
                        ?.subscriberCountText
                        ?.simpleText?.split(" ")?.firstOrNull(),
                uploadDate = baseForTitle?.dateText?.simpleText,
                viewCount = returnYouTubeDislikeResponse.viewCount,
                like = returnYouTubeDislikeResponse.likes,
                dislike = returnYouTubeDislikeResponse.dislikes,
            )

        }
}
