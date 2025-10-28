package com.github.swent.swisstravel.model.trip

import com.github.swent.swisstravel.model.user.Preference
import com.google.firebase.Timestamp
import kotlin.math.round

/**
 * Represents a trip profile.
 *
 * @property startDate The start date of the trip.
 * @property endDate The end date of the trip.
 * @property preferredLocations The list of locations the user wants to see for the trip.
 * @property preferences The list of preferences for the trip.
 * @property adults The number of adults for the trip.
 * @property children The number of children for the trip.
 */
data class TripProfile(
    val startDate: Timestamp,
    val endDate: Timestamp,
    val preferredLocations: List<Location> = emptyList(),
    val preferences: List<Preference> = emptyList(),
    val adults: Int = 1,
    val children: Int = 0,
    val arrivalLocation : Location? = null,
    val departureLocation : Location? = null
) {
  /** Returns the total time of the trip in hours. */
  fun getTotalTime(): Double {
    val hours = (endDate.seconds - startDate.seconds) / 3600.0
    return (round(hours * 10) / 10)
  }
}
