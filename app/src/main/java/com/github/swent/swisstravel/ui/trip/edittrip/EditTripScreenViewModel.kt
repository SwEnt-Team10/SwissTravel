package com.github.swent.swisstravel.ui.trip.edittrip

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.swent.swisstravel.algorithm.selectactivities.SelectActivities
import com.github.swent.swisstravel.model.trip.Location
import com.github.swent.swisstravel.model.trip.Trip
import com.github.swent.swisstravel.model.trip.TripsRepository
import com.github.swent.swisstravel.model.trip.TripsRepositoryFirestore
import com.github.swent.swisstravel.model.trip.activity.ActivityRepository
import com.github.swent.swisstravel.model.trip.activity.ActivityRepositoryMySwitzerland
import com.github.swent.swisstravel.model.user.Preference
import com.github.swent.swisstravel.model.user.PreferenceRules
import com.github.swent.swisstravel.ui.tripcreation.TripArrivalDeparture
import com.github.swent.swisstravel.ui.tripcreation.TripDate
import com.github.swent.swisstravel.ui.tripcreation.TripSettings
import com.github.swent.swisstravel.ui.tripcreation.TripTravelers
import com.github.swent.swisstravel.ui.tripcreation.ValidationEvent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditTripUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val savingProgress: Float = 0f,
    val errorMsg: String? = null,
    val tripId: String = "",
    val tripName: String = "",
    val adults: Int = 1,
    val children: Int = 0,
    val selectedPrefs: Set<Preference> = emptySet()
)

class EditTripScreenViewModel(
    private val tripRepository: TripsRepository = TripsRepositoryFirestore(),
    private val activityRepository: ActivityRepository = ActivityRepositoryMySwitzerland()
) : ViewModel() {

  private val _uiState = MutableStateFlow(EditTripUiState())
  val state: StateFlow<EditTripUiState> = _uiState
  private lateinit var originalTrip: Trip

  private val _validationEventChannel = Channel<ValidationEvent>()
  val validationEvents = _validationEventChannel.receiveAsFlow()

  /**
   * Loads a trip from the repository and fills the UI
   *
   * @param tripId The ID of the trip to load.
   * @throws Exception if the trip could not be loaded.
   */
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
                selectedPrefs = originalTrip.tripProfile.preferences.toSet())
          }
        } catch (e: Exception) {
          _uiState.update { it.copy(isLoading = false, errorMsg = e.message ?: "Failed to load") }
        }
      }

  /**
   * Deletes the current trip.
   *
   * @throws Exception if the trip could not be deleted.
   */
  fun deleteTrip() {
    viewModelScope.launch {
      try {
        tripRepository.deleteTrip(originalTrip.uid)
      } catch (e: Exception) {
        _uiState.update { it.copy(errorMsg = e.message ?: "Failed to delete trip") }
      }
    }
  }

  /**
   * Toggles a preference in the current trip.
   *
   * @param pref The preference to toggle.
   */
  fun togglePref(pref: Preference) =
      _uiState.update { state ->
        val next = PreferenceRules.toggleWithExclusivity(state.selectedPrefs, pref)
        state.copy(selectedPrefs = next.toSet())
      }

  /**
   * Saves the current trip with the edited information. This will re-calculate activities based on
   * the new preferences.
   */
  fun save() {
    viewModelScope.launch {
      _uiState.update { it.copy(isSaving = true, savingProgress = 0f) }
      try {
        val state = _uiState.value
        val sanitizedPrefs = PreferenceRules.enforceMutualExclusivity(state.selectedPrefs)

        // Create a temporary TripSettings object to pass to SelectActivities
        val tempTripSettings =
            TripSettings(
                name = state.tripName,
                date =
                    TripDate(
                        originalTrip.tripProfile.startDate
                            .toDate()
                            .toInstant()
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate(),
                        originalTrip.tripProfile.endDate
                            .toDate()
                            .toInstant()
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate()),
                travelers = TripTravelers(state.adults, state.children),
                preferences = sanitizedPrefs.toList(),
                arrivalDeparture =
                    TripArrivalDeparture(
                        originalTrip.tripProfile.arrivalLocation,
                        originalTrip.tripProfile.departureLocation),
                destinations = originalTrip.locations)

        val selectActivities =
            SelectActivities(
                tripSettings = tempTripSettings,
                onProgress = { progress -> _uiState.update { it.copy(savingProgress = progress) } },
                activityRepository = activityRepository)
        val selectedActivities = selectActivities.addActivities()

        val updatedTripProfile =
            originalTrip.tripProfile.copy(
                adults = state.adults,
                children = state.children,
                preferences = sanitizedPrefs.toList(),
            )

        val newLocation = mutableListOf<Location>()
        newLocation.add(originalTrip.tripProfile.arrivalLocation!!)
        newLocation.addAll(selectedActivities.map { it.location })
        newLocation.add(originalTrip.tripProfile.departureLocation!!)

        val updatedTrip =
            originalTrip.copy(
                name = state.tripName,
                tripProfile = updatedTripProfile,
                activities = selectedActivities,
                locations = newLocation)

        tripRepository.editTrip(state.tripId, updatedTrip)
        _validationEventChannel.send(ValidationEvent.SaveSuccess)
      } catch (e: Exception) {
        val errorMsg = e.message ?: "Failed to save trip"
        _uiState.update { it.copy(errorMsg = errorMsg) }
        _validationEventChannel.send(ValidationEvent.SaveError(errorMsg))
      } finally {
        _uiState.update { it.copy(isSaving = false) }
      }
    }
  }

  /**
   * Sets the number of adults for the traveler composable.
   *
   * @param value The new value
   */
  fun setAdults(value: Int) {
    _uiState.update { it.copy(adults = value) }
  }

  /**
   * Sets the number of children for the traveler composable.
   *
   * @param value The new value
   */
  fun setChildren(value: Int) {
    _uiState.update { it.copy(children = value) }
  }

  /**
   * Sets the name of the trip.
   *
   * @param value The new value
   */
  fun editTripName(value: String) {
    _uiState.update { it.copy(tripName = value) }
  }
  /** Clears the current error message. */
  fun clearErrorMsg() {
    _uiState.update { it.copy(errorMsg = null) }
  }
}
