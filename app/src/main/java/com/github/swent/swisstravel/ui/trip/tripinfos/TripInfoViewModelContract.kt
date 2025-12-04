package com.github.swent.swisstravel.ui.trip.tripinfos

import com.github.swent.swisstravel.model.trip.TripElement
import com.github.swent.swisstravel.model.trip.activity.Activity
import com.github.swent.swisstravel.ui.tripcreation.TripSettings
import com.mapbox.geojson.Point
import kotlinx.coroutines.flow.StateFlow

/** Contract for the TripInfo ViewModel */
interface TripInfoViewModelContract {
  val uiState: StateFlow<TripInfoUIState>
  /**
   * Loads trip information for the given UID.
   *
   * @param uid The unique identifier of the trip to load.
   */
  fun loadTripInfo(uid: String?)
  /** Toggles the favorite status of the trip. */
  fun toggleFavorite()
  /** Clears any error message in the UI state. */
  fun clearErrorMsg()
  /**
   * Sets an error message in the UI state.
   *
   * @param errorMsg The error message to set.
   */
  fun setErrorMsg(errorMsg: String)

  /**
   * Toggles the fullscreen mode of the map.
   *
   * @param fullscreen The new fullscreen mode state.
   */
  fun toggleFullscreen(fullscreen: Boolean)

  fun selectActivity(activity: Activity?)

  /**
   * Sets the current day index for the daily view.
   *
   * @param index The new index.
   */
  fun setCurrentDayIndex(index: Int)

  /**
   * Sets the selected step in the daily view.
   *
   * @param step The selected trip element.
   */
  fun setSelectedStep(step: TripElement?)

  /**
   * Toggles whether to draw the route from the current user position.
   *
   * @param enabled True to enable, false to disable.
   */
  fun setDrawFromCurrentPosition(enabled: Boolean)

  /**
   * Updates the current user GPS location.
   *
   * @param point The new GPS point.
   */
  fun updateUserLocation(point: Point)

  /**
   * Likes the given activities, adding them to the list of liked activities.
   *
   * @param activities The activities to like.
   */
  fun likeActivities(activities: List<Activity>)

  /**
   * Unlikes the given activities, removing them from the list of liked activities.
   *
   * @param activities The activities to unlike.
   */
  fun unlikeActivities(activities: List<Activity>)

  /**
   * Helper to map the tripInfoUIState to a TripSettings.
   *
   * If the tripProfile parameter from the uiState is null, it will return TripSettings with default
   * parameters
   */
  fun mapToTripSettings(): TripSettings
}
