package com.jhellerstedt.dawarich.util

import com.jhellerstedt.dawarich.api.models.NearbyPlace

object AddressFormat {
    fun fromNearby(place: NearbyPlace?): String? {
        if (place == null) return null
        val streetLine = listOfNotNull(
            place.housenumber?.trim()?.takeIf { it.isNotEmpty() },
            place.street?.trim()?.takeIf { it.isNotEmpty() },
        ).joinToString(" ").ifBlank { null }
        val primary = streetLine
            ?: place.name.trim().takeIf { it.isNotEmpty() && !it.equals("Unknown Place", ignoreCase = true) }
            ?: place.city?.trim()?.takeIf { it.isNotEmpty() }
            ?: return null
        val city = place.city?.trim()?.takeIf { it.isNotEmpty() && !primary.contains(it, ignoreCase = true) }
        return if (city != null) "$primary, $city" else primary
    }

    fun coordinates(latitude: Double, longitude: Double): String =
        String.format("%.5f, %.5f", latitude, longitude)
}
