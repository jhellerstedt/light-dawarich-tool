package com.jhellerstedt.dawarich.ui

import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.jhellerstedt.dawarich.DawarichServices
import com.jhellerstedt.dawarich.TOOL_DISPLAY_NAME
import com.jhellerstedt.dawarich.location.LocationHelper
import com.jhellerstedt.dawarich.util.Geo
import com.thelightphone.sdk.InitialScreen
import com.thelightphone.sdk.LightScreen
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.rememberPermissionRequestLauncher
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightBottomBar
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

@InitialScreen
class HomeScreen(sealedActivity: SealedLightActivity) :
    LightScreen<Unit, HomeViewModel>(sealedActivity) {

    override val viewModelClass: Class<HomeViewModel>
        get() = HomeViewModel::class.java

    override fun createViewModel(): HomeViewModel =
        HomeViewModel(DawarichServices.settings(lightContext))

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val state by viewModel.uiState.collectAsState()
        val permissionLauncher = rememberPermissionRequestLauncher(
            Manifest.permission.ACCESS_FINE_LOCATION,
        )

        LaunchedEffect(Unit) {
            viewModel.refreshConnection()
            if (!LocationHelper.hasFineLocationPermission()) {
                permissionLauncher?.launch()
            }
        }

        LaunchedEffect(Unit) {
            LocationHelper.withLocationLease(
                onFix = { viewModel.onFix(it) },
                pollInterval = LocationHelper.HOME_POLL_INTERVAL,
            )
        }

        LightTheme(colors = themeColors) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background),
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    LightTopBar(
                        center = LightTopBarCenter.Text(TOOL_DISPLAY_NAME),
                        rightButton = LightBarButton.LightIcon(
                            icon = LightIcons.SETTINGS,
                            onClick = {
                                navigateTo(screenFactory = { SettingsScreen(it) }) {
                                    viewModel.refreshConnection()
                                }
                            },
                            contentDescription = "Settings",
                        ),
                        modifier = Modifier.padding(bottom = 1f.gridUnitsAsDp()),
                    )

                    LightScrollView(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 1f.gridUnitsAsDp()),
                    ) {
                        if (!state.configured) {
                            LightText(
                                text = "Not configured — open Settings (gear)",
                                variant = LightTextVariant.Copy,
                                modifier = Modifier.padding(bottom = 1f.gridUnitsAsDp()),
                            )
                        }

                        LightText(
                            text = "Location",
                            variant = LightTextVariant.Superfine,
                        )
                        LightText(
                            text = viewModel.describeFix(state.fix, state.addressLabel),
                            variant = LightTextVariant.Copy,
                            modifier = Modifier.padding(bottom = 1f.gridUnitsAsDp()),
                        )

                        if (state.autoUploadEnabled && state.configured) {
                            LightText(
                                text = "Auto-upload every 5 min while open",
                                variant = LightTextVariant.Superfine,
                                modifier = Modifier.padding(bottom = 1f.gridUnitsAsDp()),
                            )
                        }

                        LightText(
                            text = "Nearest places",
                            variant = LightTextVariant.Superfine,
                            modifier = Modifier.padding(top = 0.5f.gridUnitsAsDp()),
                        )
                        if (state.nearestPlaces.isEmpty()) {
                            LightText(
                                text = "None nearby — tap for sync / filter",
                                variant = LightTextVariant.Copy,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 0.35f.gridUnitsAsDp())
                                    .lightClickable(onClick = {
                                        navigateTo(screenFactory = { PlacesScreen(it) }) {
                                            viewModel.reloadNearestPreview()
                                        }
                                    }),
                            )
                        } else {
                            state.nearestPlaces.forEach { item ->
                                val distance = if (item.distanceMeters.isFinite()) {
                                    Geo.formatDistance(item.distanceMeters)
                                } else {
                                    "—"
                                }
                                LightText(
                                    text = "${item.place.name} · $distance",
                                    variant = LightTextVariant.Copy,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 0.35f.gridUnitsAsDp())
                                        .lightClickable(onClick = {
                                            navigateTo(screenFactory = { PlacesScreen(it) }) {
                                                viewModel.reloadNearestPreview()
                                            }
                                        }),
                                )
                            }
                        }
                        LightText(
                            text = "Refreshes every ${LocationHelper.HOME_POLL_INTERVAL.inWholeSeconds}s on home",
                            variant = LightTextVariant.Superfine,
                            modifier = Modifier.padding(bottom = 0.75f.gridUnitsAsDp()),
                        )

                        MenuRow("Upload current point") {
                            navigateTo(screenFactory = { UploadPointScreen(it) }) {
                                viewModel.clearMessages()
                                if (it == true) {
                                    viewModel.refreshConnection()
                                }
                            }
                        }

                        if (state.busy) {
                            LightText(
                                text = "Working…",
                                variant = LightTextVariant.Copy,
                                modifier = Modifier.padding(top = 1f.gridUnitsAsDp()),
                            )
                        }
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
                                align = TextAlign.Start,
                                modifier = Modifier.padding(top = 1f.gridUnitsAsDp()),
                            )
                        }
                    }

                    LightBottomBar(
                        items = listOf(
                            LightBarButton.LightIcon(
                                icon = LightIcons.REFRESH,
                                onClick = {
                                    viewModel.clearMessages()
                                    viewModel.refreshConnection()
                                    viewModel.reloadNearestPreview()
                                    state.fix?.let { viewModel.onFix(it) }
                                },
                                contentDescription = "Refresh",
                            ),
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun MenuRow(label: String, onClick: () -> Unit) {
    LightText(
        text = label,
        variant = LightTextVariant.Copy,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 0.75f.gridUnitsAsDp())
            .lightClickable(onClick = onClick),
    )
}
