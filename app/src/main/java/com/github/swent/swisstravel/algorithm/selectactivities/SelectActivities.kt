package com.github.swent.swisstravel.algorithm.selectactivities

import android.util.Log
import com.github.swent.swisstravel.model.trip.Coordinate
import com.github.swent.swisstravel.model.trip.Location
import com.github.swent.swisstravel.model.trip.activity.Activity
import com.github.swent.swisstravel.model.trip.activity.ActivityRepository
import com.github.swent.swisstravel.model.trip.activity.ActivityRepositoryMySwitzerland
import com.github.swent.swisstravel.model.user.Preference
import com.github.swent.swisstravel.model.user.PreferenceCategories
import com.github.swent.swisstravel.ui.trip.tripinfos.TripInfoViewModel
import com.github.swent.swisstravel.ui.trip.tripinfos.TripInfoViewModelContract
import com.github.swent.swisstravel.ui.tripcreation.TripSettings
import java.time.temporal.ChronoUnit
import kotlin.math.ceil
import kotlin.math.max
import kotlinx.coroutines.delay

// This file has been done with the help of AI
/**
 * Time in milliseconds to wait between consecutive API calls to avoid exceeding rate limits. The
 * MySwitzerland API allows a maximum of 1 request per second (with bursts up to 10/s).
 */
private const val API_CALL_DELAY_MS = 1000L

/** Distance to consider as near a point (default radius). */
private const val NEAR = 15000

/** Number of activities done in one day */
private const val NB_ACTIVITIES_PER_DAY = 3

/** Radius in km to group locations together in the first pass */
private const val GROUPING_RADIUS_KM = 5.0

