package com.github.swent.swisstravel.data.trips

data class Location(val coordinate: Coordinate, val name: String) {

  /**
   * Computes the distance between two coordinates in km using the Haversine formula.
   *
   * @param l The other location.
   * @return The distance in km rounded to two decimal.
   */
  fun haversineDistanceTo(l: Location): Double {
    return coordinate.haversineDistanceTo(l.coordinate)
  }
}
