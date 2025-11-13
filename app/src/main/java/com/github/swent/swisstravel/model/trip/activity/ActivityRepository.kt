package com.github.swent.swisstravel.model.trip.activity

import com.github.swent.swisstravel.model.trip.Coordinate
import com.github.swent.swisstravel.model.user.Preference

interface ActivityRepository {

  /**
   * Fetches a list of the most popular activities.
   *
   * @param limit Maximum number of activities to return.
   * @param page Page number of the response.
   * @return List of activities (could be empty if none found or request fails).
   */
  suspend fun getMostPopularActivities(limit: Int = 5, page: Int = 0): List<Activity>

  /**
   * Fetches a list of activities near a specific coordinate.
   *
   * @param coordinate The central coordinate to search around.
   * @param radiusMeters Search radius in meters.
   * @param limit Maximum number of activities to return.
   * @return List of activities (could be empty if none found or request fails).
   */
  suspend fun getActivitiesNear(
      coordinate: Coordinate,
      radiusMeters: Int = 5000,
      limit: Int = 5
  ): List<Activity>

  /**
   * Fetches activities by category (e.g. "hiking", "museum") optionally filtered by area.
   *
   * @param category The category of activities to search for.
   * @param limit Maximum number of activities to return.
   * @return List of activities (could be empty if none found or request fails).
   */
  suspend fun getActivitiesByPreferences(
      preferences: List<Preference>,
      limit: Int = 5
  ): List<Activity>

  /**
   * Searches for destinations (locations) based on a text query.
   *
   * This is typically used for autocomplete features where a user is typing a location name.
   *
   * @param query The search string entered by the user.
   * @param limit The maximum number of destination suggestions to return. Defaults to 3.
   * @return A list of [Activity] objects representing potential destinations. The list could be
   *   empty if no matches are found or if the request fails.
   */
  suspend fun searchDestinations(query: String, limit: Int = 3): List<Activity>

  /**
   * Get activities near the given coordinate with the given preferences.
   *
   * @param preferences The preferences to use.
   * @param coordinate The coordinate to get activities near.
   * @param radiusMeters The radius in meters to search for activities.
   * @param limit The limit of the number of activities to return.
   * @return A list of activities near the given coordinate with the given preferences.
   */
  suspend fun getActivitiesNearWithPreference(
      preferences: List<Preference>,
      coordinate: Coordinate,
      radiusMeters: Int,
      limit: Int
  ): List<Activity>
}
