package com.github.swent.swisstravel.algorithm.selectactivities

import android.util.Log
import com.github.swent.swisstravel.model.trip.Coordinate
import com.github.swent.swisstravel.model.trip.Location
import com.github.swent.swisstravel.model.trip.activity.Activity
import com.github.swent.swisstravel.model.trip.activity.ActivityRepository
import com.github.swent.swisstravel.model.trip.activity.ActivityRepositoryMySwitzerland
import com.github.swent.swisstravel.model.user.Preference
import com.github.swent.swisstravel.model.user.PreferenceCategories
import com.github.swent.swisstravel.ui.tripcreation.TripSettings
import java.time.temporal.ChronoUnit
import kotlin.math.ceil
import kotlinx.coroutines.delay

/**
 * Time in milliseconds to wait between consecutive API calls to avoid exceeding rate limits. The
 * MySwitzerland API allows a maximum of 1 request per second (with bursts up to 10/s).
 */
private const val API_CALL_DELAY_MS = 1000L

/** Distance to consider as near a point. */
private const val NEAR = 15000

/** Number of activities done in one day */
private const val NB_ACTIVITIES_PER_DAY = 3

/**
 * Selects activities for a trip based on user-defined settings and preferences. This class fetches
 * activities from a repository for specified destinations and filters them according to the user's
 * preferences.
 *
 * @param tripSettings The settings for the trip, including destinations and preferences.
 * @param activityRepository The repository to fetch activities from.
 */
