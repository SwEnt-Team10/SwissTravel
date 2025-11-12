package com.github.swent.swisstravel.algorithm.selectactivities

import android.util.Log
import com.github.swent.swisstravel.model.trip.Location
import com.github.swent.swisstravel.model.trip.activity.Activity
import com.github.swent.swisstravel.model.trip.activity.ActivityRepository
import com.github.swent.swisstravel.model.trip.activity.ActivityRepositoryMySwitzerland
import com.github.swent.swisstravel.model.user.Preference
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
private const val NB_ACTIVITIES_PER_DAY = 4

/**
 * Selects activities for a trip based on user-defined settings and preferences. This class fetches
 * activities from a repository for specified destinations and filters them according to the user's
 * preferences.
 *
 * @param tripSettings The settings for the trip, including destinations and preferences.
 * @param onProgress A callback function to report the progress of the selection process (from 0.0
 *   to 1.0).
 * @param activityRepository The repository to fetch activities from.
 */
class SelectActivities(
    private val tripSettings: TripSettings,
    private val onProgress: (Float) -> Unit,
    private val activityRepository: ActivityRepository = ActivityRepositoryMySwitzerland()
) {

  /**
   * Fetches and selects activities based on the trip settings.
   *
   * @return A list of [Activity] based on the user preferences and points of interest
   */
  suspend fun addActivities(): List<Activity> {
    val allDestinations = buildDestinationList()
    val userPreferences = tripSettings.preferences.toMutableList()
    val days = ChronoUnit.DAYS.between(tripSettings.date.startDate, tripSettings.date.endDate)
    val totalNbActivities = (NB_ACTIVITIES_PER_DAY * days).toDouble()

    // Calculate the total number of steps for progress reporting.
    val totalSteps =
        if (userPreferences.isNotEmpty()) {
          // Each destination-preference pair counts as a step.
          userPreferences.size * allDestinations.size
        } else {
          // If no preferences, each destination counts as one step.
          allDestinations.size
        }

    val numberOfActivityToFetchPerStep = ceil(totalNbActivities / totalSteps).toInt()
    var completedSteps = 0

    val filteredActivities =
        if (userPreferences.isNotEmpty()) {
          val (mandatoryPrefs, optionalPrefs) = separateMandatoryPreferences(userPreferences)

          // Fetch all activities that match any of the optional preferences for each destination.
          val allFetchedActivities = mutableListOf<Activity>()
          for (destination in allDestinations) {
            if (optionalPrefs.isNotEmpty()) {
              for (preference in optionalPrefs) {
                val fetched =
                    activityRepository.getActivitiesNearWithPreference(
                        mandatoryPrefs + preference,
                        destination.coordinate,
                        NEAR,
                        numberOfActivityToFetchPerStep)
                allFetchedActivities.addAll(fetched)
                // Update progress after each API call.
                completedSteps++
                onProgress(completedSteps.toFloat() / totalSteps)
                delay(API_CALL_DELAY_MS) // Respect API rate limit.
              }
            } else {
              val fetched =
                  activityRepository.getActivitiesNearWithPreference(
                      mandatoryPrefs, destination.coordinate, NEAR, numberOfActivityToFetchPerStep)
              allFetchedActivities.addAll(fetched)
              completedSteps++
              onProgress(completedSteps.toFloat() / totalSteps)
              delay(API_CALL_DELAY_MS) // Respect API rate limit.
            }
          }
          // Remove duplicate activities that may have been fetched for different preferences.
          allFetchedActivities.distinctBy { it.location }
        } else {
          // If no preferences are set, fetch general activities near each destination.
          val allFetchedActivities = mutableListOf<Activity>()
          for (destination in allDestinations) {
            val fetched =
                activityRepository.getActivitiesNear(
                    destination.coordinate, NEAR, numberOfActivityToFetchPerStep)
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
   * Creates a comprehensive list of destinations for which to search activities. This includes
   * user-selected stops, as well as the trip's start and end points.
   *
   * @return A list of [Location] objects.
   */
  private fun buildDestinationList(): List<Location> {
    val allDestinations = tripSettings.destinations.toMutableList()
    // TODO: Implement logic to add locations along the route between destinations.
    tripSettings.arrivalDeparture.arrivalLocation?.let { allDestinations.add(it) }
    tripSettings.arrivalDeparture.departureLocation?.let { allDestinations.add(it) }
    return allDestinations.distinct() // Ensure all locations are unique.
  }

  /**
   * Separates user preferences into mandatory and optional categories. Mandatory preferences (e.g.,
   * wheelchair accessibility) are applied to all searches.
   *
   * @param preferences The list of user preferences.
   * @return A Pair containing a list of mandatory preferences and a list of optional ones.
   */
  private fun separateMandatoryPreferences(
      preferences: MutableList<Preference>
  ): Pair<List<Preference>, List<Preference>> {
    val mandatoryPrefs = mutableListOf<Preference>()

    if (preferences.contains(Preference.WHEELCHAIR_ACCESSIBLE)) {
      mandatoryPrefs.add(Preference.WHEELCHAIR_ACCESSIBLE)
      preferences.remove(Preference.WHEELCHAIR_ACCESSIBLE)
    }
    if (preferences.contains(Preference.PUBLIC_TRANSPORT)) {
      mandatoryPrefs.add(Preference.PUBLIC_TRANSPORT)
      preferences.remove(Preference.PUBLIC_TRANSPORT)
    }

    return Pair(mandatoryPrefs, preferences)
  }
}
