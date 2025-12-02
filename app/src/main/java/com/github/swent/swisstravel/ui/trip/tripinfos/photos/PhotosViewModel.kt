package com.github.swent.swisstravel.ui.trip.tripinfos.photos.add

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
data class PhotosUIState(
    val listUri: List<Uri> = emptyList(),
    val editMode: Boolean = false
    )

/** ViewModel for the AddPhotosScreen */
class PhotosViewModel(
    private val tripsRepository: TripsRepository = TripsRepositoryProvider.repository
) : ViewModel() {
  private val _uiState = MutableStateFlow(PhotosUIState())
  val uiState: StateFlow<PhotosUIState> = _uiState.asStateFlow()

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
      _uiState.value = PhotosUIState(listUri = trip.listUri)
    }
  }
    fun isOnEditMode(): Boolean {
        return uiState.value.editMode
    }
    fun switchOnEditMode() {
        _uiState.value = _uiState.value.copy(editMode = !_uiState.value.editMode)
    }
}
