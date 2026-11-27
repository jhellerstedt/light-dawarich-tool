package com.jhellerstedt.dawarich.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Credentials embedded in the QR code on Dawarich's Account → API access page.
 *
 * Dawarich encodes: `{"server_url":"<root_url>","api_key":"<key>"}` (see Freika/dawarich
 * `UserHelper#api_key_qr_code`).
 */
data class DawarichQrCredentials(
    val serverUrl: String,
    val apiKey: String,
)

@Serializable
private data class DawarichQrPayload(
    @SerialName("server_url") val serverUrl: String? = null,
    @SerialName("api_key") val apiKey: String? = null,
)

object DawarichQrLogin {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Parse a scanned Dawarich Account QR payload into normalized credentials.
     */
    fun parse(raw: String): Result<DawarichQrCredentials> {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("QR code was empty"))
        }

        val payload = runCatching {
            json.decodeFromString<DawarichQrPayload>(trimmed)
        }.getOrElse {
            return Result.failure(
                IllegalArgumentException(
                    "QR is not a Dawarich login code. Open Account → API access in Dawarich and scan that QR.",
                ),
            )
        }

        val serverUrl = payload.serverUrl?.trim().orEmpty()
        val apiKey = payload.apiKey?.trim().orEmpty()
        if (serverUrl.isEmpty() || apiKey.isEmpty()) {
            return Result.failure(
                IllegalArgumentException("QR is missing server_url or api_key"),
            )
        }

        DawarichClient.baseUrlSchemeError(serverUrl)?.let { error ->
            return Result.failure(IllegalArgumentException(error))
        }

        return Result.success(
            DawarichQrCredentials(
                serverUrl = DawarichClient.normalizeBaseUrl(serverUrl),
                apiKey = apiKey,
            ),
        )
    }
}
