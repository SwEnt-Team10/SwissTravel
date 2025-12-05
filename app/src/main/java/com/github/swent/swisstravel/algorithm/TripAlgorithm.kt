package com.github.swent.swisstravel.algorithm

import android.content.Context
import android.util.Log
import com.github.swent.swisstravel.algorithm.cache.DurationCacheLocal
import com.github.swent.swisstravel.algorithm.orderlocations.OrderedRoute
import com.github.swent.swisstravel.algorithm.orderlocationsv2.DurationMatrixHybrid
import com.github.swent.swisstravel.algorithm.orderlocationsv2.ProgressiveRouteOptimizer
import com.github.swent.swisstravel.algorithm.selectactivities.SelectActivities
import com.github.swent.swisstravel.algorithm.tripschedule.ScheduleParams
import com.github.swent.swisstravel.algorithm.tripschedule.scheduleTrip
import com.github.swent.swisstravel.model.trip.Coordinate
import com.github.swent.swisstravel.model.trip.Location
import com.github.swent.swisstravel.model.trip.TransportMode
import com.github.swent.swisstravel.model.trip.TripElement
import com.github.swent.swisstravel.model.trip.TripProfile
import com.github.swent.swisstravel.model.trip.activity.Activity
import com.github.swent.swisstravel.model.trip.activity.ActivityRepository
import com.github.swent.swisstravel.model.user.Preference
import com.github.swent.swisstravel.ui.tripcreation.TripSettings
import kotlin.collections.zipWithNext
import kotlin.math.abs
import kotlin.math.roundToLong
import kotlin.random.Random

const val DISTANCE_PER_STOP_KM = 90.0
const val RADIUS_NEW_ACTIVITY_M = 15000
const val INVALID_DURATION = -1.0
const val RESCHEDULE_PENALTY_PER_ACTIVITY_SEC = (0.25 * 3600) // 15 minutes
const val EPSILON = 1e-6f
/**
 * Data class representing the progression weights for each step of the trip computation.
 *
 * @param selectActivities Weight for the activity selection step.
 * @param optimizeRoute Weight for the route optimization step.
 * @param fetchInBetweenActivities Weight for fetching in-between activities step.
 * @param scheduleTrip Weight for the trip scheduling step.
 *
 * The sum of all weights must equal 1.0.
 */
data class Progression(
    val selectActivities: Float,
    val optimizeRoute: Float,
    val fetchInBetweenActivities: Float,
    val scheduleTrip: Float
) {
  init {
    val sum = selectActivities + optimizeRoute + scheduleTrip + fetchInBetweenActivities
    require(abs(sum - 1.0f) < EPSILON) { "Progression values must sum to 1.0, but got $sum" }
  }
}

data class RescheduleProgression(
    val schedule: Float,
    val analyzeAndRemove: Float,
    val recomputeRoute: Float,
    val reschedule: Float
) {
  init {
    val sum = schedule + analyzeAndRemove + recomputeRoute + reschedule
    require(abs(sum - 1.0f) < EPSILON) { "Progression values must sum to 1.0, but got $sum" }
  }
}

/**
 * Main class to compute a trip based on data the user passed through the trip creation process. It
 * integrates activity selection, route optimization, and trip scheduling.
 *
 * @property activitySelector The component responsible for selecting activities based on user
 *   preferences.
 * @property routeOptimizer The component responsible for optimizing the route between locations.
 * @property scheduleParams Parameters for scheduling the trip.
 * @property progression Weights for each step of the trip computation process.
 */
