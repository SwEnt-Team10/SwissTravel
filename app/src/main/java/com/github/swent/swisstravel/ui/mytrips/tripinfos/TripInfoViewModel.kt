package com.github.swent.swisstravel.ui.mytrips.tripinfos

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.swent.swisstravel.model.trip.Location
import com.github.swent.swisstravel.model.trip.RouteSegment
import com.github.swent.swisstravel.model.trip.TripProfile
import com.github.swent.swisstravel.model.trip.TripsRepository
import com.github.swent.swisstravel.model.trip.TripsRepositoryProvider
import com.github.swent.swisstravel.model.trip.activity.Activity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** UI state for the TripInfo screen */
data class TripInfoUIState(
    val uid: String = "",
    val name: String = "",
    val ownerId: String = "",
    val locations: List<Location> = emptyList(),
    val routeSegments: List<RouteSegment> = emptyList(),
    val activities: List<Activity> = emptyList(),
    val tripProfile: TripProfile? = null,
    val isFavorite: Boolean = false,
    val errorMsg: String? = null
)
/** ViewModel for the TripInfo screen */
class TripInfoViewModel(
    private val tripsRepository: TripsRepository = TripsRepositoryProvider.repository
) : ViewModel() {
  private val _uiState = MutableStateFlow(TripInfoUIState())
  val uiState: StateFlow<TripInfoUIState> = _uiState.asStateFlow()
  /** Clears the error message in the UI state */
  fun clearErrorMsg() {
    _uiState.value = _uiState.value.copy(errorMsg = null)
  }
  /**
   * Sets the error message in the UI state
   *
   * @param errorMsg the error message to set
   */
  private fun setErrorMsg(errorMsg: String) {
    _uiState.value = _uiState.value.copy(errorMsg = errorMsg)
  }
  /**
   * Loads the trip information for the given trip ID
   *
   * @param tripId the unique identifier of the trip
   */
  fun loadTripInfo(tripId: String?) {
    if (tripId.isNullOrBlank()) {
      Log.e("TripInfoViewModel", "Trip ID is null or blank")
      setErrorMsg("Trip ID is invalid")
      return
    }
    viewModelScope.launch {
      try {
        val trip = tripsRepository.getTrip(tripId)
        _uiState.value =
            TripInfoUIState(
                uid = trip.uid,
                name = trip.name,
                ownerId = trip.ownerId,
                locations = trip.locations,
                routeSegments = trip.routeSegments,
                activities = trip.activities,
                tripProfile = trip.tripProfile,
                isFavorite = trip.isFavorite)
      } catch (e: Exception) {
        Log.e("TripInfoViewModel", "Error loading trip info", e)
        setErrorMsg("Failed to load trip info: ${e.message}")
      }
    }
  }

  /**
   * Toggles the favorite status of the current trip.
   *
   * Updates the UI and persists the change to the repository. Rolls back and sets an error message
   * if persistence fails.
   */
  fun toggleFavorite() {
    val current = _uiState.value
    val newFavorite = !current.isFavorite

    viewModelScope.launch {
      try {
        // Get the full trip object from the repository (if needed)
        val trip = tripsRepository.getTrip(current.uid)

        // Create an updated version of it
        val updatedTrip = trip.copy(isFavorite = newFavorite)

        // Persist using the existing updateTrip() method
        tripsRepository.editTrip(current.uid, updatedTrip)

        // Update UI
        _uiState.value = current.copy(isFavorite = newFavorite)
      } catch (e: Exception) {
        Log.e("TripInfoViewModel", "Failed to update favorite", e)
        // Roll back on failure
        _uiState.value = current
        setErrorMsg("Failed to update favorite: ${e.message}")
      }
    }
  }
}
