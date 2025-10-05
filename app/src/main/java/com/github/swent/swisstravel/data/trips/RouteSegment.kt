package com.github.swent.swisstravel.data.trips

data class RouteSegment(
    val from: Location,
    val to: Location,
    val distanceMeter: Double,
    val durationMinutes: Double,
    val path: List<Coordinate>,
    val transportMode: TransportMode,
) {

    /**
     * Returns the duration in hours.
     */
    fun getDurationHours(): Double {
        return durationMinutes / 60
    }

    /**
     * Returns the distance in km.
     */
    fun getDistanceKm(): Double {
        return distanceMeter / 1000
    }
}
