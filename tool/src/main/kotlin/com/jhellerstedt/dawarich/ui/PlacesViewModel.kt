package com.jhellerstedt.dawarich.ui

import androidx.lifecycle.viewModelScope
import com.jhellerstedt.dawarich.api.DawarichApiException
import com.jhellerstedt.dawarich.api.models.CreatePlaceRequest
import com.jhellerstedt.dawarich.api.models.DawarichPlace
import com.jhellerstedt.dawarich.api.models.DawarichTag
import com.jhellerstedt.dawarich.data.DawarichSettings
import com.jhellerstedt.dawarich.location.Fix
import com.jhellerstedt.dawarich.location.LocationHelper
import com.jhellerstedt.dawarich.util.Geo
import com.jhellerstedt.dawarich.util.PlaceDistance
import com.jhellerstedt.dawarich.util.TagSuggest
import com.thelightphone.sdk.LightViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlacesUiState(
    val places: List<PlaceDistance> = emptyList(),
    val fix: Fix? = null,
    val usingCache: Boolean = false,
    val filterTagQuery: String = "",
    val recentFilterTags: List<String> = emptyList(),
    val tagSuggestions: List<String> = emptyList(),
    val busy: Boolean = false,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
)

class PlacesViewModel(
    private val settings: DawarichSettings,
) : LightViewModel<Unit>() {
    private val _uiState = MutableStateFlow(PlacesUiState())
    val uiState: StateFlow<PlacesUiState> = _uiState.asStateFlow()

    private var allPlaces: List<DawarichPlace> = emptyList()
    private var knownTags: List<DawarichTag> = emptyList()

    init {
        viewModelScope.launch {
            allPlaces = settings.loadCachedPlaces()
            knownTags = settings.loadCachedTags()
            val recent = settings.loadRecentFilterTags()
            _uiState.update {
                it.copy(
                    usingCache = allPlaces.isNotEmpty(),
                    recentFilterTags = recent,
                )
            }
            refreshSuggestions()
            recompute()
            refreshTagsFromNetwork()
        }
    }

    fun onFix(fix: Fix?) {
        _uiState.update { it.copy(fix = fix) }
        recompute()
    }

    fun setFilterTagQuery(query: String) {
        _uiState.update { it.copy(filterTagQuery = query) }
        refreshSuggestions()
        recompute()
        val trimmed = query.trim()
        if (trimmed.isNotEmpty() && TagSuggest.resolveId(knownTags, trimmed) != null) {
            viewModelScope.launch { rememberFilterTag(trimmed) }
        }
    }

    fun selectFilterTag(name: String) {
        _uiState.update { it.copy(filterTagQuery = name) }
        refreshSuggestions()
        recompute()
        viewModelScope.launch { rememberFilterTag(name) }
    }

    fun clearFilterTag() {
        _uiState.update { it.copy(filterTagQuery = "") }
        refreshSuggestions()
        recompute()
    }

    fun syncPlaces() {
        viewModelScope.launch {
            val connection = settings.getConnection()
            if (connection == null) {
                _uiState.update { it.copy(errorMessage = "Set base URL and API key in Settings") }
                return@launch
            }
            _uiState.update { it.copy(busy = true, errorMessage = null, statusMessage = null) }
            val client = settings.openClient(connection)
            try {
                val starTag = connection.starTagId
                val places = client.listPlaces(
                    tagIds = listOfNotNull(starTag),
                    filter = null,
                    maskPrivacyZones = false,
                )
                allPlaces = places
                knownTags = TagSuggest.uniqueSorted(places.flatMap { it.tags })
                settings.cachePlaces(places)
                _uiState.update {
                    it.copy(
                        busy = false,
                        usingCache = false,
                        statusMessage = "Synced ${places.size} place(s)",
                    )
                }
                refreshSuggestions()
                recompute()
            } catch (error: DawarichApiException) {
                _uiState.update {
                    it.copy(busy = false, errorMessage = error.message ?: "Sync failed")
                }
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(busy = false, errorMessage = error.message ?: "Sync failed")
                }
            } finally {
                client.close()
            }
        }
    }

    fun starHere(name: String) {
        viewModelScope.launch {
            val connection = settings.getConnection()
            if (connection == null) {
                _uiState.update { it.copy(errorMessage = "Set base URL and API key in Settings") }
                return@launch
            }
            val fix = _uiState.value.fix ?: LocationHelper.resolveFix()
            if (fix == null) {
                _uiState.update {
                    it.copy(errorMessage = "No location fix. Enable location or set a default.")
                }
                return@launch
            }
            val placeName = name.trim().ifBlank { "Starred place" }
            _uiState.update { it.copy(busy = true, errorMessage = null, statusMessage = null) }
            val client = settings.openClient(connection)
            try {
                val created = client.createPlace(
                    CreatePlaceRequest(
                        name = placeName,
                        latitude = fix.latitude,
                        longitude = fix.longitude,
                        tagIds = listOfNotNull(connection.starTagId),
                    ),
                )
                allPlaces = allPlaces + created
                knownTags = TagSuggest.uniqueSorted(knownTags + created.tags)
                settings.cachePlaces(allPlaces)
                _uiState.update {
                    it.copy(busy = false, statusMessage = "Saved \"$placeName\"")
                }
                refreshSuggestions()
                recompute()
            } catch (error: DawarichApiException) {
                _uiState.update {
                    it.copy(busy = false, errorMessage = error.message ?: "Save failed")
                }
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(busy = false, errorMessage = error.message ?: "Save failed")
                }
            } finally {
                client.close()
            }
        }
    }

    private suspend fun refreshTagsFromNetwork() {
        val connection = settings.getConnection() ?: return
        val client = settings.openClient(connection)
        try {
            val tags = client.listTagsFromPlaces()
            knownTags = tags
            settings.cacheTags(tags)
            refreshSuggestions()
            recompute()
        } catch (_: Exception) {
            // Offline / unreachable — keep cached tags.
        } finally {
            client.close()
        }
    }

    private suspend fun rememberFilterTag(name: String) {
        val canonical = TagSuggest.uniqueSorted(knownTags)
            .firstOrNull { it.name.equals(name.trim(), ignoreCase = true) }
            ?.name
            ?: return
        val updated = TagSuggest.rememberRecent(canonical, _uiState.value.recentFilterTags)
        settings.saveRecentFilterTags(updated)
        _uiState.update { it.copy(recentFilterTags = updated) }
        refreshSuggestions()
    }

    private fun refreshSuggestions() {
        val state = _uiState.value
        _uiState.update {
            it.copy(
                tagSuggestions = TagSuggest.filterSuggestions(
                    tags = knownTags,
                    query = state.filterTagQuery,
                    recent = state.recentFilterTags,
                    limit = 8,
                ),
            )
        }
    }

    private fun recompute() {
        val fix = _uiState.value.fix
        val origin = fix?.toLatLng()
        val query = _uiState.value.filterTagQuery.trim()
        val tagId = TagSuggest.resolveId(knownTags, query)
        val tagIds = if (query.isEmpty()) {
            emptySet()
        } else if (tagId != null) {
            setOf(tagId)
        } else {
            // Typed text that matches no known tag → empty result.
            setOf(-1)
        }
        val ranked = if (origin == null) {
            val filtered = if (tagIds.isEmpty()) {
                allPlaces
            } else {
                allPlaces.filter { place -> place.tags.any { it.id in tagIds } }
            }
            filtered
                .sortedBy { it.name.lowercase() }
                .map { PlaceDistance(it, Double.POSITIVE_INFINITY) }
        } else {
            Geo.nearestPlaces(origin, allPlaces, tagIds = tagIds)
        }
        _uiState.update { it.copy(places = ranked) }
    }
}
