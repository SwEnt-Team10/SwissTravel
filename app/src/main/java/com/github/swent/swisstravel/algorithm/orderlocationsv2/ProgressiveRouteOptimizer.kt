package com.github.swent.swisstravel.algorithm.orderlocationsv2

import android.util.Log
import com.github.swent.swisstravel.algorithm.cache.DurationCache
import com.github.swent.swisstravel.algorithm.orderlocations.OrderedRoute
import com.github.swent.swisstravel.model.trip.Coordinate
import com.github.swent.swisstravel.model.trip.Location
import com.github.swent.swisstravel.model.trip.TransportMode
import com.github.swent.swisstravel.model.trip.activity.Activity
import kotlin.math.acos
import kotlin.math.max
import kotlin.math.sqrt

private const val LARGE_VALUE = 1e9
private const val DEFAULT_K = 7
private const val UNREACHABLE_LEG =
    LARGE_VALUE / 20 // a trip should not exceed around 50 days in travel time

// average car speed in km/h for fallback estimation
// set as 80 because national roads in Switzerland have speed limits of 80 km/h
private const val CAR_SPEED = 80.0

// average train speed in km/h for fallback estimation
// set as 100 because train's speed in Switzerland generally range between 80km/h to 120km/h
private const val TRAIN_SPEED = 100.0

// average speed in km/h for fallback estimation when the transport mode is unknown
// set as 60 because it's a reasonable average speed for mixed transport modes
private const val UNKNOWN_SPEED = 60.0
private const val FROM_HOUR_TO_MINUTES = 60.0
private const val FROM_MINUTES_TO_SECONDS = 60.0

// Done with the help of AI
/**
 * ProgressiveRouteOptimizer:
 * - At each step, find k nearest unvisited locations from current.
 * - Check cache for durations for current -> candidate.
 * - Request missing ones in groups (per start) using DurationMatrixHybrid.
 * - Score candidates with travelTime + activityTime + penalty, pick smallest.
 * - Repeat until all visited or end reached.
 *
 * @param cacheManager An object that manages to manage a cache for the durations of trips
 * @param matrixHybrid Duration matrix that uses the cache to be faster and cheaper
 * @param k The number of neighbors we need to check from each destinations
 * @param penaltyConfig An object that stores weights for penalties on some parameters
 */
