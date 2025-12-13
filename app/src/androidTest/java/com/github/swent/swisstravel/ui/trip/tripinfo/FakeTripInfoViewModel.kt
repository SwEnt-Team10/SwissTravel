package com.github.swent.swisstravel.ui.trip.tripinfo

import android.content.Context
import android.util.Log
import com.github.swent.swisstravel.model.trip.Location
import com.github.swent.swisstravel.model.trip.RouteSegment
import com.github.swent.swisstravel.model.trip.TripElement
import com.github.swent.swisstravel.model.trip.TripProfile
import com.github.swent.swisstravel.model.trip.activity.Activity
import com.github.swent.swisstravel.model.user.User
import com.github.swent.swisstravel.ui.trip.tripinfos.TripInfoUIState
import com.github.swent.swisstravel.ui.trip.tripinfos.TripInfoViewModelContract
import com.github.swent.swisstravel.ui.tripcreation.TripArrivalDeparture
import com.github.swent.swisstravel.ui.tripcreation.TripDate
import com.github.swent.swisstravel.ui.tripcreation.TripSettings
import com.github.swent.swisstravel.ui.tripcreation.TripTravelers
import com.mapbox.geojson.Point
import java.time.ZoneId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class FakeTripInfoViewModel : TripInfoViewModelContract {

  private val _ui =
      MutableStateFlow(
          TripInfoUIState(
              uid = "",
              name = "Trip",
              ownerId = "",
              locations = emptyList(),
              routeSegments = emptyList(),
              activities = emptyList(),
              tripProfile = null,
              isFavorite = false,
              errorMsg = null))

  override val uiState: StateFlow<TripInfoUIState> = _ui

  override fun loadTripInfo(uid: String?) {
    _ui.value = _ui.value.copy(uid = uid ?: "")
  }

  override fun clearErrorMsg() {
    _ui.value = _ui.value.copy(errorMsg = null)
  }

  override fun setErrorMsg(errorMsg: String) {
    _ui.value = _ui.value.copy(errorMsg = errorMsg)
  }

  override fun toggleFullscreen(fullscreen: Boolean) {
    _ui.value = _ui.value.copy(fullscreen = fullscreen)
  }

  override fun toggleFavorite() {
    _ui.value = _ui.value.copy(isFavorite = !_ui.value.isFavorite)
  }

  override fun selectActivity(activity: Activity?) {
    _ui.value = _ui.value.copy(selectedActivity = activity)
  }

  override fun setCurrentDayIndex(index: Int) {
    _ui.value = _ui.value.copy(currentDayIndex = index)
  }

  override fun setSelectedStep(step: TripElement?) {
    _ui.value = _ui.value.copy(selectedStep = step)
  }

  override fun setDrawFromCurrentPosition(enabled: Boolean) {
    _ui.value = _ui.value.copy(drawFromCurrentPosition = enabled)
  }

  override fun updateUserLocation(point: Point) {
    _ui.value = _ui.value.copy(currentGpsPoint = point)
  }

  private fun recomputeSchedule() {
    val current = _ui.value
    val tripSegments = current.routeSegments.map { TripElement.TripSegment(it) }
    val tripActivities = current.activities.map { TripElement.TripActivity(it) }
    val newSchedule = (tripSegments + tripActivities).sortedBy { it.startDate }

    val newGrouped =
        newSchedule
            .groupBy {
              it.startDate.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
            }
            .toSortedMap()
    val newDays = newGrouped.keys.toList()

    _ui.value = current.copy(schedule = newSchedule, groupedSchedule = newGrouped, days = newDays)
  }

  // ——— convenience setters for tests ———
  fun setName(name: String) {
    _ui.value = _ui.value.copy(name = name)
  }

  fun setLocations(locs: List<Location>) {
    _ui.value = _ui.value.copy(locations = locs)
  }

  fun setActivities(acts: List<Activity>) {
    _ui.value = _ui.value.copy(activities = acts)
    recomputeSchedule()
  }

  fun setTripProfile(profile: TripProfile?) {
    _ui.value = _ui.value.copy(tripProfile = profile)
  }

  fun setFavorite(value: Boolean) {
    _ui.value = _ui.value.copy(isFavorite = value)
  }

  fun setRouteSegments(segments: List<RouteSegment>) {
    _ui.value = _ui.value.copy(routeSegments = segments)
    recomputeSchedule()
  }

  override fun likeActivities(activities: List<Activity>) {
    val current = _ui.value
    val newLiked = current.likedActivities.toMutableList().apply { addAll(activities) }
    _ui.value = current.copy(likedActivities = newLiked)
  }

  override fun unlikeSelectedActivities() {
      _ui.update { state -> state.copy(likedActivities = state.likedActivities - state.selectedLikedActivities, selectedLikedActivities = emptyList()) }
  }

  override fun selectLikedActivity(activity: Activity) {
    _ui.update { state -> state.copy(selectedLikedActivities = state.selectedLikedActivities + activity)}
  }

  override fun deselectLikedActivity(activity: Activity) {
      _ui.update { state -> state.copy(selectedLikedActivities = state.selectedLikedActivities - activity)}
  }

    override fun scheduleSelectedActivities(context: Context) {
        _ui.update { state -> state }   // does nothing for the moment since scheduling logic is not done yet
    }

    fun setActivitiesQueue(activitiesQueue: ArrayDeque<Activity>) {
        _ui.update { state -> state.copy(activitiesQueue = activitiesQueue, currentActivity = activitiesQueue.first(), backActivity = activitiesQueue.getOrNull(1)) }
    }

  override fun swipeActivity(liked: Boolean) {
    if (_ui.value.activitiesQueue.isEmpty()) return
    val current = _ui.value
    val activity = current.activitiesQueue.first()
    val newQueue = current.activitiesQueue
    newQueue.removeFirst()
    val newLikedActivities =
        if (liked) current.likedActivities + activity else current.likedActivities
    _ui.value =
        current.copy(
            activitiesQueue = newQueue,
            likedActivities = newLikedActivities,
            currentActivity = newQueue.firstOrNull(),
            backActivity = newQueue.getOrNull(1))
  }

  override fun mapToTripSettings(): TripSettings {
    val profile: TripProfile? = uiState.value.tripProfile
    if (profile == null) return TripSettings(name = uiState.value.name)
    else
        return TripSettings(
            name = uiState.value.name,
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

  override fun addCollaborator(user: User) {
    // Add the user to the collaborators list in the fake state
    val currentCollaborators = _ui.value.collaborators.toMutableList()
    if (!currentCollaborators.any { it.uid == user.uid }) {
      currentCollaborators.add(user)
    }
    _ui.value = _ui.value.copy(collaborators = currentCollaborators)
  }

  override fun loadCollaboratorData() {
    // no op
  }

  override fun removeCollaborator(user: User) {
    // Remove the user from the collaborators list
    val currentCollaborators = _ui.value.collaborators.filter { it.uid != user.uid }
    _ui.value = _ui.value.copy(collaborators = currentCollaborators)
  }

  // Helper to set friends for testing the dialog
  fun setAvailableFriends(friends: List<User>) {
    _ui.value = _ui.value.copy(availableFriends = friends)
  }

  fun setCurrentUserIsOwner(value: Boolean) {
    _ui.value = _ui.value.copy(currentUserIsOwner = value)
  }
}
