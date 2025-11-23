package com.github.swent.swisstravel.model.map

import com.github.swent.swisstravel.model.trip.Location

/** Repository interface for searching locations. */
fun interface LocationRepository {
  /**
   * Searches for locations matching the given query.
   *
   * @param query The search query.
   * @return A list of matching locations.
   */
  suspend fun search(query: String): List<Location>
}