class ProgressiveRouteOptimizer(
    private val cacheManager: DurationCache,
    private val matrixHybrid: DurationMatrixHybrid,
    private val k: Int = DEFAULT_K,
    private val penaltyConfig: PenaltyConfig = PenaltyConfig()
) {

  /**
   * Optimize route using the greedy progressive algorithm.
   *
   * @param start Starting Location
   * @param end Ending Location
   * @param allLocations All candidate locations (including start/end)
   * @param activities List of activities (may be empty); activity location equality uses reference
   *   equality or matching coordinate
   * @param mode Transport mode to use when fetching durations
   * @param onProgress Callback to report progress (0.0 to 1.0)
   * @return OrderedRoute with ordered locations, total duration, and segment durations
   */
  suspend fun optimize(
      start: Location,
      end: Location,
      allLocations: List<Location>,
      activities: List<Activity> = emptyList(),
      mode: TransportMode = TransportMode.CAR,
      onProgress: (Float) -> Unit
  ): OrderedRoute {
    val unvisited = allLocations.toMutableList().apply { removeAll { sameLocation(it, start) } }
    val ordered = mutableListOf(start)
    val segmentDurations = mutableListOf<Double>()
    var totalDuration = 0.0
    var current = start

    val activityByCoord = activities.associateBy { it.location.coordinate }
    val activitiesMap = activityByCoord.mapValues { it.value.estimatedTime() }
    var completedSteps = 0
    val totalSteps = unvisited.size

    if (unvisited.isNotEmpty()) {
      do {
        val candidates = selectCandidates(current, end, unvisited)
        val durations = fetchDurations(current, candidates, mode)
        val best =
            pickBestCandidate(
                current, ordered, unvisited, candidates, durations, activitiesMap, end)
        appendCandidate(best, ordered, segmentDurations, unvisited).also { totalDuration += it }
        current = best.location
        completedSteps++
        onProgress(completedSteps.toFloat() / totalSteps)
      } while (unvisited.isNotEmpty() && !sameLocation(current, end))
    }

    if (!sameLocation(ordered.last(), end)) {
      totalDuration += appendFinalEnd(current, end, ordered, segmentDurations, mode)
    }
    onProgress(1f)

    return OrderedRoute(
        orderedLocations = ordered,
        totalDuration = if (totalDuration >= UNREACHABLE_LEG) -1.0 else totalDuration,
        segmentDuration = segmentDurations)
  }

  /**
   * Recompute only the segments of an existing ordered route that correspond to newly added
   * locations.
   *
   * @param orderedLocations Existing ordered route.
   * @param addedIndexes Indexes of locations in the ordered route that were newly added.
   * @param mode Transport mode to use when fetching durations.
   * @param invalidDuration Duration value that indicates an invalid or missing segment duration.
   * @param onProgress Callback to report progress (0.0 to 1.0).
   * @return New OrderedRoute with updated segment durations.
   */
  suspend fun recomputeOrderedRoute(
      orderedLocations: OrderedRoute,
      addedIndexes: List<Int>,
      mode: TransportMode = TransportMode.CAR,
      invalidDuration: Double,
      onProgress: (Float) -> Unit
  ): OrderedRoute {
    val locations = orderedLocations.orderedLocations
    val newSegmentDurations = orderedLocations.segmentDuration.toMutableList()
    var progression = 1
    for (index in addedIndexes) {
      if (index > 0 && index < locations.size - 1) {
        val from = locations[index - 1]
        val to = locations[index]
        val next = locations[index + 1]
        var durationFromTo = newSegmentDurations[index - 1]
        var durationToNext = newSegmentDurations[index]
        // Fetch duration for from -> to if we didn't do it already
        if (newSegmentDurations[index - 1] == invalidDuration) {
          durationFromTo =
              fetchDurations(from, listOf(to), mode)[to] ?: fallbackDuration(from, to, mode)
        }
        // Fetch duration for to -> next if we didn't do it already
        if (newSegmentDurations[index] == invalidDuration) {
          durationToNext =
              fetchDurations(to, listOf(next), mode)[next] ?: fallbackDuration(to, next, mode)
        }
        newSegmentDurations[index - 1] = max(0.0, durationFromTo)
        newSegmentDurations[index] = max(0.0, durationToNext)
      }
      onProgress(progression.toFloat() / addedIndexes.size)
      progression++
    }

    val totalDuration = newSegmentDurations.sum()
    return OrderedRoute(
        orderedLocations = locations,
        totalDuration = if (totalDuration >= UNREACHABLE_LEG) -1.0 else totalDuration,
        segmentDuration = newSegmentDurations)
  }

  /**
   * Select the k nearest unvisited candidate locations from the current location.
   *
   * @param current Current location.
   * @param end Ending location.
   * @param unvisited List of unvisited locations.
   * @return List of candidate locations to consider next.
   */
  private fun selectCandidates(
      current: Location,
      end: Location,
      unvisited: List<Location>
  ): List<Location> {
    return if (unvisited.size == 1 && unvisited.contains(end)) listOf(end)
    else
        unvisited
            .filter { !sameLocation(it, end) }
            .sortedBy { current.haversineDistanceTo(it) }
            .take(k.coerceAtMost(unvisited.size))
  }

  /**
   * Fetch durations from cache or the duration matrix for a list of candidate locations. This
   * function first attempts to retrieve all durations from the local cache. For any durations not
   * found in the cache, it fetches them from the network/matrix and then saves the new results back
   * into the cache for future use.
   *
   * @param current Starting location for durations.
   * @param candidates Candidate locations to fetch durations to.
   * @param mode Transport mode.
   * @return Map of candidate locations to their travel duration in seconds (null if unreachable).
   */
  private suspend fun fetchDurations(
      current: Location,
      candidates: List<Location>,
      mode: TransportMode
  ): Map<Location, Double?> {
    // Attempt to get all durations from the cache first.
    val cachedDurations = getCachedDurations(current, candidates, mode)

    // Identify which candidates are missing from the cache.
    val missingCandidates = candidates.filterNot { cachedDurations.containsKey(it) }

    // If all candidates were found in the cache, we are done.
    if (missingCandidates.isEmpty()) {
      return cachedDurations
    }

    // Fetch the missing durations and combine with the cached results.
    val fetchedDurations = fetchAndCacheMissingDurations(current, missingCandidates, mode)
    return cachedDurations + fetchedDurations
  }

  /**
   * Retrieves durations from the cache for a list of candidate locations.
   *
   * @param current The starting location.
   * @param candidates The list of destination locations.
   * @param mode The transport mode.
   * @return A map of locations to their cached duration. It only contains entries that were
   *   successfully found in the cache.
   */
  private suspend fun getCachedDurations(
      current: Location,
      candidates: List<Location>,
      mode: TransportMode
  ): Map<Location, Double?> {
    return candidates
        .mapNotNull { candidate ->
          try {
            cacheManager.getDuration(current.coordinate, candidate.coordinate, mode)?.let {
              // treat non-positive durations as missing/invalid
              if (it.duration > 0.0) candidate to it.duration else null
            }
          } catch (e: Exception) {
            Log.d("Error getting duration from cache", e.toString())
            null
          }
        }
        .toMap()
  }

  /**
   * Fetches durations for missing candidates from the matrix and saves them to the cache.
   *
   * @param current The starting location.
   * @param missingCandidates The list of candidates for which durations are missing.
   * @param mode The transport mode.
   * @return A map of the newly fetched locations to their durations.
   */
  private suspend fun fetchAndCacheMissingDurations(
      current: Location,
      missingCandidates: List<Location>,
      mode: TransportMode
  ): Map<Location, Double?> {
    // Fetch durations from the hybrid matrix.
    val fetchedDurations =
        try {
          matrixHybrid.fetchDurationsFromStart(current, missingCandidates, mode)
        } catch (e: Exception) {
          Log.d("Error fetching durations", e.toString())
          emptyMap()
        }

    // Create a map from the results and save valid new durations to the cache.
    return missingCandidates.associateWith { candidate ->

      // Fetch result
      val duration = fetchedDurations[current.coordinate to candidate.coordinate]

      // Apply fallback
      val finalDuration =
          if (duration != null && duration > 0) {
            duration
          } else {
            fallbackDuration(current, candidate, mode)
          }

      // Cache only if valid
      if (finalDuration > 0) {
        try {
          cacheManager.saveDuration(current.coordinate, candidate.coordinate, finalDuration, mode)
        } catch (_: Exception) {
          // Ignore cache-saving errors.
        }
      }

      finalDuration
    }
  }

  /**
   * Pick the best candidate location based on travel duration, activity time, and penalties.
   *
   * @param current Current location.
   * @param ordered List of already ordered locations.
   * @param unvisited List of remaining unvisited locations.
   * @param candidates Candidate locations to consider.
   * @param durations Map of candidate locations to their travel durations.
   * @param activities Map of activity times keyed by coordinate.
   * @return CandidateScore containing the best location and its computed score.
   */
  private fun pickBestCandidate(
      current: Location,
      ordered: List<Location>,
      unvisited: List<Location>,
      candidates: List<Location>,
      durations: Map<Location, Double?>,
      activities: Map<Coordinate, Int>,
      end: Location
  ): CandidateScore {
    val scored =
        candidates.map { candidate ->
          val travelSec = durations[candidate] ?: LARGE_VALUE
          val activitySec = (activities[candidate.coordinate] ?: 0) / 8.0
          val previous = ordered.getOrNull(ordered.size - 2)
          val penalty =
              computePenalty(
                  current,
                  candidate,
                  previous,
                  unvisited.filter { it != candidate },
                  activities,
                  penaltyConfig,
                  end)
          CandidateScore(candidate, travelSec + activitySec + penalty, travelSec)
        }
    return scored.minByOrNull { it.score }
        ?: run {
          val fallback = unvisited.minByOrNull { current.haversineDistanceTo(it) }!!
          CandidateScore(fallback, LARGE_VALUE + current.haversineDistanceTo(fallback), LARGE_VALUE)
        }
  }

  /**
   * Append the chosen candidate to the ordered route, update segment durations and unvisited list.
   *
   * @param best CandidateScore of the chosen location.
   * @param ordered Mutable list of ordered locations.
   * @param segmentDurations Mutable list of segment durations.
   * @param unvisited Mutable list of unvisited locations.
   * @return Travel duration to the appended candidate in seconds (non-negative).
   */
  private fun appendCandidate(
      best: CandidateScore,
      ordered: MutableList<Location>,
      segmentDurations: MutableList<Double>,
      unvisited: MutableList<Location>
  ): Double {
    ordered.add(best.location)
    segmentDurations.add(best.travelSeconds)
    unvisited.removeAll { sameLocation(it, best.location) }
    return max(0.0, best.travelSeconds)
  }

  /**
   * Append the final end location to the route, fetching duration if necessary.
   *
   * @param last Last location in the current route.
   * @param end Final destination location.
   * @param ordered Mutable list of ordered locations.
   * @param segmentDurations Mutable list of segment durations.
   * @param mode Transport mode.
   * @return Travel duration to the final destination (LARGE_VALUE if unavailable).
   */
  private suspend fun appendFinalEnd(
      last: Location,
      end: Location,
      ordered: MutableList<Location>,
      segmentDurations: MutableList<Double>,
      mode: TransportMode
  ): Double {
    var finalDuration: Double? = null
    try {
      val cached = cacheManager.getDuration(last.coordinate, end.coordinate, mode)
      finalDuration =
          cached?.duration
              ?: matrixHybrid
                  .fetchDurationsFromStart(last, listOf(end), mode)[
                      Pair(last.coordinate, end.coordinate)]
      finalDuration?.let { cacheManager.saveDuration(last.coordinate, end.coordinate, it, mode) }
    } catch (_: Exception) {
      // ignore cache-saving errors
    }
    ordered.add(end)
    segmentDurations.add(finalDuration ?: LARGE_VALUE)
    return finalDuration ?: LARGE_VALUE
  }

  /**
   * Data class representing the score of a location
   *
   * @param location The location
   * @param score The score attributed to the location
   * @param travelSeconds The time it takes to travel to it some predefined location
   */
  private data class CandidateScore(
      val location: Location,
      val score: Double,
      val travelSeconds: Double
  )

  /**
   * A data class that stores weights for penalties in the algorithm
   *
   * @param zigzagMultiplier Multiplier for zigzag penalty
   * @param activityDiffMultiplier Multiplier for activity difference penalty
   * @param centerDistanceMultiplier Multiplier for center-of-mass penalty
   * @param endDirectionMultiplier Multiplier for end-direction penalty
   */
  data class PenaltyConfig(
      val zigzagMultiplier: Double = 10.0,
      val activityDiffMultiplier: Double = 0.0,
      val centerDistanceMultiplier: Double = 1.0,
      val endDirectionMultiplier: Double = 75.0
  )

  /**
   * A function that computes a penalty to add to the time between two locations so that the trip
   * looks more natural
   *
   * @param from The start location
   * @param to The destination
   * @param previous The previous location if it existed
   * @param remaining The remaining list of destinations after [to]
   * @param activities The activities
   * @param config The penalty configuration
   * @param end The final destination
   * @return The computed penalty in arbitrary units to add to the travel time
   */
  private fun computePenalty(
      from: Location,
      to: Location,
      previous: Location?,
      remaining: List<Location>,
      activities: Map<Coordinate, Int>,
      config: PenaltyConfig,
      end: Location
  ): Double {
    val distKm = from.haversineDistanceTo(to)
    var penalty = distKm // base distance penalty

    // 1) Zigzag penalty
    if (previous != null) {
      val angle = angleBetween(previous.coordinate, from.coordinate, to.coordinate)
      if (angle > 90.0 || angle < 25.0) {
        penalty += angle * config.zigzagMultiplier
      }
    }

    // 2) Activity balance penalty
    val remainingActivityTimes = remaining.map { activities[it.coordinate] ?: 0 }
    val avgRemaining =
        if (remainingActivityTimes.isNotEmpty()) remainingActivityTimes.average() else 0.0
    val candidateActivity = activities[to.coordinate] ?: 0
    if (candidateActivity < avgRemaining) {
      penalty += (avgRemaining - candidateActivity) * config.activityDiffMultiplier
    }

    // 3) Center-of-mass penalty
    if (remaining.isNotEmpty()) {
      val centerLat = remaining.map { it.coordinate.latitude }.average()
      val centerLon = remaining.map { it.coordinate.longitude }.average()
      val center =
          Location(from.coordinate.copy(latitude = centerLat, longitude = centerLon), "center")
      val distToCenter = to.haversineDistanceTo(center)
      penalty += distToCenter * config.centerDistanceMultiplier
    }

    // 4) End-direction penalty
    val toEnd = to.haversineDistanceTo(end)

    // Larger distances from the end = lower penalty
    // Closer to the end = higher penalty
    // Scaled by remaining stops (early steps penalise more)
    val progress = 1.0 - (remaining.size / (remaining.size + 1.0))

    // Invert distance: closer = larger penalty, farther = smaller penalty
    val closeness = 100.0 / (toEnd + 1.0) // avoid div by zero

    penalty += closeness * config.endDirectionMultiplier * (100.0 * progress)

    return penalty
  }

  /** Helper: angle between three points (previous -> current -> candidate) in 2D lat/lon plane */
  private fun angleBetween(a: Coordinate, b: Coordinate, c: Coordinate): Double {
    val abX = b.longitude - a.longitude
    val abY = b.latitude - a.latitude
    val bcX = c.longitude - b.longitude
    val bcY = c.latitude - b.latitude
    val dot = abX * bcX + abY * bcY
    val magAB = sqrt(abX * abX + abY * abY)
    val magBC = sqrt(bcX * bcX + bcY * bcY)
    if (magAB * magBC == 0.0) return 0.0
    val cosTheta = (dot / (magAB * magBC)).coerceIn(-1.0, 1.0)
    return Math.toDegrees(acos(cosTheta))
  }

  /** compare locations by coordinates */
  private fun sameLocation(a: Location, b: Location): Boolean {
    return a.coordinate.latitude == b.coordinate.latitude &&
        a.coordinate.longitude == b.coordinate.longitude
  }

  /**
   * Estimate duration in seconds between two locations based on haversine distance and average
   * speed.
   */
  private fun estimateDurationSecondsByDistance(
      a: Location,
      b: Location,
      avgSpeedKmh: Double = UNKNOWN_SPEED
  ): Double {
    val distKm = a.haversineDistanceTo(b)
    val minutes = (distKm / avgSpeedKmh) * FROM_HOUR_TO_MINUTES
    return max(5.0, minutes) * FROM_MINUTES_TO_SECONDS // always ≥ 5 min
  }

  /** Fallback duration estimation when no data is available in cache or matrix. */
  private fun fallbackDuration(
      current: Location,
      candidate: Location,
      mode: TransportMode
  ): Double {
    return when (mode) {
      TransportMode.CAR -> estimateDurationSecondsByDistance(current, candidate, CAR_SPEED)
      TransportMode.TRAIN -> estimateDurationSecondsByDistance(current, candidate, TRAIN_SPEED)
      else -> estimateDurationSecondsByDistance(current, candidate, UNKNOWN_SPEED)
    }
  }
}
