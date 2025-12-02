package com.github.swent.swisstravel.algorithm.selectactivities

import android.util.Log
import com.github.swent.swisstravel.model.trip.Location
import com.github.swent.swisstravel.model.trip.activity.Activity
import com.github.swent.swisstravel.model.trip.activity.ActivityRepository
import com.github.swent.swisstravel.model.trip.activity.ActivityRepositoryMySwitzerland
import com.github.swent.swisstravel.model.user.Preference
import com.github.swent.swisstravel.ui.trip.tripinfos.TripInfoViewModelContract
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
    private val tripSettings: TripSettings,
    private val activityRepository: ActivityRepository = ActivityRepositoryMySwitzerland()
) {

  /**
   * Fetches and selects activities based on the trip settings.
   *
   * @param onProgress A callback function to report the progress of the selection process (from 0.0
   *   to 1.0).
   * @return A list of [Activity] based on the user preferences and points of interest
   */
  suspend fun addActivities(onProgress: (Float) -> Unit): List<Activity> {
    val allDestinations = buildDestinationList()
    val userPreferences = tripSettings.preferences.toMutableList()

    // Removes preferences that are not supported by mySwitzerland.
    // Avoids unnecessary API calls.
    userPreferences.remove(Preference.QUICK)
    userPreferences.remove(Preference.SLOW_PACE)
    userPreferences.remove(Preference.EARLY_BIRD)
    userPreferences.remove(Preference.NIGHT_OWL)

    // add 1 day since the last day is excluded
    val days =
        ChronoUnit.DAYS.between(
            tripSettings.date.startDate, tripSettings.date.endDate!!.plusDays(1))
    val totalNbActivities = (NB_ACTIVITIES_PER_DAY * days).toDouble()

    // Calculate the total number of steps for progress reporting.
    val totalSteps = totalSteps(allDestinations.size, userPreferences.size)

    val numberOfActivityToFetchPerStep = ceil(totalNbActivities / totalSteps).toInt()
    var completedSteps = 0

    val filteredActivities =
        if (userPreferences.isNotEmpty()) {
          val (mandatoryPrefs, optionalPrefs) = separateMandatoryPreferences(userPreferences)

          // Fetch all activities that match any of the optional preferences for each destination.
          val allFetchedActivities = mutableListOf<Activity>()
          for (destination in allDestinations) {
            val fetched =
                activityRepository.getActivitiesNearWithPreference(
                    mandatoryPrefs + optionalPrefs,
                    destination.coordinate,
                    NEAR,
                    numberOfActivityToFetchPerStep)
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
   * Fetches a list of activities from mySwitzerland API, with these properties :
   * - the list has twice as many activities as the ones automatically scheduled for the trip
   * - first, it fills the list with activities that correspond to the preferences
   * - then, if there is not enough activities, propose some that don't necessarily correspond to
   *   the preferences (until there is twice as many as the scheduled ones)
   * - every activity in this list should NOT be in the scheduled activities and NOT in the liked
   *   activities
   * - the locations from which these activities are fetched is the entire list of locations of the
   *   trip (from the user's input)
   *
   * Done with the help of ChatGPT
   *
   * @param tripInfoVM The tripInfoViewModel, used for :
   * - the locations of the trip
   * - the likedActivities
   * - the scheduled activities
   */
  suspend fun fetchSwipeActivities(tripInfoVM: TripInfoViewModelContract): List<Activity> {
    val allDestinations = buildDestinationList()
    val prefs = tripSettings.preferences.toMutableList()
    val fetchedActivities = mutableListOf<Activity>()
    val (mandatoryPrefs, optionalPrefs) = separateMandatoryPreferences(prefs)
    val state = tripInfoVM.uiState.value

    // Removes preferences that are not supported by mySwitzerland.
    // Avoids unnecessary API calls.
    prefs.remove(Preference.QUICK)
    prefs.remove(Preference.SLOW_PACE)
    prefs.remove(Preference.EARLY_BIRD)
    prefs.remove(Preference.NIGHT_OWL)

    // activities to exclude
    val liked = state.likedActivities.map { it.location }.toSet()
    val scheduled = state.activities.map { it.location }.toSet()

    // limit of proposed activities (twice the amount of activities already scheduled)
    val nbProposedActivities = state.activities.size * 2

    // fetch activities with preferences
    val preferred =
        fetchWithPrefs(
                locations = allDestinations,
                fetchLimit = nbProposedActivities,
                mandatoryPrefs,
                optionalPrefs)
            .filter { it.location !in liked && it.location !in scheduled }
    fetchedActivities.addAll(preferred)

    // if not enough activities, fetch and add activities without preferences
    if (fetchedActivities.size < nbProposedActivities) {
      val notPreferred =
          fetchWithoutPrefs(locations = allDestinations, fetchLimit = nbProposedActivities).filter {
            it.location !in liked && it.location !in scheduled
          }
      fetchedActivities.addAll(notPreferred)
    }

    return fetchedActivities
        // remove duplicates (by the name of the activity)
        .distinctBy { it.getName() }
        .take(nbProposedActivities)
  }

  /**
   * Fetches activities from MySwitzerland API at the given locations, with the given preferences
   *
   * @param locations The locations were to find activities
   * @param fetchLimit The maximum number of activities to return
   * @param mandatoryPrefs Preferences that are mandatory (this function should not fetch any activity that don't have these preferences)
   * @param optionalPrefs Preferences that are optional
   */
  suspend fun fetchWithPrefs(
      locations: List<Location>,
      fetchLimit: Int,
      mandatoryPrefs: List<Preference>,
      optionalPrefs: List<Preference>
  ): List<Activity> {
    val activitiesFetched = mutableListOf<Activity>()
    for (loc in locations) {
      val fetched =
          activityRepository.getActivitiesNearWithPreference(
              mandatoryPrefs + optionalPrefs, loc.coordinate, NEAR, fetchLimit)
      activitiesFetched.addAll(fetched)
      delay(API_CALL_DELAY_MS) // Respect API rate limit.
    }
    return activitiesFetched
  }

  /**
   * Fetches activities from MySwitzerland API at the given locations, without any preferences
   *
   * @param locations The locations were to find activities
   * @param fetchLimit The maximum number of activities to return
   */
  suspend fun fetchWithoutPrefs(
      locations: List<Location>,
      fetchLimit: Int
  ): List<Activity> {
    val activitiesFetched = mutableListOf<Activity>()
    for (loc in locations) {
      val fetched = activityRepository.getActivitiesNear(loc.coordinate, NEAR, fetchLimit)
      activitiesFetched.addAll(fetched)
      delay(API_CALL_DELAY_MS) // Respect API rate limit.
    }
    return activitiesFetched
  }

  /**
   * Calculates the total number of steps based on the number of destinations and preferences.
   *
   * @param nbDestinations The number of destinations.
   * @param nbPreferences The number of user preferences.
   */
  private fun totalSteps(nbDestinations: Int, nbPreferences: Int): Int {
    return if (nbPreferences > 0) {
      // Each destination-preference pair counts as a step.
      nbPreferences * nbDestinations
    } else {
      // If no preferences, each destination counts as one step.
      nbDestinations
    }
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
    return allDestinations.distinctBy { it.coordinate } // Ensure all locations are unique.
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
