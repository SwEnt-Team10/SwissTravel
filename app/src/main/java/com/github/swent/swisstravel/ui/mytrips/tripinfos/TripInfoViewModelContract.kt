package com.github.swent.swisstravel.ui.mytrips.tripinfos

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
}
