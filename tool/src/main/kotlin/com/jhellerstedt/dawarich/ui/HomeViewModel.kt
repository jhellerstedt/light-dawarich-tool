package com.jhellerstedt.dawarich.ui

import androidx.lifecycle.viewModelScope
import com.jhellerstedt.dawarich.api.TimelinePointInput
import com.jhellerstedt.dawarich.api.models.DawarichPlace
import com.jhellerstedt.dawarich.data.DawarichConnection
import com.jhellerstedt.dawarich.data.DawarichSettings
import com.jhellerstedt.dawarich.location.Fix
import com.jhellerstedt.dawarich.location.FixSource
import com.jhellerstedt.dawarich.location.LocationHelper
import com.jhellerstedt.dawarich.util.AddressFormat
import com.jhellerstedt.dawarich.util.Geo
import com.jhellerstedt.dawarich.util.PlaceDistance
import com.thelightphone.sdk.LightViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.format.DateTimeFormatter

data class HomeUiState(
    val configured: Boolean = false,
    val autoUploadEnabled: Boolean = true,
    val fix: Fix? = null,
    val addressLabel: String? = null,
    /** Up to two nearest cached places for the home preview. */
    val nearestPlaces: List<PlaceDistance> = emptyList(),
    val statusMessage: String? = null,
    val errorMessage: String? = null,
    val busy: Boolean = false,
)

class HomeViewModel(
    private val settings: DawarichSettings,
) : LightViewModel<Unit>() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var cachedPlaces: List<DawarichPlace> = emptyList()
    private var geocodeJob: Job? = null
    private var autoUploadJob: Job? = null
    private var lastAutoUploadMs: Long = 0L

    init {
        viewModelScope.launch {
            refreshConnection()
            reloadNearestPreview()
        }
        viewModelScope.launch {
            settings.autoUploadEnabled.collect { enabled ->
                _uiState.update { it.copy(autoUploadEnabled = enabled) }
                restartAutoUploadLoop()
            }
        }
    }

    fun refreshConnection() {
        viewModelScope.launch {
            val connection = settings.getConnection()
            _uiState.update {
                it.copy(
                    configured = connection != null,
                    autoUploadEnabled = connection?.autoUploadEnabled
                        ?: settings.isAutoUploadEnabled(),
                )
            }
            restartAutoUploadLoop()
            reloadNearestPreview()
        }
    }

    fun onFix(fix: Fix?) {
        _uiState.update { it.copy(fix = fix) }
        recomputeNearest()
        if (fix != null) {
            requestReverseGeocode(fix)
        } else {
            _uiState.update { it.copy(addressLabel = null) }
        }
    }

    /** Reload cached places and refresh the two-nearest home preview. */
    fun reloadNearestPreview() {
        viewModelScope.launch {
            cachedPlaces = settings.loadCachedPlaces()
            recomputeNearest()
        }
    }

    private fun recomputeNearest() {
        val origin = _uiState.value.fix?.toLatLng()
        val nearest = if (origin == null) {
            emptyList()
        } else {
            Geo.nearestPlaces(origin, cachedPlaces, limit = HOME_NEAREST_LIMIT)
        }
        _uiState.update { it.copy(nearestPlaces = nearest) }
    }

    fun clearMessages() {
        _uiState.update { it.copy(statusMessage = null, errorMessage = null) }
    }

    /** Quiet timeline upload used by the 5-minute auto-upload loop (no note/tag). */
    fun autoUploadCurrentPoint() {
        viewModelScope.launch {
            val connection = settings.getConnection() ?: return@launch
            if (!settings.isAutoUploadEnabled()) return@launch
            val fix = _uiState.value.fix ?: LocationHelper.resolveFix() ?: return@launch
            try {
                uploadFix(connection, fix)
                lastAutoUploadMs = System.currentTimeMillis()
                _uiState.update {
                    it.copy(statusMessage = "Auto-uploaded point", errorMessage = null)
                }
            } catch (_: Exception) {
                // Keep the loop alive; surface a soft status without blocking the UI.
                _uiState.update { it.copy(statusMessage = "Auto-upload failed; will retry") }
            }
        }
    }

    private fun restartAutoUploadLoop() {
        val enabled = _uiState.value.autoUploadEnabled && _uiState.value.configured
        if (!enabled) {
            autoUploadJob?.cancel()
            autoUploadJob = null
            return
        }
        if (autoUploadJob?.isActive == true) return
        autoUploadJob = viewModelScope.launch {
            while (isActive) {
                delay(DawarichSettings.AUTO_UPLOAD_INTERVAL_MS)
                if (!settings.isAutoUploadEnabled()) break
                if (settings.getConnection() == null) break
                val now = System.currentTimeMillis()
                if (now - lastAutoUploadMs < DawarichSettings.AUTO_UPLOAD_INTERVAL_MS / 2) {
                    continue
                }
                autoUploadCurrentPoint()
            }
        }
    }

    private fun requestReverseGeocode(fix: Fix) {
        geocodeJob?.cancel()
        geocodeJob = viewModelScope.launch {
            val connection = settings.getConnection()
            if (connection == null) {
                _uiState.update { it.copy(addressLabel = null) }
                return@launch
            }
            val client = settings.openClient(connection)
            try {
                val nearby = client.nearbyPlaces(fix.latitude, fix.longitude)
                val label = AddressFormat.fromNearby(nearby.firstOrNull())
                _uiState.update { it.copy(addressLabel = label) }
            } catch (_: Exception) {
                _uiState.update { it.copy(addressLabel = null) }
            } finally {
                client.close()
            }
        }
    }

    private suspend fun uploadFix(connection: DawarichConnection, fix: Fix) {
        val client = settings.openClient(connection)
        try {
            val timestamp = Instant.ofEpochMilli(fix.timestampMs ?: System.currentTimeMillis())
            client.uploadTimelinePoints(
                listOf(
                    TimelinePointInput(
                        latitude = fix.latitude,
                        longitude = fix.longitude,
                        timestampIso = DateTimeFormatter.ISO_INSTANT.format(timestamp),
                        accuracyMeters = fix.accuracyMeters,
                    ),
                ),
            )
        } finally {
            client.close()
        }
    }

    fun describeFix(fix: Fix?, addressLabel: String?): String {
        if (fix == null) return "No fix yet"
        val source = when (fix.source) {
            FixSource.Current -> "GPS"
            FixSource.Default -> "default"
        }
        val accuracy = fix.accuracyMeters?.let { " ±${it.toInt()}m" }.orEmpty()
        val primary = addressLabel?.takeIf { it.isNotBlank() }
            ?: AddressFormat.coordinates(fix.latitude, fix.longitude)
        return "$primary ($source$accuracy)"
    }

    companion object {
        /** Home nested preview shows only this many nearest places. */
        const val HOME_NEAREST_LIMIT = 2
    }
}
