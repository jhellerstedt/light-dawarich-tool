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
import com.jhellerstedt.dawarich.DawarichServices
import com.jhellerstedt.dawarich.location.LocationHelper
import com.jhellerstedt.dawarich.util.Geo
import com.thelightphone.sdk.LightScreen
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.rememberPermissionRequestLauncher
import com.thelightphone.sdk.textentry.LightTextEditResult
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

class PlacesScreen(sealedActivity: SealedLightActivity) :
    LightScreen<Unit, PlacesViewModel>(sealedActivity) {

    override val viewModelClass: Class<PlacesViewModel>
        get() = PlacesViewModel::class.java

    override fun createViewModel(): PlacesViewModel =
        PlacesViewModel(DawarichServices.settings(lightContext))

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val state by viewModel.uiState.collectAsState()
        val permissionLauncher = rememberPermissionRequestLauncher(
            Manifest.permission.ACCESS_FINE_LOCATION,
        )

        LaunchedEffect(Unit) {
            if (!LocationHelper.hasFineLocationPermission()) {
                permissionLauncher?.launch()
            }
        }

        LaunchedEffect(Unit) {
            LocationHelper.withLocationLease(
                onFix = { viewModel.onFix(it) },
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
                        leftButton = LightBarButton.LightIcon(
                            icon = LightIcons.BACK,
                            onClick = { goBack() },
                        ),
                        center = LightTopBarCenter.Text("Places"),
                        modifier = Modifier.padding(bottom = 1f.gridUnitsAsDp()),
                    )

                    LightScrollView(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 1f.gridUnitsAsDp()),
                    ) {
                        val filterLabel = state.filterTagQuery.trim().ifBlank { "all" }
                        LightText(
                            text = buildString {
                                append(if (state.usingCache) "Cached" else "Live")
                                append(" · ")
                                append(filterLabel)
                                append(" · open Directions manually with the address")
                            },
                            variant = LightTextVariant.Superfine,
                            modifier = Modifier.padding(bottom = 1f.gridUnitsAsDp()),
                        )

                        LightText(
                            text = "Sync places",
                            variant = LightTextVariant.Copy,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 0.5f.gridUnitsAsDp())
                                .lightClickable(onClick = { viewModel.syncPlaces() }),
                        )

                        LightText(
                            text = "Filter tag",
                            variant = LightTextVariant.Superfine,
                            modifier = Modifier.padding(top = 0.5f.gridUnitsAsDp()),
                        )
                        LightText(
                            text = state.filterTagQuery.ifBlank { "All places — tap to type" },
                            variant = LightTextVariant.Copy,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 0.25f.gridUnitsAsDp())
                                .lightClickable(onClick = {
                                    editText(
                                        title = "Filter tag",
                                        text = state.filterTagQuery,
                                        singleLine = true,
                                    ) { result ->
                                        if (result is LightTextEditResult.Submitted) {
                                            viewModel.setFilterTagQuery(result.text)
                                        }
                                    }
                                }),
                        )
                        if (state.filterTagQuery.isNotBlank()) {
                            LightText(
                                text = "Clear filter",
                                variant = LightTextVariant.Copy,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 0.25f.gridUnitsAsDp())
                                    .lightClickable(onClick = { viewModel.clearFilterTag() }),
                            )
                        }

                        val recentShown = if (state.filterTagQuery.isBlank()) {
                            state.recentFilterTags.filter { recent ->
                                state.tagSuggestions.any { it.equals(recent, ignoreCase = true) }
                            }.take(2)
                        } else {
                            emptyList()
                        }
                        if (recentShown.isNotEmpty()) {
                            LightText(
                                text = "Recent",
                                variant = LightTextVariant.Superfine,
                                modifier = Modifier.padding(top = 0.5f.gridUnitsAsDp()),
                            )
                            recentShown.forEach { suggestion ->
                                LightText(
                                    text = suggestion,
                                    variant = LightTextVariant.Copy,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 0.35f.gridUnitsAsDp())
                                        .lightClickable(onClick = {
                                            viewModel.selectFilterTag(suggestion)
                                        }),
                                )
                            }
                        }

                        val autocomplete = state.tagSuggestions.filter { name ->
                            recentShown.none { it.equals(name, ignoreCase = true) }
                        }
                        if (autocomplete.isNotEmpty()) {
                            LightText(
                                text = "Suggestions",
                                variant = LightTextVariant.Superfine,
                                modifier = Modifier.padding(top = 0.5f.gridUnitsAsDp()),
                            )
                            autocomplete.forEach { suggestion ->
                                LightText(
                                    text = suggestion,
                                    variant = LightTextVariant.Copy,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 0.35f.gridUnitsAsDp())
                                        .lightClickable(onClick = {
                                            viewModel.selectFilterTag(suggestion)
                                        }),
                                )
                            }
                        }

                        LightText(
                            text = "Star / save here",
                            variant = LightTextVariant.Copy,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 0.5f.gridUnitsAsDp())
                                .lightClickable(onClick = {
                                    editText(
                                        title = "Place name",
                                        text = "Starred place",
                                    ) { result ->
                                        if (result is LightTextEditResult.Submitted) {
                                            viewModel.starHere(result.text)
                                        }
                                    }
                                }),
                        )

                        if (state.busy) {
                            LightText(
                                text = "Working…",
                                variant = LightTextVariant.Copy,
                                modifier = Modifier.padding(vertical = 0.5f.gridUnitsAsDp()),
                            )
                        }
                        state.statusMessage?.let { message ->
                            LightText(
                                text = message,
                                variant = LightTextVariant.Copy,
                                modifier = Modifier.padding(vertical = 0.5f.gridUnitsAsDp()),
                            )
                        }
                        state.errorMessage?.let { message ->
                            LightText(
                                text = message,
                                variant = LightTextVariant.Copy,
                                modifier = Modifier.padding(vertical = 0.5f.gridUnitsAsDp()),
                            )
                        }

                        if (state.places.isEmpty()) {
                            LightText(
                                text = "No places yet. Sync from Dawarich or star here.",
                                variant = LightTextVariant.Copy,
                                modifier = Modifier.padding(top = 1f.gridUnitsAsDp()),
                            )
                        } else {
                            state.places.forEach { item ->
                                val distance = if (item.distanceMeters.isFinite()) {
                                    Geo.formatDistance(item.distanceMeters)
                                } else {
                                    "—"
                                }
                                val tags = item.place.tags.joinToString(", ") { it.name }
                                    .ifBlank { "untagged" }
                                LightText(
                                    text = "${item.place.name} · $distance",
                                    variant = LightTextVariant.Copy,
                                    modifier = Modifier.padding(top = 0.75f.gridUnitsAsDp()),
                                )
                                LightText(
                                    text = String.format(
                                        "%.5f, %.5f · %s",
                                        item.place.latitude,
                                        item.place.longitude,
                                        tags,
                                    ),
                                    variant = LightTextVariant.Superfine,
                                    modifier = Modifier.padding(bottom = 0.25f.gridUnitsAsDp()),
                                )
                            }
                        }
                    }

                    LightBottomBar(
                        items = listOf(
                            LightBarButton.LightIcon(
                                icon = LightIcons.REFRESH,
                                onClick = { viewModel.syncPlaces() },
                                contentDescription = "Sync places",
                            ),
                        ),
                    )
                }
            }
        }
    }
}
