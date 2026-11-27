package com.jhellerstedt.dawarich.location

import android.Manifest
import com.jhellerstedt.dawarich.util.LatLng
import com.thelightphone.sdk.callRemoteServiceMethod
import com.thelightphone.sdk.checkPermission
import com.thelightphone.sdk.shared.LightServiceMethod
import com.thelightphone.sdk.shared.asKotlinResult
import com.thelightphone.sdk.shared.getOrNull
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.seconds

data class Fix(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Double? = null,
    val timestampMs: Long? = null,
    val source: FixSource,
) {
    fun toLatLng(): LatLng = LatLng(latitude, longitude)
}

enum class FixSource {
    Current,
    Default,
}

/**
 * LightOS location lease helper.
 *
 * Call [RequestLocationUpdates] while the tool is open, renew about every 20s
 * (lease ~30s), poll [GetCurrentLocation], and fall back to [GetDefaultLocation]
 * when the current fix is null. Always [ReleaseLocationUpdates] on exit.
 */
object LocationHelper {
    val RENEW_INTERVAL = 20.seconds
    val POLL_INTERVAL = 2.seconds

    suspend fun hasFineLocationPermission(): Boolean {
        val result = checkPermission(Manifest.permission.ACCESS_FINE_LOCATION).asKotlinResult
        return result
            .map { it.permissionResult == LightServiceMethod.GetPermission.Result.Granted }
            .getOrDefault(false)
    }

    suspend fun getDefaultFix(): Fix? {
        val response = callRemoteServiceMethod(LightServiceMethod.GetDefaultLocation, Unit)
            .getOrNull()
        val lat = response?.latitude ?: return null
        val lon = response?.longitude ?: return null
        return Fix(lat, lon, source = FixSource.Default)
    }

    suspend fun getCurrentFixOrNull(): Fix? {
        val response = callRemoteServiceMethod(LightServiceMethod.GetCurrentLocation, Unit)
            .getOrNull()
        val lat = response?.latitude ?: return null
        val lon = response?.longitude ?: return null
        return Fix(
            latitude = lat,
            longitude = lon,
            accuracyMeters = response.accuracyMeters,
            timestampMs = response.timestampMs,
            source = FixSource.Current,
        )
    }

    /**
     * Prefer a live fix; otherwise the user's LightOS default location.
     */
    suspend fun resolveFix(): Fix? =
        getCurrentFixOrNull() ?: getDefaultFix()

    /**
     * While [block] runs, keep a location updates lease alive and poll the current fix.
     * Invokes [onFix] whenever a usable current or default fix is available.
     */
    suspend fun withLocationLease(
        onFix: suspend (Fix?) -> Unit,
        block: suspend () -> Unit = {
            while (true) delay(POLL_INTERVAL)
        },
    ) {
        if (!hasFineLocationPermission()) {
            onFix(getDefaultFix())
            block()
            return
        }

        coroutineScope {
            val renewJob = launch {
                while (isActive) {
                    callRemoteServiceMethod(LightServiceMethod.RequestLocationUpdates, Unit)
                    delay(RENEW_INTERVAL)
                }
            }
            val pollJob = launch {
                while (isActive) {
                    onFix(resolveFix())
                    delay(POLL_INTERVAL)
                }
            }
            try {
                block()
            } finally {
                renewJob.cancel()
                pollJob.cancel()
                withContext(NonCancellable) {
                    callRemoteServiceMethod(LightServiceMethod.ReleaseLocationUpdates, Unit)
                }
            }
        }
    }
}
