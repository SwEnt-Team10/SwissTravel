package com.github.swent.swisstravel.ui.mytrips

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.swent.swisstravel.model.trip.Trip
import com.github.swent.swisstravel.model.trip.TripsRepository
import com.github.swent.swisstravel.model.trip.TripsRepositoryProvider
import com.github.swent.swisstravel.model.trip.isCurrent
import com.github.swent.swisstravel.model.trip.isUpcoming
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class TripSortType {
  START_DATE_ASC,
  START_DATE_DESC,
  END_DATE_ASC,
  END_DATE_DESC,
  NAME_ASC,
  NAME_DESC
}

data class MyTripsUIState(
    val currentTrip: Trip? = null,
    val upcomingTrips: List<Trip> = emptyList(),
    val errorMsg: String? = null,
    val sortType: TripSortType = TripSortType.START_DATE_ASC
)

class MyTripsViewModel(
    private val tripsRepository: TripsRepository = TripsRepositoryProvider.repository
) : ViewModel() {

  private val _uiState = MutableStateFlow(MyTripsUIState())
  val uiState: StateFlow<MyTripsUIState> = _uiState.asStateFlow()

  init {
    getAllTrips()
  }

  /** Clears the error message in the UI state. */
  fun clearErrorMsg() {
    _uiState.value = _uiState.value.copy(errorMsg = null)
  }

  /** Sets an error message in the UI state. */
  private fun setErrorMsg(errorMsg: String) {
    _uiState.value = _uiState.value.copy(errorMsg = errorMsg)
  }

  /** Refreshes the UI state by fetching all Trips items from the repository. */
  fun refreshUIState() {
    getAllTrips()
  }

  /** Fetches all Trips from the repository and updates the UI state. */
  private fun getAllTrips() {
    viewModelScope.launch {
      try {
        val trips = tripsRepository.getAllTrips()
        val currentTrip = trips.find { it.isCurrent() }
        val upcomingTrips = trips.filter { it.isUpcoming() }
        val sortedTrips = sortTrips(upcomingTrips, _uiState.value.sortType)

        _uiState.value = _uiState.value.copy(currentTrip = currentTrip, upcomingTrips = sortedTrips)
      } catch (e: Exception) {
        Log.e("MyTripsViewModel", "Error fetching trips", e)
        setErrorMsg("Failed to load trips: ${e.message}")
      }
    }
  }

  private fun sortTrips(trips: List<Trip>, sortType: TripSortType): List<Trip> {
    return when (sortType) {
      TripSortType.START_DATE_ASC -> trips.sortedBy { it.tripProfile.startDate }
      TripSortType.START_DATE_DESC -> trips.sortedByDescending { it.tripProfile.startDate }
      TripSortType.END_DATE_ASC -> trips.sortedBy { it.tripProfile.endDate }
      TripSortType.END_DATE_DESC -> trips.sortedByDescending { it.tripProfile.endDate }
      TripSortType.NAME_ASC -> trips.sortedBy { it.name.lowercase() }
      TripSortType.NAME_DESC -> trips.sortedByDescending { it.name.lowercase() }
    }
  }

  fun updateSortType(sortType: TripSortType) {
    val trips = _uiState.value.upcomingTrips
    _uiState.value =
        _uiState.value.copy(sortType = sortType, upcomingTrips = sortTrips(trips, sortType))
  }
}
