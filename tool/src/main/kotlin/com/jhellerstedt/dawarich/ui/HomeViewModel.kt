package com.jhellerstedt.dawarich.ui

import androidx.lifecycle.viewModelScope
import com.jhellerstedt.dawarich.api.DawarichApiException
import com.jhellerstedt.dawarich.api.TimelinePointInput
import com.jhellerstedt.dawarich.data.DawarichConnection
import com.jhellerstedt.dawarich.data.DawarichSettings
import com.jhellerstedt.dawarich.location.Fix
import com.jhellerstedt.dawarich.location.FixSource
import com.jhellerstedt.dawarich.location.LocationHelper
import com.thelightphone.sdk.LightViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.format.DateTimeFormatter

data class HomeUiState(
    val configured: Boolean = false,
    val baseUrl: String? = null,
    val fix: Fix? = null,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
    val busy: Boolean = false,
)

class HomeViewModel(
    private val settings: DawarichSettings,
) : LightViewModel<Unit>() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            refreshConnection()
        }
    }

    fun refreshConnection() {
        viewModelScope.launch {
            val connection = settings.getConnection()
            _uiState.update {
                it.copy(
                    configured = connection != null,
                    baseUrl = connection?.baseUrl,
                )
            }
        }
    }

    fun onFix(fix: Fix?) {
        _uiState.update { it.copy(fix = fix) }
    }

    fun clearMessages() {
        _uiState.update { it.copy(statusMessage = null, errorMessage = null) }
    }

    fun uploadCurrentPoint() {
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
            _uiState.update { it.copy(busy = true, errorMessage = null, statusMessage = null) }
            try {
                uploadFix(connection, fix)
                _uiState.update {
                    it.copy(
                        busy = false,
                        statusMessage = "Uploaded point (${fix.source.name.lowercase()} fix)",
                    )
                }
            } catch (error: DawarichApiException) {
                _uiState.update {
                    it.copy(busy = false, errorMessage = error.message ?: "Upload failed")
                }
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(busy = false, errorMessage = error.message ?: "Upload failed")
                }
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

    fun describeFix(fix: Fix?): String {
        if (fix == null) return "No fix yet"
        val source = when (fix.source) {
            FixSource.Current -> "GPS"
            FixSource.Default -> "default"
        }
        val accuracy = fix.accuracyMeters?.let { " ±${it.toInt()}m" }.orEmpty()
        return String.format("%.5f, %.5f (%s%s)", fix.latitude, fix.longitude, source, accuracy)
    }
}
