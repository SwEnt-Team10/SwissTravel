package com.github.swent.swisstravel.ui.tripcreation

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.algorithm.TripAlgorithm
import com.github.swent.swisstravel.model.trip.Coordinate
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
import java.time.temporal.ChronoUnit
import kotlin.random.Random
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

const val DEFAULT_DURATION = 3

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

  private val _isRandomTrip = MutableStateFlow(false)
  val isRandomTrip: StateFlow<Boolean> = _isRandomTrip.asStateFlow()

  fun setRandomTrip(isRandom: Boolean) {
    _isRandomTrip.value = isRandom
  }

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
   * Generates a random trip based on the current trip settings.
   *
   * @param context The context used for initializing components that require it.
   * @param seed The seed to use for the random number generator. If null, a random seed will be
   *   used.
   */
  fun randomTrip(context: Context, seed: Int? = null) {
    val grandTour =
        context.resources.getStringArray(R.array.grand_tour).map {
          val parts = it.split(";")
          Location(Coordinate(parts[1].toDouble(), parts[2].toDouble()), parts[0], "")
        }

    val random = seed?.let { Random(it) } ?: Random
    val settings = _tripSettings.value

    // Pick distinct start and end locations at random
    val availableCities = grandTour.toMutableList()
    val start = availableCities.removeAt(random.nextInt(availableCities.size))
    val end = availableCities.removeAt(random.nextInt(availableCities.size))

    // Determine a manageable number of intermediate destinations based on trip duration
    val tripDurationDays =
        if (settings.date.startDate != null && settings.date.endDate != null) {
          val duration =
              ChronoUnit.DAYS.between(settings.date.startDate, settings.date.endDate).toInt() + 1
          Log.d("TripSettingsViewModel", "Trip duration in days: $duration")
          duration
        } else {
          Log.d("TripSettingsViewModel", "Dates are null")
          DEFAULT_DURATION // Default to a 3-day trip if dates are not set, should not happen
        }
    // Rule: roughly one new city every 2 days. Minimum 0, max 3.
    val numIntermediateDestinations = (tripDurationDays / 2).coerceAtMost(3).coerceAtLeast(0)

    // Select random, distinct intermediate destinations
    val intermediateDestinations =
        if (numIntermediateDestinations > 0) {
          availableCities.shuffled(random).take(numIntermediateDestinations)
        } else {
          emptyList()
        }

    // Update the TripSettings state with the new random locations
    _tripSettings.update {
      it.copy(
          name = "Random Swiss Adventure",
          arrivalDeparture = TripArrivalDeparture(start, end),
          destinations = intermediateDestinations)
    }

    // Call setDestinations to construct the full list and then save the trip
    setDestinations(intermediateDestinations)
    saveTrip(context)
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
                isCurrentTrip = false)

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

  // --- Suggestions Logic ---

  private val _suggestions = MutableStateFlow<List<Location>>(emptyList())
  val suggestions: StateFlow<List<Location>> = _suggestions.asStateFlow()

  private val _selectedSuggestions = MutableStateFlow<List<Location>>(emptyList())
  val selectedSuggestions: StateFlow<List<Location>> = _selectedSuggestions.asStateFlow()

  fun generateSuggestions(context: Context) {
    if (_suggestions.value.isEmpty()) {
      val grandTourArray = context.resources.getStringArray(R.array.grand_tour)
      val newSuggestions =
          grandTourArray
              .asSequence()
              .shuffled()
              .take(5)
              .mapNotNull { entry ->
                val parts = entry.split(";")
                if (parts.size >= 3) {
                  val name = parts[0]
                  val lat = parts[1].toDoubleOrNull()
                  val lon = parts[2].toDoubleOrNull()
                  if (lat != null && lon != null) {
                    Location(Coordinate(lat, lon), name)
                  } else null
                } else null
              }
              .toList()
      _suggestions.value = newSuggestions
    }
  }

  fun toggleSuggestion(location: Location) {
    _selectedSuggestions.update { current ->
      if (current.any { it.name == location.name && it.coordinate == location.coordinate }) {
        current.filterNot { it.name == location.name && it.coordinate == location.coordinate }
      } else {
        current + location
      }
    }
  }
}
