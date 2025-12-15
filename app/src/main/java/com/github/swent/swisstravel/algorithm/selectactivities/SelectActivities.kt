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
 * @param activityRepository The repository to fetch activities from.
 */
class SelectActivities(
    private var tripSettings: TripSettings,
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
    Log.d("SelectActivities", "Found ${filteredActivities.size} activities")

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
   * Done using AI
   *
   * Groups locations using a two-pass clustering strategy:
   * 1. Greedy Clustering: Group points within 5km of a dense center.
   * 2. Agglomerative Merging: Merge clusters if their centers are within 10km.
   * 3. Dynamic Radius: Adjust fetch radius to cover merged areas.
   *
   * @param locations The list of locations to group.
   * @return A list of SearchZone objects containing the center and radius.
   */
  private fun groupNearbyLocations(locations: List<Location>): List<SearchZone> {
    if (locations.isEmpty()) return emptyList()

    // --- Pass 1: Initial Greedy Clustering (5km) ---
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
        // Fallback (should theoretically not be reached if unvisited is not empty)
        val orphan = unvisited.removeAt(0)
        clusters.add(Cluster(mutableListOf(orphan)))
      }
    }

    // --- Pass 2: Merge Overlapping Clusters ---
    var merged = true
    while (merged) {
      merged = false
      // Find closest pair of clusters
      var bestPair: Pair<Cluster, Cluster>? = null
      var minDist = Double.MAX_VALUE

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

        // Combine points into a new cluster
        val newPoints = (c1.points + c2.points).toMutableList()
        clusters.add(Cluster(newPoints))

        merged = true // Continue checking for more merges
      }
    }

    // --- Pass 3: Convert to SearchZones with Dynamic Radius ---
    return clusters.map { cluster ->
      val center = cluster.center

      // Calculate radius: Distance to the furthest point + 15km buffer
      // Ensure it is at least the default NEAR radius (15km)
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
