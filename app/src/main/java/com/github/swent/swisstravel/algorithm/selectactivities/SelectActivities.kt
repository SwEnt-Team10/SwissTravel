package com.github.swent.swisstravel.algorithm.selectactivities

import com.github.swent.swisstravel.model.trip.Coordinate
import com.github.swent.swisstravel.model.trip.Location
import com.github.swent.swisstravel.model.trip.Trip
import com.github.swent.swisstravel.model.trip.TripProfile
import com.github.swent.swisstravel.model.trip.activity.Activity
import com.github.swent.swisstravel.model.trip.activity.ActivityRepository
import com.github.swent.swisstravel.model.trip.activity.ActivityRepositoryMySwitzerland
import com.github.swent.swisstravel.model.trip.activity.CityConfig
import com.github.swent.swisstravel.model.trip.activity.NUMBER_ACTIVITIES_TO_FETCH
import com.github.swent.swisstravel.model.user.Preference
import com.github.swent.swisstravel.model.user.PreferenceCategories
import com.github.swent.swisstravel.model.user.PreferenceCategories.category
import com.github.swent.swisstravel.ui.tripcreation.TripSettings
import java.time.temporal.ChronoUnit
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.delay

// This file has been done with the help of AI
/**
 * Time in milliseconds to wait between consecutive API calls to avoid exceeding rate limits. The
 * MySwitzerland API allows a maximum of 1 request per second (with bursts up to 10/s).
 */
private const val API_CALL_DELAY_MS = 1000L

/** Distance to consider as near a point (default radius). */
private const val NEAR = 15000

/** Distance to consider for fetching new activities to put in the swipe activities */
private const val FETCHING_RADIUS_FOR_SWIPE = 25000

/** Number of activities done in one day */
private const val NB_ACTIVITIES_PER_DAY = 3

/** Radius in km to group locations together in the first pass */
private const val GROUPING_RADIUS_KM = 5.0

/** Radius in km to merge clusters in the second pass */
private const val MERGE_CLUSTERS_RADIUS_KM = 10.0

private const val NUMBER_OF_CITIES_MAX = 7
// Arbitrary limits to stop fetching if the user has swiped too much (saving API calls)
private const val NUMBER_OF_LIKED_MAX = 28
private const val NUMBER_OF_FETCH_MAX = 75
private const val QUEUE_SIZE_BEFORE_FETCH = 3

/**
 * Selects activities for a trip based on user-defined settings and preferences. This class fetches
 * activities from a repository for specified destinations and filters them according to the user's
 * preferences.
 *
 * @param tripSettings The settings for the trip, including destinations and preferences.
 * @param activityRepository The repository to fetch activities from.
 */
