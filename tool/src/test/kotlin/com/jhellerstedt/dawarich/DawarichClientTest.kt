package com.jhellerstedt.dawarich

import com.jhellerstedt.dawarich.api.DawarichApiException
import com.jhellerstedt.dawarich.api.DawarichClient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class DawarichClientTest {
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
}
