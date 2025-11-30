package com.github.swent.swisstravel.ui.tripcreation

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.algorithm.TripAlgorithm
import com.github.swent.swisstravel.model.trip.Location
import com.github.swent.swisstravel.model.trip.RouteSegment
import com.github.swent.swisstravel.model.trip.Trip
import com.github.swent.swisstravel.model.trip.TripElement
import com.github.swent.swisstravel.model.trip.TripProfile
import com.github.swent.swisstravel.model.trip.TripsRepository
import com.github.swent.swisstravel.model.trip.TripsRepositoryFirestore
import com.github.swent.swisstravel.model.trip.activity.Activity
import com.github.swent.swisstravel.model.trip.activity.ActivityRepository
import com.github.swent.swisstravel.model.trip.activity.ActivityRepositoryMySwitzerland
import com.github.swent.swisstravel.model.user.Preference
import com.github.swent.swisstravel.model.user.UserRepository
import com.github.swent.swisstravel.model.user.UserRepositoryFirebase
import com.google.firebase.Timestamp
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
    val destinations: List<Location> = emptyList(),
    val invalidNameMsg: Int? = null
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
 * @property userRepository Repository for managing user data.
 * @property activityRepository Repository for managing activity data.
 * @property algorithmFactory Factory function to create a TripAlgorithm instance.
 */
open class TripSettingsViewModel(
    private val tripsRepository: TripsRepository = TripsRepositoryFirestore(),
    private val userRepository: UserRepository = UserRepositoryFirebase(),
    private val activityRepository: ActivityRepository = ActivityRepositoryMySwitzerland(),
    private val algorithmFactory: (Context, TripSettings) -> TripAlgorithm = { context, settings ->
      TripAlgorithm.init(
          context = context, tripSettings = settings, activityRepository = activityRepository)
    }
) : ViewModel() {

  private val _tripSettings = MutableStateFlow(TripSettings())
  val tripSettings: StateFlow<TripSettings> = _tripSettings

  private val _validationEventChannel = Channel<ValidationEvent>()
  val validationEvents = _validationEventChannel.receiveAsFlow()

  private val _isLoading = MutableStateFlow(false)

  private val _loadingProgress = MutableStateFlow(0f)
  val loadingProgress = _loadingProgress.asStateFlow()

  fun updateName(name: String) {
    _tripSettings.value =
        _tripSettings.value.copy(
            name = name, invalidNameMsg = if (name.isBlank()) R.string.name_empty else null)
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
    val settings = _tripSettings.value

    val fullDestinationsList = buildList {
      settings.arrivalDeparture.arrivalLocation?.let { add(it) }
      addAll(destinations)
      settings.arrivalDeparture.departureLocation?.let { add(it) }
    }

    _tripSettings.update { it.copy(destinations = fullDestinationsList) }
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
   *
   * @param context The context used for initializing components that require it.
   */
  fun saveTrip(context: Context) {
    if (_isLoading.value) return

    viewModelScope.launch {
      _isLoading.value = true
      _loadingProgress.value = 0f

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
        val finalName = settings.name.ifBlank { "Trip from ${settings.date.startDate}" }

        val tripProfile =
            TripProfile(
                startDate = startTs,
                endDate = endTs,
                preferredLocations = settings.destinations,
                preferences = settings.preferences,
                adults = settings.travelers.adults,
                children = settings.travelers.children,
                arrivalLocation = settings.arrivalDeparture.arrivalLocation,
                departureLocation = settings.arrivalDeparture.departureLocation)

        // Run the algorithm
        val algorithm = algorithmFactory(context, tripSettings.value)
        val schedule =
            algorithm.runTripAlgorithm(
                tripSettings = tripSettings.value, tripProfile = tripProfile) { progress ->
                  _loadingProgress.value = progress
                }

        // Extract activities and route segments
        val selectedActivities: List<Activity> =
            schedule.filterIsInstance<TripElement.TripActivity>().map { it.activity }

        val routeSegments: List<RouteSegment> =
            schedule.filterIsInstance<TripElement.TripSegment>().map { it.route }

        // Merge all locations from route segments and activities
        // Done using AI
        val allLocations =
            (routeSegments.sortedBy { it.startDate }.flatMap { listOf(it.from, it.to) } +
                    selectedActivities.sortedBy { it.startDate }.map { it.location })
                .distinctBy { "${it.name}-${it.coordinate.latitude}-${it.coordinate.longitude}" }

        val trip =
            Trip(
                uid = newUid,
                name = finalName,
                ownerId = user.uid,
                locations = allLocations,
                routeSegments = routeSegments,
                activities = selectedActivities,
                tripProfile = tripProfile,
                isFavorite = false,
                isCurrentTrip = false,
                listUri = emptyList())

        tripsRepository.addTrip(trip)
        _validationEventChannel.send(ValidationEvent.SaveSuccess)
      } catch (e: Exception) {
        _validationEventChannel.send(
            ValidationEvent.SaveError(e.message ?: "An unknown error occurred"))
      } finally {
        _isLoading.value = false
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
