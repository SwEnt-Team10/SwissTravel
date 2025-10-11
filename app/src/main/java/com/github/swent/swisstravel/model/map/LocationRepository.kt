package com.github.swent.swisstravel.model.map

interface LocationRepository {
    suspend fun search(query: String): List<Location>
}