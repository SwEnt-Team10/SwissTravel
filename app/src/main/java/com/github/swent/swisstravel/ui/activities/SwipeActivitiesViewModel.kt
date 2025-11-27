package com.github.swent.swisstravel.ui.activities

import androidx.lifecycle.ViewModel
import com.github.swent.swisstravel.model.trip.activity.Activity
import com.github.swent.swisstravel.ui.trip.tripinfos.TripInfoViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SwipeActivitiesUIState(
    val activitiesQueue: List<Activity>? = null,
    val currentActivity: Activity? = activitiesQueue?.getOrNull(0),
    val backActivity: Activity? = activitiesQueue?.getOrNull(1)
)

/** Done with the help of ChatGPT */
class SwipeActivitiesViewModel(private val tripInfoViewModel: TripInfoViewModel) : ViewModel() {

  /** UI state for the Swipe Activities screen */
  private val _uiState: MutableStateFlow<SwipeActivitiesUIState> =
      MutableStateFlow(
          SwipeActivitiesUIState(
              activitiesQueue =
                  ArrayDeque(
                      tripInfoViewModel.uiState.value.activities.filter { activity ->
                        activity !in tripInfoViewModel.uiState.value.likedActivities
                      })))
  val uiState = _uiState.asStateFlow()

  fun loadNextActivity() {
    val backActivityIndex: Int? =
        _uiState.value.activitiesQueue?.indexOf(_uiState.value.backActivity)
    // current activity becomes back activity
    // back activity becomes the next in the queue
    _uiState.value =
        _uiState.value.copy(
            currentActivity = _uiState.value.backActivity,
            backActivity = _uiState.value.activitiesQueue?.getOrNull(backActivityIndex!! + 1))
  }

  fun likeCurrentActivity() {
    // Remove currentActivity from the queue
    // Add currentActivity to liked activities
    _uiState.value = _uiState.value.copy(activitiesQueue = _uiState.value.activitiesQueue?.drop(1))
    tripInfoViewModel.likeActivity(_uiState.value.currentActivity!!)
  }

  fun dislikeCurrentActivity() {
    // remove first element
    // put it at the end of the queue
    _uiState.value =
        _uiState.value.copy(
            activitiesQueue =
                _uiState.value.activitiesQueue
                    ?.drop(1)
                    ?.plus(listOf(_uiState.value.currentActivity!!)),
        )
  }

  fun swipeActivity(liked: Boolean) {
    if (liked) likeCurrentActivity() else dislikeCurrentActivity()
    loadNextActivity()
  }
}
