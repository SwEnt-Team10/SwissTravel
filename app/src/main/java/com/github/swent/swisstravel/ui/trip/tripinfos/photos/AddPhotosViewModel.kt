package com.github.swent.swisstravel.ui.trip.tripinfos.photos

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.github.swent.swisstravel.model.trip.TripsRepository
import com.github.swent.swisstravel.model.trip.TripsRepositoryProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AddPhotosUIState(
    val listUri: List<Uri> = emptyList<Uri>(),
    val numberUri: Int = 0
)
class AddPhotosViewModel(
    private val tripsRepository: TripsRepository = TripsRepositoryProvider.repository
): ViewModel() {
    private val _uiState = MutableStateFlow(AddPhotosUIState())
    val uiState: StateFlow<AddPhotosUIState> = _uiState.asStateFlow()
    fun addUri(uri: List<Uri>) {
        _uiState.value = _uiState.value.copy(listUri = _uiState.value.listUri + uri, numberUri = _uiState.value.numberUri + 1)
    }
}