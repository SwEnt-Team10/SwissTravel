package com.github.swent.swisstravel.ui.activities

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.github.swent.swisstravel.model.trip.Location
import com.github.swent.swisstravel.model.trip.activity.Activity
import com.github.swent.swisstravel.ui.trip.tripinfos.TripInfoViewModel

/** Done with the help of ChatGPT */
class FindActivitiesViewModel(private val tripInfoViewModel: TripInfoViewModel) : ViewModel() {

  // All activities ever fetched (IDs or unique keys)
  val activitiesFetchedQueue = tripInfoViewModel.uiState.value.activities

  // The current activity displayed on screen
  val currentActivity = mutableStateOf<Activity?>(null)

  fun loadNextActivity(locations: List<Location>) {
    /*
    var activity: Activity? = null
    viewModelScope.launch {
      activity = selectActivities.fetchActivity(locations, activitiesFetchedYet)
    }

    // If null → plus rien à fetch
    currentActivity.value = activity

    activity?.let { activitiesFetchedYet.add(it) }

     */

    currentActivity.value = activitiesFetchedQueue.random()
  }

  fun likeCurrentActivity() {
    currentActivity.value?.let {
      // TODO : Here you would add the activity to the user's liked activities in your data source
    }
  }

  fun dislikeCurrentActivity() {
    currentActivity.value?.let {
      // TODO : put it back on the queue of activities fetched
    }
  }
}
