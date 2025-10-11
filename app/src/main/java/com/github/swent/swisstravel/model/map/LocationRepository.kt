package com.github.swent.swisstravel.model.map
import com.github.swent.swisstravel.model.trip.Location

interface LocationRepository {
    suspend fun search(query: String): List<Location>
}