/** Radius in km to merge clusters in the second pass */
private const val MERGE_CLUSTERS_RADIUS_KM = 10.0

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
    private var tripSettings: TripSettings = TripSettings(),
    private val tripInfoVM: TripInfoViewModelContract = TripInfoViewModel(),
    private val activityRepository: ActivityRepository = ActivityRepositoryMySwitzerland()
) {

  /**
   * Helper class to define a search zone.
   *
   * @param location The center of the search zone.
   * @param radius The radius in meters to search around the location.
   */
  private data class SearchZone(val location: Location, val radius: Int)

  /** Internal helper class to manage clusters during processing. */
  private data class Cluster(val points: MutableList<Location>) {
    val center: Coordinate
      get() {
        if (points.isEmpty()) return Coordinate(0.0, 0.0)
        val avgLat = points.map { it.coordinate.latitude }.average()
        val avgLon = points.map { it.coordinate.longitude }.average()
        return Coordinate(avgLat, avgLon)
      }
  }

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
    // Group destinations into search zones with dynamic radii
    val searchZones = groupNearbyLocations(buildDestinationList())
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
    val totalSteps = searchZones.size

    // Avoid division by zero if no zones (though unlikely with a valid trip)
    val numberOfActivityToFetchPerStep =
        if (totalSteps > 0) ceil(totalNbActivities / totalSteps).toInt() else 0
    var completedSteps = 0

    val allFetchedActivities = mutableListOf<Activity>()

    if (finalPreferences.isNotEmpty()) {
      for (zone in searchZones) {
        val fetched =
            activityRepository.getActivitiesNearWithPreference(
                finalPreferences,
                zone.location.coordinate,
                zone.radius, // Use dynamic radius
                numberOfActivityToFetchPerStep,
                activityBlackList,
                cachedActivities)
        allFetchedActivities.addAll(fetched)
        completedSteps++
        onProgress(completedSteps.toFloat() / max(1, totalSteps))
        delay(API_CALL_DELAY_MS)
      }
    }

    val filteredActivities = allFetchedActivities.distinctBy { it.location }

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
              userPreferences, coords, radius, limit, activityBlackList, cachedActivities)
      delay(API_CALL_DELAY_MS)
    } else {
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
      if (activitiesFetched.size > fetchLimit) break
      val fetched =
          if (prefs.isNotEmpty()) {
            activityRepository.getActivitiesNearWithPreference(
                prefs, loc.coordinate, NEAR, fetchLimit)
          } else {
            activityRepository.getActivitiesNear(loc.coordinate, NEAR, fetchLimit)
          }
      fetched.distinct()
      activitiesFetched.addAll(fetched)
      delay(API_CALL_DELAY_MS) // Respect API rate limit.
    }
    return activitiesFetched.distinct()
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
    return allDestinations.distinctBy { it.coordinate }
  }

  /**
   * Groups locations using a two-pass clustering strategy:
   * 1. Greedy Clustering: Group points within 5km of a dense center.
   * 2. Agglomerative Merging: Merge clusters if their centers are within 10km.
   * 3. Dynamic Radius: Adjust fetch radius to cover merged areas.
   *
   * Done using AI
   *
   * @param locations The list of locations to group.
   * @return A list of SearchZone objects containing the center and radius.
   */
  private fun groupNearbyLocations(locations: List<Location>): List<SearchZone> {
    if (locations.isEmpty()) return emptyList()

    // --- Pass 1: Initial Greedy Clustering (5km) ---
    val initialClusters = createInitialClusters(locations)

    // --- Pass 2: Merge Overlapping Clusters ---
    val mergedClusters = mergeOverlappingClusters(initialClusters)

    // --- Pass 3: Convert to SearchZones with Dynamic Radius ---
    return convertToSearchZones(mergedClusters)
  }

  /**
   * Performs the first pass of clustering using a greedy strategy.
   *
   * It iteratively selects the "densest" location (the one with the most neighbors within
   * [GROUPING_RADIUS_KM]) to form a new cluster, removing those points from the unvisited pool.
   * This process repeats until all locations are assigned to a cluster.
   *
   * @param locations The list of raw locations to group.
   * @return A mutable list of initial [Cluster] objects.
   */
  private fun createInitialClusters(locations: List<Location>): MutableList<Cluster> {
    val unvisited = locations.toMutableList()
    val clusters = mutableListOf<Cluster>()

    while (unvisited.isNotEmpty()) {
      var bestClusterPoints: List<Location> = emptyList()
      var bestSeed: Location? = null

      // Find the location that has the most neighbors within GROUPING_RADIUS_KM
      for (seed in unvisited) {
        val neighbors =
            unvisited.filter {
              it.coordinate.haversineDistanceTo(seed.coordinate) <= GROUPING_RADIUS_KM
            }

        if (neighbors.size > bestClusterPoints.size) {
          bestClusterPoints = neighbors
          bestSeed = seed
        }
      }

      if (bestClusterPoints.isNotEmpty() && bestSeed != null) {
        clusters.add(Cluster(bestClusterPoints.toMutableList()))
        unvisited.removeAll(bestClusterPoints)
      } else {
        // Fallback for isolated points
        val orphan = unvisited.removeAt(0)
        clusters.add(Cluster(mutableListOf(orphan)))
      }
    }
    return clusters
  }

  /**
   * Performs the second pass of clustering by merging clusters that are physically close to each
   * other.
   *
   * It iteratively finds the pair of clusters whose centers are closest and within
   * [MERGE_CLUSTERS_RADIUS_KM]. It merges them into a single cluster and repeats the process until
   * no two clusters are close enough to merge.
   *
   * @param initialClusters The list of clusters generated by the initial pass.
   * @return The consolidated list of [Cluster] objects.
   */
  private fun mergeOverlappingClusters(
      initialClusters: MutableList<Cluster>
  ): MutableList<Cluster> {
    val clusters = initialClusters
    var merged = true

    while (merged) {
      merged = false
      var bestPair: Pair<Cluster, Cluster>? = null
      var minDist = Double.MAX_VALUE

      // Find closest pair of clusters
      for (i in clusters.indices) {
        for (j in i + 1 until clusters.size) {
          val c1 = clusters[i]
          val c2 = clusters[j]
          val dist = c1.center.haversineDistanceTo(c2.center)

          if (dist <= MERGE_CLUSTERS_RADIUS_KM && dist < minDist) {
            minDist = dist
            bestPair = c1 to c2
          }
        }
      }

      // Merge if valid pair found
      if (bestPair != null) {
        val (c1, c2) = bestPair
        clusters.remove(c1)
        clusters.remove(c2)

        val newPoints = (c1.points + c2.points).toMutableList()
        clusters.add(Cluster(newPoints))

        merged = true // Restart loop to check for new merge opportunities
      }
    }
    return clusters
  }

  /**
   * Converts the final clusters into [SearchZone] objects with dynamic radii.
   *
   * The radius of each zone is calculated as the distance from the cluster center to its furthest
   * point plus a safety buffer ([NEAR]). This ensures that even scattered clusters are fully
   * covered by the search radius. The radius is clamped to a minimum of [NEAR].
   *
   * @param clusters The final list of merged clusters.
   * @return A list of [SearchZone] objects ready for the API search.
   */
  private fun convertToSearchZones(clusters: List<Cluster>): List<SearchZone> {
    return clusters.map { cluster ->
      val center = cluster.center

      // Calculate radius: Distance to the furthest point + buffer
      val maxDistKm =
          cluster.points.maxOfOrNull { it.coordinate.haversineDistanceTo(center) } ?: 0.0

      val computedRadius = ((maxDistKm * 1000) + NEAR).toInt()
      val finalRadius = max(NEAR, computedRadius)

      // Use the name of the first point or a generic name
      val representativeName = cluster.points.firstOrNull()?.name ?: "Area"

      SearchZone(
          Location(center, representativeName, cluster.points.firstOrNull()?.imageUrl), finalRadius)
    }
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
