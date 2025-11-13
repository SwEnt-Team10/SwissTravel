package com.github.swent.swisstravel.ui.trip.tripinfos

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.swent.swisstravel.model.trip.Location
import com.github.swent.swisstravel.model.trip.RouteSegment
import com.github.swent.swisstravel.model.trip.TripProfile
import com.github.swent.swisstravel.model.trip.TripsRepository
import com.github.swent.swisstravel.model.trip.TripsRepositoryProvider
import com.github.swent.swisstravel.model.trip.activity.Activity
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

/** UI state for the TripInfo screen */
data class TripInfoUIState(
    val uid: String = "",
    val name: String = "Trip Name",
    val ownerId: String = "",
    val locations: List<Location> = emptyList(),
    val routeSegments: List<RouteSegment> = emptyList(),
    val activities: List<Activity> = emptyList(),
    val tripProfile: TripProfile? = null,
    val isFavorite: Boolean = false,
    val errorMsg: String? = null,
    val fullscreen: Boolean = false,
)
/** ViewModel for the TripInfo screen */
@OptIn(FlowPreview::class)
class TripInfoViewModel(
    private val tripsRepository: TripsRepository = TripsRepositoryProvider.repository
) : ViewModel(), TripInfoViewModelContract {
  private val _uiState = MutableStateFlow(TripInfoUIState())
  override val uiState: StateFlow<TripInfoUIState> = _uiState.asStateFlow()

  private val favoriteDebounceMs = 800L
  private val _favoriteToggleFlow = MutableStateFlow<Boolean?>(null)

  init {
    // Debounce favorite changes to avoid spamming database
    viewModelScope.launch {
      _favoriteToggleFlow
          .debounce(favoriteDebounceMs) // wait 800ms after last toggle
          .filterNotNull()
          .distinctUntilChanged() // only persist when state truly changes
          .collect { newFavorite -> persistFavoriteChange(newFavorite) }
    }
  }

  /** Clears the error message in the UI state */
  override fun clearErrorMsg() {
    _uiState.value = _uiState.value.copy(errorMsg = null)
  }

  /**
   * Sets the error message in the UI state
   *
   * @param errorMsg the error message to set
   */
  override fun setErrorMsg(errorMsg: String) {
    _uiState.value = _uiState.value.copy(errorMsg = errorMsg)
  }

  /**
   * Loads the trip information for the given trip ID
   *
   * @param uid the unique identifier of the trip
   */
  override fun loadTripInfo(uid: String?) {
    if (uid.isNullOrBlank()) {
      Log.e("TripInfoViewModel", "Trip ID is null or blank")
      setErrorMsg("Trip ID is invalid")
      return
    }
    viewModelScope.launch {
      try {
        val trip = tripsRepository.getTrip(uid)
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
        Log.d("Activities", trip.activities.toString())
      } catch (e: Exception) {
        Log.e("TripInfoViewModel", "Error loading trip info", e)
        setErrorMsg("Failed to load trip info: ${e.message}")
      }
    }
  }

  /**
   * Toggles the favorite status of the current trip.
   *
   * Updates the UI immediately and emits the new state to a debounced flow, which later persists
   * the change to the repository. Prevents redundant or rapid writes to the database.
   */
  override fun toggleFavorite() {
    val current = _uiState.value
    if (current.uid.isBlank()) return

    val newFavorite = !current.isFavorite

    // Update UI immediately
    _uiState.value = current.copy(isFavorite = newFavorite)

    // Emit to debounce flow (will persist after delay)
    _favoriteToggleFlow.value = newFavorite
  }

  /**
   * Persists the favorite change to the repository after debouncing.
   *
   * Skips redundant writes if the state is unchanged. Rolls back and sets an error message if
   * persistence fails. *Debounce features were made with the help of AI.*
   */
  private suspend fun persistFavoriteChange(newFavorite: Boolean) {
    val current = _uiState.value
    try {
      val trip = tripsRepository.getTrip(current.uid)

      // Avoid redundant write if already correct
      if (trip.isFavorite == newFavorite) return

      val updatedTrip = trip.copy(isFavorite = newFavorite)
      tripsRepository.editTrip(current.uid, updatedTrip)

      Log.d("TripInfoViewModel", "Favorite state updated: $newFavorite")
    } catch (e: Exception) {
      Log.e("TripInfoViewModel", "Failed to persist favorite", e)
      setErrorMsg("Failed to update favorite: ${e.message}")
      // Rollback to last known correct state
      _uiState.value = current.copy(isFavorite = !newFavorite)
    }
  }

  /**
   * Toggles the fullscreen mode of the map.
   *
   * @param fullscreen whether to set fullscreen mode or not
   */
  override fun toggleFullscreen(fullscreen: Boolean) {
    _uiState.value = _uiState.value.copy(fullscreen = fullscreen)
  }
}
