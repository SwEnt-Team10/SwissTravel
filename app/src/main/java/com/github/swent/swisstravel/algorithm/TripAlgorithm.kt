package com.github.swent.swisstravel.algorithm

import android.content.Context
import android.util.Log
import com.github.swent.swisstravel.algorithm.cache.DurationCacheLocal
import com.github.swent.swisstravel.algorithm.orderlocationsv2.DurationMatrixHybrid
import com.github.swent.swisstravel.algorithm.orderlocationsv2.ProgressiveRouteOptimizer
import com.github.swent.swisstravel.algorithm.selectactivities.SelectActivities
import com.github.swent.swisstravel.algorithm.tripschedule.ScheduleParams
import com.github.swent.swisstravel.algorithm.tripschedule.scheduleTrip
import com.github.swent.swisstravel.model.trip.TransportMode
import com.github.swent.swisstravel.model.trip.TripElement
import com.github.swent.swisstravel.model.trip.TripProfile
import com.github.swent.swisstravel.model.trip.activity.ActivityRepository
import com.github.swent.swisstravel.model.user.Preference
import com.github.swent.swisstravel.ui.tripcreation.TripSettings

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
      val fullDestinationList = buildList {
        tripSettings.arrivalDeparture.arrivalLocation?.let { add(it) }
        addAll(selectedActivities.map { it.location })
        tripSettings.arrivalDeparture.departureLocation?.let { add(it) }
      }

      // ---- STEP 2: Optimize route ----
      val startLocation =
          tripSettings.arrivalDeparture.arrivalLocation
              ?: throw IllegalArgumentException("Arrival location must not be null")
      val endLocation =
          tripSettings.arrivalDeparture.departureLocation
              ?: throw IllegalArgumentException("Departure location must not be null")

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

      // ---- STEP 3: Schedule trip ----
      val schedule =
          try {
            scheduleTrip(tripProfile, optimizedRoute, selectedActivities, scheduleParams) { progress
              ->
              onProgress(
                  progression.selectActivities +
                      progression.optimizeRoute +
                      progression.scheduleTrip * progress)
            }
          } catch (e: Exception) {
            throw IllegalStateException("Trip scheduling failed: ${e.message}", e)
          }

      check(!(schedule.isEmpty())) { "Scheduled trip is empty" }

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
