package com.github.swent.swisstravel.model.trainstimetable

import com.github.swent.swisstravel.model.trip.Location
import com.github.swent.swisstravel.model.trip.RouteSegment

/** Interface for fetching train timetable information. */
interface TrainTimetable {

  /**
   * Finds the fastest route between two locations.
   *
   * @param from The starting location.
   * @param to The destination location.
   * @return A list of [RouteSegment] representing the fastest route, or null if no route is found.
   */
  suspend fun getFastestRoute(from: Location, to: Location): List<RouteSegment>?

  /**
   * Computes the duration matrix for a list of locations. The duration from A to B is the time of
   * the fastest route in seconds.
   *
   * @param locations The list of locations for which to compute the matrix.
   * @return A 2D array where matrix[i][j] is the travel duration in seconds from locations[i] to
   *   locations[j]. Diagonal elements (i, i) are 0.
   */
  suspend fun getDurationMatrix(locations: List<Location>): Array<DoubleArray>
}
