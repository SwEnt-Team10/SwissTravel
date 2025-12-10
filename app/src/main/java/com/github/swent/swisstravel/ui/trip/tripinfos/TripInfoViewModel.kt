package com.github.swent.swisstravel.ui.trip.tripinfos

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.swent.swisstravel.algorithm.selectactivities.SelectActivities
import com.github.swent.swisstravel.model.trip.Location
import com.github.swent.swisstravel.model.trip.RouteSegment
import com.github.swent.swisstravel.model.trip.Trip
import com.github.swent.swisstravel.model.trip.TripElement
import com.github.swent.swisstravel.model.trip.TripProfile
import com.github.swent.swisstravel.model.trip.TripsRepository
import com.github.swent.swisstravel.model.trip.TripsRepositoryProvider
import com.github.swent.swisstravel.model.trip.activity.Activity
import com.github.swent.swisstravel.model.user.FriendStatus
import com.github.swent.swisstravel.model.user.User
import com.github.swent.swisstravel.model.user.UserRepository
import com.github.swent.swisstravel.model.user.UserRepositoryFirebase
import com.github.swent.swisstravel.ui.tripcreation.TripArrivalDeparture
import com.github.swent.swisstravel.ui.tripcreation.TripDate
import com.github.swent.swisstravel.ui.tripcreation.TripSettings
import com.github.swent.swisstravel.ui.tripcreation.TripTravelers
import com.mapbox.geojson.Point
import java.time.LocalDate
import java.time.ZoneId
import kotlin.collections.ArrayDeque
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
    // fields for swipe and like
    val likedActivities: List<Activity> = emptyList(),
    val activitiesQueue: ArrayDeque<Activity> = ArrayDeque(),
    val allFetchedForSwipe: List<Activity> = emptyList(),
    val currentActivity: Activity? = null,
    val backActivity: Activity? = null,
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

  /**
   * The trip corresponding to this ViewModel's UI state.
   *
   * Initialized in the loading trip info block (not init block) to make sure the uid is the right
   * one.
   */
  private val trip: MutableStateFlow<Trip?> = MutableStateFlow(null)

  private val activitiesFetcher = SelectActivities(tripInfoVM = this)

  private val favoriteDebounceMs = 800L
  private val _favoriteToggleFlow = MutableStateFlow<Boolean?>(null)

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
        trip.update { trip -> tripsRepository.getTrip(uid) }
        val trip = trip.value!!
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
                likedActivities = trip.likedActivities,
                activitiesQueue = trip.activitiesQueue,
                allFetchedForSwipe = trip.allFetchedForSwipe,
                currentActivity = trip.activitiesQueue.firstOrNull(),
                backActivity = trip.activitiesQueue.getOrNull(1),
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

  // ========== Functions for swipe and like activities ==========

  /**
   * Adds the given activities to the list of liked activities in :
   * - the UI state (because the tripInfoUIState is used to display the liked activities and
   *   schedule them)
   * - the Trip (its new values are kept on the database, so that, when the user quits the app and
   *   comes back later, the values are the same)
   */
  override fun likeActivities(activities: List<Activity>) {
    _uiState.update { state ->
      state.copy(likedActivities = (state.likedActivities + activities).distinct())
    }
    // also update the trip on the repository
    viewModelScope.launch {
      val trip = tripsRepository.getTrip(_uiState.value.uid)
      tripsRepository.editTrip(
          trip.uid,
          updatedTrip = trip.copy(likedActivities = (trip.likedActivities + activities).distinct()))
    }
  }

  /**
   * Removes the given activities from the list of liked activities in :
   * - the UI state (because the tripInfoUIState is used to display the liked activities and
   *     * schedule them)
   * - the Trip (its new values are kept on the database, so that, when the user quits the app and
   *   comes back later, the values are the same)
   */
  override fun unlikeActivities(activities: List<Activity>) {
    _uiState.update { state ->
      state.copy(likedActivities = (state.likedActivities - activities).distinct())
    }
    // also update the trip on the repository
    viewModelScope.launch {
      tripsRepository.editTrip(
          trip.value!!.uid,
          updatedTrip =
              trip.value!!.copy(
                  likedActivities = (trip.value!!.likedActivities - activities).distinct()))
    }
  }

  /**
   * Updates :
   * - the trip info UI state's queue (because the ui state is used to display the activities in the
   *   SwipeActivitiesScreen, and fetch new activities)
   * - the Trip (its new values are kept on the database, so that, when the user quits the app and
   *   comes back later, the values are the same)
   *
   * Also, it refreshes the current activity and back activity
   *
   * @param newQueue The queue of activities that will be set.
   */
  override fun updateQueue(newQueue: ArrayDeque<Activity>) {
    _uiState.update { state ->
      state.copy(
          activitiesQueue = newQueue,
          currentActivity = newQueue.firstOrNull(),
          backActivity = newQueue.getOrNull(1))
    }
    // also update the trip on the repository
    viewModelScope.launch {
      tripsRepository.editTrip(
          trip.value!!.uid, updatedTrip = trip.value!!.copy(activitiesQueue = newQueue))
    }
  }

  /**
   * Updates the set of all activities that have been fetched for swiping in :
   * - the UI state (because it is used to keep track of all fetched activities in the
   *   SwipeActivitiesScreen)
   * - the Trip (its new values are kept on the database, so that, when the user quits the app and
   *   comes back later, the values are the same)
   *
   * @param newFetched The list of activities that are newly fetched for swiping (they are added to
   *   the existing set)
   */
  override fun updateAllFetchedForSwipe(newFetched: List<Activity>) {
    _uiState.update { state ->
      state.copy(allFetchedForSwipe = (state.allFetchedForSwipe + newFetched).distinct())
    }
    // also update the trip on the repository
    viewModelScope.launch {
      tripsRepository.editTrip(
          trip.value!!.uid,
          updatedTrip =
              trip.value!!.copy(
                  allFetchedForSwipe = (trip.value!!.allFetchedForSwipe + newFetched).distinct()))
    }
  }

  /**
   * If you liked the activity, it will add the activity to the liked activities list of the trip.
   *
   * Otherwise, it is considered as a dislike
   *
   * @param liked a boolean indicating whether you liked the activity or not
   */
  override fun swipeActivity(liked: Boolean) {
    val current = _uiState.value.currentActivity ?: return
    val newQueue = _uiState.value.activitiesQueue

    if (liked) likeActivities(listOf(current))

    // remove the first activity from the queue
    if (newQueue.isNotEmpty()) {
      newQueue.removeFirst()
    }

    // will also refresh the current activity and back activity
    updateQueue(newQueue)

    // fetches new activity to put on the back of the queue, and adds it to all fetched
    viewModelScope.launch { fetchSwipeActivity() }
  }

  /**
   * - Fetches a new activity to swipe
   * - Adds it to the end of the activities queue
   * - Updates the set of all fetched swipe activities
   */
  private suspend fun fetchSwipeActivity() {
    val state = _uiState.value

    // fetch a new activity
    val newActivity =
        activitiesFetcher.fetchUniqueSwipe(
            toExclude = (state.allFetchedForSwipe + state.activities).toSet())
    if (newActivity == null) {
      Log.e("TripInfoVM", "No new unique activity could be fetched.")
      return
    }

    // add it to the new queue
    val newQueue = _uiState.value.activitiesQueue
    newQueue.addLast(newActivity)

    // update queue and all fetched
    updateQueue(newQueue)
    updateAllFetchedForSwipe(listOf(newActivity))
  }

  /**
   * Fetches the initial activities to populate the swipe queue.
   *
   * By default, it fetches 5 activities.
   */
  fun fetchFirstActivities() {
    viewModelScope.launch {
      val state = _uiState.value
      val initialActivities =
          ArrayDeque(
              activitiesFetcher.fetchSwipeActivities(
                  toExclude = (state.allFetchedForSwipe + state.activities).toSet()))

      Log.d("TRIP_INFO_VM", "current activity = ${state.currentActivity}")
      // update the tripInfo and the Trip on the repo
      updateQueue(initialActivities)
      updateAllFetchedForSwipe(initialActivities)

      Log.d("TRIP_INFO_VM", "current activity = ${state.currentActivity}")
    }
  }

  /**
   * Helper to map the tripInfoUIState to a TripSettings.
   *
   * If the tripProfile parameter from the uiState is null, it will return TripSettings with default
   * parameters
   */
  override fun mapToTripSettings(): TripSettings {
    val profile: TripProfile? = _uiState.value.tripProfile
    if (profile == null) return TripSettings(name = _uiState.value.name)
    else
        return TripSettings(
            name = _uiState.value.name,
            date =
                TripDate(
                    profile.startDate
                        .toDate()
                        .toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate(),
                    profile.endDate
                        .toDate()
                        .toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()),
            travelers = TripTravelers(adults = profile.adults, children = profile.children),
            preferences = profile.preferences,
            arrivalDeparture =
                TripArrivalDeparture(
                    arrivalLocation = profile.arrivalLocation,
                    departureLocation = profile.departureLocation),
            destinations = profile.preferredLocations
            // InvalidNameMsg should stay null since the tripInfo should already have a valid name
            )
  }


    /**
     * Loads the list of friends available to be added as collaborators and the list of current
     * collaborators for the trip.
     *
     * Fetches the current user's friends (accepted status only) and filters out those who are already
     * collaborators. Also fetches the full User objects for the current trip's collaborators.
     */
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

    /**
     * Adds a user as a collaborator to the current trip.
     *
     * Updates the trip in the repository by appending the user's UID to the collaborators list and
     * reloads the local collaborator data.
     *
     * @param user The user to add as a collaborator.
     */
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

    /**
     * Removes a user from the current trip's collaborators.
     *
     * Updates the trip in the repository by removing the user's UID from the collaborators list and
     * reloads the local collaborator data.
     *
     * @param user The user to remove.
     */
    override fun removeCollaborator(user: User) {
        val currentTripId = _uiState.value.uid
        if (currentTripId.isBlank()) return

        viewModelScope.launch {
            try {
                tripsRepository.removeCollaborator(currentTripId, user.uid)

                loadCollaboratorData()
            } catch (e: Exception) {
                setErrorMsg("Failed to remove collaborator: ${e.message}")
            }
        }
    }
}
