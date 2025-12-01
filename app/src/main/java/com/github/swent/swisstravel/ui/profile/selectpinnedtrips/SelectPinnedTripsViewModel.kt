package com.github.swent.swisstravel.ui.profile.selectpinnedtrips

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.github.swent.swisstravel.model.trip.TripsRepository
import com.github.swent.swisstravel.model.trip.TripsRepositoryProvider
import com.github.swent.swisstravel.ui.trips.TripsViewModel
import kotlinx.coroutines.launch

/**
 * ViewModel responsible for managing the state and logic of the "Select Pinned Trips" screen.
 *
 * Extends [TripsViewModel] to manage the user's trips, including:
 * - Fetching all trips from the repository.
 * - Applying the selected sort order to all trips.
 * - Updating UI state and handling errors.
 *
 * Trips are loaded automatically on initialization and can be refreshed as needed.
 *
 * @param tripsRepository The repository responsible for managing user trips.
 */
class SelectPinnedTripsViewModel(
    tripsRepository: TripsRepository = TripsRepositoryProvider.repository
) : TripsViewModel(tripsRepository) {

  /** Initializes the ViewModel by loading all trips. */
  init {
    toggleSelectionMode(true)
    viewModelScope.launch { getAllTrips() }
  }

  override suspend fun getAllTrips() {
    try {
      val trips = tripsRepository.getAllTrips()
      val sortedTrips = sortTrips(trips, _uiState.value.sortType)

      _uiState.value = _uiState.value.copy(tripsList = sortedTrips)
    } catch (e: Exception) {
      Log.e("SelectPinnedTripsViewModel", "Error fetching trips", e)
      setErrorMsg("Failed to load trips.")
    }
  }
}
