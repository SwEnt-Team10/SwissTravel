package com.github.swent.swisstravel.ui.tripcreation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
/**
 * A composable screen where the user can select the starting destination for their trip.
 *
 * This screen will likely contain an address input field (e.g., [AddressAutocompleteTextField])
 * that allows the user to search for and select a location. The chosen location is then updated in
 * the shared [TripSettingsViewModel].
 *
 * @param viewModel The [TripSettingsViewModel] instance that holds the overall trip configuration
 *   and will be updated with the selected starting destination.
 * @param onNext A callback function to be invoked when the user has selected a destination and
 *   wishes to proceed to the next step in the trip creation flow.
 */
fun FirstDestinationScreen(
    viewModel: TripSettingsViewModel = viewModel(),
    onNext: () -> Unit = {}
) {}
