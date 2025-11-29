package com.github.swent.swisstravel.algorithm

import android.content.Context
import android.util.Log
import com.github.swent.swisstravel.algorithm.cache.DurationCacheLocal
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

const val DISTANCE_PER_STOP_KM = 50.0
const val RADIUS_NEW_ACTIVITY_M = 15000

/**
 * Data class representing the progression weights for each step of the trip computation.
 *
 * @param selectActivities Weight for the activity selection step.
 * @param optimizeRoute Weight for the route optimization step.
 * @param scheduleTrip Weight for the trip scheduling step.
 *
 * The sum of all weights must equal 1.0.
 */
data class Progression(
    val selectActivities: Float,
    val optimizeRoute: Float,
    val scheduleTrip: Float
) {
  init {
    val sum = selectActivities + optimizeRoute + scheduleTrip
    require(sum == 1.0f) { "Progression values must sum to 1.0, but got $sum" }
  }
}

/**
 * Main class to compute a trip based on data the user passed through the trip creation process. It
 * integrates activity selection, route optimization, and trip scheduling.
 *
 * @param activitySelector The component responsible for selecting activities based on user
 *   preferences.
 * @param routeOptimizer The component responsible for optimizing the route between locations.
 * @param scheduleParams Parameters for scheduling the trip.
 */
class TripAlgorithm(
    private val activitySelector: SelectActivities,
    private val routeOptimizer: ProgressiveRouteOptimizer,
    private val scheduleParams: ScheduleParams = ScheduleParams(),
    private val progression: Progression =
        Progression(selectActivities = 0.20f, optimizeRoute = 0.40f, scheduleTrip = 0.40f)
) {
  /**
   * Computes a trip based on the provided settings and profile.
   *
   * @param tripSettings The settings for the trip.
   * @param tripProfile The profile of the trip.
   * @param onProgress A callback function to report the progress of the computation (from 0.0 to
   *   1.0).
   * @return A list of [TripElement] representing the computed trip.
   */
  suspend fun computeTrip(
      tripSettings: TripSettings,
      tripProfile: TripProfile,
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
      val fullDestinationList = buildList {
        addAll(tripSettings.destinations)
        addAll(selectedActivities.map { it.location })
      }

      Log.d("TripAlgorithm", "Full destination list: $fullDestinationList")

      // ---- STEP 2: Optimize route ----
      val optimizedRoute =
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

      // ---- STEP 2b: Insert in-between activities if preference enabled ---- (helped by AI)
      if (tripSettings.preferences.contains(Preference.INTERMEDIATE_STOPS)) {
        // 1. Get the optimized ordered locations
        val optimizedLocations = optimizedRoute.orderedLocations

        // 2. Build segments along the optimized route
        val segmentPairs = optimizedLocations.zipWithNext() // consecutive pairs

        // 3. Decide how many new stops to add per segment
        val stopsPerSegment =
            segmentPairs.map { (a, b) ->
              val distKm = a.coordinate.haversineDistanceTo(b.coordinate)
              (distKm / DISTANCE_PER_STOP_KM).toInt() // floor
            }

        // 4. Total number of stops to insert
        val totalNewStops = stopsPerSegment.sum()
        var toFree = totalNewStops

        if (toFree > 0) {
          // 5. Identify clusters in the selected activities
          val clustersMutable = activityList.groupBy { it.location }.toMutableMap()

          // 6. Remove activities from clusters to make room
          val clusterOrder = clustersMutable.entries.sortedByDescending { it.value.size }
          for ((base, list) in clusterOrder) {
            if (toFree <= 0) break
            if (list.isEmpty()) continue
            val removeCount = minOf(1, list.size, toFree)
            val removed =
                removeWorstActivitiesFromCluster(base, list as MutableList<Activity>, removeCount)
            activityList.removeAll { act ->
              removed.any { r -> act.location.sameLocation(r.location) }
            }
            toFree -= removed.size
          }

          // 7. Insert new in-between activities along each segment
          segmentPairs.forEachIndexed { index, (startSeg, endSeg) ->
            val numStops = stopsPerSegment[index]
            if (numStops <= 0) return@forEachIndexed

            for (i in 1..numStops) {
              val newActivities =
                  generateActivitiesBetween(start = startSeg, end = endSeg, count = numStops)

              activityList.addAll(newActivities)
            }
          }
        }
      }

      // ---- STEP 3: Schedule trip ----
      val schedule =
          try {
            scheduleTrip(tripProfile, optimizedRoute, activityList, scheduleParams) { progress ->
              onProgress(
                  progression.selectActivities +
                      progression.optimizeRoute +
                      progression.scheduleTrip * progress)
            }
          } catch (e: Exception) {
            throw IllegalStateException("Trip scheduling failed: ${e.message}", e)
          }

      check(schedule.isNotEmpty()) { "Scheduled trip is empty" }

      onProgress(1.0f)
      return schedule
    } catch (e: Exception) {
      Log.e("TripAlgorithm", "Trip computation failed", e)
      throw e
    }
  }

  /**
   * Runs the trip algorithm with the given settings and profile.
   *
   * @param tripSettings The settings for the trip.
   * @param tripProfile The profile of the trip.
   * @param onProgress A callback function to report progress.
   * @return A list of TripElement representing the computed trip.
   */
  suspend fun runTripAlgorithm(
      tripSettings: TripSettings,
      tripProfile: TripProfile,
      onProgress: (Float) -> Unit
  ): List<TripElement> {
    return computeTrip(
        tripSettings = tripSettings, tripProfile = tripProfile, onProgress = onProgress)
  }

  /**
   * Remove the worst activities from the cluster around baseLocation using a simple heuristic:
   * remove those farthest from the baseLocation first. Returns the list of removed activities.
   *
   * @param baseLocation The base location to measure distance from.
   * @param cluster The mutable list of activities in the cluster.
   * @param count The number of activities to remove.
   * @return The list of removed activities.
   */
  private fun removeWorstActivitiesFromCluster(
      baseLocation: Location,
      cluster: MutableList<Activity>,
      count: Int
  ): List<Activity> {
    if (count <= 0) return emptyList()
    if (cluster.isEmpty()) return emptyList()
    // TODO: For now we use distance to the center of the cluster only; could be improved with
    // activity ratings, popularity, or randomly.
    val sorted =
        cluster.sortedByDescending {
          baseLocation.coordinate.haversineDistanceTo(it.location.coordinate)
        }
    val toRemove = sorted.take(count)
    cluster.removeAll(toRemove.toSet())
    return toRemove
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
          activitySelector.getOneActivityNear(coords = coord, radius = RADIUS_NEW_ACTIVITY_M)

      if (activity != null) newActivities.add(activity)
    }

    return newActivities
  }

  /** Extension function to generate a random double in a closed range */
  private fun ClosedFloatingPointRange<Double>.random() =
      (start + Math.random() * (endInclusive - start))

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
}
