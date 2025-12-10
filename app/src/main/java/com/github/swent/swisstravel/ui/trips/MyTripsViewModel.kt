package com.github.swent.swisstravel.ui.trips

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.github.swent.swisstravel.model.trip.Trip
import com.github.swent.swisstravel.model.trip.TripsRepository
import com.github.swent.swisstravel.model.trip.TripsRepositoryProvider
import com.github.swent.swisstravel.model.trip.isCurrent
import com.github.swent.swisstravel.model.trip.isUpcoming
import com.github.swent.swisstravel.model.user.UserRepository
import com.github.swent.swisstravel.model.user.UserRepositoryFirebase
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
class MyTripsViewModel(
    userRepository: UserRepository = UserRepositoryFirebase(),
    tripsRepository: TripsRepository = TripsRepositoryProvider.repository
) : TripsViewModel(userRepository = userRepository, tripsRepository = tripsRepository) {

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
        val currentUser = userRepository.getCurrentUser()
        val favoriteTrips = currentUser.favoriteTripsUids.toSet()

        val trips = tripsRepository.getAllTrips()
        val currentTrip = trips.find { it.isCurrent() }
        val upcomingTrips = trips.filter { it.isUpcoming() }
        val sortedTrips = sortTrips(upcomingTrips, _uiState.value.sortType, favoriteTrips)
        val collaboratorsByTrip = buildCollaboratorsByTrip(trips, userRepository)

        _uiState.value =
            _uiState.value.copy(
                currentTrip = currentTrip,
                tripsList = sortedTrips,
                collaboratorsByTripId = collaboratorsByTrip,
                favoriteTrips = favoriteTrips)
      } catch (e: Exception) {
        Log.e("MyTripsViewModel", "Error fetching trips", e)
        setErrorMsg("Failed to load trips.")
      }
    }
  }

  /**
   * Updates the previous current trip to no longer be current, and sets the new trip as current.
   *
   * @param trip The trip to set as the new current trip.
   */
  fun changeCurrentTrip(trip: Trip) {
    viewModelScope.launch {
      try {
        // Get all trips to find the current one (if any)
        val trips = tripsRepository.getAllTrips()
        val previousCurrentTrip = trips.find { it.isCurrent() }
        // If the selected trip is already the current one, do nothing
        if (previousCurrentTrip == trip) return@launch

        // If there was a current trip, unset it
        previousCurrentTrip?.let { current ->
          val updatedOldTrip = current.copy(isCurrentTrip = false)
          tripsRepository.editTrip(current.uid, updatedOldTrip)
        }

        // Set the selected trip as the new current one
        val updatedNewTrip = trip.copy(isCurrentTrip = true)
        tripsRepository.editTrip(trip.uid, updatedNewTrip)

        refreshUIState()
      } catch (e: Exception) {
        Log.e("MyTripsViewModel", "Error changing current trip", e)
        setErrorMsg("Failed to change current trip.")
      }
    }
  }
}
