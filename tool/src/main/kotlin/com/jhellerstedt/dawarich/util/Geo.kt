package com.jhellerstedt.dawarich.util

import com.jhellerstedt.dawarich.api.models.DawarichPlace
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

data class LatLng(
    val latitude: Double,
    val longitude: Double,
)

data class PlaceDistance(
    val place: DawarichPlace,
    val distanceMeters: Double,
)

object Geo {
    private const val EARTH_RADIUS_METERS = 6_371_000.0

    fun haversineMeters(a: LatLng, b: LatLng): Double {
        val dLat = Math.toRadians(b.latitude - a.latitude)
        val dLon = Math.toRadians(b.longitude - a.longitude)
        val lat1 = Math.toRadians(a.latitude)
        val lat2 = Math.toRadians(b.latitude)
        val h = sin(dLat / 2) * sin(dLat / 2) +
            cos(lat1) * cos(lat2) * sin(dLon / 2) * sin(dLon / 2)
        return 2 * EARTH_RADIUS_METERS * atan2(sqrt(h), sqrt(1 - h))
    }

    fun nearestPlaces(
        origin: LatLng,
        places: List<DawarichPlace>,
        tagIds: Set<Int> = emptySet(),
        limit: Int = 20,
    ): List<PlaceDistance> {
        val filtered = if (tagIds.isEmpty()) {
            places
        } else {
            places.filter { place -> place.tags.any { it.id in tagIds } }
        }
        return filtered
            .map { place ->
                PlaceDistance(
                    place = place,
                    distanceMeters = haversineMeters(
                        origin,
                        LatLng(place.latitude, place.longitude),
                    ),
                )
            }
            .sortedBy { it.distanceMeters }
            .take(limit)
    }

    fun formatDistance(meters: Double): String =
        when {
            meters < 1000 -> "${meters.roundToInt()} m"
            meters < 10_000 -> String.format("%.1f km", meters / 1000.0)
            else -> "${(meters / 1000.0).roundToInt()} km"
        }
}
