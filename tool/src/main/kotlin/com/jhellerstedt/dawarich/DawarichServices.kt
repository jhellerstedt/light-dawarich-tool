package com.jhellerstedt.dawarich

import com.jhellerstedt.dawarich.data.DawarichSettings
import com.thelightphone.sdk.SealedLightContext
import java.util.concurrent.ConcurrentHashMap

object DawarichServices {
    private val settingsByContext = ConcurrentHashMap<Int, DawarichSettings>()

    fun settings(lightContext: SealedLightContext): DawarichSettings =
        settingsByContext.getOrPut(System.identityHashCode(lightContext.dataStore)) {
            DawarichSettings(lightContext.dataStore)
        }
}
