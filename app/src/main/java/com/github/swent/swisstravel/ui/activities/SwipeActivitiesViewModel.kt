package com.github.swent.swisstravel.ui.activities

import androidx.lifecycle.ViewModel
import com.github.swent.swisstravel.model.trip.activity.Activity
import com.github.swent.swisstravel.ui.trip.tripinfos.TripInfoViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * UI state for the Swipe Activities screen
 *
 * @param activitiesQueue The queue of activities to swipe through
 * @param currentActivity The current activity being displayed
 * @param backActivity The next activity to be displayed (so that it is more fluid, and to make the
 *   illusion that there is a stack of cards
 */
data class SwipeActivitiesUIState(
    val activitiesQueue: List<Activity>? = null,
    val currentActivity: Activity? = null,
    val backActivity: Activity? = null
)

/** Done with the help of ChatGPT */
class SwipeActivitiesViewModel(private val tripInfoViewModel: TripInfoViewModel) : ViewModel() {

  /** UI state for the Swipe Activities screen */
  private val _uiState = MutableStateFlow(SwipeActivitiesUIState())
  val uiState = _uiState.asStateFlow()

  init {
    val all = tripInfoViewModel.uiState.value.activities
    val liked = tripInfoViewModel.uiState.value.likedActivities
    _uiState.value = _uiState.value.copy(activitiesQueue = ArrayDeque(all.filter { it !in liked }))
    // load initial cards
    updateCards()
  }

  private fun updateCards() {
    val queue = _uiState.value.activitiesQueue ?: return
    _uiState.value =
        _uiState.value.copy(currentActivity = queue.getOrNull(0), backActivity = queue.getOrNull(1))
  }

  fun swipeActivity(liked: Boolean) {
    val current = _uiState.value.currentActivity ?: return

    val newQueue =
        if (liked) {
          tripInfoViewModel.likeActivity(current)
          // like => remove the activity from the queue
          _uiState.value.activitiesQueue?.drop(1)
        } else {
          // dislike => move the activity to the back of the queue
          _uiState.value.activitiesQueue?.drop(1)?.plusElement(current)
        }

    // if the queue is null, it means there are no more activities, so put an emptyList
    _uiState.value = _uiState.value.copy(activitiesQueue = ArrayDeque(newQueue ?: emptyList()))
    // load next cards
    updateCards()
  }
}
