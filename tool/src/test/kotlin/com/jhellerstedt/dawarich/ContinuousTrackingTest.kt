package com.jhellerstedt.dawarich

import com.jhellerstedt.dawarich.util.ContinuousTracking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ContinuousTrackingTest {
    @Test
    fun shouldUploadOnOpenOnlyWhenEnabledConfiguredAndNotYetDone() {
        assertTrue(
            ContinuousTracking.shouldUploadOnOpen(
                enabled = true,
                configured = true,
                alreadyUploadedThisOpen = false,
            ),
        )
        assertFalse(
            ContinuousTracking.shouldUploadOnOpen(
                enabled = false,
                configured = true,
                alreadyUploadedThisOpen = false,
            ),
        )
        assertFalse(
            ContinuousTracking.shouldUploadOnOpen(
                enabled = true,
                configured = false,
                alreadyUploadedThisOpen = false,
            ),
        )
        assertFalse(
            ContinuousTracking.shouldUploadOnOpen(
                enabled = true,
                configured = true,
                alreadyUploadedThisOpen = true,
            ),
        )
    }

    @Test
    fun homeNeverShowsPeriodicAutoUploadChrome() {
        assertFalse(ContinuousTracking.showHomeAutoUploadChrome(enabled = true))
        assertFalse(ContinuousTracking.showHomeAutoUploadChrome(enabled = false))
    }

    @Test
    fun settingsCopyDescribesUploadOnOpen() {
        assertEquals("Continuous tracking", ContinuousTracking.SETTINGS_LABEL)
        assertEquals("On — upload once when opened", ContinuousTracking.settingsValue(enabled = true))
        assertEquals("Off — manual upload only", ContinuousTracking.settingsValue(enabled = false))
        assertEquals(
            "Continuous tracking on (upload once when tool opens)",
            ContinuousTracking.toggleStatusMessage(enabled = true),
        )
        assertEquals(
            "Continuous tracking off",
            ContinuousTracking.toggleStatusMessage(enabled = false),
        )
    }
}
