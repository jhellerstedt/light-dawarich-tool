package com.jhellerstedt.dawarich.api

import com.jhellerstedt.dawarich.api.models.CreatePlaceBody
import com.jhellerstedt.dawarich.api.models.CreatePlaceRequest
import com.jhellerstedt.dawarich.api.models.DawarichPlace
import com.jhellerstedt.dawarich.api.models.TimelineBatchRequest
import com.jhellerstedt.dawarich.api.models.TimelineLocationFeature
import com.jhellerstedt.dawarich.api.models.TimelinePointGeometry
import com.jhellerstedt.dawarich.api.models.TimelinePointProperties
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import java.io.IOException

class DawarichClient(
    val baseUrl: String,
    private val apiKey: String,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val client = HttpClient(OkHttp) {
        install(HttpTimeout)
        install(ContentNegotiation) {
            json(json)
        }
    }

    suspend fun listPlaces(
        tagIds: List<Int> = emptyList(),
        filter: String? = null,
        maskPrivacyZones: Boolean = false,
    ): List<DawarichPlace> {
        val body = if (tagIds.isEmpty()) {
            val params = linkedMapOf(
                "api_key" to apiKey,
                "mask_privacy_zones" to maskPrivacyZones.toString(),
            )
            if (filter != null) {
                params["filter"] = filter
            }
            get("places", params)
        } else {
            // Rails Array(params[:tag_ids]) accepts repeated tag_ids[] values.
            getWithRepeatedTagIds(filter, maskPrivacyZones, tagIds)
        }
        return json.decodeFromString(body)
    }

    suspend fun createPlace(request: CreatePlaceRequest): DawarichPlace {
        // PlacesController wraps params under `place`.
        val body = postJson("places", CreatePlaceBody(place = request))
        return json.decodeFromString(body)
    }

    /**
     * Upload timeline points via Dawarich's batch points API
     * (`POST /api/v1/points` with Overland-style Feature locations).
     */
    suspend fun uploadTimelinePoints(
        points: List<TimelinePointInput>,
    ): String {
        require(points.isNotEmpty()) { "At least one point is required" }
        val payload = TimelineBatchRequest(
            locations = points.map { point ->
                TimelineLocationFeature(
                    geometry = TimelinePointGeometry(
                        coordinates = listOf(point.longitude, point.latitude),
                    ),
                    properties = TimelinePointProperties(
                        timestamp = point.timestampIso,
                        horizontalAccuracy = point.accuracyMeters,
                        deviceId = point.deviceId,
                    ),
                )
            },
        )
        return postJson("points", payload)
    }

    private suspend fun getWithRepeatedTagIds(
        filter: String?,
        maskPrivacyZones: Boolean,
        tagIds: List<Int>,
    ): String {
        val parts = mutableListOf(
            "api_key=${encode(apiKey)}",
            "mask_privacy_zones=$maskPrivacyZones",
        )
        if (filter != null) {
            parts += "filter=${encode(filter)}"
        }
        tagIds.forEach { id ->
            parts += "tag_ids[]=${encode(id.toString())}"
        }
        val response = networkRequest {
            client.get("$apiBase/places?${parts.joinToString("&")}") {
                header(HttpHeaders.UserAgent, USER_AGENT)
            }
        }
        return parseResponse(response.status.value, responseBody(response))
    }

    private suspend fun get(path: String, params: Map<String, String>): String {
        val query = params.entries.joinToString("&") { (key, value) ->
            "${encode(key)}=${encode(value)}"
        }
        val response = networkRequest {
            client.get("$apiBase/$path?$query") {
                header(HttpHeaders.UserAgent, USER_AGENT)
            }
        }
        return parseResponse(response.status.value, responseBody(response))
    }

    private suspend inline fun <reified T> postJson(path: String, body: T): String {
        val response = networkRequest {
            client.post("$apiBase/$path?api_key=${encode(apiKey)}") {
                header(HttpHeaders.UserAgent, USER_AGENT)
                contentType(ContentType.Application.Json)
                setBody(body)
            }
        }
        return parseResponse(response.status.value, responseBody(response))
    }

    private suspend fun <T> networkRequest(block: suspend () -> T): T =
        try {
            block()
        } catch (error: CancellationException) {
            throw error
        } catch (error: HttpRequestTimeoutException) {
            throw DawarichApiException.Network(error.message ?: "HTTP request timed out", error)
        } catch (error: IOException) {
            throw DawarichApiException.Network(error.message ?: "Network request failed", error)
        }

    private suspend fun responseBody(response: HttpResponse): String =
        networkRequest { response.bodyAsText() }

    internal fun parseResponse(statusCode: Int, body: String): String {
        if (statusCode !in 200..299) {
            val snippet = body.trim().take(200).ifBlank { "empty body" }
            throw DawarichApiException.ApiError(statusCode, snippet)
        }
        return body
    }

    fun close() {
        client.close()
    }

    private val apiBase: String
        get() = "${baseUrl.trimEnd('/')}/api/v1"

    companion object {
        private const val USER_AGENT = "DawarichLightOS/0.1"

        fun normalizeBaseUrl(input: String): String {
            var url = input.trim()
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://$url"
            }
            return url.trimEnd('/')
        }

        fun baseUrlSchemeError(input: String): String? {
            val trimmed = input.trim()
            if (trimmed.isEmpty()) return "Server URL is required"
            return null
        }

        private fun encode(value: String): String =
            java.net.URLEncoder.encode(value, Charsets.UTF_8.name())
    }
}

data class TimelinePointInput(
    val latitude: Double,
    val longitude: Double,
    val timestampIso: String,
    val accuracyMeters: Double? = null,
    val deviceId: String = "light-phone",
)
