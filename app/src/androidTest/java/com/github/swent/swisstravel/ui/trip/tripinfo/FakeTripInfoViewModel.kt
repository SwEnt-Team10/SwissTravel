package com.github.swent.swisstravel.ui.trip.tripinfo

import com.github.swent.swisstravel.model.trip.Location
import com.github.swent.swisstravel.model.trip.RouteSegment
import com.github.swent.swisstravel.model.trip.TripElement
import com.github.swent.swisstravel.model.trip.TripProfile
import com.github.swent.swisstravel.model.trip.activity.Activity
import com.github.swent.swisstravel.ui.trip.tripinfos.TripInfoUIState
import com.github.swent.swisstravel.ui.trip.tripinfos.TripInfoViewModelContract
import com.mapbox.geojson.Point
import java.time.ZoneId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

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
}
