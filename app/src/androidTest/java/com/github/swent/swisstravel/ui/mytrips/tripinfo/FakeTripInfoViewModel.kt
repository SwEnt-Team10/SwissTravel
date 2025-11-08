package com.github.swent.swisstravel.ui.mytrips.tripinfo

import com.github.swent.swisstravel.model.trip.Coordinate
import com.github.swent.swisstravel.model.trip.Location
import com.github.swent.swisstravel.ui.mytrips.tripinfos.TripInfoUIState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Fake ViewModel used in androidTest to drive TripInfoScreen UI.
 *
 * Public API mirrors the real ViewModel enough for UI tests:
 * - exposes uiState as a StateFlow
 * - provides loadTripInfo, toggleFavorite and clearErrorMsg
 * - helper methods allow tests to inject locations / errors deterministically
 */
class FakeTripInfoViewModel {
  // Backing mutable state for tests to observe
  private val _uiState =
      MutableStateFlow(
          TripInfoUIState(
              uid = "",
              name = "Test Trip",
              ownerId = "owner_test",
              locations = emptyList(),
              routeSegments = emptyList(),
              activities = emptyList(),
              tripProfile = null,
              isFavorite = false,
              errorMsg = null))
  // Public read-only stateflow (same shape as production ViewModel)
  val uiState: StateFlow<TripInfoUIState> = _uiState.asStateFlow()

  /**
   * Simulate loading trip info. If uid is null or blank, set an error message; otherwise update
   * name and uid.
   */
  fun loadTripInfoWithLocation(uid: String?) {
    if (uid.isNullOrBlank()) {
      _uiState.value = _uiState.value.copy(errorMsg = "Trip ID is invalid")
      return
    }
    // Populate minimal predictable test data; tests can further modify via helpers
    generateFakeLocations()
    _uiState.value =
        _uiState.value.copy(
            uid = uid,
            name = "Test Trip $uid",
            ownerId = "owner_$uid",
            isFavorite = false,
            errorMsg = null)
  }

  /**
   * Toggle favorite immediately in UI state. Persistence is not simulated here; tests can assert UI
   * changes directly.
   */
  fun toggleFavorite() {
    val current = _uiState.value
    _uiState.value = current.copy(isFavorite = !current.isFavorite)
  }

  /** Clear visible error message. */
  fun clearErrorMsg() {
    _uiState.value = _uiState.value.copy(errorMsg = null)
  }

  /**
   * Helper for tests: set locations list (use real `Location` instances from production models).
   * This lets tests exercise the branches rendering location names and steps.
   */
  fun setLocations(locations: List<Location>) {
    _uiState.value = _uiState.value.copy(locations = locations)
  }

  /** Helper to inject an error message for UI error handling tests. */
  fun emitError(message: String) {
    _uiState.value = _uiState.value.copy(errorMsg = message)
  }
  /** Generate some fake locations and set them in the UI state. */
  private fun generateFakeLocations() {
    val locations =
        listOf(
            Location(name = "Location 1", coordinate = Coordinate(46.9481, 7.4474)),
            Location(name = "Location 2", coordinate = Coordinate(47.3769, 8.5417)),
            Location(name = "Location 3", coordinate = Coordinate(46.2044, 6.1432)))
    setLocations(locations)
  }
  /**
   * Simulate loading trip info. If uid is null or blank, set an error message; otherwise update
   * name and uid.
   */
  fun loadTripInfo(uid: String?) {
    if (uid.isNullOrBlank()) {
      _uiState.value = _uiState.value.copy(errorMsg = "Trip ID is invalid")
      return
    }
    // Populate minimal predictable test data; tests can further modify via helpers
    _uiState.value =
        _uiState.value.copy(
            uid = uid,
            name = "Test Trip $uid",
            ownerId = "owner_$uid",
            isFavorite = false,
            errorMsg = null)
  }
}
