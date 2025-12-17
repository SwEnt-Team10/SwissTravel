package com.github.swent.swisstravel.ui.trip.tripinfos

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.swent.swisstravel.algorithm.TripAlgorithm
import com.github.swent.swisstravel.algorithm.selectactivities.SelectActivities
import com.github.swent.swisstravel.model.trip.Coordinate
import com.github.swent.swisstravel.model.trip.Location
import com.github.swent.swisstravel.model.trip.RouteSegment
import com.github.swent.swisstravel.model.trip.Trip
import com.github.swent.swisstravel.model.trip.TripElement
import com.github.swent.swisstravel.model.trip.TripProfile
import com.github.swent.swisstravel.model.trip.TripsRepository
import com.github.swent.swisstravel.model.trip.TripsRepositoryProvider
import com.github.swent.swisstravel.model.trip.activity.Activity
import com.github.swent.swisstravel.model.trip.activity.ActivityRepositoryMySwitzerland
import com.github.swent.swisstravel.model.trip.activity.CityConfig
import com.github.swent.swisstravel.model.trip.activity.MajorSwissCities
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
import kotlin.collections.plus
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
    val uriLocation: Map<Uri, Location> = emptyMap(),
    val routeSegments: List<RouteSegment> = emptyList(),
    val activities: List<Activity> = emptyList(),
    val tripProfile: TripProfile? = null,
    val isFavorite: Boolean = false,
    val isCurrentTrip: Boolean = false,
    val isRandom: Boolean = false,
    val errorMsg: String? = null,
    val fullscreen: Boolean = false,
    val selectedActivity: Activity? = null,
    val cachedActivities: List<Activity> = emptyList(),
    // fields for swipe and like
    val likedActivities: List<Activity> = emptyList(),
    val activitiesQueue: List<Activity> = emptyList(),
    val allFetchedForSwipe: List<Activity> = emptyList(),
    val allFetchedLocations: List<Location> = emptyList(),
    val currentActivity: Activity? = null,
    val backActivity: Activity? = null,
    val selectedLikedActivities: List<Activity> = emptyList(),
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
    val isLoading: Boolean = false,
    val availableFriends: List<User> = emptyList(),
    val collaborators: List<User> = emptyList(),
    val savingProgress: Float = 0.0f,
    val isScheduling: Boolean = false
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

  private val activitiesFetcher = SelectActivities()

  private val favoriteDebounceMs = 800L
  private val _favoriteToggleFlow = MutableStateFlow<Boolean?>(null)

  private val _majorSwissCities = MutableStateFlow<List<CityConfig>>(emptyList())
  val majorSwissCities = _majorSwissCities.asStateFlow()

  init {
    Log.d("TRIP_INFO_VM", "Initialized a tripInfoVM")
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
   * @param forceReload whether to force a reload from the repository
   */
  override fun loadTripInfo(uid: String?, forceReload: Boolean) {
    if (uid.isNullOrBlank()) {
      Log.e("TRIP_INFO_VM", "Trip ID is null or blank")
      setErrorMsg("Trip ID is invalid")
      return
    }

    // load the info only if the uid is different from the current one OR if forceReload is true
    if (!forceReload && uid == _uiState.value.uid) {
      Log.d("TRIP_INFO_VM", "loadTripInfo called with same uid = $uid, skipping reload")
      return
    }

    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true) }
      try {
        val current = _uiState.value
        trip.update { _ -> tripsRepository.getTrip(uid) }
        val trip = trip.value!!
        val isSameTrip = current.uid == trip.uid
        val currentUser = userRepository.getCurrentUser()
        val isFavorite = currentUser.favoriteTripsUids.contains(trip.uid)
        val tripActivitiesQueue = trip.activitiesQueue.shuffled()

        _uiState.value =
            TripInfoUIState(
                uid = trip.uid,
                name = trip.name,
                ownerId = trip.ownerId,
                locations = trip.locations,
                uriLocation = trip.uriLocation,
                routeSegments = trip.routeSegments,
                activities = trip.activities,
                tripProfile = trip.tripProfile,
                isFavorite = isFavorite,
                isRandom = trip.isRandom,
                cachedActivities = trip.cachedActivities,
                likedActivities = trip.likedActivities,
                activitiesQueue = tripActivitiesQueue,
                allFetchedForSwipe = trip.allFetchedForSwipe,
                allFetchedLocations = trip.allFetchedLocations,
                currentActivity = tripActivitiesQueue.firstOrNull(),
                backActivity = tripActivitiesQueue.getOrNull(1),
                // Preserve transient state if reloading the same trip
                currentDayIndex = if (isSameTrip) current.currentDayIndex else 0,
                selectedStep = if (isSameTrip) current.selectedStep else null,
                drawFromCurrentPosition = isSameTrip && current.drawFromCurrentPosition,
                currentGpsPoint = if (isSameTrip) current.currentGpsPoint else null,
                currentUserIsOwner = trip.isOwner(currentUser.uid),
                isLoading = false,
                isScheduling = false,
                savingProgress = 0f)
        computeSchedule()

        Log.d("TRIP_INFO_VM", "activities queue from trip = ${trip.activitiesQueue}")
        Log.d("Activities", trip.activities.map { it.getName() }.toString())
      } catch (e: Exception) {
        Log.e("TRIP_INFO_VM", "Error loading trip info", e)
        setErrorMsg("Failed to load trip info: ${e.message}")
        _uiState.update { it.copy(isLoading = false) }
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
    val currentUiState = _uiState.value
    try {
      val trip = tripsRepository.getTrip(currentUiState.uid)
      val currentUser = userRepository.getCurrentUser()

      // Avoid redundant write if already correct
      if (currentUser.favoriteTripsUids.contains(trip.uid) == newFavorite) return

      if (newFavorite) {
        userRepository.addFavoriteTrip(currentUser.uid, currentUiState.uid)
      } else {
        userRepository.removeFavoriteTrip(currentUser.uid, currentUiState.uid)
      }

      Log.d("TripInfoViewModel", "Favorite state updated: $newFavorite")
    } catch (e: Exception) {
      Log.e("TripInfoViewModel", "Failed to persist favorite", e)
      setErrorMsg("Failed to update favorite: ${e.message}")
      // Rollback to last known correct state
      _uiState.value = currentUiState.copy(isFavorite = !newFavorite)
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
                  Coordinate(
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
   *
   * @param activities The activities to like.
   */
  override fun likeActivities(activities: List<Activity>) {
    _uiState.update { state ->
      state.copy(likedActivities = (state.likedActivities + activities).distinct())
    }
    // also update the trip in this class and on the repository
    trip.update { trip ->
      trip!!.copy(likedActivities = (trip.likedActivities + activities).distinct())
    }
    viewModelScope.launch { tripsRepository.editTrip(trip.value!!.uid, updatedTrip = trip.value!!) }
  }

  /**
   * Removes the given activities from the list of liked activities in :
   * - the UI state (because the tripInfoUIState is used to display the liked activities in the
   *   LikedActivitiesScreen and schedule them)
   * - the Trip (its new values are kept on the database, so that, when the user quits the app and
   *   comes back later, the values are the same)
   */
  override fun unlikeSelectedActivities() {
    val selected = _uiState.value.selectedLikedActivities
    _uiState.update { state ->
      state.copy(
          likedActivities = (state.likedActivities - selected).distinct(),
          // clear selected activities after unliking them
          selectedLikedActivities = emptyList())
    }
    // also update the trip in this class and on the repository
    trip.update { trip ->
      trip!!.copy(likedActivities = (trip.likedActivities - selected).distinct())
    }
    viewModelScope.launch { tripsRepository.editTrip(trip.value!!.uid, updatedTrip = trip.value!!) }
  }

  /**
   * Updates the queue of activities, the set of all activities that have been fetched for swiping,
   * the list of fetched locations, and the cached activities in:
   * - the UI state (because it is used to keep track of all fetched activities in the
   *   SwipeActivitiesScreen)
   * - the Trip (its new values are kept on the database, so that, when the user quits the app and
   *   comes back later, the values are the same)
   *
   * Also, it refreshes the current activity and back activity
   *
   * @param newQueue The queue of activities that will be set.
   * @param newFetched The list of activities that are newly fetched for swiping (they are added to
   *   the existing set of allFetchedForSwipe)
   * @param newFetchedLocations The updated list of locations where we have already fetched
   *   activities
   * @param newCachedActivities The updated list of cached activities
   */
  fun updateQueueAndAllFetched(
      newQueue: List<Activity>,
      newFetched: List<Activity>,
      newFetchedLocations: List<Location>,
      newCachedActivities: List<Activity>
  ) {
    val fullFetched = (_uiState.value.allFetchedForSwipe + newFetched).distinct()
    _uiState.update {
      it.copy(
          activitiesQueue = newQueue,
          allFetchedForSwipe = fullFetched,
          allFetchedLocations = newFetchedLocations,
          currentActivity = newQueue.firstOrNull(),
          backActivity = newQueue.getOrNull(1))
    }

    // update the trip locally
    trip.update {
      it!!.copy(
          activitiesQueue = newQueue,
          allFetchedForSwipe = fullFetched,
          allFetchedLocations = newFetchedLocations,
          cachedActivities = newCachedActivities)
    }

    // update trip in database
    viewModelScope.launch { tripsRepository.editTrip(trip.value!!.uid, trip.value!!) }
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
    val newQueue = _uiState.value.activitiesQueue.toMutableList()

    if (liked) likeActivities(listOf(current))

    // remove the first activity from the queue
    if (newQueue.isNotEmpty()) {
      newQueue.removeAt(0)
    }

    // Add the activity that was just swiped (whether liked or disliked) to the allFetchedForSwipe
    // list
    // so it is excluded next time.
    // Note: We use emptyList() for newFetchedLocations and existing cachedActivities because this
    // update
    // only concerns moving the item from Queue -> History.
    // The Queue refill happens in fetchSwipeActivities below.
    updateQueueAndAllFetched(
        newQueue = newQueue,
        newFetched = listOf(current),
        newFetchedLocations = _uiState.value.allFetchedLocations,
        newCachedActivities = trip.value?.cachedActivities ?: emptyList())

    // Try to fetch new activities (refill queue)
    // The activitiesFetcher logic will decide if it needs to fetch from API or just use
    // cache/return
    viewModelScope.launch { fetchSwipeActivity() }
  }

  /**
   * - Fetches a new activity to swipe using the mass-dump cache logic
   * - Adds it to the end of the activities queue
   * - Updates the set of all fetched swipe activities
   */
  override fun fetchSwipeActivity() {
    viewModelScope.launch {
      val currentTrip = trip.value ?: return@launch
      val cities = majorSwissCities.value

      // This is the suspending call, now safe inside the launch block
      val updatedTrip = activitiesFetcher.fetchSwipeActivities(currentTrip, cities)

      // Update the VM state
      updateQueueAndAllFetched(
          newQueue = updatedTrip.activitiesQueue,
          newFetched = emptyList(),
          newFetchedLocations = updatedTrip.allFetchedLocations,
          newCachedActivities = updatedTrip.cachedActivities)
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
    return if (profile == null) {
      TripSettings(name = _uiState.value.name)
    } else {
      TripSettings(
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
  }

  /**
   * Selects an activity (in the LikedActivitiesScreen) to later unlike it or schedule it
   *
   * @param activity The activity to add to the list of selected liked activities
   */
  override fun selectLikedActivity(activity: Activity) {
    _uiState.update { state ->
      state.copy(selectedLikedActivities = state.selectedLikedActivities + activity)
    }
  }

  /**
   * Deselects an activity (in the LikedActivitiesScreen) (used if the user doesn't want to schedule
   * the activity or unlike it)
   *
   * @param activity The activity to add to the list of selected liked activities
   */
  override fun deselectLikedActivity(activity: Activity) {
    _uiState.update { state ->
      state.copy(selectedLikedActivities = state.selectedLikedActivities - activity)
    }
  }

  /**
   * Schedules the selected liked activities.
   *
   * If there is no room for selected activities to be scheduled, it will respond with a toast
   */
  override fun scheduleSelectedActivities(context: Context) {
    viewModelScope.launch {
      _uiState.update { it.copy(isScheduling = true, savingProgress = 0f) }

      try {
        val tripSettings = mapToTripSettings()
        val algorithm =
            TripAlgorithm.init(
                tripSettings = tripSettings,
                activityRepository = ActivityRepositoryMySwitzerland(),
                context = context)

        // Prepare lists for the algorithm
        val currentActivities = _uiState.value.activities
        val fetchedForSwipe = _uiState.value.allFetchedForSwipe

        val activitiesNotInFetched =
            currentActivities.filter { activity ->
              fetchedForSwipe.none { fetched ->
                fetched.getName() == activity.getName() &&
                    fetched.location.sameLocation(activity.location)
              }
            }

        val cachedActivities =
            (_uiState.value.cachedActivities +
                    _uiState.value.activitiesQueue +
                    activitiesNotInFetched)
                .distinct()
                .toMutableList()

        val tripProfile = _uiState.value.tripProfile
        val blackList =
            (_uiState.value.allFetchedForSwipe +
                    _uiState.value.activitiesQueue +
                    _uiState.value.cachedActivities)
                .filter { activity ->
                  // Keep activity in blacklist ONLY if it is NOT in the selected list
                  _uiState.value.selectedLikedActivities.none { selected ->
                    selected.getName() == activity.getName() &&
                        selected.location.sameLocation(activity.location)
                  }
                }
                .map { it.getName() }
                .toSet()

        val selectionParameters =
            TripAlgorithm.ActivitySelectionParameters(
                activityBlacklist = blackList.toList(),
                protectedActivities = _uiState.value.selectedLikedActivities,
                cachedActivities = cachedActivities,
                allFetchedLocations = _uiState.value.allFetchedLocations)

        val schedule =
            algorithm.computeTrip(
                tripSettings = tripSettings,
                tripProfile = tripProfile!!,
                isRandomTrip = _uiState.value.isRandom,
                selectionParams = selectionParameters) { progress ->
                  _uiState.update { it.copy(savingProgress = progress) }
                }

        // Extract activities and route segments
        val selectedActivities =
            schedule.filterIsInstance<TripElement.TripActivity>().map { it.activity }

        val routeSegments = schedule.filterIsInstance<TripElement.TripSegment>().map { it.route }

        // Merge all locations from route segments and activities
        // Done using AI
        val allLocations =
            (routeSegments.sortedBy { it.startDate }.flatMap { listOf(it.from, it.to) } +
                    selectedActivities.sortedBy { it.startDate }.map { it.location })
                .distinctBy { "${it.name}-${it.coordinate.latitude}-${it.coordinate.longitude}" }

        _uiState.update {
          it.copy(
              activities = selectedActivities,
              routeSegments = routeSegments,
              locations = allLocations,
              cachedActivities = cachedActivities, // This list was modified by the algorithm
              selectedLikedActivities = emptyList(),
              activitiesQueue = emptyList() // Clear the queue since we dumped it into cache
              )
        }

        val updatedTrip = getTripFromState()

        tripsRepository.editTrip(_uiState.value.uid, updatedTrip)
        computeSchedule()

        _uiState.update { it.copy(isScheduling = false, savingProgress = 1f) }
      } catch (e: Exception) {
        Log.e("TripInfoViewModel", "Error scheduling activities", e)
        setErrorMsg("Scheduling failed: ${e.message}")
        _uiState.update { it.copy(isScheduling = false) }
      }
    }
  }
  // ========== Functions for managing collaborators ==========

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

  /**
   * Parses the list of major Swiss cities from the application resources and updates the state.
   *
   * This method reads the string array `R.array.swiss_major_cities`, parses each entry into a
   * [CityConfig] object (containing location, radius, and max days), and updates the
   * [_majorSwissCities] flow.
   *
   * @param context The context used to access application resources.
   */
  override fun getMajorSwissCities(context: Context) {
    _majorSwissCities.value = MajorSwissCities().getMajorSwissCities(context)
  }

  /**
   * Helper function to construct a [Trip] object from the current [TripInfoUIState].
   *
   * This ensures consistency between the UI state and the domain model. It also updates the local
   * [trip] flow with the newly created object.
   *
   * @return The [Trip] object reflecting the current values in the UI state.
   */
  private fun getTripFromState(): Trip {
    val newTrip =
        Trip(
            uid = _uiState.value.uid,
            name = _uiState.value.name,
            ownerId = _uiState.value.ownerId,
            locations = _uiState.value.locations,
            routeSegments = _uiState.value.routeSegments,
            activities = _uiState.value.activities,
            tripProfile = _uiState.value.tripProfile!!,
            collaboratorsId = _uiState.value.collaborators.map { it.uid },
            isRandom = _uiState.value.isRandom,
            uriLocation = _uiState.value.uriLocation,
            cachedActivities = _uiState.value.cachedActivities,
            likedActivities = _uiState.value.likedActivities,
            activitiesQueue = _uiState.value.activitiesQueue,
            allFetchedForSwipe = _uiState.value.allFetchedForSwipe,
            allFetchedLocations = _uiState.value.allFetchedLocations)
    trip.value = newTrip
    return newTrip
  }

  /**
   * Resets the saving progress state to 0.0f.
   *
   * This is typically called after a successful save navigation to prevent immediate re-triggering
   * of success observers when returning to the screen.
   */
  override fun resetSchedulingState() {
    _uiState.update { it.copy(savingProgress = 0f) }
  }
}
