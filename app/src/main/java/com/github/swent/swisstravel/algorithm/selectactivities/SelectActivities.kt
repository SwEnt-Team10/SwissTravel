package com.github.swent.swisstravel.algorithm.selectactivities

import android.util.Log
import com.github.swent.swisstravel.model.trip.Coordinate
import com.github.swent.swisstravel.model.trip.Location
import com.github.swent.swisstravel.model.trip.activity.Activity
import com.github.swent.swisstravel.model.trip.activity.ActivityRepository
import com.github.swent.swisstravel.model.trip.activity.ActivityRepositoryMySwitzerland
import com.github.swent.swisstravel.model.user.Preference
import com.github.swent.swisstravel.ui.trip.tripinfos.TripInfoViewModel
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
 * @param tripInfoVM The information of the trip (This parameter is used to fetch activities if you
 *   don't have access to the tripSettings anymore, e.g. when the trip is already created)
 * @param activityRepository The repository to fetch activities from.
 */
class SelectActivities(
    private val tripSettings: TripSettings = TripSettings(),
    private val tripInfoVM: TripInfoViewModelContract = TripInfoViewModel(),
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
    removeUnsupportedPreferences(userPreferences)

    // add 1 day since the last day is excluded
    val days =
        ChronoUnit.DAYS.between(
            tripSettings.date.startDate, tripSettings.date.endDate!!.plusDays(1))
    // Estimate the total number of activities needed for the trip.
    // If there are intermediate stops, we reduce the total number of activities by estimating that
    // one activity will be done between two very distant locations.
    val totalNbActivities =
        if (userPreferences.contains(Preference.INTERMEDIATE_STOPS)) {
          NB_ACTIVITIES_PER_DAY * days.toDouble() - tripSettings.destinations.size
        } else {
          (NB_ACTIVITIES_PER_DAY) * days.toDouble()
        }
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
   * Fetches one activity near the given location for testing purposes. If user preferences are set,
   * it tries to fetch an activity matching those preferences. Otherwise, it fetches any activity
   * near the location.
   *
   * @param coords The [Coordinate] around which to search for an activity.
   * @param radius The search radius in meters. Defaults to [NEAR].
   * @return A single [Activity] found near the specified location or null.
   */
  suspend fun getOneActivityNearWithPreferences(coords: Coordinate, radius: Int = NEAR): Activity? {
    val userPreferences = tripSettings.preferences.toMutableList()
    removeUnsupportedPreferences(userPreferences)
    var fetched: List<Activity>?
    // If the user has preferences, separate mandatory and optional ones and fetch accordingly.
    if (userPreferences.isNotEmpty()) {
      val (mandatoryPrefs, optionalPrefs) = separateMandatoryPreferences(userPreferences)
      if (optionalPrefs.isNotEmpty()) {
        fetched =
            activityRepository.getActivitiesNearWithPreference(
                mandatoryPrefs + optionalPrefs, coords, NEAR, 1)
        delay(API_CALL_DELAY_MS) // Respect API rate limit.
      } else {
        fetched =
            activityRepository.getActivitiesNearWithPreference(mandatoryPrefs, coords, NEAR, 1)
        delay(API_CALL_DELAY_MS) // Respect API rate limit.
      }
    } else { // No preferences, fetch any activity near the location.
      fetched = activityRepository.getActivitiesNear(coords, radius, 1)
    }
    return fetched.firstOrNull()
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
  suspend fun fetchSwipeActivities(): List<Activity> {
    val state = tripInfoVM.uiState.value
    val allDestinations = state.locations
    val prefs = state.tripProfile?.preferences?.toMutableList() ?: mutableListOf()
    val fetchedActivities = mutableListOf<Activity>()
    val (mandatoryPrefs, optionalPrefs) = separateMandatoryPreferences(prefs)

    // Removes preferences that are not supported by mySwitzerland.
    // Avoids unnecessary API calls.
    removeUnsupportedPreferences(prefs)

    // activities to exclude
    val liked = state.likedActivities.map { it.location }.toSet()
    val scheduled = state.activities.map { it.location }.toSet()

    // limit of proposed activities
    val nbProposedActivities = state.activities.size * 2

    // fetch activities with preferences
    val preferred =
        fetchWithPrefs(
                locations = allDestinations,
                fetchLimit = nbProposedActivities,
                mandatoryPrefs,
                optionalPrefs)
            .filter { it.location !in liked && it.location !in scheduled }
    fetchedActivities.addAll(preferred.shuffled())

    // if not enough activities, fetch activities without preferences
    if (fetchedActivities.size < nbProposedActivities) {
      val notPreferred =
          fetchWithoutPrefs(locations = allDestinations, fetchLimit = nbProposedActivities).filter {
            it.location !in liked && it.location !in scheduled
          }
      fetchedActivities.addAll(notPreferred.shuffled())
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
   * @param mandatoryPrefs Preferences that are mandatory (this function should not fetch any
   *   activity that don't have these preferences)
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
  suspend fun fetchWithoutPrefs(locations: List<Location>, fetchLimit: Int): List<Activity> {
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
