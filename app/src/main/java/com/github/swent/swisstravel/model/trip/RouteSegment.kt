package com.github.swent.swisstravel.model.trip

import com.google.firebase.Timestamp
import kotlin.math.roundToInt

/**
 * Represents a route segment.
 *
 * @property from The starting location of the route segment.
 * @property to The ending location of the route segment.
 * @property durationMinutes The duration of the route segment in minutes.
 * @property transportMode The transport mode of the route segment.
 * @property startDate The start date of the route segment.
 * @property endDate The end date of the route segment.
 */
data class RouteSegment(
    val from: Location,
    val to: Location,
    val durationMinutes: Int,
    val transportMode: TransportMode?,
    val startDate: Timestamp,
    val endDate: Timestamp
) {

  /** Returns the duration in hours with two decimals. */
  fun getDurationHours(): Double {
    val duration = durationMinutes / 60.0
    val roundedDuration = (duration * 100).roundToInt() / 100.0
    return roundedDuration
  }
}
