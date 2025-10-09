package com.github.swent.swisstravel.model.trip

import com.google.firebase.Timestamp

data class Trip(
    val uid: String,
    val name: String,
    val startDate: Timestamp,
    val endDate: Timestamp,
    val ownerId: String,
    val locations: List<Location>,
    val routeSegments: List<RouteSegment>
) {
  // TODO add more parameters
}
