package com.github.swent.swisstravel.ui.profile.selectpinnedtrips

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewModelScope
import com.github.swent.swisstravel.model.trip.Trip
import com.github.swent.swisstravel.model.trip.TripsRepository
import com.github.swent.swisstravel.model.trip.TripsRepositoryProvider
import com.github.swent.swisstravel.model.user.User
import com.github.swent.swisstravel.model.user.UserRepository
import com.github.swent.swisstravel.model.user.UserRepositoryFirebase
import com.github.swent.swisstravel.ui.trips.TripsViewModel
import com.github.swent.swisstravel.ui.trips.buildCollaboratorsByTrip
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
 * @param userRepository The repository responsible for managing user information.
 */
class SelectPinnedTripsViewModel(
    tripsRepository: TripsRepository = TripsRepositoryProvider.repository,
    userRepository: UserRepository = UserRepositoryFirebase()
) : TripsViewModel(userRepository = userRepository, tripsRepository = tripsRepository) {

  private var currentUser: User? = null

  private val _saveSuccess = mutableStateOf<Boolean?>(null)
  val saveSuccess = _saveSuccess

  /** Initializes the ViewModel by loading all trips. */
  init {
    toggleSelectionMode(true)
    viewModelScope.launch {
      try {
        val user = userRepository.getCurrentUser()
        currentUser = user
        val trips = tripsRepository.getAllTrips()
        val favoriteTrips = user.favoriteTripsUids.toSet()
        val selected =
            user.pinnedTripsUids.mapNotNull { uid -> trips.find { it.uid == uid } }.toSet()
        val sortedTrips = sortTrips(trips, _uiState.value.sortType, favoriteTrips)
        val collaboratorsByTrip = buildCollaboratorsByTrip(trips, userRepository)
        _uiState.value =
            _uiState.value.copy(
                tripsList = sortedTrips,
                selectedTrips = selected,
                collaboratorsByTripId = collaboratorsByTrip,
                favoriteTrips = favoriteTrips)
      } catch (e: Exception) {
        setErrorMsg("Failed to load pinned trips: ${e.message}")
        Log.e("SelectPinnedTripsViewModel", "Error initializing", e)
      }
    }
  }

  /** Refreshes the list of trips by fetching them from the repository. */
  override suspend fun getAllTrips() {
    try {
      val trips = tripsRepository.getAllTrips()
      val favoriteTrips = userRepository.getCurrentUser().favoriteTripsUids.toSet()
      val sortedTrips = sortTrips(trips, _uiState.value.sortType, favoriteTrips)
      _uiState.value =
          _uiState.value.copy(
              tripsList = sortedTrips,
              selectedTrips = _uiState.value.selectedTrips,
              favoriteTrips = favoriteTrips)
    } catch (e: Exception) {
      Log.e("SelectPinnedTripsViewModel", "Error fetching trips", e)
      setErrorMsg("Failed to load trips.")
    }
  }

  /** Toggles the selection state of a trip. */
  override fun onToggleTripSelection(trip: Trip) {
    val current = _uiState.value.selectedTrips.toMutableSet()
    if (current.size >= 3 && !current.contains(trip)) {
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
        _saveSuccess.value = false
        return@launch
      }
      try {
        userRepository.updateUser(
            uid = user.uid, pinnedTripsUids = _uiState.value.selectedTrips.map { it.uid })
        _saveSuccess.value = true
      } catch (e: Exception) {
        setErrorMsg("Error updating selected Trips.")
        Log.e("SelectPinnedTripsViewModel", "Error saving selected trips", e)
        _saveSuccess.value = false
      }
    }
  }

  /** Resets the save success state to null. */
  fun resetSaveSuccess() {
    _saveSuccess.value = null
  }
}
