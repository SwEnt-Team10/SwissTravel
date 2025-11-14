package com.github.swent.swisstravel.ui.trip.tripinfo

import com.github.swent.swisstravel.model.trip.Location
import com.github.swent.swisstravel.model.trip.RouteSegment
import com.github.swent.swisstravel.model.trip.TripProfile
import com.github.swent.swisstravel.model.trip.activity.Activity
import com.github.swent.swisstravel.ui.trip.tripinfos.TripInfoUIState
import com.github.swent.swisstravel.ui.trip.tripinfos.TripInfoViewModelContract
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

  // ——— convenience setters for tests ———
  fun setName(name: String) {
    _ui.value = _ui.value.copy(name = name)
  }

  fun setLocations(locs: List<Location>) {
    _ui.value = _ui.value.copy(locations = locs)
  }

  fun setActivities(acts: List<Activity>) {
    _ui.value = _ui.value.copy(activities = acts)
  }

  fun setTripProfile(profile: TripProfile?) {
    _ui.value = _ui.value.copy(tripProfile = profile)
  }

  fun setFavorite(value: Boolean) {
    _ui.value = _ui.value.copy(isFavorite = value)
  }

  fun setRouteSegments(segments: List<RouteSegment>) {
    _ui.value = _ui.value.copy(routeSegments = segments)
  }
}
