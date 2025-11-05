package com.github.swent.swisstravel.model.map

import android.util.Log
import androidx.compose.runtime.remember
import com.github.swent.swisstravel.model.trip.Location
import com.github.swent.swisstravel.model.trip.activity.Activity
import com.github.swent.swisstravel.model.trip.activity.ActivityRepositoryMySwitzerland
import kotlinx.coroutines.flow.MutableStateFlow

class MySwitzerlandLocationRepository(
    private val activityRepository: ActivityRepositoryMySwitzerland =
        ActivityRepositoryMySwitzerland()
) : LocationRepository {
  override suspend fun search(query: String): List<Location> {
    val temp = activityRepository.searchDestinations(query, 3)
    if (temp.isEmpty()) {Log.e("MySwitzerlandLocationRepository", "No results found")}
    else{ Log.d("MySwitzerlandLocationRepository", "Found ${temp.size} results")}
    return temp.map { activity ->
        print(activity.location.imageUrl)
        activity.location
    }
  }

}
