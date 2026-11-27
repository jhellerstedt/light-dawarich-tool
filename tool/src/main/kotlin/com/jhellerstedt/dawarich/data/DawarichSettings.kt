package com.jhellerstedt.dawarich.data

import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.jhellerstedt.dawarich.api.DawarichClient
import com.jhellerstedt.dawarich.api.models.DawarichPlace
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class DawarichConnection(
    val baseUrl: String,
    val apiKey: String,
    val starTagId: Int? = null,
)

class DawarichSettings(
    private val dataStore: DataStore<Preferences>,
    private val cipher: CredentialCipher = CredentialCipher(),
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    val baseUrl: Flow<String?> =
        dataStore.data.map { prefs -> prefs[BASE_URL] }

    val starTagId: Flow<Int?> =
        dataStore.data.map { prefs -> prefs[STAR_TAG_ID] }

    val cachedPlacesJson: Flow<String?> =
        dataStore.data.map { prefs -> prefs[PLACES_CACHE] }

    suspend fun getConnection(): DawarichConnection? {
        val prefs = dataStore.data.first()
        val url = prefs[BASE_URL] ?: return null
        val encrypted = prefs[ENCRYPTED_API_KEY] ?: return null
        val apiKey = runCatching {
            cipher.decrypt(Base64.decode(encrypted, Base64.DEFAULT))
        }.getOrNull() ?: return null
        if (apiKey.isBlank()) return null
        return DawarichConnection(
            baseUrl = url,
            apiKey = apiKey,
            starTagId = prefs[STAR_TAG_ID],
        )
    }

    suspend fun saveBaseUrl(rawUrl: String) {
        val normalized = DawarichClient.normalizeBaseUrl(rawUrl)
        dataStore.edit { prefs ->
            prefs[BASE_URL] = normalized
        }
    }

    suspend fun saveApiKey(apiKey: String) {
        val encrypted = Base64.encodeToString(cipher.encrypt(apiKey.trim()), Base64.DEFAULT)
        dataStore.edit { prefs ->
            prefs[ENCRYPTED_API_KEY] = encrypted
        }
    }

    suspend fun saveStarTagId(tagId: Int?) {
        dataStore.edit { prefs ->
            if (tagId == null) {
                prefs.remove(STAR_TAG_ID)
            } else {
                prefs[STAR_TAG_ID] = tagId
            }
        }
    }

    suspend fun clearCredentials() {
        dataStore.edit { prefs ->
            prefs.remove(BASE_URL)
            prefs.remove(ENCRYPTED_API_KEY)
            prefs.remove(STAR_TAG_ID)
            prefs.remove(PLACES_CACHE)
        }
    }

    suspend fun cachePlaces(places: List<DawarichPlace>) {
        dataStore.edit { prefs ->
            prefs[PLACES_CACHE] = json.encodeToString(places)
        }
    }

    suspend fun loadCachedPlaces(): List<DawarichPlace> {
        val raw = dataStore.data.first()[PLACES_CACHE] ?: return emptyList()
        return runCatching { json.decodeFromString<List<DawarichPlace>>(raw) }
            .getOrDefault(emptyList())
    }

    fun openClient(connection: DawarichConnection): DawarichClient =
        DawarichClient(connection.baseUrl, connection.apiKey)

    companion object {
        private val BASE_URL = stringPreferencesKey("dawarich_base_url")
        private val ENCRYPTED_API_KEY = stringPreferencesKey("dawarich_encrypted_api_key")
        private val STAR_TAG_ID = intPreferencesKey("dawarich_star_tag_id")
        private val PLACES_CACHE = stringPreferencesKey("dawarich_places_cache")
    }
}
