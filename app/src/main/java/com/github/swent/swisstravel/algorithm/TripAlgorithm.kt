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
    private val scheduleParams: ScheduleParams
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
    onProgress(0.20f)

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
              onProgress(0.20f + progress * 0.40f)
            }

    // ---- STEP 3: Schedule trip ----
    val schedule =
        scheduleTrip(tripProfile, optimizedRoute, selectedActivities, scheduleParams) { progress ->
          // Map scheduling progress 0..1 → global 0.60..1.00
          onProgress(0.60f + progress * 0.40f)
        }

    onProgress(1.0f)
    return schedule
  }
}
