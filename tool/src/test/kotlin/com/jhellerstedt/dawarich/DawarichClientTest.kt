package com.jhellerstedt.dawarich

import com.jhellerstedt.dawarich.api.DawarichApiException
import com.jhellerstedt.dawarich.api.DawarichClient
import com.jhellerstedt.dawarich.api.models.NearbyPlacesResponse
import com.jhellerstedt.dawarich.util.AddressFormat
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class DawarichClientTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun normalizeBaseUrlAddsHttpsAndStripsSlash() {
        assertEquals("https://dawarich.example", DawarichClient.normalizeBaseUrl("dawarich.example/"))
        assertEquals("http://192.168.1.10:3000", DawarichClient.normalizeBaseUrl("http://192.168.1.10:3000/"))
        assertEquals("https://host", DawarichClient.normalizeBaseUrl("https://host"))
    }

    @Test
    fun baseUrlSchemeErrorOnlyForBlank() {
        assertEquals("Server URL is required", DawarichClient.baseUrlSchemeError("  "))
        assertNull(DawarichClient.baseUrlSchemeError("http://lan.local"))
        assertNull(DawarichClient.baseUrlSchemeError("https://secure.example"))
    }

    @Test
    fun parseResponseRejectsNonSuccess() {
        val client = DawarichClient("https://example.invalid", "key")
        assertFailsWith<DawarichApiException.ApiError> {
            client.parseResponse(401, """{"error":"unauthorized"}""")
        }
        assertEquals("{}", client.parseResponse(200, "{}"))
        client.close()
    }

    @Test
    fun nearbyPlacesResponseParsesPhotonShape() {
        val body = """
            {"places":[{"name":"Collins St","latitude":-37.81,"longitude":144.96,
              "street":"Collins St","housenumber":"120","city":"Melbourne","source":"photon"}]}
        """.trimIndent()
        val parsed = json.decodeFromString<NearbyPlacesResponse>(body)
        assertEquals(1, parsed.places.size)
        assertEquals(
            "120 Collins St, Melbourne",
            AddressFormat.fromNearby(parsed.places.first()),
        )
    }
}
