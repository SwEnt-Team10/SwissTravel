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
private const val DEFAULT_K = 5
private const val UNREACHABLE_LEG = LARGE_VALUE / 100

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
   */
  suspend fun optimize(
      start: Location,
      end: Location,
      allLocations: List<Location>,
      activities: List<Activity> = emptyList(),
      mode: TransportMode = TransportMode.CAR // default
  ): OrderedRoute {
    // Setup
    val unvisited = allLocations.toMutableList()
    // ensure start exists and remove it from unvisited
    unvisited.removeAll { sameLocation(it, start) }
    val ordered = mutableListOf<Location>()
    ordered.add(start)
    var current = start
    val segmentDurations = mutableListOf<Double>()
    var totalDuration = 0.0

    // Pre-index activities by location coordinate for quick lookup
    val activityByCoord = activities.associateBy { it.location.coordinate }
    val activitiesMap = activityByCoord.mapValues { it.value.estimatedTime() }

    // Main loop
    while (unvisited.isNotEmpty()) {
      // If we've reached end and want to stop, add end and break
      if (sameLocation(current, end)) {
        break
      }

      // 1) pick k closest unvisited to current by haversine
      val candidates =
          if (unvisited.size == 1 && unvisited.contains(end)) {
            // Only endLocation remains — go straight there
            listOf(end)
          } else {
            // Regular logic: find k closest among non-visited, excluding the end for now
            unvisited
                .filter { sameLocation(it, end) }
                .sortedBy { current.haversineDistanceTo(it) }
                .take(k.coerceAtMost(unvisited.size))
          }

      // 2) check cache for each candidate; gather missingRequests grouped by start (here start is
      // current)
      val durationsFromCache = mutableMapOf<Location, Double?>()
      val missingEnds = mutableListOf<Location>()

      for (candidate in candidates) {
        val cached =
            try {
              cacheManager.getDuration(
                  Coordinate(current.coordinate.latitude, current.coordinate.longitude),
                  Coordinate(candidate.coordinate.latitude, candidate.coordinate.longitude),
                  mode)
            } catch (e: Exception) {
              Log.w("ProgressiveRoute", "Cache read failed: ${e.message}")
              null
            }
        if (cached != null && cached.duration >= 0) {
          // update timestamp in cache asynchronously (we already implemented copy of timestamp in
          // getDuration)
          durationsFromCache[candidate] = cached.duration
        } else {
          missingEnds.add(candidate)
        }
      }

      // 3) fetch missing ends in a single grouped call per current start
      if (missingEnds.isNotEmpty()) {
        val fetched =
            try {
              matrixHybrid.fetchDurationsFromStart(
                  Coordinate(current.coordinate.latitude, current.coordinate.longitude),
                  missingEnds.map { Coordinate(it.coordinate.latitude, it.coordinate.longitude) },
                  mode)
            } catch (e: Exception) {
              Log.e("ProgressiveRoute", "Mapbox fetch failed: ${e.message}")
              emptyMap<Pair<Coordinate, Coordinate>, Double?>()
            }

        // Map back fetched durations to the Location instances and save to cache
        for (candidate in missingEnds) {
          val key =
              Pair(
                  Coordinate(current.coordinate.latitude, current.coordinate.longitude),
                  Coordinate(candidate.coordinate.latitude, candidate.coordinate.longitude))
          val dur = fetched[key]
          durationsFromCache[candidate] = dur
          if (dur != null && dur >= 0) {
            // Save to cache (fire-and-forget)
            try {
              cacheManager.saveDuration(key.first, key.second, dur, mode)
            } catch (e: Exception) {
              Log.w("ProgressiveRoute", "Cache save failed: ${e.message}")
            }
          }
        }
      }

      // If no candidate had a valid duration (all null), we must pick something: fall back to
      // nearest by haversine.
      val scoredCandidates =
          candidates.map { candidate ->
            val travelSec = durationsFromCache[candidate] ?: LARGE_VALUE
            val activityMins = activityByCoord[candidate.coordinate]?.estimatedTime() ?: 0
            val activitySec = activityMins * 60.0
            val previousLocation = if (ordered.size >= 2) ordered[ordered.size - 2] else null
            val penalty =
                computePenalty(
                    from = current,
                    to = candidate,
                    previous = previousLocation,
                    remaining =
                        unvisited.filter {
                          it != candidate && it != end
                        }, // remaining except candidate and end
                    activities = activitiesMap,
                    config = penaltyConfig)
            val score = travelSec + activitySec + penalty
            CandidateScore(candidate, score, travelSec)
          }

      // ensure at least one reachable candidate exists; otherwise pick nearest by haversine and
      // assign large travel
      val best =
          scoredCandidates.minByOrNull { it.score }
              ?: run {
                // fallback
                val fallback = unvisited.minByOrNull { current.haversineDistanceTo(it) }!!
                CandidateScore(
                    fallback, LARGE_VALUE + current.haversineDistanceTo(fallback), LARGE_VALUE)
              }

      // Append chosen next
      ordered.add(best.location)
      segmentDurations.add(best.travelSeconds)
      totalDuration += max(0.0, best.travelSeconds) // ignore negative durations
      // Mark visited
      unvisited.removeAll { sameLocation(it, best.location) }
      // move current
      current = best.location
      // If the chosen is end and end should terminate, we can break - but we respect user's comment
      // that start & end can be same
      if (sameLocation(current, end)) break
    }

    // If end is not included yet, ensure it's appended (try to fetch duration from last to end)
    val last = ordered.last()
    if (!sameLocation(last, end)) {
      // try to get duration from cache / mapbox quickly
      var finalDuration: Double? = null
      try {
        val cached =
            cacheManager.getDuration(
                Coordinate(last.coordinate.latitude, last.coordinate.longitude),
                Coordinate(end.coordinate.latitude, end.coordinate.longitude),
                mode)
        if (cached != null && cached.duration >= 0) finalDuration = cached.duration
        else {
          val fetched =
              matrixHybrid.fetchDurationsFromStart(
                  Coordinate(last.coordinate.latitude, last.coordinate.longitude),
                  listOf(Coordinate(end.coordinate.latitude, end.coordinate.longitude)),
                  mode)
          finalDuration =
              fetched[
                  Pair(
                      Coordinate(last.coordinate.latitude, last.coordinate.longitude),
                      Coordinate(end.coordinate.latitude, end.coordinate.longitude))]
          if (finalDuration != null) {
            cacheManager.saveDuration(
                Coordinate(last.coordinate.latitude, last.coordinate.longitude),
                Coordinate(end.coordinate.latitude, end.coordinate.longitude),
                finalDuration,
                mode)
          }
        }
      } catch (e: Exception) {
        Log.w("ProgressiveRoute", "Final leg retrieval failed: ${e.message}")
      }

      ordered.add(end)
      segmentDurations.add(finalDuration ?: LARGE_VALUE)
      totalDuration += finalDuration ?: LARGE_VALUE
    }

    val result =
        OrderedRoute(
            orderedLocations = ordered,
            totalDuration = if (totalDuration >= UNREACHABLE_LEG) -1.0 else totalDuration,
            segmentDuration = segmentDurations)
    return result
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
   */
  data class PenaltyConfig(
      val zigzagMultiplier: Double = 2.0,
      val activityDiffMultiplier: Double = 30.0,
      val centerDistanceMultiplier: Double = 10.0
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
   */
  private fun computePenalty(
      from: Location,
      to: Location,
      previous: Location?,
      remaining: List<Location>,
      activities: Map<Coordinate, Int>,
      config: PenaltyConfig
  ): Double {
    val distKm = from.haversineDistanceTo(to)
    var penalty = distKm * 60.0 // base distance penalty

    // 1) Zigzag penalty
    if (previous != null) {
      val angle = angleBetween(previous.coordinate, from.coordinate, to.coordinate)
      if (angle > 90.0) {
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
}
