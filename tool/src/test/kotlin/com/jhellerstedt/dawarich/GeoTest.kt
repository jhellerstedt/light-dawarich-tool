package com.jhellerstedt.dawarich

import com.jhellerstedt.dawarich.api.models.DawarichPlace
import com.jhellerstedt.dawarich.api.models.DawarichTag
import com.jhellerstedt.dawarich.util.Geo
import com.jhellerstedt.dawarich.util.LatLng
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GeoTest {
    @Test
    fun haversineSamePointIsZero() {
        val point = LatLng(-37.8136, 144.9631)
        assertEquals(0.0, Geo.haversineMeters(point, point), absoluteTolerance = 0.001)
    }

    @Test
    fun nearestPlacesSortsByDistanceAndFiltersTags() {
        val origin = LatLng(0.0, 0.0)
        val near = DawarichPlace(
            id = 1,
            name = "Near",
            latitude = 0.01,
            longitude = 0.0,
            tags = listOf(DawarichTag(id = 9, name = "star")),
        )
        val far = DawarichPlace(
            id = 2,
            name = "Far",
            latitude = 1.0,
            longitude = 0.0,
            tags = emptyList(),
        )
        val ranked = Geo.nearestPlaces(origin, listOf(far, near), tagIds = setOf(9))
        assertEquals(listOf("Near"), ranked.map { it.place.name })
        assertTrue(ranked.first().distanceMeters < 2_000)
    }

    @Test
    fun formatDistanceUsesMetersThenKilometers() {
        assertEquals("250 m", Geo.formatDistance(250.0))
        assertEquals("1.5 km", Geo.formatDistance(1_500.0))
        assertEquals("12 km", Geo.formatDistance(12_400.0))
    }
}
