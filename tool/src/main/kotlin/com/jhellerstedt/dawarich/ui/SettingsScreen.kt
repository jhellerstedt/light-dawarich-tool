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
import com.jhellerstedt.dawarich.DawarichServices
import com.thelightphone.sdk.LightScreen
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.textentry.LightTextEditResult
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
import com.thelightphone.sdk.ui.lightClickable

class SettingsScreen(sealedActivity: SealedLightActivity) :
    LightScreen<Unit, SettingsViewModel>(sealedActivity) {

    override val viewModelClass: Class<SettingsViewModel>
        get() = SettingsViewModel::class.java

    override fun createViewModel(): SettingsViewModel =
        SettingsViewModel(DawarichServices.settings(lightContext))

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val state by viewModel.uiState.collectAsState()

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
                        center = LightTopBarCenter.Text("Settings"),
                        modifier = Modifier.padding(bottom = 1f.gridUnitsAsDp()),
                    )

                    LightScrollView(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 1f.gridUnitsAsDp()),
                    ) {
                        LightText(
                            text = "HTTP and HTTPS are both allowed. Plain HTTP needs the cleartext-http capability (already set in lighttool.toml).",
                            variant = LightTextVariant.Superfine,
                            modifier = Modifier.padding(bottom = 1f.gridUnitsAsDp()),
                        )

                        SettingRow(
                            label = "Server URL",
                            value = state.baseUrl.ifBlank { "Not set" },
                        ) {
                            editText(
                                title = "Server URL",
                                text = state.baseUrl,
                            ) { result ->
                                if (result is LightTextEditResult.Submitted) {
                                    viewModel.saveBaseUrl(result.text)
                                }
                            }
                        }

                        SettingRow(
                            label = "API key",
                            value = if (state.hasApiKey) "Saved (encrypted)" else "Not set",
                        ) {
                            editText(
                                title = "API key",
                                text = "",
                            ) { result ->
                                if (result is LightTextEditResult.Submitted) {
                                    viewModel.saveApiKey(result.text)
                                }
                            }
                        }

                        SettingRow(
                            label = "Star tag id",
                            value = state.starTagId.ifBlank { "Optional filter tag" },
                        ) {
                            editText(
                                title = "Star tag id",
                                text = state.starTagId,
                            ) { result ->
                                if (result is LightTextEditResult.Submitted) {
                                    viewModel.saveStarTagId(result.text)
                                }
                            }
                        }

                        LightText(
                            text = "Clear settings",
                            variant = LightTextVariant.Copy,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 1f.gridUnitsAsDp())
                                .lightClickable(onClick = { viewModel.clearAll() }),
                        )

                        state.statusMessage?.let { message ->
                            LightText(
                                text = message,
                                variant = LightTextVariant.Copy,
                                modifier = Modifier.padding(top = 1f.gridUnitsAsDp()),
                            )
                        }
                        state.errorMessage?.let { message ->
                            LightText(
                                text = message,
                                variant = LightTextVariant.Copy,
                                modifier = Modifier.padding(top = 1f.gridUnitsAsDp()),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingRow(label: String, value: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 0.75f.gridUnitsAsDp())
            .lightClickable(onClick = onClick),
    ) {
        LightText(text = label, variant = LightTextVariant.Superfine)
        LightText(text = value, variant = LightTextVariant.Copy)
    }
}
