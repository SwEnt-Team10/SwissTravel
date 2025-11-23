package com.github.swent.swisstravel.model.user

import com.github.swent.swisstravel.model.trip.RouteSegment
import com.github.swent.swisstravel.model.trip.TransportMode
import com.github.swent.swisstravel.model.trip.Trip
import kotlin.math.roundToInt

object StatsCalculator {

  fun computeStats(trips: List<Trip>): UserStats {

    if (trips.isEmpty()) {
      return UserStats()
    }

    val totalTrips = trips.size

    val totalTravelMinutes =
        trips.sumOf { it.tripProfile.getTotalTime() }.let { (it * 60).roundToInt() }

    val allRouteSegments: List<RouteSegment> = trips.flatMap { it.routeSegments }

    val longestRouteSegmentMin = allRouteSegments.maxOf { segment -> segment.durationMinutes }

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
