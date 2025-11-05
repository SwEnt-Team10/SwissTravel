package com.github.swent.swisstravel.model.map

import android.util.Log
import com.github.swent.swisstravel.model.trip.Location
import com.github.swent.swisstravel.model.trip.activity.ActivityRepositoryMySwitzerland

/**
 * An implementation of the [LocationRepository] that uses the MySwitzerland API as its data source.
 *
 * This repository is responsible for searching for geographical locations. It achieves this by
 * leveraging the [ActivityRepositoryMySwitzerland], which queries the 'destinations' endpoint of
 * the API. The results, which are originally [Activity] objects, are then mapped to the simpler
 * [Location] data class required by the application.
 *
 * @param activityRepository An instance of [ActivityRepositoryMySwitzerland] that will be used to
 *   fetch the raw destination data from the API. This is passed as a parameter to allow for
 *   dependency injection and easier testing.
 */
class MySwitzerlandLocationRepository(
    private val activityRepository: ActivityRepositoryMySwitzerland =
        ActivityRepositoryMySwitzerland()
) : LocationRepository {
  override suspend fun search(query: String): List<Location> {
    val temp = activityRepository.searchDestinations(query, 3)
    if (temp.isEmpty()) {
      Log.e("MySwitzerlandLocationRepository", "No results found")
    } else {
      Log.d("MySwitzerlandLocationRepository", "Found ${temp.size} results")
    }
    return temp.map { activity ->
      print(activity.location.imageUrl)
      activity.location
    }
  }
}
