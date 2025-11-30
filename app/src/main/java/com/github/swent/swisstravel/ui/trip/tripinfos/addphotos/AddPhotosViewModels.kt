package com.github.swent.swisstravel.ui.trip.tripinfos.addphotos

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.swent.swisstravel.model.trip.TripsRepository
import com.github.swent.swisstravel.model.trip.TripsRepositoryProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** UI State for the AddPhotosScreen */
data class AddPhotosUIState(val listUri: List<Uri> = emptyList())

/** ViewModel for the AddPhotosScreen */
class AddPhotosViewModel(
    private val tripsRepository: TripsRepository = TripsRepositoryProvider.repository
) : ViewModel() {
  private val _uiState = MutableStateFlow(AddPhotosUIState())
  val uiState: StateFlow<AddPhotosUIState> = _uiState.asStateFlow()

  /**
   * Add the uris of photos to the state
   *
   * @param uris the uris of photos to add
   */
  fun addUri(uris: List<Uri>) {
    _uiState.value = _uiState.value.copy(listUri = _uiState.value.listUri + uris)
  }

  /**
   * Save the Uris of the photos added before to the Trip
   *
   * @param tripId the Id of the trip
   */
  fun savePhotos(tripId: String) {
    viewModelScope.launch {
      val oldTrip = tripsRepository.getTrip(tripId)
      val newTrip = oldTrip.copy(listUri = _uiState.value.listUri)
      tripsRepository.editTrip(tripId, newTrip)
    }
  }

  /**
   * Load the Uris of the photos from the trip
   *
   * @param tripId the Id of the trip
   */
  fun loadPhotos(tripId: String) {
    viewModelScope.launch {
      val trip = tripsRepository.getTrip(tripId)
      _uiState.value = AddPhotosUIState(listUri = trip.listUri)
    }
  }
}
