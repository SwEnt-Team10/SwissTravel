package com.github.swent.swisstravel.model.trip.activity

import com.github.swent.swisstravel.model.trip.Coordinate
import com.github.swent.swisstravel.model.user.UserPreference

interface ActivityRepository {

  /**
   * Fetches a list of the most popular activities.
   *
   * @param limit Maximum number of activities to return.
   * @return List of activities (could be empty if none found or request fails).
   */
  suspend fun getMostPopularActivities(limit: Int = 5): List<Activity>

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
      preferences: List<UserPreference>,
      limit: Int = 5
  ): List<Activity>
}
