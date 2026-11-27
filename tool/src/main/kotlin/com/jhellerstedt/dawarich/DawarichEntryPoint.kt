package com.jhellerstedt.dawarich

import com.thelightphone.sdk.EntryPoint
import com.thelightphone.sdk.LightEntryPoint
import com.thelightphone.sdk.shared.LightServerData
import kotlinx.coroutines.flow.StateFlow

const val TOOL_DISPLAY_NAME = "Dawarich"

const val UNOFFICIAL_DISCLAIMER =
    "Unofficial client. Not affiliated with Dawarich or The Light Phone."

@EntryPoint
object DawarichEntryPoint : LightEntryPoint {
    override suspend fun onToolCreate(serverData: StateFlow<LightServerData?>) {
        serverData.collect { }
    }

    override val enablePushNotifications: Boolean
        get() = false
}
