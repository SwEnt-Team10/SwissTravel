package com.github.swent.swisstravel.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.swent.swisstravel.model.trip.Coordinate
import com.github.swent.swisstravel.model.trip.Location
import com.github.swent.swisstravel.model.trip.RouteSegment
import com.github.swent.swisstravel.model.trip.TransportMode
import com.github.swent.swisstravel.model.trip.Trip
import com.github.swent.swisstravel.model.trip.TripProfile
import com.github.swent.swisstravel.model.trip.TripsRepository
import com.github.swent.swisstravel.model.trip.TripsRepositoryProvider
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DummyScreenViewModel(
    private val repository: TripsRepository = TripsRepositoryProvider.repository
) : ViewModel() {
  private val _trip = MutableStateFlow<Trip?>(null)
  val trip: StateFlow<Trip?> = _trip.asStateFlow()

  fun addTripModel() {
    viewModelScope.launch {
      val date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse("01/01/2025")!!
      val routeSegment =
          RouteSegment(
              Location(Coordinate(46.52, 6.57), "EPFL"),
              Location(Coordinate(35.1449, 136.9007), "Nagoya temple"),
              1000000,
              500,
              listOf(
                  Coordinate(46.52, 6.57),
                  Coordinate(47.37, 8.54),
                  Coordinate(46.94, 7.44),
                  Coordinate(46.02, 7.74),
                  Coordinate(35.1449, 136.9007)),
              TransportMode.TRAIN,
              startDate = Timestamp(date),
              endDate = Timestamp(date),
          )
      val routeSegment2 =
          RouteSegment(
              Location(Coordinate(0.0, 0.0), "test"),
              Location(Coordinate(1.0, 1.0), "test destination"),
              150,
              5,
              listOf(Coordinate(0.0, 0.0), Coordinate(0.0, 1.0), Coordinate(1.0, 1.0)),
              TransportMode.UNKNOWN,
              startDate = Timestamp(date),
              endDate = Timestamp(date))

      val trip =
          Trip(
              uid = "testID",
              name = "testName",
              ownerId = "testOwner",
              locations =
                  listOf(
                      Location(Coordinate(46.52, 6.57), "EPFL"),
                      Location(Coordinate(35.1449, 136.9007), "Nagoya temple")),
              routeSegments = listOf(routeSegment, routeSegment2),
              activities = emptyList(),
              tripProfile = TripProfile(Timestamp(date), Timestamp(date), emptyList(), emptyList()))
      repository.addTrip(trip)
    }
  }

  fun getTripModel(tripId: String) {
    viewModelScope.launch { _trip.value = repository.getTrip(tripId) }
  }
}
