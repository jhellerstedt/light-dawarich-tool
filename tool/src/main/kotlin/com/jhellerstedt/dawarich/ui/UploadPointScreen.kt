package com.jhellerstedt.dawarich.ui

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

/**
 * Manual upload “pop-up”: note + tag (autocomplete from Dawarich place tags), then upload.
 */
class UploadPointScreen(sealedActivity: SealedLightActivity) :
    LightScreen<Boolean, UploadPointViewModel>(sealedActivity) {

    override val viewModelClass: Class<UploadPointViewModel>
        get() = UploadPointViewModel::class.java

    override fun createViewModel(): UploadPointViewModel =
        UploadPointViewModel(DawarichServices.settings(lightContext))

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val state by viewModel.uiState.collectAsState()

        LaunchedEffect(state.uploaded) {
            if (state.uploaded) {
                goBack(true)
            }
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
                            onClick = { goBack(false) },
                        ),
                        center = LightTopBarCenter.Text("Upload point"),
                        rightButton = LightBarButton.Text(
                            text = if (state.busy) "…" else "SEND",
                            onClick = { if (!state.busy) viewModel.upload() },
                        ),
                        modifier = Modifier.padding(bottom = 1f.gridUnitsAsDp()),
                    )

                    LightScrollView(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 1f.gridUnitsAsDp()),
                    ) {
                        LightText(
                            text = "Location",
                            variant = LightTextVariant.Superfine,
                        )
                        LightText(
                            text = state.locationLabel,
                            variant = LightTextVariant.Copy,
                            modifier = Modifier.padding(bottom = 1f.gridUnitsAsDp()),
                        )

                        FieldRow(
                            label = "Note",
                            value = state.note.ifBlank { "Optional" },
                        ) {
                            editText(
                                title = "Note",
                                text = state.note,
                            ) { result ->
                                if (result is LightTextEditResult.Submitted) {
                                    viewModel.setNote(result.text)
                                }
                            }
                        }

                        FieldRow(
                            label = "Tag",
                            value = state.tagQuery.ifBlank { "Optional — tap to type" },
                        ) {
                            editText(
                                title = "Tag",
                                text = state.tagQuery,
                                singleLine = true,
                            ) { result ->
                                if (result is LightTextEditResult.Submitted) {
                                    viewModel.setTagQuery(result.text)
                                }
                            }
                        }

                        if (state.tagSuggestions.isNotEmpty()) {
                            LightText(
                                text = "Suggestions",
                                variant = LightTextVariant.Superfine,
                                modifier = Modifier.padding(top = 0.5f.gridUnitsAsDp()),
                            )
                            state.tagSuggestions.forEach { suggestion ->
                                LightText(
                                    text = suggestion,
                                    variant = LightTextVariant.Copy,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 0.5f.gridUnitsAsDp())
                                        .lightClickable(onClick = {
                                            viewModel.selectTag(suggestion)
                                        }),
                                )
                            }
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
private fun FieldRow(label: String, value: String, onClick: () -> Unit) {
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
