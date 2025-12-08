package com.github.swent.swisstravel.algorithm

import android.content.Context
import android.util.Log
import com.github.swent.swisstravel.R
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
import com.google.firebase.Timestamp
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.collections.zipWithNext
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong
import kotlin.random.Random

const val DISTANCE_PER_STOP_KM = 90.0
const val RADIUS_NEW_ACTIVITY_M = 15000
const val RADIUS_EXTENSION_M = 12500
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
 * @param finalScheduling Weight for the final scheduling step.
 *
 * The sum of all weights must equal 1.0.
 */
data class Progression(
    val selectActivities: Float,
    val optimizeRoute: Float,
    val fetchInBetweenActivities: Float,
    val scheduleTrip: Float,
    val finalScheduling: Float
) {
  init {
    val sum =
        selectActivities + optimizeRoute + scheduleTrip + fetchInBetweenActivities + finalScheduling
    require(abs(sum - 1.0f) < EPSILON) { "Progression values must sum to 1.0, but got $sum" }
  }
}

/**
 * Data class representing the progression weights for each step of the trip computation.
 *
 * @param schedule Weight for the trip scheduling step.
 * @param analyzeAndRemove Weight for analyzing and removing activities step.
 * @param recomputeRoute Weight for recomputing the route step.
 * @param reschedule Weight for the rescheduling step.
 *
 * The sum of all weights must equal 1.0.
 */
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
 * Data class representing the progression weights for the final scheduling step.
 *
 * @param selectNewActivities Weight for selecting new activities step.
 * @param optimize Weight for optimizing the route step.
 * @param finalScheduling Weight for the final scheduling step.
 *
 * The sum of all weights must equal 1.0.
 */
