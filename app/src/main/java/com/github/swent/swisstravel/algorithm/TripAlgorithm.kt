package com.github.swent.swisstravel.algorithm

import com.github.swent.swisstravel.algorithm.orderlocationsv2.ProgressiveRouteOptimizer
import com.github.swent.swisstravel.algorithm.selectactivities.SelectActivities
import com.github.swent.swisstravel.algorithm.tripschedule.ScheduleParams
import com.github.swent.swisstravel.algorithm.tripschedule.scheduleTrip
import com.github.swent.swisstravel.model.trip.TransportMode
import com.github.swent.swisstravel.model.trip.TripElement
import com.github.swent.swisstravel.model.trip.TripProfile
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
    private val scheduleParams: ScheduleParams,
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

    // ---- STEP 1: Select activities ----
    onProgress(0.0f)
    val selectedActivities = activitySelector.addActivities()
    onProgress(progression.selectActivities)

    // ---- STEP 2: Optimize route based on time costs ----
    val optimizedRoute =
        routeOptimizer.optimize(
            start = tripSettings.arrivalDeparture.arrivalLocation!!,
            end = tripSettings.arrivalDeparture.departureLocation!!,
            allLocations = tripSettings.destinations,
            activities = selectedActivities,
            mode =
                if (tripSettings.preferences.contains(Preference.PUBLIC_TRANSPORT)) {
                  TransportMode.TRAIN
                } else TransportMode.CAR) { progress ->
              onProgress(progression.selectActivities + progression.optimizeRoute * progress)
            }

    // ---- STEP 3: Schedule trip ----
    val schedule =
        scheduleTrip(tripProfile, optimizedRoute, selectedActivities, scheduleParams) { progress ->
          onProgress(
              progression.selectActivities +
                  progression.optimizeRoute +
                  progression.scheduleTrip * progress)
        }

    onProgress(1.0f)
    return schedule
  }
}