class SelectActivities(
    private var tripSettings: TripSettings = TripSettings(),
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
    addAllPreferences(userPreferences)

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

    // Should never be empty but check to be sure
    if (userPreferences.isNotEmpty()) {
      for (zone in searchZones) {
        val fetched =
            activityRepository.getActivitiesNearWithPreference(
                userPreferences,
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
   * Fetches activities to populate the swipe queue.
   *
   * Logic:
   * 1. Dump ALL valid activities from `cachedActivities` into the `activitiesQueue`.
   * 2. If queue is NOT empty, return (saving API calls).
   * 3. If queue IS empty, check limits (cities, liked, etc.).
   * 4. If limits allow, fetch a full batch (40) from a new Major City.
   * 5. Add ALL fetched activities to the queue (and mark city as fetched).
   *
   * @param trip The current trip.
   * @param majorCities The list of major Swiss cities configuration.
   * @return The updated trip with the new queue and potentially updated
   *   allFetchedLocations/cachedActivities.
   */
  suspend fun fetchSwipeActivities(trip: Trip, majorCities: List<CityConfig>): Trip {
    val currentQueue = trip.activitiesQueue.toMutableList()
    val fetchedLocations = trip.allFetchedLocations.toMutableList()
    val cachedActivities = trip.cachedActivities.toMutableList()
    val likedActivities = trip.likedActivities
    val allFetched = trip.allFetchedForSwipe
    val activities = trip.activities

    // ============================================================================================
    // 1. CACHE STRATEGY: DUMP EVERYTHING
    // ============================================================================================

    // We check against exclusion list to avoid duplicates
    val exclusionListForCache =
        (trip.activities + trip.likedActivities + trip.allFetchedForSwipe + currentQueue)
            .map { it.getName() }
            .toSet()

    val exclusionListForActivities =
        (trip.likedActivities + trip.allFetchedForSwipe + currentQueue).map { it.getName() }.toSet()

    // Find all activities in cache that are not already known
    val validFromCache = cachedActivities.filter { !exclusionListForCache.contains(it.getName()) }
    val validFromActivities =
        activities.filter { !exclusionListForActivities.contains(it.getName()) }

    // Add them all to the queue
    currentQueue.addAll(validFromCache)
    // Also add the ones that were already in the trip and that have never been in the queue
    currentQueue.addAll(validFromActivities)

    // remove duplicated from the queue
    val uniqueQueue =
        currentQueue.distinctBy { activity -> activity.getName() to activity.location }

    currentQueue.clear()
    currentQueue.addAll(uniqueQueue)

    // Clear the cache because we've consumed it into the queue
    cachedActivities.clear()

    // If queue is populated, STOP. We have content to show.
    if (currentQueue.isNotEmpty() && currentQueue.size > QUEUE_SIZE_BEFORE_FETCH) {
      return trip.copy(
          activitiesQueue = currentQueue, cachedActivities = cachedActivities // Empty now
          )
    }

    // ============================================================================================
    // 2. CHECK LIMITS BEFORE API CALL
    // ============================================================================================

    val startDate =
        trip.tripProfile.startDate
            .toDate()
            .toInstant()
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDate()
    val endDate =
        trip.tripProfile.endDate
            .toDate()
            .toInstant()
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDate()
    val duration = ChronoUnit.DAYS.between(startDate, endDate) + 1 // +1 because inclusive

    // Calculate the limit based on duration: min(ceil(days / 2), 7)
    val cityLimit = min(ceil(duration / 2.0) + 1, NUMBER_OF_CITIES_MAX.toDouble()).toInt()

    val canFetchMore =
        fetchedLocations.size < cityLimit &&
            likedActivities.size < NUMBER_OF_LIKED_MAX &&
            allFetched.size < NUMBER_OF_FETCH_MAX

    if (!canFetchMore) {
      // Limits reached, return what we have (empty queue if cache was empty)
      return trip.copy(activitiesQueue = currentQueue, cachedActivities = cachedActivities)
    }

    // ============================================================================================
    // 3. FETCH FROM MAJOR CITIES
    // ============================================================================================

    val prefs = trip.tripProfile.preferences.toMutableList()
    removeUnsupportedPreferences(prefs)
    addAllPreferences(prefs, true, trip.tripProfile)

    val newActivities = mutableListOf<Activity>()
    val tripDestinations = trip.locations

    // Define blacklist for API call (includes what we just added to queue, though queue is likely
    // empty here)
    val exclusionList =
        (trip.activities +
                trip.cachedActivities +
                trip.allFetchedForSwipe +
                likedActivities +
                currentQueue)
            .map { it.getName() }
            .toSet()

    // Filter out cities we have already fully explored/fetched from
    val availableCities =
        majorCities.filter { cityConfig ->
          fetchedLocations.none { it.name == cityConfig.location.name }
        }

    if (availableCities.isNotEmpty()) {
      // Find the closest available major city to any of the trip's destinations
      val closestCityConfig =
          availableCities.minByOrNull { city ->
            tripDestinations.minOfOrNull { dest ->
              dest.coordinate.haversineDistanceTo(city.location.coordinate)
            } ?: Double.MAX_VALUE
          }

      if (closestCityConfig != null) {
        // Fetch from this specific city.
        // We pass 'cachedActivities' but we also consume the return value immediately.
        val cityActivities =
            activityRepository.getActivitiesNearWithPreference(
                prefs,
                closestCityConfig.location.coordinate,
                FETCHING_RADIUS_FOR_SWIPE, // Fetch further than normal since the user has to choose
                // it will give them more options
                NUMBER_ACTIVITIES_TO_FETCH, // Fetch a full batch (40)
                exclusionList.toList(),
                cachedActivities // Mutable cache used for overflow (though we dump it all below
                // anyway)
                )

        // Filter unique ones for the current batch
        val uniqueFetched = cityActivities.filter { !exclusionList.contains(it.getName()) }
        newActivities.addAll(uniqueFetched)

        // Mark this city as fetched
        fetchedLocations.add(closestCityConfig.location)
      }
    }

    // ============================================================================================
    // 4. ADD EVERYTHING TO QUEUE
    // ============================================================================================

    // Add everything we just fetched to the queue
    currentQueue.addAll(newActivities)

    // Add anything that might have slipped into cache via the Repo call (unlikely if limit=40, but
    // safe to check) and wasn't in the returned list
    val extraFromCache =
        cachedActivities.filter {
          !exclusionList.contains(it.getName()) &&
              newActivities.none { new -> new.getName() == it.getName() }
        }
    currentQueue.addAll(extraFromCache)
    cachedActivities.clear()

    return trip.copy(
        activitiesQueue = currentQueue,
        allFetchedLocations = fetchedLocations,
        cachedActivities = cachedActivities // Empty
        )
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
    val mergedClusters = mergeOverlappingClusters(initialClusters).toList()

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

  /**
   * Ensures that the preferences list contains at least one preference from each major category
   * (Activity Type and Environment).
   *
   * If a category is missing (e.g., no specific environment selected), it implies that "any" is
   * acceptable, so all preferences of that category are added to the list.
   *
   * @param preferences The mutable list of preferences to validate and update.
   * @param forceAllPrefs If true, adds all preferences from both categories regardless of current
   *   selections. Defaults to false.
   * @param tripProfile The tripProfile of the trip
   */
  private fun addAllPreferences(
      preferences: MutableList<Preference>,
      forceAllPrefs: Boolean = false,
      tripProfile: TripProfile? = null
  ) {
    tripProfile?.let { profile ->
      when {
        profile.children > 0 -> preferences.add(Preference.CHILDREN_FRIENDLY)
        profile.adults == 1 -> preferences.add(Preference.INDIVIDUAL)
        profile.adults >= 3 -> preferences.add(Preference.GROUP)
        else -> Unit
      }
    }
    val activityTypeCount =
        preferences.count { it.category() == PreferenceCategories.Category.ACTIVITY_TYPE }
    val environmentCount =
        preferences.count { it.category() == PreferenceCategories.Category.ENVIRONMENT }

    when {
      // If forced OR both counts are zero, add everything
      forceAllPrefs || (activityTypeCount == 0 && environmentCount == 0) -> {
        preferences.addAll(PreferenceCategories.activityTypePreferences)
        preferences.addAll(PreferenceCategories.environmentPreferences)
      }
      // If only activity types are missing
      activityTypeCount == 0 -> {
        preferences.addAll(PreferenceCategories.activityTypePreferences)
      }
      // If only environment types are missing
      environmentCount == 0 -> {
        preferences.addAll(PreferenceCategories.environmentPreferences)
      }
      else -> Unit
    }
  }
}
