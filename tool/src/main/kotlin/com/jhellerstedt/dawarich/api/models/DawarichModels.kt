package com.jhellerstedt.dawarich.api.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DawarichPlace(
    val id: Int,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val source: String? = null,
    @SerialName("visit_radius") val visitRadius: Int? = null,
    val archived: Boolean = false,
    val geofence: Boolean? = null,
    val icon: String? = null,
    val color: String? = null,
    @SerialName("visits_count") val visitsCount: Int? = null,
    @SerialName("created_at") val createdAt: String? = null,
    val tags: List<DawarichTag> = emptyList(),
)

@Serializable
data class DawarichTag(
    val id: Int,
    val name: String,
    val icon: String? = null,
    val color: String? = null,
)

@Serializable
data class CreatePlaceRequest(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val source: String = "manual",
    @SerialName("visit_radius") val visitRadius: Int = 50,
    @SerialName("tag_ids") val tagIds: List<Int> = emptyList(),
)

@Serializable
data class CreatePlaceBody(
    val place: CreatePlaceRequest,
)

@Serializable
data class TimelineBatchRequest(
    val locations: List<TimelineLocationFeature>,
)

@Serializable
data class TimelineLocationFeature(
    val type: String = "Feature",
    val geometry: TimelinePointGeometry,
    val properties: TimelinePointProperties,
)

@Serializable
data class TimelinePointGeometry(
    val type: String = "Point",
    /** GeoJSON order: longitude, latitude */
    val coordinates: List<Double>,
)

@Serializable
data class TimelinePointProperties(
    val timestamp: String,
    @SerialName("horizontal_accuracy") val horizontalAccuracy: Double? = null,
    val altitude: Double? = null,
    val speed: Double? = null,
    @SerialName("device_id") val deviceId: String = "light-phone",
)

@Serializable
data class NearbyPlacesResponse(
    val places: List<NearbyPlace> = emptyList(),
)

@Serializable
data class NearbyPlace(
    val name: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val distance: Double? = null,
    val type: String? = null,
    val street: String? = null,
    val housenumber: String? = null,
    val city: String? = null,
    val country: String? = null,
    val postcode: String? = null,
    val source: String? = null,
)

@Serializable
data class CreateNoteBody(
    val note: CreateNoteRequest,
)

@Serializable
data class CreateNoteRequest(
    val title: String? = null,
    val body: String? = null,
    val latitude: Double,
    val longitude: Double,
    @SerialName("noted_at") val notedAt: String,
)

@Serializable
data class DawarichNote(
    val id: Int,
    val title: String? = null,
    val body: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    @SerialName("noted_at") val notedAt: String? = null,
)
