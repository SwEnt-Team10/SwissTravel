package com.github.swent.swisstravel.ui.tripSettings

import androidx.lifecycle.ViewModel
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

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

class TripSettingsViewModel() : ViewModel() {

  // TODO connect to Firebase
  // TODO default values are user preferences

  private val _tripSettings = MutableStateFlow(TripSettings())
  val tripSettings: StateFlow<TripSettings> = _tripSettings

  fun updateDates(start: LocalDate, end: LocalDate) {
    _tripSettings.update { it.copy(date = TripDate(start, end)) }
  }

  fun updateTravelers(adults: Int, children: Int) {
    _tripSettings.update { it.copy(travelers = TripTravelers(adults, children)) }
  }

  fun updatePreferences(prefs: TripPreferences) {
    _tripSettings.update { it.copy(preferences = prefs) }
  }
}
