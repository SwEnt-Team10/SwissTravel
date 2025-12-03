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

/** UI State for the AddPhotosScreen */
data class PhotosUIState(
    val listUri: List<Uri> = emptyList()
    )

/** ViewModel for the AddPhotosScreen */
class PhotosViewModel(
    private val tripsRepository: TripsRepository = TripsRepositoryProvider.repository
) : ViewModel() {
  private val _uiState = MutableStateFlow(PhotosUIState())
  val uiState: StateFlow<PhotosUIState> = _uiState.asStateFlow()

  /**
   * Add photos to the trip and save the trip on the repo
   *
   * @param tripId the uid of the trip
   * @param uris the uris of the photos to add to the trip
   */
  fun addPhotos(tripId: String, uris: List<Uri>) {
    viewModelScope.launch {
      val oldTrip = tripsRepository.getTrip(tripId)
      val newTrip = oldTrip.copy(listUri = oldTrip.listUri + uris)
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
}
