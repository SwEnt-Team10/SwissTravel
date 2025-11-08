package com.github.swent.swisstravel.ui.mytrips.tripinfo

import com.github.swent.swisstravel.ui.mytrips.tripinfos.TripInfoUIState
import com.github.swent.swisstravel.ui.mytrips.tripinfos.TripInfoViewModelContract
import kotlinx.coroutines.flow.StateFlow

/**
 * Test implementation of [TripInfoViewModelContract] that delegates to a [FakeTripInfoViewModel].
 */
class TestTripInfoViewModel(private val fake: FakeTripInfoViewModel) : TripInfoViewModelContract {
  override val uiState: StateFlow<TripInfoUIState> = fake.uiState

  override fun loadTripInfo(uid: String?) = fake.loadTripInfo(uid)

  override fun toggleFavorite() = fake.toggleFavorite()

  override fun clearErrorMsg() = fake.clearErrorMsg()
}
