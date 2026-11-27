package com.jhellerstedt.dawarich.ui

import androidx.lifecycle.viewModelScope
import com.jhellerstedt.dawarich.api.DawarichApiException
import com.jhellerstedt.dawarich.api.models.CreatePlaceRequest
import com.jhellerstedt.dawarich.api.models.DawarichPlace
import com.jhellerstedt.dawarich.data.DawarichSettings
import com.jhellerstedt.dawarich.location.Fix
import com.jhellerstedt.dawarich.location.LocationHelper
import com.jhellerstedt.dawarich.util.Geo
import com.jhellerstedt.dawarich.util.PlaceDistance
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
    val filterTaggedOnly: Boolean = false,
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

    init {
        viewModelScope.launch {
            allPlaces = settings.loadCachedPlaces()
            if (allPlaces.isNotEmpty()) {
                _uiState.update { it.copy(usingCache = true) }
                recompute()
            }
        }
    }

    fun onFix(fix: Fix?) {
        _uiState.update { it.copy(fix = fix) }
        recompute()
    }

    fun toggleTaggedFilter() {
        _uiState.update { it.copy(filterTaggedOnly = !it.filterTaggedOnly) }
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
                val filter = if (_uiState.value.filterTaggedOnly) "tagged" else null
                val starTag = connection.starTagId
                val places = client.listPlaces(
                    tagIds = listOfNotNull(starTag),
                    filter = filter,
                    maskPrivacyZones = false,
                )
                allPlaces = places
                settings.cachePlaces(places)
                _uiState.update {
                    it.copy(
                        busy = false,
                        usingCache = false,
                        statusMessage = "Synced ${places.size} place(s)",
                    )
                }
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
                settings.cachePlaces(allPlaces)
                _uiState.update {
                    it.copy(busy = false, statusMessage = "Saved \"$placeName\"")
                }
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

    private fun recompute() {
        val fix = _uiState.value.fix
        val origin = fix?.toLatLng()
        val taggedOnly = _uiState.value.filterTaggedOnly
        val candidates = if (taggedOnly) {
            allPlaces.filter { it.tags.isNotEmpty() }
        } else {
            allPlaces
        }
        val ranked = if (origin == null) {
            candidates
                .sortedBy { it.name.lowercase() }
                .map { PlaceDistance(it, Double.POSITIVE_INFINITY) }
        } else {
            Geo.nearestPlaces(origin, candidates)
        }
        _uiState.update { it.copy(places = ranked) }
    }
}
