package com.github.swent.swisstravel.ui.composable

import com.github.swent.swisstravel.model.trip.Trip

/**
 * A data class that groups the interaction and selection parameters for the TripList composable.
 *
 * @param onClick Callback when a trip element is clicked.
 * @param onLongPress Callback when a trip element is long-pressed.
 * @param isSelected Function to determine if a trip is selected.
 * @param isSelectionMode Whether the selection mode is active.
 */
data class TripInteraction(
    val onClick: (Trip?) -> Unit = {},
    val onLongPress: (Trip?) -> Unit = {},
    val isSelected: (Trip) -> Boolean = { false },
    val isSelectionMode: Boolean = false
)
