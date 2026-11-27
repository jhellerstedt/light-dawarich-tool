package com.jhellerstedt.dawarich.ui

import androidx.lifecycle.viewModelScope
import com.jhellerstedt.dawarich.api.DawarichClient
import com.jhellerstedt.dawarich.data.DawarichSettings
import com.thelightphone.sdk.LightViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val baseUrl: String = "",
    val hasApiKey: Boolean = false,
    val starTagId: String = "",
    val statusMessage: String? = null,
    val errorMessage: String? = null,
)

class SettingsViewModel(
    private val settings: DawarichSettings,
) : LightViewModel<Unit>() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { reload() }
    }

    suspend fun reload() {
        val connection = settings.getConnection()
        _uiState.update {
            it.copy(
                baseUrl = connection?.baseUrl.orEmpty(),
                hasApiKey = connection != null,
                starTagId = connection?.starTagId?.toString().orEmpty(),
            )
        }
    }

    fun saveBaseUrl(raw: String) {
        viewModelScope.launch {
            val error = DawarichClient.baseUrlSchemeError(raw)
            if (error != null) {
                _uiState.update { it.copy(errorMessage = error, statusMessage = null) }
                return@launch
            }
            settings.saveBaseUrl(raw)
            _uiState.update {
                it.copy(
                    baseUrl = DawarichClient.normalizeBaseUrl(raw),
                    statusMessage = "Saved server URL",
                    errorMessage = null,
                )
            }
        }
    }

    fun saveApiKey(raw: String) {
        viewModelScope.launch {
            if (raw.isBlank()) {
                _uiState.update { it.copy(errorMessage = "API key is required", statusMessage = null) }
                return@launch
            }
            settings.saveApiKey(raw)
            _uiState.update {
                it.copy(hasApiKey = true, statusMessage = "Saved API key", errorMessage = null)
            }
        }
    }

    fun saveStarTagId(raw: String) {
        viewModelScope.launch {
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) {
                settings.saveStarTagId(null)
                _uiState.update {
                    it.copy(starTagId = "", statusMessage = "Cleared star tag filter", errorMessage = null)
                }
                return@launch
            }
            val id = trimmed.toIntOrNull()
            if (id == null) {
                _uiState.update { it.copy(errorMessage = "Star tag id must be a number", statusMessage = null) }
                return@launch
            }
            settings.saveStarTagId(id)
            _uiState.update {
                it.copy(starTagId = id.toString(), statusMessage = "Saved star tag id", errorMessage = null)
            }
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            settings.clearCredentials()
            _uiState.update {
                SettingsUiState(statusMessage = "Cleared Dawarich settings")
            }
        }
    }
}
