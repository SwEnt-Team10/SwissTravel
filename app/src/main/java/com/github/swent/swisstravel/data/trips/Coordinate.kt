package com.github.swent.swisstravel.data.trips

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class Coordinate(val x: Double, val y: Double) {

    /**
     * Computes the distance between two coordinates in km using the Haversine formula.
     * @param c The other coordinate.
     * https://en.wikipedia.org/wiki/Haversine_formula
     */
    fun haversineDistanceTo(c: Coordinate): Double {
        val R = 6371.0 // Radius of the earth in km
        val dLat = Math.toRadians(y - c.y) // Distance between latitudes
        val dLon = Math.toRadians(x - c.x) // Distance between longitudes

        val hav = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(y)) * cos(Math.toRadians(c.y)) *
                sin(dLon / 2).pow(2)
        return 2 * R * asin(sqrt(hav))
    }
}
