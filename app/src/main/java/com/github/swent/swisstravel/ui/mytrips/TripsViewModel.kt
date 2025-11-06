package com.github.swent.swisstravel.ui.mytrips

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.swent.swisstravel.model.trip.Trip
import com.github.swent.swisstravel.model.trip.TripsRepository
import com.github.swent.swisstravel.model.trip.TripsRepositoryProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Represents the different sorting options for trips on the screen. */
enum class TripSortType {
  START_DATE_ASC,
  START_DATE_DESC,
  END_DATE_ASC,
  END_DATE_DESC,
  NAME_ASC,
  NAME_DESC,
  FAVORITES_FIRST,
}

/**
 * Base ViewModel for any screen displaying a list of trips. Handles selection, sorting, error
 * state, and bulk actions.
 */
abstract class TripsViewModel(
    protected val tripsRepository: TripsRepository = TripsRepositoryProvider.repository
) : ViewModel() {
  /**
   * Represents the UI state for a "Trips" screen.
   *
   * @property currentTrip The user's current active trip, if any.
   * @property tripsList The list of trips.
   * @property errorMsg An optional error message to display in the UI.
   * @property sortType The current sorting preference for upcoming trips.
   * @property isSelectionMode Whether the user is currently selecting multiple trips.
   * @property selectedTrips The set of trips currently selected in selection mode.
   */
  data class TripsUIState(
      val currentTrip: Trip? = null,
      val tripsList: List<Trip> = emptyList(),
      val errorMsg: String? = null,
      val sortType: TripSortType = TripSortType.START_DATE_ASC,
      val isSelectionMode: Boolean = false,
      val selectedTrips: Set<Trip> = emptySet(),
  )

  /**
   * Subclasses can override this to set the initial UI state (e.g. if they want to use a different
   * dataclass).
   */
  protected fun createInitialState() = TripsUIState()

  protected val _uiState = MutableStateFlow(createInitialState())
  /** Public read-only access to the UI state. */
  val uiState: StateFlow<TripsUIState> = _uiState.asStateFlow()

  /** Subclasses define how all trips from the repository are fetched. */
  protected abstract suspend fun getAllTrips()

  /** Refreshes the list of trips by re-fetching them from the repository. */
  fun refreshUIState() {
    viewModelScope.launch { getAllTrips() }
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
  fun setErrorMsg(errorMsg: String) {
    _uiState.value = _uiState.value.copy(errorMsg = errorMsg)
  }

  /**
   * Enables or disables selection mode. When disabled, all previously selected trips are cleared.
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
   * Adds or removes the trip from the set of selected trips. If no trips remain selected, selection
   * mode is automatically disabled.
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
   * On success, exits selection mode and refreshes the UI state. On failure, logs the error and
   * displays a generic error message.
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
   * Toggles the favorite status of all selected trips.
   *
   * Updates each selected trip by inverting its `isFavorite` flag, saves changes via
   * [TripsRepository.editTrip], exits selection mode, and refreshes the UI.
   */
  fun toggleFavoriteForSelectedTrips() {
    val selected = _uiState.value.selectedTrips
    viewModelScope.launch {
      try {
        selected.forEach { trip ->
          val updatedTrip = trip.copy(isFavorite = !trip.isFavorite)
          tripsRepository.editTrip(trip.uid, updatedTrip)
        }
        toggleSelectionMode(false)
        refreshUIState()
      } catch (e: Exception) {
        Log.e("MyTripsViewModel", "Error toggling favorites", e)
        setErrorMsg("Failed to update favorites.")
      }
    }
  }

  /**
   * Selects all trips.
   *
   * This is triggered when the user chooses "Select All" in selection mode.
   */
  fun selectAllTrips() {
    val allTrips = buildList {
      uiState.value.currentTrip?.let { add(it) }
      addAll(uiState.value.tripsList)
    }
    _uiState.value = _uiState.value.copy(selectedTrips = allTrips.toSet())
  }

  /**
   * Sorts the provided list of trips according to the given sort type.
   *
   * @param trips The list of trips to sort.
   * @param sortType The sorting order to apply.
   * @return The sorted list of trips.
   */
  fun sortTrips(trips: List<Trip>, sortType: TripSortType): List<Trip> {
    return when (sortType) {
      TripSortType.START_DATE_ASC -> trips.sortedBy { it.tripProfile.startDate }
      TripSortType.START_DATE_DESC -> trips.sortedByDescending { it.tripProfile.startDate }
      TripSortType.END_DATE_ASC -> trips.sortedBy { it.tripProfile.endDate }
      TripSortType.END_DATE_DESC -> trips.sortedByDescending { it.tripProfile.endDate }
      TripSortType.NAME_ASC -> trips.sortedBy { it.name.lowercase() }
      TripSortType.NAME_DESC -> trips.sortedByDescending { it.name.lowercase() }
      TripSortType.FAVORITES_FIRST -> trips.sortedByDescending { it.isFavorite }
    }
  }

  /**
   * Updates the current sort type and re-sorts the list of upcoming trips.
   *
   * @param sortType The new sorting preference selected by the user.
   */
  fun updateSortType(sortType: TripSortType) {
    val trips = _uiState.value.tripsList
    _uiState.value =
        _uiState.value.copy(sortType = sortType, tripsList = sortTrips(trips, sortType))
  }
}
