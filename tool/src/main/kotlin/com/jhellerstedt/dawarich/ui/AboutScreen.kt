package com.jhellerstedt.dawarich.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.jhellerstedt.dawarich.TOOL_DISPLAY_NAME
import com.jhellerstedt.dawarich.UNOFFICIAL_DISCLAIMER
import com.thelightphone.sdk.LightScreen
import com.thelightphone.sdk.LightViewModel
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightScrollView
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.LightTopBar
import com.thelightphone.sdk.ui.LightTopBarCenter
import com.thelightphone.sdk.ui.gridUnitsAsDp

class AboutViewModel : LightViewModel<Unit>()

class AboutScreen(sealedActivity: SealedLightActivity) :
    LightScreen<Unit, AboutViewModel>(sealedActivity) {

    override val viewModelClass: Class<AboutViewModel>
        get() = AboutViewModel::class.java

    override fun createViewModel(): AboutViewModel = AboutViewModel()

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()

        LightTheme(colors = themeColors) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background),
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    LightTopBar(
                        leftButton = LightBarButton.LightIcon(
                            icon = LightIcons.BACK,
                            onClick = { goBack() },
                        ),
                        center = LightTopBarCenter.Text("About"),
                        modifier = Modifier.padding(bottom = 1f.gridUnitsAsDp()),
                    )

                    LightScrollView(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 1f.gridUnitsAsDp()),
                    ) {
                        LightText(
                            text = TOOL_DISPLAY_NAME,
                            variant = LightTextVariant.Copy,
                            modifier = Modifier.padding(bottom = 1f.gridUnitsAsDp()),
                        )
                        LightText(
                            text = UNOFFICIAL_DISCLAIMER,
                            variant = LightTextVariant.Copy,
                            modifier = Modifier.padding(bottom = 1f.gridUnitsAsDp()),
                        )
                        LightText(
                            text = "Location is only tracked while this tool is open. " +
                                "Continuous tracking (when enabled) uploads one point when " +
                                "you open the tool; there is no background lease yet. On the " +
                                "home screen, the location fix and two nearest places refresh " +
                                "every 30 seconds to save battery and data; Places and Upload " +
                                "still poll about every 2 seconds.",
                            variant = LightTextVariant.Superfine,
                        )
                    }
                }
            }
        }
    }
}
