package com.github.swent.swisstravel.ui.edittrip

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.swent.swisstravel.model.trip.Trip
import com.github.swent.swisstravel.model.trip.TripsRepository
import com.github.swent.swisstravel.model.trip.TripsRepositoryFirestore
import com.github.swent.swisstravel.model.user.Preference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditTripUiState(
    val isLoading: Boolean = true,
    val errorMsg: String? = null,
    val tripId: String = "",
    val tripName: String = "",
    val adults: Int = 1,
    val children: Int = 0,
    val selectedPrefs: List<Preference> = emptyList()
)

class EditTripScreenViewModel(
    private val tripRepository: TripsRepository = TripsRepositoryFirestore()
) : ViewModel() {

  private val _uiState = MutableStateFlow(EditTripUiState())
  val state: StateFlow<EditTripUiState> = _uiState
  private lateinit var originalTrip: Trip

  fun loadTrip(tripId: String) =
      viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true, errorMsg = null, tripId = tripId) }

        try {
          originalTrip = tripRepository.getTrip(tripId)
          _uiState.update {
            it.copy(
                isLoading = false,
                tripName = originalTrip.name,
                adults = originalTrip.tripProfile.adults,
                children = originalTrip.tripProfile.children,
                selectedPrefs = originalTrip.tripProfile.preferences)
          }
        } catch (e: Exception) {
          _uiState.update { it.copy(isLoading = false, errorMsg = e.message ?: "Failed to load") }
        }
      }

  fun togglePref(pref: Preference) =
      _uiState.update {
        val selected = it.selectedPrefs.toMutableList()
        if (!selected.add(pref)) selected.remove(pref)
        it.copy(selectedPrefs = selected)
      }

  fun save() {
    viewModelScope.launch {
      try {
        val state = _uiState.value
        val updatedTripProfile =
            originalTrip.tripProfile.copy(
                adults = state.adults, children = state.children, preferences = state.selectedPrefs)
        val updatedTrip = originalTrip.copy(tripProfile = updatedTripProfile)
        tripRepository.editTrip(state.tripId, updatedTrip)
      } catch (e: Exception) {
        _uiState.update { it.copy(errorMsg = e.message ?: "Failed to save trip") }
      }
    }
  }

  fun setAdults(value: Int) {
    _uiState.update { it.copy(adults = value) }
  }

  fun setChildren(value: Int) {
    _uiState.update { it.copy(children = value) }
  }

  fun clearErrorMsg() {
    _uiState.update { it.copy(errorMsg = null) }
  }
}
