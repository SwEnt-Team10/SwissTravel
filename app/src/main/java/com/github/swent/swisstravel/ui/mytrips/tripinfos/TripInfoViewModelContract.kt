package com.github.swent.swisstravel.ui.mytrips.tripinfos

import kotlinx.coroutines.flow.StateFlow

interface TripInfoViewModelContract {
  val uiState: StateFlow<TripInfoUIState>

  fun loadTripInfo(uid: String?)

  fun toggleFavorite()

  fun clearErrorMsg()
}
