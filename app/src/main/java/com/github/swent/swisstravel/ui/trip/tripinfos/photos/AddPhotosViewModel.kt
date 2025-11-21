package com.github.swent.swisstravel.ui.trip.tripinfos.photos

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.swent.swisstravel.model.trip.TripsRepository
import com.github.swent.swisstravel.model.trip.TripsRepositoryProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AddPhotosUIState(
    val listUri: List<Uri> = emptyList()
)
class AddPhotosViewModel(
    private val tripsRepository: TripsRepository = TripsRepositoryProvider.repository
): ViewModel() {
    private val _uiState = MutableStateFlow(AddPhotosUIState())
    val uiState: StateFlow<AddPhotosUIState> = _uiState.asStateFlow()
    fun addUri(uri: List<Uri>) {
        _uiState.value = _uiState.value.copy(listUri = _uiState.value.listUri + uri)
    }
    fun savePhoto(tripId: String) {
        viewModelScope.launch {
            val oldTrip = tripsRepository.getTrip(tripId)
            val newTrip = oldTrip.copy(listUri = _uiState.value.listUri)
            tripsRepository.editTrip(tripId, newTrip)
        }
    }
    fun loadPhotos(tripId: String) {
        viewModelScope.launch {
            val trip = tripsRepository.getTrip(tripId)
            _uiState.value = AddPhotosUIState(listUri = trip.listUri)
        }
    }
}