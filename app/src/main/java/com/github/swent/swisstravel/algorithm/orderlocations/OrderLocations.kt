package com.github.swent.swisstravel.algorithm.orderlocations

import android.content.Context
import com.github.swent.swisstravel.model.trip.Location

/**
 * Represents the result of an optimized travel route.
 *
 * @property orderedLocations The ordered list of locations that defines the optimal travel path,
 *   starting from the given start location and ending at the given end location.
 * @property totalDuration The total estimated travel time of the entire route in seconds. A
 *   negative value (−1.0) indicates that the duration matrix could not be retrieved or the route
 *   could not be computed.
 * @property segmentDuration The list of travel times (in seconds) between each consecutive pair of
 *   locations in [orderedLocations]. For example, if the route is `[A, B, C]`, this list contains
 *   the duration for `A → B` and `B → C`.
 */
data class OrderedRoute(
    val orderedLocations: List<Location>,
    val totalDuration: Double,
    val segmentDuration: List<Double>
)

/**
 * Orders a list of locations to form an optimized travel route between a fixed start and end point.
 *
 * ### Behavior
 * - If duplicate locations (same coordinates) are provided, only one instance is kept.
 * - If the start or end locations are not included in [locations], they are automatically added.
 * - If the start and end locations are the same, the route forms a closed loop beginning and ending
 *   at that point.
 * - If the duration matrix cannot be retrieved (e.g., API failure), the callback receives an
 *   [OrderedRoute] with a `totalDuration` of −1.0 and an empty list of segment durations.
 *
 * @param context The application context, used to access Mapbox configuration and API credentials.
 * @param locations The list of user-selected locations to visit. Duplicates are removed by
 *   coordinate.
 * @param start The fixed starting location for the trip.
 * @param end The fixed ending location for the trip.
 * @param onResult A callback invoked with the resulting [OrderedRoute] once the computation
 *   completes.
 *
 * The callback is executed asynchronously after the duration matrix is retrieved and the route
 * optimized.
 */
fun orderLocations(
    context: Context,
    locations: List<Location>,
    start: Location,
    end: Location,
    onResult: (OrderedRoute) -> Unit
) {
  // Eliminates all locations with the same coordinates except one.
  val unique = locations.distinctBy { it.coordinate }.toMutableList()

  // If start or end is not in the list, add it.
  if (unique.none { it.coordinate == start.coordinate }) unique.add(0, start)
  if (unique.none { it.coordinate == end.coordinate }) unique.add(end)

  // Check if there is only one location
  if (unique.size == 1) {
    onResult(OrderedRoute(unique, 0.0, emptyList()))
    return
  }

  // If the number of locations is invalid, return
  if (unique.size !in 1..MAX_POINTS) {
    onResult(OrderedRoute(locations, -1.0, emptyList()))
    return
  }

  // Maps each location to its coordinates
  val coords = unique.map { it.coordinate }

  // Gets the duration between each location
  val durationMatrix = DurationMatrix(context)
  durationMatrix.getDurations(coords) { durations ->
    // If there is no durations, return
    if (durations == null) {
      onResult(OrderedRoute(locations, -1.0, emptyList()))
      return@getDurations
    }

    // Compute the shortest path between the start and end locations
    val startIndex = unique.indexOfFirst { it.coordinate == start.coordinate }
    val endIndex = unique.indexOfFirst { it.coordinate == end.coordinate }
    if (startIndex == -1 || endIndex == -1) {
      onResult(OrderedRoute(locations, -1.0, emptyList()))
      return@getDurations
    }
    val order =
        if (startIndex == endIndex) {
          val tsp = ClosedTsp()
          tsp.closedTsp(durations, startIndex)
        } else {
          val tsp = OpenTsp()
          tsp.openTsp(durations, startIndex, endIndex)
        }

    // Maps back the order to the original locations
    val ordered = order.map { unique[it] }
    // Extract the time needed for each segment
    val segmentDuration = order.zipWithNext { a, b -> durations[a][b] }
    // Calculates the total duration
    val totalTime = segmentDuration.sum()

    // Returns the result
    onResult(OrderedRoute(ordered, totalTime, segmentDuration))
  }
}
