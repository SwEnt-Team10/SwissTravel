package com.github.swent.swisstravel.model.trip

import com.github.swent.swisstravel.model.user.RatedPreferences
import com.google.firebase.Timestamp
import kotlin.math.round

/**
 * Represents a trip profile.
 *
 * @property startDate The start date of the trip.
 * @property endDate The end date of the trip.
 * @property preferredLocations The list of locations the user wants to see for the trip.
 * @property preferences The list of preferences for the trip.
 */
data class TripProfile(
    val startDate: Timestamp,
    val endDate: Timestamp,
    val preferredLocations: List<Location>,
    val preferences: List<RatedPreferences>
) {
  /** Returns the total time of the trip in hours. */
  fun getTotalTime(): Double {
    val hours = (endDate.seconds - startDate.seconds) / 3600.0
    return (round(hours * 10) / 10)
  }
}
