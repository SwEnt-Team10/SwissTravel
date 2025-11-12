package com.github.swent.swisstravel.ui.tripcreation

import com.github.swent.swisstravel.model.trip.Coordinate
import com.github.swent.swisstravel.model.trip.activity.Activity
import com.github.swent.swisstravel.model.trip.activity.ActivityRepository
import com.github.swent.swisstravel.model.user.Preference

/**
 * A fake implementation of ActivityRepository for testing purposes. It avoids making real API
 * calls.
 */
class FakeActivityRepository : ActivityRepository {
  override suspend fun getMostPopularActivities(limit: Int, page: Int): List<Activity> {
    return emptyList()
  }

  override suspend fun getActivitiesNear(
      coordinate: Coordinate,
      radiusMeters: Int,
      limit: Int
  ): List<Activity> {
    return emptyList()
  }

  override suspend fun getActivitiesByPreferences(
      preferences: List<Preference>,
      limit: Int
  ): List<Activity> {
    return emptyList()
  }

  override suspend fun searchDestinations(query: String, limit: Int): List<Activity> {
    return emptyList()
  }

  override suspend fun getActivitiesNearWithPreference(
      preferences: List<Preference>,
      coordinate: Coordinate,
      radiusMeters: Int,
      limit: Int
  ): List<Activity> {
    // This is the function called by SelectActivities.
    // Return an empty list or a specific list of mock activities if needed for your test.
    return emptyList()
  }
}
