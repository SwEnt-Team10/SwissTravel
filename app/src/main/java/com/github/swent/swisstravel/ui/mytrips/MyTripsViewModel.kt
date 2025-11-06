package com.github.swent.swisstravel.ui.mytrips

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.github.swent.swisstravel.model.trip.TripsRepository
import com.github.swent.swisstravel.model.trip.TripsRepositoryProvider
import com.github.swent.swisstravel.model.trip.isCurrent
import com.github.swent.swisstravel.model.trip.isUpcoming
import kotlinx.coroutines.launch

/**
 * ViewModel responsible for managing the state and logic of the "My Trips" screen.
 *
 * Extends [TripsViewModel] to manage the user's trips, including:
 * - Fetching all trips from the repository.
 * - Categorizing trips into current and upcoming.
 * - Applying the selected sort order to upcoming trips.
 * - Updating UI state and handling errors.
 *
 * Trips are loaded automatically on initialization and can be refreshed as needed.
 *
 * @property tripsRepository The repository to fetch trips from.
 */
class MyTripsViewModel(tripsRepository: TripsRepository = TripsRepositoryProvider.repository) :
    TripsViewModel(tripsRepository) {

  /** Initializes the ViewModel by loading all trips. */
  init {
    viewModelScope.launch { getAllTrips() }
  }

  /**
   * Fetches all trips from the repository and updates the UI state.
   *
   * Categorizes trips into current and upcoming ones based on their profile dates, and applies the
   * active sorting type to upcoming trips.
   */
  override suspend fun getAllTrips() {
    viewModelScope.launch {
      try {
        val trips = tripsRepository.getAllTrips()
        val currentTrip = trips.find { it.isCurrent() }
        val upcomingTrips = trips.filter { it.isUpcoming() }
        val sortedTrips = sortTrips(upcomingTrips, _uiState.value.sortType)

        _uiState.value = _uiState.value.copy(currentTrip = currentTrip, tripsList = sortedTrips)
      } catch (e: Exception) {
        Log.e("MyTripsViewModel", "Error fetching trips", e)
        setErrorMsg("Failed to load trips.")
      }
    }
  }
}
