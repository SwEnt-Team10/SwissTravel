package com.github.swent.swisstravel.ui.tripSettings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.swent.swisstravel.model.trip.Trip
import com.github.swent.swisstravel.model.trip.TripProfile
import com.github.swent.swisstravel.model.trip.TripsRepository
import com.github.swent.swisstravel.model.trip.TripsRepositoryFirestore
import com.github.swent.swisstravel.model.user.RatedPreferences
import com.github.swent.swisstravel.model.user.UserPreference
import com.google.firebase.Timestamp
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TripDate(val startDate: LocalDate? = null, val endDate: LocalDate? = null)

data class TripTravelers(val adults: Int = 1, val children: Int = 0)

data class TripPreferences(
    val quickTraveler: Boolean = false,
    val sportyLevel: Boolean = false,
    val foodyLevel: Boolean = false,
    val museumInterest: Boolean = false,
    val hasHandicap: Boolean = false
)

data class TripSettings(
    val date: TripDate = TripDate(),
    val travelers: TripTravelers = TripTravelers(),
    val preferences: TripPreferences = TripPreferences()
)

sealed interface ValidationEvent {
  object Proceed : ValidationEvent

  object SaveSuccess : ValidationEvent

  data class SaveError(val message: String) : ValidationEvent

  object EndDateIsBeforeStartDateError : ValidationEvent
}

class TripSettingsViewModel(
    private val tripsRepository: TripsRepository = TripsRepositoryFirestore()
) : ViewModel() {

  // TODO default values are user preferences

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

  fun updatePreferences(prefs: TripPreferences) {
    _tripSettings.update { it.copy(preferences = prefs) }
  }

  // Trip should be saved once an internet connection is available
  fun saveTrip() {
    viewModelScope.launch {
      try {
        val settings = _tripSettings.value
        val newUid = tripsRepository.getNewUid()

        val tripProfile =
            TripProfile(
                startDate =
                    settings.date.startDate?.atStartOfDay(ZoneId.systemDefault())?.let {
                      Timestamp(it.toEpochSecond(), 0)
                    } ?: Timestamp.now(),
                endDate =
                    settings.date.endDate?.atStartOfDay(ZoneId.systemDefault())?.let {
                      Timestamp(it.toEpochSecond(), 0)
                    } ?: Timestamp.now(),
                preferredLocations = emptyList(), // Placeholder
                preferences = mapToRatedPreferences(settings.preferences),
                adults = settings.travelers.adults,
                children = settings.travelers.children)

        val trip =
            Trip(
                uid = newUid,
                name = "Trip from ${settings.date.startDate}", // TODO Placeholder name
                ownerId = "", // TODO set owner ID from auth
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

  private fun mapToRatedPreferences(
      prefs: TripPreferences
  ): List<RatedPreferences> { // TODO check how do rating works
    val ratedPrefs = mutableListOf<RatedPreferences>()
    if (prefs.quickTraveler) ratedPrefs.add(RatedPreferences(UserPreference.QUICK, 5))
    if (prefs.sportyLevel) ratedPrefs.add(RatedPreferences(UserPreference.HIKING, 5))
    if (prefs.foodyLevel) ratedPrefs.add(RatedPreferences(UserPreference.FOODIE, 5))
    if (prefs.museumInterest) ratedPrefs.add(RatedPreferences(UserPreference.MUSEUMS, 5))
    if (prefs.hasHandicap) ratedPrefs.add(RatedPreferences(UserPreference.HANDICAP, 5))
    return ratedPrefs
  }

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