data class FinalSchedulingProgression(
    val selectNewActivities: Float,
    val optimize: Float,
    val finalScheduling: Float
) {
  init {
    val sum = selectNewActivities + optimize + finalScheduling
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
open class TripAlgorithm(
    private val activitySelector: SelectActivities,
    private val routeOptimizer: ProgressiveRouteOptimizer,
    private val scheduleParams: ScheduleParams = ScheduleParams(),
    private val progression: Progression =
        Progression(
            selectActivities = 0.20f,
            optimizeRoute = 0.20f,
            fetchInBetweenActivities = 0.10f,
            scheduleTrip = 0.15f,
            finalScheduling = 0.35f),
    private val rescheduleProgression: RescheduleProgression =
        RescheduleProgression(
            schedule = 0.30f, analyzeAndRemove = 0.10f, recomputeRoute = 0.40f, reschedule = 0.20f),
    private val finalSchedulingProgression: FinalSchedulingProgression =
        FinalSchedulingProgression(
            selectNewActivities = 0.25f, optimize = 0.25f, finalScheduling = 0.50f)
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
   * A list of pairs representing Swiss major cities and their corresponding coordinates.
   *
   * Each pair contains a [Location] object representing the city and a radius in km from the
   * center.
   */
  private val swissMajorCities: List<Pair<Location, Int>> =
      listOf(
          Pair(Location(Coordinate(47.3769, 8.5417), "Zürich"), 15), // Zürich
          Pair(Location(Coordinate(46.2044, 6.1432), "Genève"), 12), // Geneva
          Pair(Location(Coordinate(47.5596, 7.5886), "Basel"), 10), // Basel
          Pair(Location(Coordinate(46.5197, 6.6323), "Lausanne"), 8), // Lausanne
          Pair(Location(Coordinate(46.9480, 7.4474), "Bern"), 10), // Bern
          Pair(Location(Coordinate(47.4988, 8.7241), "Winterthur"), 6), // Winterthur
          Pair(Location(Coordinate(47.0502, 8.3093), "Luzern"), 7), // Lucerne
          Pair(Location(Coordinate(47.4239, 9.3744), "St. Gallen"), 6), // St. Gallen
          Pair(Location(Coordinate(46.0048, 8.9511), "Lugano"), 7), // Lugano
          Pair(Location(Coordinate(47.1379, 7.2464), "Biel/Bienne"), 5), // Biel/Bienne
          Pair(Location(Coordinate(46.7578, 7.6206), "Thun"), 5), // Thun
          Pair(Location(Coordinate(46.1959, 9.0220), "Bellinzona"), 5), // Bellinzona
          Pair(Location(Coordinate(46.8133, 7.4189), "Köniz"), 5), // Köniz (near Bern)
          Pair(Location(Coordinate(46.8065, 7.1513), "Fribourg"), 5), // Fribourg
          Pair(Location(Coordinate(47.6970, 8.6383), "Schaffhausen"), 4), // Schaffhausen
          Pair(Location(Coordinate(47.0980, 6.8319), "La Chaux-de-Fonds"), 4), // La Chaux-de-Fonds
          Pair(Location(Coordinate(46.8490, 9.5300), "Chur"), 5), // Chur
          Pair(Location(Coordinate(47.3490, 8.7186), "Uster"), 4), // Uster (ZH agglomeration)
          Pair(Location(Coordinate(46.2335, 7.3573), "Sion"), 5), // Sion
          Pair(Location(Coordinate(46.4300, 6.9100), "Vevey"), 3), // Vevey
          Pair(Location(Coordinate(46.4310, 6.9110), "Montreux"), 4), // Montreux
          Pair(Location(Coordinate(46.1697, 8.7971), "Locarno"), 5)) // Locarno

  /**
   * A data class used to store the result of [addGrandTourActivities]
   *
   * @param added Whether at least one activity was successfully added.
   * @param shouldRetryCenter Whether the center should be retried next time.
   */
  private data class AddGrandTourResult(val added: Boolean, val shouldRetryCenter: Boolean)

  /**
   * Computes a trip based on the provided settings and profile.
   *
   * @param tripSettings The settings for the trip.
   * @param tripProfile The profile of the trip.
   * @param onProgress A callback function to report the progress of the computation (from 0.0 to
   *   1.0).
   * @param isRandomTrip Whether the trip is random or not.
   * @return A list of [TripElement] representing the computed trip.
   */
  suspend fun computeTrip(
      tripSettings: TripSettings,
      tripProfile: TripProfile,
      isRandomTrip: Boolean = false,
      context: Context,
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

      // ---- STEP 4: Change schedule if needed ----
      val finalSchedule =
          adjustFinalSchedule(
              originalSchedule = schedule,
              originalOrderedRoute = optimizedRoute,
              tripProfile = tripProfile,
              intermediateActivities = intermediateActivities,
              isRandom = isRandomTrip,
              context = context) { progress ->
                onProgress(
                    progression.selectActivities +
                        progression.optimizeRoute +
                        progression.fetchInBetweenActivities +
                        progression.scheduleTrip +
                        progression.finalScheduling * progress)
              }

      check(finalSchedule.isNotEmpty()) { "Final scheduled trip is empty" }

      onProgress(1.0f)
      return finalSchedule
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
      distancePerStop: Double = DISTANCE_PER_STOP_KM,
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
          (distKm / distancePerStop).toInt()
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
   * - Compute deficitSeconds = sum(missing estimatedTime) + missingCount * penalty (the overtime of
   *   the trip)
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
  open suspend fun attemptRescheduleIfNeeded(
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
    val rand = Random.Default

    val candidateList = normalActivities.toMutableList()
    candidateList.shuffle(rand)

    data class Cluster(val center: Location, val acts: MutableList<Activity>)

    // Build map: preferredLocation -> closest activities
    val clusters =
        tripProfile.preferredLocations.map { prefLoc ->
          Cluster(center = prefLoc, acts = mutableListOf())
        }

    // Assign each candidate activity to the nearest preferred location
    for (act in candidateList) {
      val loc = act.location
      val nearestCluster =
          clusters.minByOrNull { cluster ->
            loc.coordinate.haversineDistanceTo(cluster.center.coordinate)
          }!!
      nearestCluster.acts.add(act)
    }

    // Identify clusters that have only ONE activity → “protected”
    val protectedActivities =
        clusters
            .filter { it.acts.size <= 1 } // clusters with <= 1 activity
            .flatMap { it.acts }
            .toMutableSet()

    // Also protect intermediate activities
    protectedActivities.addAll(intermediateActivities)

    val toRemove = mutableListOf<Activity>()
    var remainingDeficit = deficitSeconds
    val pool = candidateList.toMutableList()

    while (remainingDeficit > 0 && pool.isNotEmpty()) {

      // Respect protected clusters unless impossible
      val allowedPool =
          pool
              .filter { it !in protectedActivities }
              .ifEmpty { pool } // fallback to full pool if needed

      // 1) overshoots
      val overshoots = allowedPool.filter { it.estimatedTime.toLong() >= remainingDeficit }
      val bestOvershoot = overshoots.minByOrNull { it.estimatedTime.toLong() - remainingDeficit }

      // 2) undershoot fallback
      val bestUndershoot =
          allowedPool
              .filter { it.estimatedTime.toLong() < remainingDeficit }
              .maxByOrNull { it.estimatedTime.toLong() }

      val chosen = bestOvershoot ?: bestUndershoot ?: break

      toRemove.add(chosen)
      remainingDeficit -= chosen.estimatedTime.toLong()
      pool.remove(chosen)

      // If a protected activity had to be removed (fallback case), unprotect the cluster
      if (chosen in protectedActivities) {
        protectedActivities.remove(chosen)
      }

      onProgress(
          rescheduleProgression.schedule +
              rescheduleProgression.analyzeAndRemove *
                  (1f - remainingDeficit.toFloat() / deficitSeconds))
    }

    if (toRemove.isEmpty()) {
      // Could not find candidates to remove, return original result
      return firstSchedule
    }

    // 5) Update activityList by removing chosen activities
    val removedLocations = toRemove.map { it.location }.toSet()
    activityList.removeAll { act ->
      !intermediateActivities.contains(act) && toRemove.any { rem -> activitiesMatch(act, rem) }
    }

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

  /**
   * Adjusts the final schedule to ensure it ends on the correct day.
   *
   * @param originalSchedule The original schedule to be adjusted.
   * @param originalOrderedRoute The original ordered route.
   * @param tripProfile The profile of the trip.
   * @param intermediateActivities The list of activities between distant locations.
   * @param isRandom Whether the trip is random.
   * @param onProgress A callback function to report progress (from 0.0 to 1.0).
   * @return The adjusted schedule.
   *
   * Done with AI
   */
  private suspend fun adjustFinalSchedule(
      originalSchedule: List<TripElement>,
      originalOrderedRoute: OrderedRoute,
      tripProfile: TripProfile,
      intermediateActivities: List<Activity> = emptyList(),
      isRandom: Boolean = false,
      context: Context,
      onProgress: (Float) -> Unit = {}
  ): List<TripElement> {
    onProgress(0.0f)

    // Already ending on the correct day → nothing to change
    if (sameDate(originalSchedule.last().endDate, tripProfile.endDate)) {
      return originalSchedule
    }

    val newSchedule = originalSchedule.toMutableList()

    // Extract last segment
    val last = newSchedule.last()
    val lastSegment =
        last as? TripElement.TripSegment
            ?: throw IllegalStateException("Last TripElement must be a TripSegment")

    val startLoc = lastSegment.route.from
    val endLoc = lastSegment.route.to

    val intermediate =
        originalOrderedRoute.orderedLocations.drop(1).dropLast(1) // remove start & end

    // take one out of two
    val sampled = intermediate.filterIndexed { index, _ -> index % 2 == 0 }

    val listOfLocations = listOf(startLoc) + sampled + listOf(endLoc)

    // This will hold the activities that we add during the extension
    val addedActivities = mutableListOf<Activity>()
    val oldActivities =
        originalSchedule.filterIsInstance<TripElement.TripActivity>().map { it.activity }
    val activityBlackList = oldActivities.map { it.getName() }.toMutableList()

    var nearWhich = 0
    var radius = RADIUS_NEW_ACTIVITY_M
    var index = 0
    val maxIndex =
        computeIndex(
            targetEndDate = tripProfile.endDate, currentEndDate = originalSchedule.last().endDate)

    // ---- Loop until the final processed schedule reaches correct end date ----
    try {
      var ordered = originalOrderedRoute.copy()
      while (!sameDate(newSchedule.last().endDate, tripProfile.endDate) && index < maxIndex) {

        val targetLoc = listOfLocations[nearWhich].coordinate

        val newAct =
            activitySelector.getOneActivityNearWithPreferences(targetLoc, radius, activityBlackList)

        if (newAct != null) {
          // Verify that the new activity is not already in the trip even though it should never
          // happen
          if (addedActivities.any { it.location.sameLocation(newAct.location) } ||
              originalSchedule.any {
                it is TripElement.TripActivity && it.activity.location.sameLocation(newAct.location)
              }) {
            index += 1
            continue
          }
          addedActivities.add(newAct)
          activityBlackList.add(newAct.getName())

          // ---- Build a temporary OrderedRoute ----
          val newLocation = addedActivities.last().location

          // Append location to the orderedLocations list
          val newOrderedLocations = ordered.orderedLocations + newLocation

          // Keep existing segment durations for the old route
          val existingDurations = ordered.segmentDuration.toMutableList()

          // Add a single INVALID_DURATION for the new segment
          existingDurations.add(INVALID_DURATION)

          // Update total duration
          val newTotalDuration = existingDurations.sum()

          // Build updated OrderedRoute
          ordered =
              ordered.copy(
                  orderedLocations = newOrderedLocations,
                  segmentDuration = existingDurations,
                  totalDuration = newTotalDuration)

          val startIndex = ordered.orderedLocations.size - addedActivities.size
          val indexes = (startIndex until ordered.orderedLocations.size).toList()

          // ---- Recompute new ordered route ----
          val recomputed =
              routeOptimizer.recomputeOrderedRoute(
                  orderedLocations = ordered,
                  addedIndexes = indexes,
                  mode = TransportMode.CAR,
                  invalidDuration = INVALID_DURATION) {}

          // ---- Reschedule using this new route ----
          val reScheduled =
              scheduleTrip(
                  tripProfile = tripProfile,
                  ordered = recomputed,
                  activities = (oldActivities + addedActivities).toMutableList(),
                  params = scheduleParams) {}

          // Replace newSchedule so we can test endDate again
          newSchedule.clear()
          newSchedule.addAll(reScheduled)
        }

        // If we completed a full cycle without finishing => widen radius
        val cycleSize = listOfLocations.size

        nearWhich = (nearWhich + 1) % cycleSize
        if (nearWhich == cycleSize - 1) {
          index += 1
          radius += RADIUS_EXTENSION_M / (index + 1)
        }

        nearWhich = (nearWhich + 1) % 3

        onProgress(finalSchedulingProgression.selectNewActivities * index.toFloat() / maxIndex)
      }
    } catch (e: Exception) {
      Log.e("TripAlgorithm", "Final schedule extension failed, using original schedule", e)
      onProgress(1.0f)
      return originalSchedule
    }
    onProgress(finalSchedulingProgression.selectNewActivities)

    val allActivities = oldActivities + addedActivities
    val allLocations = allActivities.map { it.location }.toMutableList()

    if (isRandom) {
      allLocations.addAll(tripProfile.preferredLocations)
    }
    // Should not be too heavy because most of it should be cached already
    val optimizedOrderedRoute =
        routeOptimizer.optimize(
            start = startLoc,
            end = endLoc,
            allLocations = allLocations,
            activities = allActivities,
            mode =
                if (tripProfile.preferences.contains(Preference.PUBLIC_TRANSPORT)) {
                  TransportMode.TRAIN
                } else TransportMode.CAR,
        ) {
          onProgress(
              finalSchedulingProgression.selectNewActivities +
                  finalSchedulingProgression.optimize * it)
        }
    onProgress(finalSchedulingProgression.selectNewActivities + finalSchedulingProgression.optimize)

    // Final scheduling by adding even more activities if needed
    val finalSchedule =
        addMoreActivities(
            optimizedOrderedRoute, allActivities, tripProfile, intermediateActivities, context) {
              onProgress(
                  finalSchedulingProgression.selectNewActivities +
                      finalSchedulingProgression.optimize +
                      finalSchedulingProgression.finalScheduling * it)
            }

    onProgress(1.0f)
    return finalSchedule
  }

  /**
   * kdoc done with AI Iteratively adds activities to a trip until the trip reaches its target end
   * date.
   *
   * This function enriches the trip schedule by adding city visits and Grand Tour activities
   * according to the user's preferences, while continuously rescheduling and optimizing the route.
   * It ensures that intermediate and urban preferences are respected and avoids duplicate or
   * conflicting activities.
   *
   * **Algorithm overview:**
   * 1. Start with the original list of activities and reschedule them using
   *    [attemptRescheduleIfNeeded], reporting initial progress via [onProgress].
   * 2. While the last scheduled trip element does not reach the trip's end date:
   *     - If the user has both INTERMEDIATE_STOPS and URBAN preferences:
   *         - Randomly choose to add either a city activity or a Grand Tour activity.
   *     - If only INTERMEDIATE_STOPS is present, attempt to add a Grand Tour activity.
   *     - Otherwise, attempt to add a city activity.
   *     - Keep track of which activities were successfully added to avoid duplicates.
   * 3. After each addition, optimize the route using [routeOptimizer.optimize].
   * 4. Recompute the schedule using [attemptRescheduleIfNeeded] to ensure timing and ordering
   *    remain valid.
   * 5. Update progress after each cycle.
   * 6. Repeat until the trip's end date is reached or a maximum number of cycles is exceeded.
   *
   * @param originalOrderedRoute The initial [OrderedRoute] containing the trip's ordered locations.
   * @param activityList The current list of [Activity] objects to schedule in the trip.
   * @param tripProfile The [TripProfile] containing user preferences, arrival/departure locations,
   *   and dates.
   * @param intermediateActivities Optional list of [Activity] that must remain in the schedule
   *   during rescheduling.
   * @param context Android [Context] used to access resources for creating new activities.
   * @param onProgress Lambda to receive progress updates as a [Float] between 0 and 1.
   * @return A [List] of [TripElement] representing the fully scheduled trip with all added
   *   activities.
   */
  private suspend fun addMoreActivities(
      originalOrderedRoute: OrderedRoute,
      activityList: List<Activity>,
      tripProfile: TripProfile,
      intermediateActivities: List<Activity>,
      context: Context,
      onProgress: (Float) -> Unit = {},
  ): List<TripElement> {
    val allActivities = activityList.toMutableList()

    // Schedule trip
    var schedule =
        attemptRescheduleIfNeeded(
            tripProfile, originalOrderedRoute, allActivities, intermediateActivities) {
              onProgress(it * 0.25f)
            }

    val hasUrbanPreference = tripProfile.preferences.contains(Preference.URBAN)
    val hasIntermediatePreference = tripProfile.preferences.contains(Preference.INTERMEDIATE_STOPS)

    var addCityWorks = true
    var addGrandTourWorks = true
    var stayAtCenterRetry = true
    val visitedLocations = mutableListOf<Location>()
    var numberOfCycle = 0
    val limit = computeIndex(tripProfile.endDate, schedule.last().endDate) + 1
    while ((addCityWorks || addGrandTourWorks) &&
        numberOfCycle <= limit &&
        !sameDate(schedule.last().endDate, tripProfile.endDate)) {
      val rand = Random.Default

      // This part was done with AI
      when {
        // Both can be added
        addCityWorks && addGrandTourWorks && hasUrbanPreference && hasIntermediatePreference -> {
          if (rand.nextBoolean()) {
            addCityWorks = addCityActivity(tripProfile, allActivities, context)
          } else {
            val result =
                addGrandTourActivities(
                    tripProfile, allActivities, visitedLocations, stayAtCenterRetry, context)
            addGrandTourWorks = result.added
            stayAtCenterRetry = result.shouldRetryCenter
          }
        }

        // Only city
        addCityWorks && hasUrbanPreference -> {
          addCityWorks = addCityActivity(tripProfile, allActivities, context)
        }

        // Only Grand Tour
        addGrandTourWorks && hasIntermediatePreference -> {
          val result =
              addGrandTourActivities(
                  tripProfile, allActivities, visitedLocations, stayAtCenterRetry, context)
          addGrandTourWorks = result.added
          stayAtCenterRetry = result.shouldRetryCenter
        }

        // Fallback: try both randomly
        else -> {
          if (rand.nextBoolean() && addCityWorks) {
            addCityWorks = addCityActivity(tripProfile, allActivities, context)
          } else {
            val result =
                addGrandTourActivities(
                    tripProfile, allActivities, visitedLocations, stayAtCenterRetry, context)
            addGrandTourWorks = result.added
            stayAtCenterRetry = result.shouldRetryCenter
          }
        }
      }

      val newActivities =
          allActivities.filter { act -> activityList.none { it.location == act.location } }
      val optimizedRoute =
          routeOptimizer.optimize(
              tripProfile.arrivalLocation!!,
              tripProfile.departureLocation!!,
              (originalOrderedRoute.orderedLocations + newActivities.map { it.location }),
              allActivities,
              TransportMode.CAR,
          ) {}

      schedule =
          attemptRescheduleIfNeeded(
              tripProfile, optimizedRoute, allActivities, intermediateActivities) {}

      onProgress(
          (0.25f + (0.75f / (1 + dateDifference(schedule.last().endDate, tripProfile.endDate)))))
      numberOfCycle++
    }

    onProgress(1.0f)
    return schedule
  }

  /**
   * Done with AI Attempts to add a “Visit <City>” activity to the trip based on the user’s
   * preferred locations.
   *
   * The function works as follows:
   * 1. Shuffle the user's preferred locations.
   * 2. For each location in the shuffled list, check whether it falls inside the radius of any
   *    major Swiss city (defined in `swissMajorCities`).
   * 3. If it matches:
   *     - Create an activity for visiting that city (8 hours).
   *     - If an identical activity is NOT already in the activity list, add it immediately.
   *     - If such an activity already exists, store the new one as a fallback candidate.
   * 4. After scanning all preferred locations, if at least one fallback candidate exists, add the
   *    first one.
   *
   * This ensures:
   * - At most **one** city-visit activity is added.
   * - Duplicate city-visit activities are avoided.
   * - A fallback exists in case the first matched city is already present.
   *
   * @param tripProfile The user's trip profile containing preferred locations.
   * @param activityList The mutable list of activities where the new city activity may be added.
   * @param context The Android context used to fetch string resources.
   * @return `true` if a city activity was added (either directly or via fallback), `false`
   *   otherwise.
   */
  private fun addCityActivity(
      tripProfile: TripProfile,
      activityList: MutableList<Activity>,
      context: Context
  ): Boolean {
    // Shuffle user preferred locations for randomness
    val shuffled = tripProfile.preferredLocations.shuffled()

    val tempCandidates = mutableListOf<Activity>() // store duplicates as fallback

    for (loc in shuffled) {
      // Try to match the location to a major Swiss city
      val matchingCity =
          swissMajorCities.firstOrNull { (cityLoc, radiusKm) ->
            loc.coordinate.haversineDistanceTo(cityLoc.coordinate) <= radiusKm
          }

      if (matchingCity != null) {
        val (cityLoc, _) = matchingCity

        val activityName = context.getString(R.string.city_visit_name, cityLoc.name)

        // Create new activity (estimated time to 8 hours)
        val newActivity =
            Activity(
                location = cityLoc.copy(name = activityName),
                estimatedTime = 8 * 3600,
                startDate = Timestamp.now(), // placeholder
                endDate = Timestamp.now(), // placeholder
                description = context.getString(R.string.city_activity_description),
                imageUrls = emptyList(),
            )

        val existingCount = activityList.count { it.location.sameLocation(newActivity.location) }

        if (existingCount == 0) {
          activityList.add(newActivity)
          return true
        }

        // Add to tempCandidates only if it doesn't already exist twice
        if (existingCount < 2) {
          tempCandidates.add(newActivity)
        }
      }
    }

    if (tempCandidates.isNotEmpty()) {
      activityList.add(tempCandidates.first())
      return true
    }

    return false
  }

  /**
   * Done with AI Adds one or two "Grand Tour" activities to the trip based on the user's preferred
   * locations and trip profile.
   *
   * This function selects Grand Tour stops according to two modes:
   * 1. **stayAtCenter = true**:
   *     - Computes a dynamic reference point along the line between the first and last preferred
   *       location.
   *     - Chooses preferred locations closest to this reference point as candidates for Grand Tour
   *       stops.
   *     - Within a radius starting at 30km and increasing in steps of 30km up to 120km, finds a
   *       matching Grand Tour city and adds activities nearby.
   * 2. **stayAtCenter = false**:
   *     - Finds the Grand Tour city closest to either the start or end preferred location
   *       (whichever is closer).
   *     - Skips cities already in the visited list.
   *     - Adds activities near the closest unvisited city.
   *
   * The function uses a **fallback radius** strategy to expand the search area if no activities are
   * found initially.
   *
   * For each selected Grand Tour city, up to two activities are added via
   * [activitySelector.getOneActivityNearWithPreferences], and the city is added to [visitedList] to
   * prevent duplicates in future calls.
   *
   * @param tripProfile The user's trip profile, containing preferred locations and trip
   *   preferences.
   * @param activityList The mutable list of activities; newly created activities will be added
   *   here.
   * @param visitedList The list of already visited Grand Tour locations; prevents duplicate stops.
   * @param stayAtCenter Flag determining which selection strategy to use:
   *         - `true`: dynamic reference along start-to-end line.
   *         - `false`: closest city to either start or end of trip.
   *
   * @param context The Android context, used to load Grand Tour locations from resources.
   * @return `true` if at least one activity was successfully added, `false` otherwise.
   */
  private suspend fun addGrandTourActivities(
      tripProfile: TripProfile,
      activityList: MutableList<Activity>,
      visitedList: MutableList<Location>,
      stayAtCenter: Boolean,
      context: Context
  ): AddGrandTourResult {
    val preferred = tripProfile.preferredLocations
    if (preferred.isEmpty()) return AddGrandTourResult(false, stayAtCenter)

    // Load Grand Tour cities from resources
    val grandTour =
        context.resources.getStringArray(R.array.grand_tour).map { line ->
          val parts = line.split(";")
          Location(
              coordinate = Coordinate(parts[1].toDouble(), parts[2].toDouble()),
              name = parts[0],
              imageUrl = "")
        }

    val targetCities: List<Location> =
        if (stayAtCenter) {
          // ---- Dynamic center along start-end line ----
          val startCoord = preferred.first().coordinate
          val endCoord = preferred.last().coordinate
          val rand = Random.Default
          val fraction = 0.15 + rand.nextDouble() * 0.7
          val dynamicLat =
              startCoord.latitude + (endCoord.latitude - startCoord.latitude) * fraction
          val dynamicLon =
              startCoord.longitude + (endCoord.longitude - startCoord.longitude) * fraction
          val dynamicCenter = Coordinate(dynamicLat, dynamicLon)
          // Sort preferred locations by distance to dynamic center
          preferred.sortedBy { it.coordinate.haversineDistanceTo(dynamicCenter) }
        } else {
          // ---- Closest to start or end ----
          val startCoord = preferred.first().coordinate
          val endCoord = preferred.last().coordinate
          grandTour
              .filter { city -> visitedList.none { it.name == city.name } }
              .sortedBy { city ->
                val distToStart = city.coordinate.haversineDistanceTo(startCoord)
                val distToEnd = city.coordinate.haversineDistanceTo(endCoord)
                minOf(distToStart, distToEnd)
              }
        }

    // ---- Try each candidate location ----
    var radius = 30
    while (radius <= 120) {
      for (loc in targetCities) {
        // Skip already visited
        if (visitedList.any { it.name == loc.name }) continue

        val matchingCity =
            if (stayAtCenter) {
              // Find grand tour city within radius
              grandTour.firstOrNull { city ->
                loc.coordinate.haversineDistanceTo(city.coordinate) <= radius
              }
            } else {
              // loc is already a Grand Tour city
              loc
            } ?: continue

        // Fetch 1-2 activities near the
        var added = false
        for (i in 1..2) {
          val newActivity =
              activitySelector.getOneActivityNearWithPreferences(
                  coords = matchingCity.coordinate,
                  radius = RADIUS_NEW_ACTIVITY_M,
                  activityBlackList = activityList.map { it.getName() })
          if (newActivity != null) {
            activityList.add(newActivity)
            added = true
          } else
              break // No need to search for a second activity if we didn't find one to begin with
        }

        if (added) {
          visitedList.add(matchingCity)
          return AddGrandTourResult(true, stayAtCenter)
        }
      }
      radius += 30
    }

    return AddGrandTourResult(false, stayAtCenter)
  }

  /**
   * Done with AI Function to compute an index for the while loop in [adjustFinalSchedule] based on
   * the number of missing days
   *
   * @param targetEndDate The target end date for the trip.
   * @param currentEndDate The current end date of the trip.
   * @param zone The time zone to use for date comparison.
   * @return The computed index.
   */
  private fun computeIndex(
      targetEndDate: Timestamp,
      currentEndDate: Timestamp,
      zone: ZoneId = ZoneId.systemDefault()
  ): Int {
    val currentLocal = currentEndDate.toDate().toInstant().atZone(zone).toLocalDate()
    val targetLocal = targetEndDate.toDate().toInstant().atZone(zone).toLocalDate()

    // Number of calendar days difference (0 if same day, 1 if target is next day, etc.)
    val missingDays = ChronoUnit.DAYS.between(currentLocal, targetLocal).coerceAtLeast(0)

    // min 2 iterations, cap at 7
    return max(2, min(7, missingDays.toInt()))
  }

  /**
   * Done with AI Checks if two Timestamps represent the same date.
   *
   * @param date1 The first Timestamp to compare.
   * @param date2 The second Timestamp to compare.
   * @param zone The time zone to use for date comparison.
   * @return True if the Timestamps represent the same date, false otherwise.
   */
  private fun sameDate(
      date1: Timestamp,
      date2: Timestamp,
      zone: ZoneId = ZoneId.systemDefault()
  ): Boolean {
    val d1 = date1.toDate().toInstant().atZone(zone).toLocalDate()
    val d2 = date2.toDate().toInstant().atZone(zone).toLocalDate()

    return d1 == d2
  }

  /**
   * Done with AI Computes the difference between two timestamps in the specified [ChronoUnit].
   *
   * @param date1 The first timestamp.
   * @param date2 The second timestamp.
   * @param zone The time zone to consider for date conversion (default system zone).
   * @param unit The unit in which to return the difference (default DAYS).
   * @return The difference between date1 and date2 in the given [unit]. Positive if date2 is after
   *   date1.
   */
  private fun dateDifference(
      date1: Timestamp,
      date2: Timestamp,
      zone: ZoneId = ZoneId.systemDefault(),
      unit: ChronoUnit = ChronoUnit.DAYS
  ): Long {
    val d1 = date1.toDate().toInstant().atZone(zone).toLocalDate()
    val d2 = date2.toDate().toInstant().atZone(zone).toLocalDate()

    return unit.between(d1, d2)
  }
}
