package com.github.swent.swisstravel.model.trip

data class Location(val coordinate: Coordinate, val name: String, val imageUrl: String? = null) {

  /**
   * Computes the distance between two coordinates in km using the Haversine formula.
   *
   * @param l The other location.
   * @return The distance in km rounded to two decimal.
   */
  fun haversineDistanceTo(l: Location): Double {
    return coordinate.haversineDistanceTo(l.coordinate)
  }

  /**
   * Checks if this location is within a certain distance of any location in the given list.
   *
   * @param locations The list of locations to check against.
   * @param minDistance The minimum distance in kilometers.
   * @param maxDistance The maximum distance in kilometers.
   * @return True if the location is within the specified distance range of at least one location in
   *   the list.
   */
  fun isWithinDistanceOfAny(
      locations: List<Location>,
      minDistance: Double,
      maxDistance: Double
  ): Boolean {
    return locations.any {
      val distance = haversineDistanceTo(it)
      distance > minDistance && distance < maxDistance
    }
  }

  /**
   * Compare two Locations by coordinate with a tiny epsilon tolerance.
   *
   * @param b Second location.
   * @return true if the locations are the same within the tolerance, false otherwise.
   */
  fun sameLocation(b: Location): Boolean {
    return coordinate.sameLocation(b.coordinate)
  }
}
