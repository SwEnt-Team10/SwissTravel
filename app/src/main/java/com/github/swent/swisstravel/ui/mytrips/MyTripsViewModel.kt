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

/**
 * Represents the UI state for the "My Trips" screen.
 *
 * @property currentTrip The user's current active trip, if any.
 * @property upcomingTrips The list of upcoming trips.
 * @property errorMsg An optional error message to display in the UI.
 * @property isSelectionMode Whether the user is currently selecting multiple trips.
 * @property selectedTrips The set of trips currently selected in selection mode.
 */
data class MyTripsUIState(
    val currentTrip: Trip? = null,
    val upcomingTrips: List<Trip> = emptyList(),
    val errorMsg: String? = null,
    val isSelectionMode: Boolean = false,
    val selectedTrips: Set<Trip> = emptySet(),
)

/**
 * ViewModel responsible for managing the state and logic of the "My Trips" screen.
 *
 * It handles fetching trips, managing selection mode for bulk actions, deleting trips, and handling
 * error messages.
 *
 * @param tripsRepository Repository providing access to trip data.
 */
class MyTripsViewModel(
    private val tripsRepository: TripsRepository = TripsRepositoryProvider.repository
) : ViewModel() {

  private val _uiState = MutableStateFlow(MyTripsUIState())
  /** Public read-only access to the UI state. */
  val uiState: StateFlow<MyTripsUIState> = _uiState.asStateFlow()

  init {
    getAllTrips()
  }

  /** Clears any error message currently stored in the UI state. */
  fun clearErrorMsg() {
    _uiState.value = _uiState.value.copy(errorMsg = null)
  }

  /**
   * Sets an error message in the UI state.
   *
   * @param errorMsg The error message to display.
   */
  private fun setErrorMsg(errorMsg: String) {
    _uiState.value = _uiState.value.copy(errorMsg = errorMsg)
  }

  /** Refreshes the list of trips by re-fetching them from the repository. */
  fun refreshUIState() {
    getAllTrips()
  }

  /**
   * Fetches all trips from the repository and updates the UI state.
   *
   * Categorizes trips into current and upcoming ones based on their profile dates.
   */
  private fun getAllTrips() {
    viewModelScope.launch {
      try {
        val trips = tripsRepository.getAllTrips()
        val currentTrip = trips.find { it.isCurrent() }
        val upcomingTrips = trips.filter { it.isUpcoming() }

        _uiState.value = MyTripsUIState(currentTrip = currentTrip, upcomingTrips = upcomingTrips)
      } catch (e: Exception) {
        Log.e("MyTripsViewModel", "Error fetching trips", e)
        setErrorMsg("Failed to load trips.")
      }
    }
  }

  /**
   * Enables or disables selection mode.
   *
   * When disabled, clears any selected trips.
   *
   * @param enabled Whether selection mode should be active.
   */
  fun toggleSelectionMode(enabled: Boolean) {
    _uiState.value =
        _uiState.value.copy(
            isSelectionMode = enabled,
            selectedTrips = if (!enabled) emptySet() else _uiState.value.selectedTrips)
  }

  /**
   * Toggles the selection state of a given trip.
   *
   * Automatically exits selection mode if no trips remain selected.
   *
   * @param trip The trip to select or deselect.
   */
  fun toggleTripSelection(trip: Trip) {
    val current = _uiState.value.selectedTrips.toMutableSet()
    if (current.contains(trip)) current.remove(trip) else current.add(trip)

    val newState = _uiState.value.copy(selectedTrips = current)

    _uiState.value =
        if (current.isEmpty()) {
          newState.copy(isSelectionMode = false)
        } else {
          newState
        }
  }

  /**
   * Deletes all currently selected trips from the repository.
   *
   * On success, exits selection mode and refreshes the UI state. On failure, sets an error message.
   */
  fun deleteSelectedTrips() {
    val toDelete = _uiState.value.selectedTrips
    viewModelScope.launch {
      try {
        toDelete.forEach { tripsRepository.deleteTrip(it.uid) }
        toggleSelectionMode(false)
        refreshUIState()
      } catch (e: Exception) {
        Log.e("MyTripsViewModel", "Error deleting trips", e)
        setErrorMsg("Failed to delete trips.")
      }
    }
  }

  /**
   * Selects all trips (both current and upcoming).
   *
   * This function is used when the user triggers "Select All" from the selection mode menu.
   */
  fun selectAllTrips() {
    val allTrips = buildList {
      uiState.value.currentTrip?.let { add(it) }
      addAll(uiState.value.upcomingTrips)
    }
    _uiState.value = _uiState.value.copy(selectedTrips = allTrips.toSet())
  }
}
