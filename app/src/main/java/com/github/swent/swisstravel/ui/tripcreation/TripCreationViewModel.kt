package com.github.swent.swisstravel.ui.tripcreation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.swent.swisstravel.model.trip.Trip
import com.github.swent.swisstravel.model.trip.TripProfile
import com.github.swent.swisstravel.model.trip.TripsRepository
import com.github.swent.swisstravel.model.trip.TripsRepositoryFirestore
import com.github.swent.swisstravel.model.user.Preference
import com.github.swent.swisstravel.model.user.UserRepository
import com.github.swent.swisstravel.model.user.UserRepositoryFirebase
import com.google.firebase.Timestamp
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** * Data class representing the start and end dates of a trip. */
data class TripDate(val startDate: LocalDate? = null, val endDate: LocalDate? = null)

/** Data class representing the number of adults and children traveling. */
data class TripTravelers(val adults: Int = 1, val children: Int = 0)

/** Data class encapsulating all trip settings: dates, travelers, and preferences. */
data class TripSettings(
    val date: TripDate = TripDate(),
    val travelers: TripTravelers = TripTravelers(),
    val preferences: List<Preference> = emptyList()
)

/** Sealed interface representing various validation events during trip settings. */
sealed interface ValidationEvent {
  object Proceed : ValidationEvent

  object SaveSuccess : ValidationEvent

  data class SaveError(val message: String) : ValidationEvent

  object EndDateIsBeforeStartDateError : ValidationEvent
}

/**
 * ViewModel managing the state and logic for trip settings.
 *
 * @property tripsRepository Repository for managing trip data.
 */
class TripSettingsViewModel(
    private val tripsRepository: TripsRepository = TripsRepositoryFirestore(),
    private val userRepository: UserRepository = UserRepositoryFirebase()
) : ViewModel() {

  private val _tripSettings = MutableStateFlow(TripSettings())
  val tripSettings: StateFlow<TripSettings> = _tripSettings

  private val _validationEventChannel = Channel<ValidationEvent>()
  val validationEvents = _validationEventChannel.receiveAsFlow()

  fun updateDates(start: LocalDate, end: LocalDate) {
    _tripSettings.update { it.copy(date = TripDate(start, end)) }
  }

  fun updateTravelers(adults: Int, children: Int) {
    _tripSettings.update { it.copy(travelers = TripTravelers(adults, children)) }
  }

  fun updatePreferences(prefs: List<Preference>) {
    _tripSettings.update { it.copy(preferences = prefs) }
    Log.d("TripSettingsViewModel", "Updated preferences: ${_tripSettings.value.preferences}")
  }

  init {
    viewModelScope.launch {
      try {
        val user = userRepository.getCurrentUser()
        _tripSettings.update { it.copy(preferences = user.preferences) }
      } catch (e: Exception) {
        Log.e("TripSettingsViewModel", "Failed to load user preferences", e)
      }
    }
  }

  /**
   * Saves the current trip settings as a new Trip in the repository.
   *
   * Trip should be saved once an internet connection is available.
   */
  fun saveTrip() {
    viewModelScope.launch {
      try {
        val settings = _tripSettings.value
        val newUid = tripsRepository.getNewUid()
        val user = userRepository.getCurrentUser()

        val start = settings.date.startDate
        val end = settings.date.endDate

        if (start == null || end == null) {
          _validationEventChannel.send(
              ValidationEvent.SaveError("Please select both start and end dates."))
          return@launch
        }

        val startTs = Timestamp(start.atStartOfDay(ZoneId.systemDefault()).toEpochSecond(), 0)
        val endTs = Timestamp(end.atStartOfDay(ZoneId.systemDefault()).toEpochSecond(), 0)

        val tripProfile =
            TripProfile(
                startDate = startTs,
                endDate = endTs,
                preferredLocations = emptyList(), // Placeholder
                preferences = settings.preferences,
                adults = settings.travelers.adults,
                children = settings.travelers.children)

        val trip =
            Trip(
                uid = newUid,
                name = "Trip from ${settings.date.startDate.toString()}", // TODO Placeholder name
                ownerId = user.uid,
                locations = emptyList(),
                routeSegments = emptyList(),
                activities = emptyList(),
                tripProfile = tripProfile)

        tripsRepository.addTrip(trip)
        _validationEventChannel.send(ValidationEvent.SaveSuccess)
      } catch (e: Exception) {
        _validationEventChannel.send(
            ValidationEvent.SaveError(e.message ?: "An unknown error occurred"))
      }
    }
  }

  /**
   * Validates the current trip settings when proceeding from the date selection screen.
   *
   * Ensures that the end date is not before the start date. Sends a validation event indicating
   * success or the type of error encountered.
   */
  fun onNextFromDateScreen() {
    viewModelScope.launch {
      val currentSettings = _tripSettings.value
      if (currentSettings.date.startDate != null &&
          currentSettings.date.endDate?.isBefore(currentSettings.date.startDate) == true) {
        _validationEventChannel.send(ValidationEvent.EndDateIsBeforeStartDateError)
      } else {
        _validationEventChannel.send(ValidationEvent.Proceed)
      }
    }
  }
}
