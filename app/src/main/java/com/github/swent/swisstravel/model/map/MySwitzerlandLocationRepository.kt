package com.github.swent.swisstravel.model.map

import com.github.swent.swisstravel.model.trip.Location
import com.github.swent.swisstravel.model.trip.activity.ActivityRepositoryMySwitzerland

class MySwitzerlandLocationRepository(
    private val activityRepository: ActivityRepositoryMySwitzerland = ActivityRepositoryMySwitzerland()
) : LocationRepository {
    override suspend fun search(query: String): List<Location> {
        return activityRepository.getDestinationByName(query, 3)
    }
}
