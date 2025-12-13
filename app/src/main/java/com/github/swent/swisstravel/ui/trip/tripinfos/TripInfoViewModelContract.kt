package com.github.swent.swisstravel.ui.trip.tripinfos

import com.github.swent.swisstravel.model.trip.TripElement
import com.github.swent.swisstravel.model.trip.activity.Activity
import com.github.swent.swisstravel.model.user.User
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

  /** Unlikes the selected activities, removing them from the list of liked activities. */
  fun unlikeSelectedActivities()

  /**
   * If you liked the activity, it will add the activity to the liked activities list of the trip.
   *
   * Otherwise, it is considered as a dislike
   *
   * @param liked a boolean indicating whether you liked the activity or not
   */
  fun swipeActivity(liked: Boolean)

  /**
   * Selects an activity (in the LikedActivitiesScreen) to later unlike it or schedule it
   *
   * @param activity The activity to add to the list of selected liked activities
   */
  fun selectLikedActivity(activity: Activity)

  /**
   * Deselects an activity (in the LikedActivitiesScreen) (used if the user doesn't want to schedule
   * the activity or unlike it)
   *
   * @param activity The activity to add to the list of selected liked activities
   */
  fun deselectLikedActivity(activity: Activity)

  /** Helper to map the tripInfoUIState to a TripSettings. */
  fun mapToTripSettings(): TripSettings

  /**
   * Adds a user as a collaborator to the current trip.
   *
   * Updates the trip in the repository by appending the user's UID to the collaborators list and
   * reloads the local collaborator data.
   *
   * @param user The user to add as a collaborator.
   */
  fun addCollaborator(user: User)

  /**
   * Loads the list of friends available to be added as collaborators and the list of current
   * collaborators for the trip.
   *
   * Fetches the current user's friends (accepted status only) and filters out those who are already
   * collaborators. Also fetches the full User objects for the current trip's collaborators.
   */
  fun loadCollaboratorData()

  /**
   * Removes a user from the current trip's collaborators.
   *
   * Updates the trip in the repository by removing the user's UID from the collaborators list and
   * reloads the local collaborator data.
   *
   * @param user The user to remove.
   */
  fun removeCollaborator(user: User)
}
