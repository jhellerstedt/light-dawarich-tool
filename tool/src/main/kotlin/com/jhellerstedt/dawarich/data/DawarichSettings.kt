package com.jhellerstedt.dawarich.data

import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.jhellerstedt.dawarich.api.DawarichClient
import com.jhellerstedt.dawarich.api.models.DawarichPlace
import com.jhellerstedt.dawarich.api.models.DawarichTag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class DawarichConnection(
    val baseUrl: String,
    val apiKey: String,
    val starTagId: Int? = null,
    val autoUploadEnabled: Boolean = true,
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

    /** Default on: auto-upload a timeline point every 5 minutes while the tool is open. */
    val autoUploadEnabled: Flow<Boolean> =
        dataStore.data.map { prefs -> prefs[AUTO_UPLOAD_ENABLED] ?: true }

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
            autoUploadEnabled = prefs[AUTO_UPLOAD_ENABLED] ?: true,
        )
    }

    suspend fun isAutoUploadEnabled(): Boolean =
        dataStore.data.first()[AUTO_UPLOAD_ENABLED] ?: true

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

    /** Persist URL + API key from a scanned Dawarich Account QR (or manual entry). */
    suspend fun saveConnection(baseUrl: String, apiKey: String) {
        val normalized = DawarichClient.normalizeBaseUrl(baseUrl)
        val encrypted = Base64.encodeToString(cipher.encrypt(apiKey.trim()), Base64.DEFAULT)
        dataStore.edit { prefs ->
            prefs[BASE_URL] = normalized
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

    suspend fun saveAutoUploadEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[AUTO_UPLOAD_ENABLED] = enabled
        }
    }

    suspend fun clearCredentials() {
        dataStore.edit { prefs ->
            prefs.remove(BASE_URL)
            prefs.remove(ENCRYPTED_API_KEY)
            prefs.remove(STAR_TAG_ID)
            prefs.remove(PLACES_CACHE)
            prefs.remove(TAGS_CACHE)
            // Keep auto-upload preference across credential clears.
        }
    }

    suspend fun cachePlaces(places: List<DawarichPlace>) {
        dataStore.edit { prefs ->
            prefs[PLACES_CACHE] = json.encodeToString(places)
            prefs[TAGS_CACHE] = json.encodeToString(places.flatMap { it.tags }.distinctBy { it.id })
        }
    }

    suspend fun cacheTags(tags: List<DawarichTag>) {
        dataStore.edit { prefs ->
            prefs[TAGS_CACHE] = json.encodeToString(tags.distinctBy { it.id })
        }
    }

    suspend fun loadCachedPlaces(): List<DawarichPlace> {
        val raw = dataStore.data.first()[PLACES_CACHE] ?: return emptyList()
        return runCatching { json.decodeFromString<List<DawarichPlace>>(raw) }
            .getOrDefault(emptyList())
    }

    suspend fun loadCachedTags(): List<DawarichTag> {
        val raw = dataStore.data.first()[TAGS_CACHE]
        if (raw != null) {
            return runCatching { json.decodeFromString<List<DawarichTag>>(raw) }
                .getOrDefault(emptyList())
        }
        return loadCachedPlaces().flatMap { it.tags }.distinctBy { it.id }
    }

    /** Up to two most recent place-filter tag names (newest first). */
    suspend fun loadRecentFilterTags(): List<String> {
        val raw = dataStore.data.first()[RECENT_FILTER_TAGS] ?: return emptyList()
        return runCatching { json.decodeFromString<List<String>>(raw) }
            .getOrDefault(emptyList())
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .take(2)
    }

    suspend fun saveRecentFilterTags(names: List<String>) {
        val trimmed = names.map { it.trim() }.filter { it.isNotEmpty() }.take(2)
        dataStore.edit { prefs ->
            if (trimmed.isEmpty()) {
                prefs.remove(RECENT_FILTER_TAGS)
            } else {
                prefs[RECENT_FILTER_TAGS] = json.encodeToString(trimmed)
            }
        }
    }

    fun openClient(connection: DawarichConnection): DawarichClient =
        DawarichClient(connection.baseUrl, connection.apiKey)

    companion object {
        private val BASE_URL = stringPreferencesKey("dawarich_base_url")
        private val ENCRYPTED_API_KEY = stringPreferencesKey("dawarich_encrypted_api_key")
        private val STAR_TAG_ID = intPreferencesKey("dawarich_star_tag_id")
        private val AUTO_UPLOAD_ENABLED = booleanPreferencesKey("dawarich_auto_upload_enabled")
        private val PLACES_CACHE = stringPreferencesKey("dawarich_places_cache")
        private val TAGS_CACHE = stringPreferencesKey("dawarich_tags_cache")
        private val RECENT_FILTER_TAGS = stringPreferencesKey("dawarich_recent_filter_tags")

        const val AUTO_UPLOAD_INTERVAL_MS = 5L * 60L * 1000L
    }
}
