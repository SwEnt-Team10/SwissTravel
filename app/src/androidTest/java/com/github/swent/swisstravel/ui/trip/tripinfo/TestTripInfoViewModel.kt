package com.github.swent.swisstravel.ui.trip.tripinfo

import com.github.swent.swisstravel.ui.trip.tripinfos.TripInfoUIState
import com.github.swent.swisstravel.ui.trip.tripinfos.TripInfoViewModelContract
import kotlinx.coroutines.flow.StateFlow

/**
 * Test implementation of [TripInfoViewModelContract] that delegates to a [FakeTripInfoViewModel].
 */
class TestTripInfoViewModel(private val fake: FakeTripInfoViewModel) : TripInfoViewModelContract {
  override val uiState: StateFlow<TripInfoUIState> = fake.uiState

  override fun loadTripInfo(uid: String?) = fake.loadTripInfo(uid)

  override fun toggleFavorite() = fake.toggleFavorite()

  override fun clearErrorMsg() = fake.clearErrorMsg()

  override fun setErrorMsg(errorMsg: String) = fake.setErrorMsg(errorMsg)
}
