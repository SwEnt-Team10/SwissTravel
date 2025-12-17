package com.github.swent.swisstravel.ui.tripcreation

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.algorithm.TripAlgorithm
import com.github.swent.swisstravel.algorithm.random.RandomTripGenerator
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
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

const val DEFAULT_DURATION = 3
const val MIN_DISTANCE = 15.0
const val MAX_DISTANCE = 50.0

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

  /**
   * Sets the random trip flag.
   *
   * @param isRandom Whether the trip is random or not.
   */
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

  /**
   * Updates the name of the trip.
   *
   * @param name The new name for the trip.
   */
  fun updateName(name: String) {
    _tripSettings.value =
        _tripSettings.value.copy(
            name = name, invalidNameMsg = if (name.isBlank()) R.string.name_empty else null)
  }

  /**
   * Updates the start and end dates of the trip.
   *
   * @param start The new start date.
   * @param end The new end date.
   */
  fun updateDates(start: LocalDate, end: LocalDate) {
    _tripSettings.update { it.copy(name = "Trip from $start", date = TripDate(start, end)) }
  }

  /**
   * Updates the number of travelers in the trip.
   *
   * @param adults The new number of adults.
   * @param children The new number of children.
   */
  fun updateTravelers(adults: Int, children: Int) {
    _tripSettings.update { it.copy(travelers = TripTravelers(adults, children)) }
  }

  /**
   * Updates the list of preferences for the trip.
   *
   * @param prefs The new list of preferences.
   */
  fun updatePreferences(prefs: List<Preference>) {
    _tripSettings.update { it.copy(preferences = prefs) }
    Log.d("TripSettingsViewModel", "Updated preferences: ${_tripSettings.value.preferences}")
  }

  /**
   * Updates the arrival location of the trip.
   *
   * @param location The new arrival location.
   */
  fun updateArrivalLocation(location: Location) {
    _tripSettings.update {
      it.copy(arrivalDeparture = it.arrivalDeparture.copy(arrivalLocation = location))
    }
  }

  /**
   * Updates the departure location of the trip.
   *
   * @param location The new departure location.
   */
  fun updateDepartureLocation(location: Location) {
    _tripSettings.update {
      it.copy(arrivalDeparture = it.arrivalDeparture.copy(departureLocation = location))
    }
  }

  /**
   * Sets the list of destinations for the trip.
   *
   * @param destinations The new list of destinations.
   */
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
    val settings = _tripSettings.value
    val (start, end, intermediateDestinations) =
        RandomTripGenerator.generateRandomDestinations(context, settings, seed)

    // Update the TripSettings state with the new random locations
    _tripSettings.update {
      val currentTime = LocalDateTime.now()
      val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy-HH-mm")
      val formattedTime = currentTime.format(formatter)
      it.copy(
          name = "Random Swiss Adventure $formattedTime",
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

        if (settings.arrivalDeparture.arrivalLocation == null) {
          _validationEventChannel.send(
              ValidationEvent.SaveError("Arrival location must not be null"))
          return@launch
        }

        if (settings.arrivalDeparture.departureLocation == null) {
          _validationEventChannel.send(
              ValidationEvent.SaveError("Departure location must not be null"))
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
        val cachedActivities = mutableListOf<Activity>()
        val schedule =
            algorithm.computeTrip(
                tripSettings = tripSettings.value,
                tripProfile = tripProfile,
                isRandomTrip = isRandomTrip.value,
                cachedActivities = cachedActivities) { progress ->
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
                uriLocation = emptyMap(),
                routeSegments = routeSegments,
                activities = selectedActivities,
                tripProfile = tripProfile,
                collaboratorsId = emptyList(),
                isRandom = _isRandomTrip.value,
                cachedActivities = cachedActivities)

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

  // --- Suggestions Logic ---

  private val _suggestions = MutableStateFlow<List<Location>>(emptyList())
  val suggestions: StateFlow<List<Location>> = _suggestions.asStateFlow()

  private val _selectedSuggestions = MutableStateFlow<List<Location>>(emptyList())
  val selectedSuggestions: StateFlow<List<Location>> = _selectedSuggestions.asStateFlow()

  /**
   * Generates location suggestions based on the user's current selections. If the user has not
   * selected any locations, it provides 5 random suggestions from the Grand Tour. Otherwise, it
   * suggests locations that are between 15 and 50 km from the already selected arrival, departure,
   * or intermediate destinations. If no such locations are found, it falls back to suggesting the 5
   * closest locations.
   *
   * @param context The context, used to access app resources like the Grand Tour locations.
   */
  fun generateSuggestions(context: Context) {
    if (suggestions.value.isNotEmpty()) {
      return
    }

    val settings = _tripSettings.value
    val userLocations =
        listOfNotNull(
            settings.arrivalDeparture.arrivalLocation,
            settings.arrivalDeparture.departureLocation) + settings.destinations

    val allPossibleLocations =
        context.resources.getStringArray(R.array.grand_tour).map {
          val parts = it.split(";")
          Location(Coordinate(parts[1].toDouble(), parts[2].toDouble()), parts[0], "")
        }

    // Filter out locations that are already part of the user's trip
    val availableSuggestions =
        allPossibleLocations.filterNot { suggestion ->
          userLocations.any { userLocation -> userLocation.sameLocation(suggestion) }
        }

    if (userLocations.isEmpty()) {
      // If no locations are selected yet, provide random suggestions. Should never happen.
      _suggestions.value = availableSuggestions.shuffled().take(5)
    } else {
      // Generate suggestions based on proximity to user's selected locations.
      _suggestions.value = generateSuggestionsFrom(userLocations, availableSuggestions).take(5)
    }
  }

  /**
   * Helper function to generate location suggestions from a list of available places, based on
   * proximity to a set of reference locations.
   *
   * @param referenceLocations The locations to measure distance from (e.g., user's selected
   *   places).
   * @param availableLocations The pool of all possible locations to suggest.
   * @return A list of suggested locations.
   */
  private fun generateSuggestionsFrom(
      referenceLocations: List<Location>,
      availableLocations: List<Location>
  ): List<Location> {
    // Attempt to find locations within the ideal distance range (>15km and <50km).
    val idealSuggestions =
        availableLocations
            .filter { it.isWithinDistanceOfAny(referenceLocations, MIN_DISTANCE, MAX_DISTANCE) }
            .shuffled() // Shuffle to provide variety if many options are available.

    if (idealSuggestions.isNotEmpty()) {
      return idealSuggestions
    }

    // If no locations are in the ideal range, find the closest ones.
    // Compute the minimum distance from each available location to any of the reference locations.
    return availableLocations
        .map { suggestion ->
          val minDistance = referenceLocations.minOf { suggestion.haversineDistanceTo(it) }
          suggestion to minDistance
        }
        .sortedBy { it.second } // Sort by the calculated minimum distance.
        .map { it.first } // Return just the locations.
  }

  /**
   * Toggles the selection of a location as a suggestion.
   *
   * @param location The location to toggle the selection for.
   */
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
