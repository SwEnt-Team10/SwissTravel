package com.github.swent.swisstravel.model.user

import com.github.swent.swisstravel.model.trip.RouteSegment
import com.github.swent.swisstravel.model.trip.TransportMode
import com.github.swent.swisstravel.model.trip.Trip
import kotlin.math.roundToInt

/** Calculates statistics for a list of trips. */
object StatsCalculator {

  /**
   * Function to calculate statistics for a list of trips.
   *
   * @param trips The list of trips to calculate statistics for.
   * @return A UserStats object containing the calculated statistics.
   */
  fun computeStats(trips: List<Trip>): UserStats {

    if (trips.isEmpty()) {
      return UserStats()
    }

    val totalTrips = trips.size

    val totalTravelMinutes =
        trips.sumOf { it.tripProfile.getTotalTime() }.let { (it * 60).roundToInt() }

    val allRouteSegments: List<RouteSegment> = trips.flatMap { it.routeSegments }

    val longestRouteSegmentMin = allRouteSegments.maxOfOrNull { segment -> segment.durationMinutes } ?: 0

    val mostUsedTransportMode: TransportMode? =
        allRouteSegments.groupingBy { it.transportMode }.eachCount().maxByOrNull { it.value }?.key

    val uniqueLocations = trips.flatMap { it.locations }.toSet().size

    return UserStats(
        totalTrips,
        totalTravelMinutes,
        uniqueLocations,
        mostUsedTransportMode,
        longestRouteSegmentMin)
  }
}
