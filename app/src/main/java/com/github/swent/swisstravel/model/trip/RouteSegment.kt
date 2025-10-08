package com.github.swent.swisstravel.model.trip

import kotlin.math.roundToInt

data class RouteSegment(
    val from: Location,
    val to: Location,
    val distanceMeter: Double,
    val durationMinutes: Double,
    val path: List<Coordinate>,
    val transportMode: TransportMode,
) {

  /** Returns the duration in hours with two decimals. */
  fun getDurationHours(): Double {
    val duration = durationMinutes / 60.0
    val roundedDuration = (duration * 100).roundToInt() / 100.0
    return roundedDuration
  }

  /** Returns the distance in km with two decimals. */
  fun getDistanceKm(): Double {
    val distance = distanceMeter / 1000.0
    val roundedDistance = (distance * 100).roundToInt() / 100.0
    return roundedDistance
  }
}
