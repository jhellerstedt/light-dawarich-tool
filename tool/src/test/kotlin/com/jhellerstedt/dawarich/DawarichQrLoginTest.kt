package com.jhellerstedt.dawarich

import com.jhellerstedt.dawarich.api.DawarichQrLogin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DawarichQrLoginTest {
    @Test
    fun parseOfficialAccountQrJson() {
        val raw = """{"server_url":"https://dawarich.example/","api_key":"abc123def"}"""
        val credentials = DawarichQrLogin.parse(raw).getOrThrow()
        assertEquals("https://dawarich.example", credentials.serverUrl)
        assertEquals("abc123def", credentials.apiKey)
    }

    @Test
    fun parseAllowsHttpLanUrl() {
        val raw = """{"server_url":"http://192.168.1.10:3000/","api_key":"lan-key"}"""
        val credentials = DawarichQrLogin.parse(raw).getOrThrow()
        assertEquals("http://192.168.1.10:3000", credentials.serverUrl)
        assertEquals("lan-key", credentials.apiKey)
    }

    @Test
    fun parseIgnoresUnknownFields() {
        val raw =
            """{"server_url":"https://host.example","api_key":"k","extra":true}"""
        val credentials = DawarichQrLogin.parse(raw).getOrThrow()
        assertEquals("https://host.example", credentials.serverUrl)
        assertEquals("k", credentials.apiKey)
    }

    @Test
    fun parseRejectsNonJson() {
        val result = DawarichQrLogin.parse("otpauth://totp/Demo")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Dawarich") == true)
    }

    @Test
    fun parseRejectsMissingFields() {
        assertTrue(DawarichQrLogin.parse("""{"server_url":"https://x"}""").isFailure)
        assertTrue(DawarichQrLogin.parse("""{"api_key":"only"}""").isFailure)
        assertTrue(DawarichQrLogin.parse("").isFailure)
    }
}
