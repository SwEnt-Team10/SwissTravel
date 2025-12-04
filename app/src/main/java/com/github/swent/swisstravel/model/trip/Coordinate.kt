package com.github.swent.swisstravel.model.trip

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

private const val R = 6371.0 // Radius of the earth in km

data class Coordinate(val latitude: Double, val longitude: Double) {
  /**
   * Computes the distance between two coordinates in km using the Haversine formula.
   *
   * @param c The other coordinate.
   * @return The distance in km rounded to two decimal.
   *   https://en.wikipedia.org/wiki/Haversine_formula
   *   https://www.movable-type.co.uk/scripts/latlong.html
   */
  fun haversineDistanceTo(c: Coordinate): Double {
    val dLat = Math.toRadians(latitude - c.latitude) // Distance between latitudes
    val dLon = Math.toRadians(longitude - c.longitude) // Distance between longitudes

    val hav =
        sin(dLat / 2).pow(2) +
            cos(Math.toRadians(latitude)) * cos(Math.toRadians(c.latitude)) * sin(dLon / 2).pow(2)
    val distance = 2 * R * atan2(sqrt(hav), sqrt(1 - hav))
    return (distance * 100).roundToInt() / 100.0
  }

  /**
   * Compare two Coordinates by latitude & longitude with tolerance.
   *
   * @param b Second coordinate.
   * @return true if the coordinates are the same within the tolerance, false otherwise.
   */
  fun sameLocation(b: Coordinate): Boolean {
    // Tolerance in degrees — roughly ~1 meter
    val epsilon = 0.00001

    return abs(latitude - b.latitude) < epsilon && abs(longitude - b.longitude) < epsilon
  }
}
