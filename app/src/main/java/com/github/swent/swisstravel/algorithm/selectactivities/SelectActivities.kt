package com.github.swent.swisstravel.algorithm.selectactivities

import com.github.swent.swisstravel.model.trip.Location
import com.github.swent.swisstravel.model.trip.activity.Activity
import com.github.swent.swisstravel.model.trip.activity.ActivityRepositoryMySwitzerland
import com.github.swent.swisstravel.model.user.Preference
import com.github.swent.swisstravel.ui.tripcreation.TripSettings
import java.lang.Thread.sleep

/**
 * Data class that represents the selected activities and destinations for a trip.
 *
 * @param activities The list of selected activities.
 * @param destinations The list of selected destinations.
 */
data class SelectedActivities(val activities: List<Activity>, val destinations: List<Location>)

// Sleep time between API calls
// Maximum 1 request per second (10 req/s burst)
private const val SLEEP = 1000.toLong()

/**
 * Class that selects activities for a trip based on the user's preferences.
 *
 * @param tripSettings The settings for the trip.
 * @param onProgress A callback to report the progress of the activity selection.
 */
class SelectActivities(
    private val tripSettings: TripSettings,
    private val onProgress: (Float) -> Unit
) {

  /**
   * Selects activities for a trip based on the user's preferences.
   *
   * @return A [SelectedActivities] object containing the selected activities and destinations.
   */
  suspend fun addActivities(): SelectedActivities {
    val activityRepository = ActivityRepositoryMySwitzerland()
    var activities = mutableListOf<Activity>()
    val destinations =
        tripSettings.destinations.toMutableList() // TODO add locations along the route
    val arrivalDeparture = tripSettings.arrivalDeparture
    destinations.add(arrivalDeparture.arrivalLocation!!)
    destinations.add(arrivalDeparture.departureLocation!!)

    val preferences = tripSettings.preferences.toMutableList()

    val totalSteps = destinations.size + if (preferences.isNotEmpty()) preferences.size else 0
    var completedSteps = 0

    // Get activities near stops
    val allActivitiesNear = mutableListOf<Activity>()
    for (place in destinations) {
      val act = activityRepository.getActivitiesNear(place.coordinate, 15000, 20)
      allActivitiesNear.addAll(act)
      completedSteps++
      onProgress(completedSteps.toFloat() / totalSteps)
      sleep(SLEEP)
    }
    activities.addAll(allActivitiesNear.distinct())

    // Get activities with preferences
    if (preferences.isNotEmpty()) {
      val allActivitiesPref = mutableListOf<Activity>()

      val wheelChair = preferences.contains(Preference.WHEELCHAIR_ACCESSIBLE)
      val publicTransport = preferences.contains(Preference.PUBLIC_TRANSPORT)

      // Mandatory filters
      val always = mutableListOf<Preference>()

      if (wheelChair) {
        preferences.remove(Preference.WHEELCHAIR_ACCESSIBLE)
        always.add(Preference.WHEELCHAIR_ACCESSIBLE)
      }
      if (publicTransport) {
        preferences.remove(Preference.PUBLIC_TRANSPORT)
        always.add(Preference.PUBLIC_TRANSPORT)
      }

      for (preference in preferences) {
        val act = activityRepository.getActivitiesByPreferences(always + preference, 100)
        allActivitiesPref.addAll(act)
        completedSteps++
        onProgress(completedSteps.toFloat() / totalSteps)
        sleep(SLEEP)
      }
      allActivitiesPref.distinct()

      // Select activities that are near and respects user preferences
      activities =
          activities.filter { activity -> allActivitiesPref.contains(activity) }.toMutableList()
    }

    onProgress(1f) // Operation is complete

    return SelectedActivities(activities, destinations + activities.map { it.location })
  }
}
