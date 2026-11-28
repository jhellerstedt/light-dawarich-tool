package com.jhellerstedt.dawarich.ui

import androidx.lifecycle.viewModelScope
import com.jhellerstedt.dawarich.api.DawarichApiException
import com.jhellerstedt.dawarich.api.TimelinePointInput
import com.jhellerstedt.dawarich.api.models.CreateNoteRequest
import com.jhellerstedt.dawarich.api.models.CreatePlaceRequest
import com.jhellerstedt.dawarich.api.models.DawarichTag
import com.jhellerstedt.dawarich.data.DawarichSettings
import com.jhellerstedt.dawarich.location.Fix
import com.jhellerstedt.dawarich.location.LocationHelper
import com.jhellerstedt.dawarich.util.AddressFormat
import com.jhellerstedt.dawarich.util.TagSuggest
import com.thelightphone.sdk.LightViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.format.DateTimeFormatter

data class UploadPointUiState(
    val note: String = "",
    val tagQuery: String = "",
    val tagSuggestions: List<String> = emptyList(),
    val locationLabel: String = "Resolving location…",
    val statusMessage: String? = null,
    val errorMessage: String? = null,
    val busy: Boolean = false,
    val uploaded: Boolean = false,
)

class UploadPointViewModel(
    private val settings: DawarichSettings,
) : LightViewModel<Boolean>() {
    private val _uiState = MutableStateFlow(UploadPointUiState())
    val uiState: StateFlow<UploadPointUiState> = _uiState.asStateFlow()

    private var knownTags: List<DawarichTag> = emptyList()
    private var fix: Fix? = null
    private var placeNameHint: String? = null

    init {
        viewModelScope.launch { prepare() }
    }

    private suspend fun prepare() {
        knownTags = settings.loadCachedTags()
        refreshSuggestions()
        fix = LocationHelper.resolveFix()
        val current = fix
        if (current == null) {
            _uiState.update {
                it.copy(
                    locationLabel = "No location fix",
                    errorMessage = "No location fix. Enable location or set a default.",
                )
            }
            return
        }
        _uiState.update {
            it.copy(locationLabel = AddressFormat.coordinates(current.latitude, current.longitude))
        }
        val connection = settings.getConnection() ?: return
        val client = settings.openClient(connection)
        try {
            val tags = client.listTagsFromPlaces()
            knownTags = tags
            settings.cacheTags(tags)
            refreshSuggestions()
            val nearby = runCatching {
                client.nearbyPlaces(current.latitude, current.longitude)
            }.getOrDefault(emptyList())
            val address = AddressFormat.fromNearby(nearby.firstOrNull())
            placeNameHint = address
            if (address != null) {
                _uiState.update { it.copy(locationLabel = address) }
            }
        } catch (error: DawarichApiException) {
            _uiState.update {
                it.copy(errorMessage = error.message ?: "Could not load tags")
            }
        } catch (error: Exception) {
            _uiState.update {
                it.copy(errorMessage = error.message ?: "Could not load tags")
            }
        } finally {
            client.close()
        }
    }

    fun setNote(note: String) {
        _uiState.update { it.copy(note = note) }
    }

    fun setTagQuery(query: String) {
        _uiState.update { it.copy(tagQuery = query) }
        refreshSuggestions()
    }

    fun selectTag(name: String) {
        _uiState.update { it.copy(tagQuery = name) }
        refreshSuggestions()
    }

    private fun refreshSuggestions() {
        val query = _uiState.value.tagQuery
        _uiState.update {
            it.copy(tagSuggestions = TagSuggest.filterNames(knownTags, query).take(8))
        }
    }

    fun upload() {
        viewModelScope.launch {
            val connection = settings.getConnection()
            if (connection == null) {
                _uiState.update { it.copy(errorMessage = "Set base URL and API key in Settings") }
                return@launch
            }
            val currentFix = fix ?: LocationHelper.resolveFix()
            if (currentFix == null) {
                _uiState.update {
                    it.copy(errorMessage = "No location fix. Enable location or set a default.")
                }
                return@launch
            }
            _uiState.update { it.copy(busy = true, errorMessage = null, statusMessage = null) }
            val client = settings.openClient(connection)
            try {
                val timestamp = Instant.ofEpochMilli(
                    currentFix.timestampMs ?: System.currentTimeMillis(),
                )
                val timestampIso = DateTimeFormatter.ISO_INSTANT.format(timestamp)
                client.uploadTimelinePoints(
                    listOf(
                        TimelinePointInput(
                            latitude = currentFix.latitude,
                            longitude = currentFix.longitude,
                            timestampIso = timestampIso,
                            accuracyMeters = currentFix.accuracyMeters,
                        ),
                    ),
                )

                val noteText = _uiState.value.note.trim()
                val tagName = _uiState.value.tagQuery.trim()
                val tagId = TagSuggest.resolveId(knownTags, tagName)
                val extras = mutableListOf<String>()

                if (noteText.isNotEmpty()) {
                    val title = tagName.takeIf { it.isNotEmpty() && tagId == null }
                    client.createNote(
                        CreateNoteRequest(
                            title = title,
                            body = noteText,
                            latitude = currentFix.latitude,
                            longitude = currentFix.longitude,
                            notedAt = timestampIso,
                        ),
                    )
                    extras += "note"
                }

                if (tagId != null) {
                    val placeName = noteText.ifBlank {
                        placeNameHint ?: "Manual pin"
                    }
                    client.createPlace(
                        CreatePlaceRequest(
                            name = placeName,
                            latitude = currentFix.latitude,
                            longitude = currentFix.longitude,
                            tagIds = listOf(tagId),
                        ),
                    )
                    extras += "tagged place"
                } else if (tagName.isNotEmpty()) {
                    extras += "tag unmatched (not created)"
                }

                val detail = if (extras.isEmpty()) {
                    "Uploaded point"
                } else {
                    "Uploaded point + ${extras.joinToString(", ")}"
                }
                _uiState.update {
                    it.copy(busy = false, uploaded = true, statusMessage = detail)
                }
            } catch (error: DawarichApiException) {
                _uiState.update {
                    it.copy(busy = false, errorMessage = error.message ?: "Upload failed")
                }
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(busy = false, errorMessage = error.message ?: "Upload failed")
                }
            } finally {
                client.close()
            }
        }
    }
}
