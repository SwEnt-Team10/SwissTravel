package com.github.swent.swisstravel.ui.activities

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.swent.swisstravel.algorithm.selectactivities.SelectActivities
import com.github.swent.swisstravel.model.trip.activity.Activity
import com.github.swent.swisstravel.ui.trip.tripinfos.TripInfoViewModelContract
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI state for the Swipe Activities screen
 *
 * @param activitiesQueue The queue of activities to swipe through
 * @param currentActivity The current activity being displayed
 * @param backActivity The next activity to be displayed (so that it is more fluid, and to make the
 *   illusion that there is a stack of cards
 * @param activitiesFetcher A class with functions to fetch activities from the MySwitzerland API
 */
data class SwipeActivitiesUIState(
    val activitiesQueue: List<Activity>? = null,
    val currentActivity: Activity? = null,
    val backActivity: Activity? = null,
    val activitiesFetcher: SelectActivities = SelectActivities()
)

/** Done with the help of ChatGPT */
class SwipeActivitiesViewModel(private val tripInfoVM: TripInfoViewModelContract) : ViewModel() {

  /** UI state for the Swipe Activities screen */
  private val _uiState = MutableStateFlow(SwipeActivitiesUIState())
  val uiState = _uiState.asStateFlow()

  init {
    _uiState.value =
        _uiState.value.copy(activitiesFetcher = SelectActivities(tripInfoVM = tripInfoVM))
    viewModelScope.launch {
      _uiState.value =
          _uiState.value.copy(
              activitiesQueue = ArrayDeque(_uiState.value.activitiesFetcher.fetchSwipeActivities()))
      // load initial cards
      updateCards()
    }
  }

  /**
   * Updates the cards to be displayed in the screen.
   *
   * Front card gets the value of the back card.
   *
   * Back card gets the value below itself.
   */
  private fun updateCards() {
    val queue = _uiState.value.activitiesQueue ?: return
    _uiState.value =
        _uiState.value.copy(currentActivity = queue.getOrNull(0), backActivity = queue.getOrNull(1))
  }

  /**
   * If you liked the activity, it will add the activity to the liked activities list of the trip
   * Otherwise, it is considered as a dislike, and it will put the activity at the beginning of the
   * queue
   *
   * @param liked a boolean indicating whether you liked the activity or not
   */
  fun swipeActivity(liked: Boolean) {
    val current = _uiState.value.currentActivity ?: return

    val newQueue =
        if (liked) {
          tripInfoVM.likeActivities(listOf(current))
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
