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
   * Compare two Locations by coordinate with a tiny epsilon tolerance.
   *
   * @param b Second location.
   * @return true if the locations are the same within the tolerance, false otherwise.
   */
  fun sameLocation(b: Location): Boolean {
    return coordinate.sameLocation(b.coordinate)
  }
}
