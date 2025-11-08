package com.github.swent.swisstravel.ui.tripcreation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.swent.swisstravel.model.trip.Location
import com.github.swent.swisstravel.model.trip.Trip
import com.github.swent.swisstravel.model.trip.TripProfile
import com.github.swent.swisstravel.model.trip.TripsRepository
import com.github.swent.swisstravel.model.trip.TripsRepositoryFirestore
import com.github.swent.swisstravel.model.trip.activity.Activity
import com.github.swent.swisstravel.model.trip.activity.ActivityRepositoryMySwitzerland
import com.github.swent.swisstravel.model.user.Preference
import com.github.swent.swisstravel.model.user.UserRepository
import com.github.swent.swisstravel.model.user.UserRepositoryFirebase
import com.google.firebase.Timestamp
import java.lang.Thread.sleep
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Wait before fetching new data to avoid overwhelming the api
private const val SLEEP = 1000.toLong()

/** * Data class representing the start and end dates of a trip. */
data class TripDate(val startDate: LocalDate? = null, val endDate: LocalDate? = null)

/** Data class representing the number of adults and children traveling. */
data class TripTravelers(val adults: Int = 1, val children: Int = 0)

/** Data class representing the arrival and departure destinations of the trip */
data class TripArrivalDeparture(
    val arrivalLocation: Location? = null,
    val departureLocation: Location? = null
)

/** Data class encapsulating all trip settings: name, dates, travelers, and preferences. */
data class TripSettings(
    val name: String = "",
    val date: TripDate = TripDate(),
    val travelers: TripTravelers = TripTravelers(),
    val preferences: List<Preference> = emptyList(),
    val arrivalDeparture: TripArrivalDeparture = TripArrivalDeparture(),
    val destinations: List<Location> = emptyList()
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

  fun updateName(name: String) {
    _tripSettings.update { it.copy(name = name) }
  }

  fun updateDates(start: LocalDate, end: LocalDate) {
    _tripSettings.update { it.copy(name = "Trip from $start", date = TripDate(start, end)) }
  }

  fun updateTravelers(adults: Int, children: Int) {
    _tripSettings.update { it.copy(travelers = TripTravelers(adults, children)) }
  }

  fun updatePreferences(prefs: List<Preference>) {
    _tripSettings.update { it.copy(preferences = prefs) }
    Log.d("TripSettingsViewModel", "Updated preferences: ${_tripSettings.value.preferences}")
  }

  fun setDestinations(destinations: List<Location>) {
    _tripSettings.update { it.copy(destinations = destinations) }
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

  /** Select activities near predetermined stops and with user preferences. */
  private suspend fun addActivities(): List<Activity> {
    val activityRepository = ActivityRepositoryMySwitzerland()
    val output = mutableListOf<Activity>()
    val destinations =
        _tripSettings.value.destinations.toMutableList() // TODO add locations along the route
    val arrivalDeparture = _tripSettings.value.arrivalDeparture
    destinations.add(arrivalDeparture.arrivalLocation!!)
    destinations.add(arrivalDeparture.departureLocation!!)

    // Get activities near stops
    val allActivitiesNear = mutableListOf<Activity>()
    for (place in destinations) {
      val activities = activityRepository.getActivitiesNear(place.coordinate, 15000, 20)
      allActivitiesNear.addAll(activities)
      sleep(SLEEP)
    }
    output.addAll(allActivitiesNear.distinct())

    // Get activities with preferences
    val preferences = _tripSettings.value.preferences.toMutableList()
    if (preferences.isNotEmpty()) {
      val allActivitiesPref = mutableListOf<Activity>()

      val wheelChair = preferences.contains(Preference.WHEELCHAIR_ACCESSIBLE)
      val publicTransport = preferences.contains(Preference.PUBLIC_TRANSPORT)

      // Mandatory filters
      val always = mutableListOf<Preference>()

      if (wheelChair) {
        preferences.remove(Preference.WHEELCHAIR_ACCESSIBLE)
        always.add(Preference.WHEELCHAIR_ACCESSIBLE)
      }
      if (publicTransport) {
        preferences.remove(Preference.PUBLIC_TRANSPORT)
        always.add(Preference.PUBLIC_TRANSPORT)
      }

      for (preference in preferences) {
        val activities = activityRepository.getActivitiesByPreferences(always + preference, 100)
        allActivitiesPref.addAll(activities)
        sleep(SLEEP)
      }
      allActivitiesPref.distinct()

      // Select activities that are near and respects user preferences
      output.filter { activity -> allActivitiesPref.contains(activity) }
    }

    setDestinations(destinations + output.map { it.location })

    return output
  }

  /**
   * Saves the current trip settings as a new Trip in the repository.
   *
   * Trip should be saved once an internet connection is available.
   */
  fun saveTrip() { // TODO Set loading screen until failure or success if success continue as
    // before, if failure continue as before
    viewModelScope.launch {
      try {
        val activities = addActivities()

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
        val finalName = settings.name.ifBlank { "Trip from ${settings.date.startDate}" }

        val tripProfile =
            TripProfile(
                startDate = startTs,
                endDate = endTs,
                preferredLocations = settings.destinations, // Placeholder
                preferences = settings.preferences,
                adults = settings.travelers.adults,
                children = settings.travelers.children,
                arrivalLocation = settings.arrivalDeparture.arrivalLocation,
                departureLocation = settings.arrivalDeparture.departureLocation)

        val trip =
            Trip(
                uid = newUid,
                name = finalName,
                ownerId = user.uid,
                locations = settings.destinations,
                routeSegments = emptyList(),
                activities = activities,
                tripProfile = tripProfile,
                isFavorite = false,
                isCurrentTrip = false)

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

  /** Update arrival location string in trip settings. */
  fun updateArrivalLocation(arrival: Location?) {
    _tripSettings.update {
      it.copy(arrivalDeparture = it.arrivalDeparture.copy(arrivalLocation = arrival))
    }
  }

  /** Update departure location string in trip settings. */
  fun updateDepartureLocation(departure: Location?) {
    _tripSettings.update {
      it.copy(arrivalDeparture = it.arrivalDeparture.copy(departureLocation = departure))
    }
  }
}
