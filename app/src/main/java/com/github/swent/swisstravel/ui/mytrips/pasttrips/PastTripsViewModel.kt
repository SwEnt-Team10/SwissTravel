package com.github.swent.swisstravel.ui.mytrips.pasttrips

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.github.swent.swisstravel.model.trip.isUpcoming
import com.github.swent.swisstravel.ui.mytrips.TripsViewModel
import kotlinx.coroutines.launch

/**
 * ViewModel responsible for managing the state and logic of the "Past Trips" screen.
 *
 * It handles fetching trips, managing selection mode for bulk actions, deleting trips, sorting
 * trips, and handling error messages.
 *
 * @param tripsRepository Repository providing access to trip data.
 */
class PastTripsViewModel() : TripsViewModel() {

  /** Initializes the ViewModel by loading all trips. */
  init {
    viewModelScope.launch { getAllTrips() }
  }

  override suspend fun getAllTrips() {
    viewModelScope.launch {
      try {
        val trips = tripsRepository.getAllTrips()
        val pastTrips = trips.filter { it.isUpcoming() }
        val sortedTrips = sortTrips(pastTrips, _uiState.value.sortType)

        _uiState.value = _uiState.value.copy(tripsList = sortedTrips)
      } catch (e: Exception) {
        Log.e("PastTripsViewModel", "Error fetching trips", e)
        setErrorMsg("Failed to load trips.")
      }
    }
  }
}
