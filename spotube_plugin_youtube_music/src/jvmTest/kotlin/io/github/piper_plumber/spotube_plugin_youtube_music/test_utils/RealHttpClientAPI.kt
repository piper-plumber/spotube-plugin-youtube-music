package io.github.piper_plumber.spotube_plugin_youtube_music.test_utils

import org.spotube.plugin_interfaces.host_apis.HttpClientAPI
import org.spotube.plugin_interfaces.host_apis.HttpMethod
import org.spotube.plugin_interfaces.host_apis.HttpResponse
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import io.ktor.http.takeFrom

class RealHttpClientAPI : HttpClientAPI {
    val httpClient = HttpClient(OkHttp)

    override suspend fun request(
        method: HttpMethod,
        url: String,
        requestHeaders: Map<String, String>?,
        body: String?
    ): HttpResponse {
        val res = httpClient.request {
            this.method = when (method) {
                HttpMethod.Get -> io.ktor.http.HttpMethod.Get
                HttpMethod.Post -> io.ktor.http.HttpMethod.Post
                HttpMethod.Put -> io.ktor.http.HttpMethod.Put
                HttpMethod.Delete -> io.ktor.http.HttpMethod.Delete
                HttpMethod.Patch -> io.ktor.http.HttpMethod.Patch
                HttpMethod.Head -> io.ktor.http.HttpMethod.Head
                HttpMethod.Options -> io.ktor.http.HttpMethod.Options
            }
            this.url {
                takeFrom(url)
            }
            requestHeaders?.forEach { (key, value) ->
                headers.append(key, value)
            }
            body?.let { setBody(it) }
        }

        return HttpResponse(
            statusCode = res.status.value,
            headers = res.headers.entries().associate { it.key to it.value.joinToString(",") },
            body = res.bodyAsText()
        )
    }
}