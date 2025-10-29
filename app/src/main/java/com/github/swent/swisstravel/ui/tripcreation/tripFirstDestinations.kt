package com.github.swent.swisstravel.ui.tripsettings

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.swent.swisstravel.ui.tripcreation.TripSettingsViewModel

@Composable
fun FirstDestinationScreen(
    viewModel: TripSettingsViewModel = viewModel(),
    onNext: () -> Unit = {}
) {}
