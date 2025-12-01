package com.github.swent.swisstravel.ui.profile.selectpinnedtrips

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.github.swent.swisstravel.model.trip.Trip
import com.github.swent.swisstravel.model.trip.TripsRepository
import com.github.swent.swisstravel.model.trip.TripsRepositoryProvider
import com.github.swent.swisstravel.model.user.User
import com.github.swent.swisstravel.model.user.UserRepository
import com.github.swent.swisstravel.model.user.UserRepositoryFirebase
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
    tripsRepository: TripsRepository = TripsRepositoryProvider.repository,
    private val userRepository: UserRepository = UserRepositoryFirebase()
) : TripsViewModel(tripsRepository) {

  private var currentUser: User? = null

  /** Initializes the ViewModel by loading all trips. */
  init {
    toggleSelectionMode(true)
    viewModelScope.launch {
      try {
        val user = userRepository.getCurrentUser()
        currentUser = user
        _uiState.value =
            _uiState.value.copy(
                selectedTrips = user.pinnedTripsUids.map { tripsRepository.getTrip(it) }.toSet())
        getAllTrips()
      } catch (e: Exception) {
        setErrorMsg("Failed to load user and trips: ${e.message}")
      }
    }
  }

  /** Refreshes the list of trips by fetching them from the repository. */
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

  /** Toggles the selection state of a trip. */
  override fun onToggleTripSelection(trip: Trip) {
    val current = _uiState.value.selectedTrips.toMutableSet()
    if (current.size > 3) {
      setErrorMsg("You can only pin up to 3 trips on your profile.")
    } else {
      if (current.contains(trip)) current.remove(trip) else current.add(trip)
      _uiState.value = _uiState.value.copy(selectedTrips = current)
    }
  }

  /** Saves the selected trips to the user's profile. */
  fun onSaveSelectedTrips() {
    viewModelScope.launch {
      val user = currentUser
      if (user == null) {
        setErrorMsg("User not logged in.")
        return@launch
      }
      try {
        userRepository.updateUser(
            uid = user.uid, pinnedTripsUids = _uiState.value.selectedTrips.map { it.uid })
      } catch (e: Exception) {
        setErrorMsg("Error updating selected Trips: ${e.message}")
      }
    }
  }
}
