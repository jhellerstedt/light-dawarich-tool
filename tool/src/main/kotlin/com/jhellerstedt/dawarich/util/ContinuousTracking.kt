package com.jhellerstedt.dawarich.util

/**
 * Until Light supports a background location lease, "continuous tracking" means:
 * upload one quiet timeline point when the tool is opened (once per open).
 */
object ContinuousTracking {
    const val SETTINGS_LABEL = "Continuous tracking"

    fun shouldUploadOnOpen(
        enabled: Boolean,
        configured: Boolean,
        alreadyUploadedThisOpen: Boolean,
    ): Boolean = enabled && configured && !alreadyUploadedThisOpen

    /** Periodic auto-upload chrome is retired; home keeps manual upload only. */
    fun showHomeAutoUploadChrome(enabled: Boolean): Boolean = false

    fun settingsValue(enabled: Boolean): String =
        if (enabled) "On — upload once when opened" else "Off — manual upload only"

    fun toggleStatusMessage(enabled: Boolean): String =
        if (enabled) {
            "Continuous tracking on (upload once when tool opens)"
        } else {
            "Continuous tracking off"
        }
}
