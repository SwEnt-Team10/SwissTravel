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
          val allFetchedActivities = mutableListOf<Activity>()
          for (destination in allDestinations) {
            val fetched =
                activityRepository.getActivitiesNearWithPreference(
                    userPreferences, destination.coordinate, NEAR, numberOfActivityToFetchPerStep)
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
    if (userPreferences.isNotEmpty()) {
      fetched = activityRepository.getActivitiesNearWithPreference(userPreferences, coords, NEAR, 1)
      delay(API_CALL_DELAY_MS) // Respect API rate limit.
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
   * @param toExclude The set of activities to exclude from the fetched results.
   */
  suspend fun fetchSwipeActivities(toExclude: Set<Activity>): List<Activity> {
    val state = tripInfoVM.uiState.value
    val allDestinations = state.locations
    val prefs = state.tripProfile?.preferences?.toMutableList() ?: mutableListOf()
    val fetchedActivities = mutableListOf<Activity>()

    // Removes preferences that are not supported by mySwitzerland.
    // Avoids unnecessary API calls.
    removeUnsupportedPreferences(prefs)

    // limit of proposed activities
    val nbProposedActivities = 5

    // fetch activities with preferences
    val preferred =
        fetchFromLocations(locations = allDestinations, fetchLimit = nbProposedActivities, prefs)
            .filter { swipeActivityIsValid(it, toExclude) }
    fetchedActivities.addAll(preferred.shuffled())

    // if not enough activities, fetch activities without preferences
    if (fetchedActivities.size < nbProposedActivities) {
      val notPreferred =
          fetchFromLocations(locations = allDestinations, fetchLimit = nbProposedActivities)
              .filter { swipeActivityIsValid(it, toExclude) }
      fetchedActivities.addAll(notPreferred.shuffled())
    }
    fetchedActivities.distinct().take(nbProposedActivities)

    Log.d("SA_VM", "fetchedActivities = $fetchedActivities")
    Log.d("SA_VM", "nbFetched = ${fetchedActivities.size}")

    return fetchedActivities
  }

  /**
   * Fetches activities from MySwitzerland API at the given locations, with the given preferences
   *
   * @param locations The locations were to find activities
   * @param fetchLimit The maximum number of activities to return
   * @param prefs The preferences to filter activities (empty by default, so that it fetches without
   *   preferences)
   */
  suspend fun fetchFromLocations(
      locations: List<Location>,
      fetchLimit: Int,
      prefs: List<Preference> = emptyList()
  ): List<Activity> {
    val activitiesFetched = mutableListOf<Activity>()
    for (loc in locations) {
      val fetched =
          if (prefs.isNotEmpty()) {
            activityRepository.getActivitiesNearWithPreference(
                prefs, loc.coordinate, NEAR, fetchLimit)
          } else {
            activityRepository.getActivitiesNear(loc.coordinate, NEAR, fetchLimit)
          }
      activitiesFetched.addAll(fetched)
      delay(API_CALL_DELAY_MS) // Respect API rate limit.
    }
    return activitiesFetched
  }

  /**
   * The activity is valid if it is not already fetched.
   *
   * Usually, the activity should not be :
   * - already scheduled
   * - already fetched during the swipes
   *
   * @param activity The activity to check
   * @param alreadyFetched The set of locations that should not contain the activity
   * @return true iff the activity is not in the set
   */
  fun swipeActivityIsValid(activity: Activity, alreadyFetched: Set<Activity>): Boolean {
    return activity !in alreadyFetched
  }

  /**
   * Fetches one single activity to make the swipe more fluid. It cannot be :
   * - an already scheduled activity
   * - an already fetched activity
   *
   * @param toExclude The set of activities to exclude from the fetched results.
   * @param attempt The current attempt number to fetch a unique activity (maximum attempts is 10).
   */
  suspend fun fetchUniqueSwipe(toExclude: Set<Activity>, attempt: Int = 1): Activity? {
    if (attempt > 10) {
      Log.d("Select Activities", "Could not fetch a unique swipe activity after $attempt attempts")
      return null
    }
    val state = tripInfoVM.uiState.value
    val allDestinations = state.locations
    val prefs = state.tripProfile?.preferences?.toMutableList() ?: mutableListOf()

    // Removes preferences that are not supported by mySwitzerland.
    // Avoids unnecessary API calls.
    removeUnsupportedPreferences(prefs)

    // if prefs is empty, it will fetch without preferences
    val activity =
        fetchFromLocations(locations = listOf(allDestinations.random()), fetchLimit = 1, prefs)

    val filtered = activity.filter { act -> swipeActivityIsValid(act, toExclude) }

    Log.d("SA_VM", "filtered = $filtered")

    return if (filtered.isEmpty()) fetchUniqueSwipe(toExclude = toExclude, attempt + 1)
    else filtered.first()
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
}
