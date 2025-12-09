package com.github.swent.swisstravel.ui.trip.tripinfos

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.swent.swisstravel.model.trip.Location
import com.github.swent.swisstravel.model.trip.RouteSegment
import com.github.swent.swisstravel.model.trip.TripElement
import com.github.swent.swisstravel.model.trip.TripProfile
import com.github.swent.swisstravel.model.trip.TripsRepository
import com.github.swent.swisstravel.model.trip.TripsRepositoryProvider
import com.github.swent.swisstravel.model.trip.activity.Activity
import com.github.swent.swisstravel.model.user.FriendStatus
import com.github.swent.swisstravel.model.user.User
import com.github.swent.swisstravel.model.user.UserRepository
import com.github.swent.swisstravel.model.user.UserRepositoryFirebase
import com.mapbox.geojson.Point
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
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
    val selectedActivity: Activity? = null,
    val likedActivities: List<Activity> = emptyList(),
    // New fields for DailyViewScreen MVVM refactor
    val schedule: List<TripElement> = emptyList(),
    val groupedSchedule: Map<LocalDate, List<TripElement>> = emptyMap(),
    val days: List<LocalDate> = emptyList(),
    val currentDayIndex: Int = 0,
    val mapLocations: List<Location> = emptyList(),
    val isComputingSchedule: Boolean = false,
    val selectedStep: TripElement? = null,
    val drawFromCurrentPosition: Boolean = false,
    val currentGpsPoint: Point? = null,
    val currentUserIsOwner: Boolean = false,
    val availableFriends: List<User> = emptyList(),
    val collaborators: List<User> = emptyList()
)
/** ViewModel for the TripInfo screen */
@OptIn(FlowPreview::class)
class TripInfoViewModel(
    private val tripsRepository: TripsRepository = TripsRepositoryProvider.repository,
    private val userRepository: UserRepository = UserRepositoryFirebase()
) : ViewModel(), TripInfoViewModelContract {
  private val _uiState = MutableStateFlow(TripInfoUIState())
  override val uiState: StateFlow<TripInfoUIState> = _uiState.asStateFlow()

  private val favoriteDebounceMs = 800L
  private val _favoriteToggleFlow = MutableStateFlow<Boolean?>(null)

  override fun loadCollaboratorData() {
    viewModelScope.launch {
      try {
        val currentUser = userRepository.getCurrentUser()
        val currentTrip = _uiState.value

        // 1. Load Friends (Accepted only)
        val friendUids =
            currentUser.friends.filter { it.status == FriendStatus.ACCEPTED }.map { it.uid }

        val friends = friendUids.mapNotNull { uid -> userRepository.getUserByUid(uid) }

        // 2. Load Collaborators for the trip
        val freshTrip = tripsRepository.getTrip(currentTrip.uid)
        val collaborators =
            freshTrip.collaboratorsId.mapNotNull { uid -> userRepository.getUserByUid(uid) }

        _uiState.update {
          it.copy(
              availableFriends =
                  friends.filter { friend -> friend.uid !in freshTrip.collaboratorsId },
              collaborators = collaborators)
        }
      } catch (e: Exception) {
        Log.e("TripInfoViewModel", "Error loading collaborators", e)
      }
    }
  }

  override fun addCollaborator(user: User) {
    val currentTripId = _uiState.value.uid
    if (currentTripId.isBlank()) return

    viewModelScope.launch {
      try {
        tripsRepository.shareTripWithUsers(currentTripId, listOf(user.uid))
        // Reload local state
        loadCollaboratorData()
      } catch (e: Exception) {
        setErrorMsg("Failed to add collaborator: ${e.message}")
      }
    }
  }

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
        val current = _uiState.value
        val trip = tripsRepository.getTrip(uid)
        val isSameTrip = current.uid == trip.uid

        _uiState.value =
            TripInfoUIState(
                uid = trip.uid,
                name = trip.name,
                ownerId = trip.ownerId,
                locations = trip.locations,
                routeSegments = trip.routeSegments,
                activities = trip.activities,
                tripProfile = trip.tripProfile,
                isFavorite = trip.isFavorite,
                likedActivities = _uiState.value.likedActivities,
                // Preserve transient state if reloading the same trip
                currentDayIndex = if (isSameTrip) current.currentDayIndex else 0,
                selectedStep = if (isSameTrip) current.selectedStep else null,
                drawFromCurrentPosition =
                    if (isSameTrip) current.drawFromCurrentPosition else false,
                currentGpsPoint = if (isSameTrip) current.currentGpsPoint else null,
                currentUserIsOwner = trip.isOwner(userRepository.getCurrentUser().uid))
        computeSchedule()
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

  override fun selectActivity(activity: Activity?) {
    _uiState.value = _uiState.value.copy(selectedActivity = activity)
  }

  /**
   * Sets the current day index for the daily view.
   *
   * @param index The new index.
   */
  override fun setCurrentDayIndex(index: Int) {
    if (index < 0 || index >= _uiState.value.days.size) return
    _uiState.value = _uiState.value.copy(currentDayIndex = index)
    updateMapLocations()
  }

  /**
   * Sets the selected step in the daily view.
   *
   * @param step The selected trip element.
   */
  override fun setSelectedStep(step: TripElement?) {
    _uiState.value = _uiState.value.copy(selectedStep = step)
    updateMapLocations()
  }

  /**
   * Toggles whether to draw the route from the current user position.
   *
   * @param enabled True to enable, false to disable.
   */
  override fun setDrawFromCurrentPosition(enabled: Boolean) {
    _uiState.value = _uiState.value.copy(drawFromCurrentPosition = enabled)
    updateMapLocations()
  }

  /**
   * Updates the current user GPS location.
   *
   * @param point The new GPS point.
   */
  override fun updateUserLocation(point: Point) {
    _uiState.value = _uiState.value.copy(currentGpsPoint = point)
    if (_uiState.value.drawFromCurrentPosition) {
      updateMapLocations()
    }
  }

  /** Computes the schedule and groups it by day. */
  private fun computeSchedule() {
    val current = _uiState.value
    if (current.locations.isEmpty() || current.tripProfile == null) return

    _uiState.value = current.copy(isComputingSchedule = true)

    viewModelScope.launch {
      val tripSegments = current.routeSegments.map { TripElement.TripSegment(it) }
      val tripActivities = current.activities.map { TripElement.TripActivity(it) }

      val newSchedule = (tripSegments + tripActivities).sortedBy { it.startDate }

      if (newSchedule != current.schedule) {
        val newGrouped =
            newSchedule
                .groupBy {
                  it.startDate.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                }
                .toSortedMap()
        val newDays = newGrouped.keys.toList()
        val newDaysCount = newDays.size

        var newIndex = current.currentDayIndex
        if (newIndex >= newDaysCount) {
          newIndex = 0
        }

        _uiState.value =
            _uiState.value.copy(
                schedule = newSchedule,
                groupedSchedule = newGrouped,
                days = newDays,
                currentDayIndex = newIndex,
                isComputingSchedule = false)
        updateMapLocations()
      } else {
        _uiState.value = _uiState.value.copy(isComputingSchedule = false)
      }
    }
  }

  /** Updates the list of locations to be displayed on the map. */
  private fun updateMapLocations() {
    val current = _uiState.value
    val currentDay = current.days.getOrNull(current.currentDayIndex)
    val dailySteps =
        if (currentDay != null) current.groupedSchedule[currentDay] ?: emptyList() else emptyList()

    val locations = mutableListOf<Location>()

    // Add current location if requested
    if (current.drawFromCurrentPosition && current.currentGpsPoint != null) {
      locations.add(
          Location(
              name = "Current Location",
              coordinate =
                  com.github.swent.swisstravel.model.trip.Coordinate(
                      current.currentGpsPoint.latitude(), current.currentGpsPoint.longitude())))
    }

    val stepsToMap =
        if (current.selectedStep != null) {
          listOf(current.selectedStep)
        } else {
          dailySteps
        }

    stepsToMap.forEach { step ->
      when (step) {
        is TripElement.TripActivity -> {
          // Do not add activity locations to the main map
        }
        is TripElement.TripSegment -> {
          locations.add(step.route.from)
          locations.add(step.route.to)
        }
      }
    }

    _uiState.value = _uiState.value.copy(mapLocations = locations.distinct())
  }

  /** Adds the given activity to the list of liked activities in the UI state. */
  override fun likeActivity(activity: Activity) {
    _uiState.update { current ->
      current.copy(likedActivities = (current.likedActivities + activity).distinct())
    }
  }
}
