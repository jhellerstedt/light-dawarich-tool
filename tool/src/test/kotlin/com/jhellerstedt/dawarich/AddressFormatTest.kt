package com.jhellerstedt.dawarich

import com.jhellerstedt.dawarich.api.models.NearbyPlace
import com.jhellerstedt.dawarich.util.AddressFormat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AddressFormatTest {
    @Test
    fun prefersStreetAndHouseNumber() {
        val place = NearbyPlace(
            name = "Cafe",
            latitude = 1.0,
            longitude = 2.0,
            street = "Collins St",
            housenumber = "120",
            city = "Melbourne",
        )
        assertEquals("120 Collins St, Melbourne", AddressFormat.fromNearby(place))
    }

    @Test
    fun fallsBackToNameThenCity() {
        val place = NearbyPlace(
            name = "Flinders Street Station",
            latitude = 1.0,
            longitude = 2.0,
            city = "Melbourne",
            country = "Australia",
        )
        assertEquals("Flinders Street Station, Melbourne", AddressFormat.fromNearby(place))
    }

    @Test
    fun returnsNullWhenEmpty() {
        assertNull(AddressFormat.fromNearby(null))
        assertNull(
            AddressFormat.fromNearby(
                NearbyPlace(name = "", latitude = 0.0, longitude = 0.0),
            ),
        )
    }

    @Test
    fun formatsCoordinateFallback() {
        assertEquals(
            "-37.81360, 144.96310",
            AddressFormat.coordinates(-37.8136, 144.9631),
        )
    }
}
