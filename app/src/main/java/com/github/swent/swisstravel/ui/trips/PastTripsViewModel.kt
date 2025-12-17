package com.github.swent.swisstravel.ui.trips

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.github.swent.swisstravel.model.trip.TripsRepository
import com.github.swent.swisstravel.model.trip.TripsRepositoryProvider
import com.github.swent.swisstravel.model.trip.isPast
import com.github.swent.swisstravel.model.user.UserRepository
import com.github.swent.swisstravel.model.user.UserRepositoryFirebase
import kotlinx.coroutines.launch

/**
 * ViewModel responsible for managing the state and logic of the "Past Trips" screen.
 *
 * Extends [TripsViewModel] to manage the user's trips, including:
 * - Fetching all trips from the repository.
 * - Applying the selected sort order to past trips.
 * - Updating UI state and handling errors.
 *
 * Trips are loaded automatically on initialization and can be refreshed as needed.
 *
 * @param tripsRepository The repository responsible for managing user trips.
 */
class PastTripsViewModel(
    userRepository: UserRepository = UserRepositoryFirebase(),
    tripsRepository: TripsRepository = TripsRepositoryProvider.repository
) : TripsViewModel(userRepository = userRepository, tripsRepository = tripsRepository) {

  /** Initializes the ViewModel by loading all trips. */
  init {
    viewModelScope.launch { getAllTrips() }
  }

  override suspend fun getAllTrips() {
    _uiState.value = _uiState.value.copy(isLoading = true)
    try {
      val currentUser = userRepository.getCurrentUser()
      val favoriteTrips = currentUser.favoriteTripsUids.toSet()

      val trips = tripsRepository.getAllTrips()
      val pastTrips = trips.filter { it.isPast() }
      val sortedTrips = sortTrips(pastTrips, _uiState.value.sortType, favoriteTrips)
      val collaboratorsByTrip = buildCollaboratorsByTrip(trips, userRepository)

      _uiState.value =
          _uiState.value.copy(
              tripsList = sortedTrips,
              collaboratorsByTripId = collaboratorsByTrip,
              favoriteTripsUids = favoriteTrips,
              isLoading = false)
    } catch (e: Exception) {
      Log.e("PastTripsViewModel", "Error fetching trips", e)
      setErrorMsg("Failed to load trips.")
      _uiState.value = _uiState.value.copy(isLoading = false)
    }
  }
}