class SelectActivities(
    private var tripSettings: TripSettings, // Changed val to var to allow updates
    private val activityRepository: ActivityRepository = ActivityRepositoryMySwitzerland()
) {

  /**
   * Fetches and selects activities based on the trip settings.
   *
   * @param cachedActivities A mutable list to store activities that were fetched but not returned.
   * @param activityBlackList A list with all the activities that are blackListed
   * @param onProgress A callback function to report the progress of the selection process (from 0.0
   *   to 1.0).
   * @return A list of [Activity] based on the user preferences and points of interest
   */
  suspend fun addActivities(
      cachedActivities: MutableList<Activity> = mutableListOf(),
      activityBlackList: List<String> = emptyList(),
      onProgress: (Float) -> Unit
  ): List<Activity> {
    val allDestinations = buildDestinationList()
    val userPreferences = tripSettings.preferences.toMutableList()

    // Removes preferences that are not supported by mySwitzerland.
    // Avoids unnecessary API calls.
    removeUnsupportedPreferences(userPreferences)

    // If the user didn't set any preference that are supported by the API -> add the basic ones to
    // make sure we still fetch some activities
    val finalPreferences =
        if (userPreferences.isEmpty()) {
          PreferenceCategories.activityTypePreferences
        } else {
          userPreferences
        }

    // add 1 day since the last day is excluded
    val days =
        ChronoUnit.DAYS.between(
            tripSettings.date.startDate, tripSettings.date.endDate!!.plusDays(1))
    val totalNbActivities = (NB_ACTIVITIES_PER_DAY) * days.toDouble()
    // Calculate the total number of steps for progress reporting.
    val totalSteps =
        allDestinations.size // we bundle all the activities together for each locations

    val numberOfActivityToFetchPerStep = ceil(totalNbActivities / totalSteps).toInt()
    var completedSteps = 0

    val filteredActivities =
        if (finalPreferences.isNotEmpty()) {
          val allFetchedActivities = mutableListOf<Activity>()
          for (destination in allDestinations) {
            val fetched =
                activityRepository.getActivitiesNearWithPreference(
                    finalPreferences,
                    destination.coordinate,
                    NEAR,
                    numberOfActivityToFetchPerStep,
                    activityBlackList,
                    cachedActivities)
            allFetchedActivities.addAll(fetched)
            // Update progress after each API call.
            completedSteps++
            onProgress(completedSteps.toFloat() / totalSteps)
            delay(API_CALL_DELAY_MS) // Respect API rate limit.
          }
          // Remove duplicate activities that may have been fetched for different preferences.
          allFetchedActivities.distinctBy { it.location }
        } else {
          // If no preferences are set, fetch general activities near each destination.
          val allFetchedActivities = mutableListOf<Activity>()
          for (destination in allDestinations) {
            val fetched =
                activityRepository.getActivitiesNear(
                    destination.coordinate,
                    NEAR,
                    numberOfActivityToFetchPerStep,
                    activityBlackList,
                    cachedActivities)
            allFetchedActivities.addAll(fetched)
            // Update progress after each API call.
            completedSteps++
            onProgress(completedSteps.toFloat() / totalSteps)
            delay(API_CALL_DELAY_MS) // Respect API rate limit.
          }
          allFetchedActivities.distinctBy { it.location }
        }

    Log.d("SelectActivities", "Found ${filteredActivities.size} activities: $filteredActivities")

    // Signal that the operation is complete.
    onProgress(1f)

    return filteredActivities
  }

  /**
   * Fetches multiple activities near the given location for testing purposes. If user preferences
   * are set, it tries to fetch an activity matching those preferences. Otherwise, it fetches any
   * activity near the location.
   *
   * @param coords The [Coordinate] around which to search for an activity.
   * @param radius The search radius in meters. Defaults to [NEAR].
   * @param limit The number of activities to fetch
   * @param activityBlackList A list of activity names to exclude from the search.
   * @param cachedActivities A mutable list to store activities that were fetched but not returned.
   * @return A single [Activity] found near the specified location or null.
   */
  suspend fun getActivitiesNearWithPreferences(
      coords: Coordinate,
      radius: Int = NEAR,
      limit: Int,
      activityBlackList: List<String> = emptyList(),
      cachedActivities: MutableList<Activity> = mutableListOf()
  ): List<Activity> {
    val userPreferences = tripSettings.preferences.toMutableList()
    removeUnsupportedPreferences(userPreferences)
    var fetched: List<Activity>?
    if (userPreferences.isNotEmpty()) {
      fetched =
          activityRepository.getActivitiesNearWithPreference(
              userPreferences, coords, NEAR, limit, activityBlackList, cachedActivities)
      delay(API_CALL_DELAY_MS) // Respect API rate limit.
    } else { // No preferences, fetch any activity near the location.
      fetched =
          activityRepository.getActivitiesNear(
              coords, radius, limit, activityBlackList, cachedActivities)
    }
    return fetched
  }

  /**
   * Removes preferences that are not supported by the activity repository to avoid unnecessary API
   * calls.
   *
   * @param preferences The mutable list of preferences to filter.
   */
  private fun removeUnsupportedPreferences(preferences: MutableList<Preference>) {
    preferences.remove(Preference.QUICK)
    preferences.remove(Preference.SLOW_PACE)
    preferences.remove(Preference.EARLY_BIRD)
    preferences.remove(Preference.NIGHT_OWL)
    preferences.remove(Preference.INTERMEDIATE_STOPS)
    preferences.remove(Preference.PUBLIC_TRANSPORT)
  }

  /**
   * Creates a comprehensive list of destinations for which to search activities. This includes
   * user-selected stops, as well as the trip's start and end points.
   *
   * @return A list of [Location] objects.
   */
  private fun buildDestinationList(): List<Location> {
    val allDestinations = tripSettings.destinations.toMutableList()
    tripSettings.arrivalDeparture.arrivalLocation?.let { allDestinations.add(it) }
    tripSettings.arrivalDeparture.departureLocation?.let { allDestinations.add(it) }
    return allDestinations.distinctBy { it.coordinate } // Ensure all locations are unique.
  }

  /**
   * Updates the preferences used for selecting activities.
   *
   * @param newPreferences The new list of preferences to apply.
   */
  fun updatePreferences(newPreferences: List<Preference>) {
    tripSettings = tripSettings.copy(preferences = newPreferences)
  }
}
