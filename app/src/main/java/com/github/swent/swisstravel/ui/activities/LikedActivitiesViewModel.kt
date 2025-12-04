package com.github.swent.swisstravel.ui.activities

import android.content.Context
import androidx.lifecycle.ViewModel
import com.github.swent.swisstravel.algorithm.TripAlgorithm
import com.github.swent.swisstravel.model.trip.activity.Activity
import com.github.swent.swisstravel.model.trip.activity.ActivityRepositoryMySwitzerland
import com.github.swent.swisstravel.ui.trip.tripinfos.TripInfoViewModel
import com.github.swent.swisstravel.ui.trip.tripinfos.TripInfoViewModelContract
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * State for the liked activities view model
 *
 * @param tripInfoVM The view model for the information of the trip (to have access to the liked
 *   activities)
 * @param selectedLikedActivities A list of the activities selected in the liked activities screen
 */
data class LikedActivitiesViewModelState(
    val tripInfoVM: TripInfoViewModelContract = TripInfoViewModel(),
    val selectedLikedActivities: List<Activity> = emptyList()
)

class LikedActivitiesViewModel(private val tripInfoVM: TripInfoViewModelContract) : ViewModel() {

  private val _state = MutableStateFlow(LikedActivitiesViewModelState(tripInfoVM = tripInfoVM))
  val state = _state.asStateFlow()

  /**
   * Selects an activity (to later unlike it or schedule it)
   *
   * @param activity The activity to add to the list of selected liked activities
   */
  fun selectActivity(activity: Activity) {
    _state.value =
        _state.value.copy(selectedLikedActivities = _state.value.selectedLikedActivities + activity)
  }

  /**
   * Deselects an activity (used if the user doesn't want to schedule the activity or to unlike it)
   *
   * @param activity The activity to add to the list of selected liked activities
   */
  fun deselectActivity(activity: Activity) {
    _state.value =
        _state.value.copy(selectedLikedActivities = _state.value.selectedLikedActivities - activity)
  }

  /**
   * Schedules the selected liked activities.
   *
   * If there is no room for selected activities to be scheduled, it will respond with a toast
   */
  fun scheduleSelectedActivities(context: Context) {
    val tripAlgo =
        TripAlgorithm.init(
            tripSettings = _state.value.tripInfoVM.mapToTripSettings(),
            activityRepository = ActivityRepositoryMySwitzerland(),
            context = context)
    // TODO : finish scheduling code

    // at the end, don't remove the selected activities from the liked activities in case the user
    // un-schedules
    // them but wants to re-schedule them afterwards (so that the user doesn't have to find them by
    // swiping)
  }

  /** Unlikes all the liked activities that were selected */
  fun unlikeSelectedActivities() {
    _state.value.tripInfoVM.unlikeActivities(_state.value.selectedLikedActivities)
    _state.value = _state.value.copy(selectedLikedActivities = emptyList())
  }
}