class TripAlgorithm(
    private val activitySelector: SelectActivities,
    private val routeOptimizer: ProgressiveRouteOptimizer,
    private val scheduleParams: ScheduleParams = ScheduleParams(),
    private val progression: Progression =
        Progression(
            selectActivities = 0.20f,
            optimizeRoute = 0.40f,
            fetchInBetweenActivities = 0.10f,
            scheduleTrip = 0.30f),
    private val rescheduleProgression: RescheduleProgression =
        RescheduleProgression(
            schedule = 0.30f, analyzeAndRemove = 0.10f, recomputeRoute = 0.40f, reschedule = 0.20f)
) {
  companion object {
    /**
     * Initializes the TripAlgorithm with the necessary components.
     *
     * @param tripSettings The settings for the trip.
     * @param activityRepository The repository to fetch activities from.
     * @param context The Android context.
     * @return An instance of TripAlgorithm.
     */
    fun init(
        tripSettings: TripSettings,
        activityRepository: ActivityRepository,
        context: Context
    ): TripAlgorithm {

      val activitySelector =
          SelectActivities(tripSettings = tripSettings, activityRepository = activityRepository)

      val cacheManager = DurationCacheLocal(context)
      val durationMatrix = DurationMatrixHybrid(context)
      val penalty = ProgressiveRouteOptimizer.PenaltyConfig()
      val optimizer =
          ProgressiveRouteOptimizer(
              cacheManager = cacheManager, matrixHybrid = durationMatrix, penaltyConfig = penalty)

      return TripAlgorithm(activitySelector, optimizer)
    }
  }

  /**
   * Computes a trip based on the provided settings and profile.
   *
   * @param tripSettings The settings for the trip.
   * @param tripProfile The profile of the trip.
   * @param onProgress A callback function to report the progress of the computation (from 0.0 to
   *   1.0).
   * @param isRandomTrip Whether the trip is a random trip.
   * @return A list of [TripElement] representing the computed trip.
   */
  suspend fun computeTrip(
      tripSettings: TripSettings,
      tripProfile: TripProfile,
      isRandomTrip: Boolean = false,
      onProgress: (Float) -> Unit = {}
  ): List<TripElement> {
    try {
      val startLocation =
          tripSettings.arrivalDeparture.arrivalLocation
              ?: throw IllegalArgumentException("Arrival location must not be null")
      val endLocation =
          tripSettings.arrivalDeparture.departureLocation
              ?: throw IllegalArgumentException("Departure location must not be null")

      // ---- STEP 1: Select activities ----
      onProgress(0.0f)
      val selectedActivities =
          try {
            activitySelector.addActivities { progress ->
              onProgress(progression.selectActivities * progress)
            }
          } catch (e: Exception) {
            throw IllegalStateException("Failed to select activities: ${e.message}", e)
          }
      onProgress(progression.selectActivities)
      val activityList = selectedActivities.toMutableList()
      val originalActivityList = selectedActivities
      val fullDestinationList = buildList {
        // If the trip is random, still go to the grand tour spots
        if (isRandomTrip) {
          addAll(tripSettings.destinations)
        } else {
          add(startLocation)
          add(endLocation)
        }
        addAll(selectedActivities.map { it.location })
      }

      Log.d("TripAlgorithm", "Full destination list: $fullDestinationList")

      // ---- STEP 2: Optimize route ----
      var optimizedRoute =
          try {
            routeOptimizer.optimize(
                start = startLocation,
                end = endLocation,
                allLocations = fullDestinationList,
                activities = selectedActivities,
                mode =
                    if (tripSettings.preferences.contains(Preference.PUBLIC_TRANSPORT)) {
                      TransportMode.TRAIN
                    } else TransportMode.CAR) { progress ->
                  onProgress(progression.selectActivities + progression.optimizeRoute * progress)
                }
          } catch (e: Exception) {
            throw IllegalStateException("Route optimization failed: ${e.message}", e)
          }

      check(optimizedRoute.totalDuration > 0) { "Optimized route duration is zero or negative" }

      // ---- STEP 2b: Insert in-between activities if preference enabled ----
      if (tripSettings.preferences.contains(Preference.INTERMEDIATE_STOPS)) {
        optimizedRoute =
            addInBetweenActivities(optimizedRoute = optimizedRoute, activityList) { progress ->
              onProgress(
                  progression.selectActivities +
                      progression.optimizeRoute +
                      progression.fetchInBetweenActivities * progress)
            }
      }

      onProgress(
          progression.selectActivities +
              progression.optimizeRoute +
              progression.fetchInBetweenActivities)

      // ---- STEP 3: Schedule trip ----
      val intermediateActivities =
          activityList.filter { act -> !originalActivityList.contains(act) }
      val schedule =
          attemptRescheduleIfNeeded(
              tripProfile = tripProfile,
              originalOptimizedRoute = optimizedRoute,
              activityList = activityList,
              intermediateActivities = intermediateActivities) { progress ->
                onProgress(
                    progression.selectActivities +
                        progression.optimizeRoute +
                        progression.fetchInBetweenActivities +
                        progression.scheduleTrip * progress)
              }

      check(schedule.isNotEmpty()) { "Rescheduled trip is empty" }

      onProgress(1.0f)
      return schedule
    } catch (e: Exception) {
      Log.e("TripAlgorithm", "Trip computation failed", e)
      throw e
    }
  }

  /**
   * Generates a list of "in-between" activities along the segment from start to end.
   *
   * @param start Starting Location.
   * @param end Ending Location.
   * @param count Number of activities to generate.
   * @return List of new Activities between start and end.
   */
  suspend fun generateActivitiesBetween(
      start: Location,
      end: Location,
      count: Int
  ): List<Activity> {
    if (count <= 0) return emptyList()

    val newActivities = mutableListOf<Activity>()
    val latStep = (end.coordinate.latitude - start.coordinate.latitude) / (count + 1)
    val lonStep = (end.coordinate.longitude - start.coordinate.longitude) / (count + 1)

    for (i in 1..count) {
      // Base coordinates for this stop
      val baseLat = start.coordinate.latitude + latStep * i
      val baseLon = start.coordinate.longitude + lonStep * i

      // Add a small random offset to avoid perfect line
      val randomOffsetLat = (-0.02..0.02).random()
      val randomOffsetLon = (-0.02..0.02).random()

      val coord =
          Coordinate(latitude = baseLat + randomOffsetLat, longitude = baseLon + randomOffsetLon)

      val activity =
          activitySelector.getOneActivityNearWithPreferences(
              coords = coord, radius = RADIUS_NEW_ACTIVITY_M)

      if (activity != null) newActivities.add(activity)
    }

    return newActivities
  }

  /**
   * Adds intermediate activities between main locations in the optimized route based on distance.
   *
   * @param optimizedRoute The optimized route containing main locations.
   * @param activities The mutable list of activities to which new activities will be added.
   * @param mode The transport mode for route optimization.
   * @param onProgress A callback function to report progress (from 0.0 to 1.0).
   * @return A new OrderedRoute including the in-between activities.
   */
  suspend fun addInBetweenActivities(
      optimizedRoute: OrderedRoute,
      activities: MutableList<Activity>,
      mode: TransportMode = TransportMode.CAR,
      onProgress: (Float) -> Unit = {}
  ): OrderedRoute {
    var totalProgress = 0f
    // 1. Get the optimized ordered main locations
    val optimizedMainLocations = optimizedRoute.orderedLocations

    // 2. Build segments along the optimized route
    val segmentPairs = optimizedMainLocations.zipWithNext()

    // 3. Decide how many new stops to add per segment
    val stopsPerSegment =
        segmentPairs.map { (a, b) ->
          val distKm = a.coordinate.haversineDistanceTo(b.coordinate)
          (distKm / DISTANCE_PER_STOP_KM).toInt()
        }

    // 4. Insert intermediate activities along each segment
    // Create a map: startSeg -> List<Activity>
    val intermediateActivitiesBySegment = mutableMapOf<Location, MutableList<Activity>>()

    segmentPairs.forEachIndexed { index, (startSeg, endSeg) ->
      val numStops = stopsPerSegment[index]
      if (numStops <= 0) return@forEachIndexed

      // Generate activities for this segment
      val newActivities = generateActivitiesBetween(startSeg, endSeg, numStops)
      activities.addAll(newActivities)

      // Store in the map under its start segment
      intermediateActivitiesBySegment.getOrPut(startSeg) { mutableListOf() }.addAll(newActivities)

      // Report progress
      for (i in 1..numStops) {
        onProgress(totalProgress)
        totalProgress = i.toFloat() / (numStops.toFloat() * 2)
      }
    }

    // If nothing to add, return early
    if (intermediateActivitiesBySegment.isEmpty()) return optimizedRoute

    // 5. Build new OrderedRoute with the new activities inserted
    val newSegmentDurations = optimizedRoute.segmentDuration.toMutableList()
    val newOrderedLocations = optimizedRoute.orderedLocations.toMutableList()
    // List of indexes where new activities were added to adjust segment durations later
    val addedIndexes = mutableListOf<Int>()
    // Add elements to the lists of our original OrderedRoute at the correct place
    for ((startSeg, activities) in intermediateActivitiesBySegment) {
      // Get the start segment index in the optimized route
      val startIndex = newOrderedLocations.indexOfFirst { it.sameLocation(startSeg) }
      if (startIndex == -1) continue

      // Add the activities location after the start segment
      for (i in 1..activities.size) {
        val activity = activities[i - 1]
        // Because each time we add a new location, the next index shifts by 1
        val insertIndex = startIndex + i
        newOrderedLocations.add(insertIndex, activity.location)
        addedIndexes.add(insertIndex)
        // Set to INVALID_DURATION so that the re-computation knows to calculate it
        newSegmentDurations[insertIndex - 1] = INVALID_DURATION
        newSegmentDurations.add(insertIndex, INVALID_DURATION)
      }
    }

    // New OrderedRoute with the new locations and placeholder durations
    val newOptimizedRoute =
        OrderedRoute(
            orderedLocations = newOrderedLocations,
            totalDuration = optimizedRoute.totalDuration,
            segmentDuration = newSegmentDurations)

    // 6. Recompute the time segments properly with the route optimizer
    val finalOptimizedRoute =
        routeOptimizer.recomputeOrderedRoute(
            newOptimizedRoute, addedIndexes, mode, INVALID_DURATION) {
              onProgress(it + totalProgress)
            }

    return finalOptimizedRoute
  }

  /** Extension function to generate a random double in a closed range */
  private fun ClosedFloatingPointRange<Double>.random(rng: Random = Random.Default): Double {
    // Guarantee closed range by adding the minimal epsilon
    return rng.nextDouble(start, endInclusive + Double.MIN_VALUE)
  }

  /** Extract scheduled activities from a produced schedule. */
  private fun extractActivitiesFromSchedule(schedule: List<TripElement>): List<Activity> {
    return schedule.mapNotNull {
      when (it) {
        is TripElement.TripActivity -> it.activity
        else -> null
      }
    }
  }

  /**
   * Very conservative activity equality/matching: match by location identity and estimatedTime.
   * Replace with id-comparison if Activity has an identifier field.
   */
  private fun activitiesMatch(a: Activity, b: Activity): Boolean {
    return a.estimatedTime == b.estimatedTime && a.location.sameLocation(b.location)
  }

  /**
   * Build a new OrderedRoute by removing given activity locations and marking affected segments
   * durations as INVALID_DURATION so that the route optimizer will recompute them.
   *
   * Returns Pair(newOrderedRoute, changedIndexes) where changedIndexes are the indices that were
   * marked invalid/need recomputation.
   */
  private fun buildRouteAfterRemovals(
      optimizedRoute: OrderedRoute,
      toRemoveLocations: Set<Location>
  ): Pair<OrderedRoute, List<Int>> {
    val newOrderedLocations = optimizedRoute.orderedLocations.toMutableList()
    val newSegmentDurations = optimizedRoute.segmentDuration.toMutableList()
    val changedIndexes = mutableListOf<Int>()

    // Remove each location (only the first occurrence) and mark adjacent durations invalid.
    for (loc in toRemoveLocations) {
      val idx = newOrderedLocations.indexOfFirst { it.sameLocation(loc) }
      if (idx == -1) continue

      // Mark previous segment as invalid
      if (idx - 1 >= 0 && idx - 1 < newSegmentDurations.size) {
        newSegmentDurations[idx - 1] = INVALID_DURATION
        changedIndexes.add(idx - 1)
      }

      // If the next segment exists, remove its duration entry (we merge segments)
      if (idx < newSegmentDurations.size) {
        // We remove the next segmentDuration entry, but mark the resulting merged segment as
        // invalid
        // only if the previous segment index exists; otherwise set the current to INVALID.
        newSegmentDurations.removeAt(idx)
        // After removal, ensure the previous index (if exists) is invalid (we already added it).
      }

      // Remove the location itself
      newOrderedLocations.removeAt(idx)
    }

    val newRoute =
        OrderedRoute(
            orderedLocations = newOrderedLocations,
            totalDuration = optimizedRoute.totalDuration,
            segmentDuration = newSegmentDurations)

    return Pair(newRoute, changedIndexes.distinct())
  }

  /**
   * Done with AI Perform the rescheduling stage:
   * - Compare scheduled result to original activities
   * - Compute deficitSeconds = sum(missing estimatedTime) + missingCount * penalty
   * - Randomly remove activities from the full activity list until removedSeconds >= deficitSeconds
   * - Update the OrderedRoute by removing those locations and invalidating the affected segment
   *   durations
   * - Recompute route with routeOptimizer.recomputeOrderedRoute
   * - Re-run scheduleTrip on the recomputed route and updated activity list
   *
   * @param tripProfile The profile of the trip.
   * @param originalOptimizedRoute The original optimized route before scheduling.
   * @param activityList The mutable list of activities to schedule.
   * @param intermediateActivities The list of intermediate activities added between main locations.
   *   (should not remove them)
   * @return The final scheduled trip after rescheduling attempt.
   */
  private suspend fun attemptRescheduleIfNeeded(
      tripProfile: TripProfile,
      originalOptimizedRoute: OrderedRoute,
      activityList: MutableList<Activity>,
      intermediateActivities: List<Activity>,
      onProgress: (Float) -> Unit
  ): List<TripElement> {
    // Filter out intermediate activities from the activity list to avoid removing them
    val normalActivities = activityList.filter { !intermediateActivities.contains(it) }

    // 1) Run first scheduling pass
    val firstSchedule =
        scheduleTrip(tripProfile, originalOptimizedRoute, activityList, scheduleParams) {
          onProgress(rescheduleProgression.schedule * it)
        }

    // 2) Compute which activities were missing compared to original activity list
    val scheduledActs = extractActivitiesFromSchedule(firstSchedule)
    val missingActivities =
        activityList.filter { act ->
          scheduledActs.none { scheduled -> activitiesMatch(act, scheduled) }
        }

    if (missingActivities.isEmpty()) {
      // Nothing missing — keep the first schedule
      return firstSchedule
    }

    // 3) Compute deficit seconds based on missing activities and penalty-per-activity
    val missingSumSec = missingActivities.sumOf { it.estimatedTime.toDouble().roundToLong() }
    val penaltySec = missingActivities.size * RESCHEDULE_PENALTY_PER_ACTIVITY_SEC.toLong()
    val deficitSeconds = missingSumSec + penaltySec

    // 4) Randomly remove activities across the whole trip until we cover the deficit
    val seed = 12345L
    val rand = Random(seed)

    val candidateList = normalActivities.toMutableList()
    candidateList.shuffle(rand)

    val toRemove = mutableListOf<Activity>()
    var removedSec = 0L
    val iterator = candidateList.iterator()
    while (removedSec < deficitSeconds && iterator.hasNext()) {
      val candidate = iterator.next()
      toRemove.add(candidate)
      removedSec += candidate.estimatedTime.toLong()
      onProgress(rescheduleProgression.analyzeAndRemove * (removedSec.toFloat() / deficitSeconds))
    }

    if (toRemove.isEmpty()) {
      // Could not find candidates to remove, return original result
      return firstSchedule
    }

    // 5) Update activityList by removing chosen activities
    val removedLocations = toRemove.map { it.location }.toSet()
    activityList.removeAll { act -> toRemove.any { rem -> activitiesMatch(act, rem) } }

    // 6) Build new OrderedRoute removing those locations and invalidating durations
    val (routeAfterRemoval, changedIndexes) =
        buildRouteAfterRemovals(originalOptimizedRoute, removedLocations)

    // 7) Recompute route durations for invalid segments
    val finalOptimizedRoute =
        try {
          routeOptimizer.recomputeOrderedRoute(
              routeAfterRemoval, changedIndexes, TransportMode.CAR, INVALID_DURATION) {
                onProgress(
                    rescheduleProgression.schedule +
                        rescheduleProgression.analyzeAndRemove +
                        rescheduleProgression.recomputeRoute * it)
              }
        } catch (e: Exception) {
          // Recompute failed — fall back to firstSchedule
          Log.w("TripAlgorithm", "Recompute after removals failed: ${e.message}")
          return firstSchedule
        }

    // 8) Re-run scheduleTrip with recomputed route and the pruned activity list
    val finalSchedule =
        try {
          scheduleTrip(tripProfile, finalOptimizedRoute, activityList, scheduleParams) {
            onProgress(
                rescheduleProgression.schedule +
                    rescheduleProgression.analyzeAndRemove +
                    rescheduleProgression.recomputeRoute +
                    rescheduleProgression.reschedule * it)
          }
        } catch (e: Exception) {
          Log.w("TripAlgorithm", "Scheduling after recompute failed: ${e.message}")
          return firstSchedule
        }

    return finalSchedule
  }
}
