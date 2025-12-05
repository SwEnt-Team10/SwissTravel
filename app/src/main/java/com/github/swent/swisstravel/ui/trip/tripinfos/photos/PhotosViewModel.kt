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
    val listUri: List<Uri> = emptyList(),
    val uriSelected: List<Int> = emptyList(),
    val error: Boolean = false,
    val toastMessage: String = "",
    // With AI
    val isLoading: Boolean = true
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
   */
  fun savePhotos(tripId: String) {
      viewModelScope.launch {
          try {
              val oldTrip = tripsRepository.getTrip(tripId)
              val newTrip = oldTrip.copy(listUri = _uiState.value.listUri)
              tripsRepository.editTrip(tripId, newTrip)
          } catch (e: Exception) {
              setErrorMessage("Could not save the photos")
          }
      }
  }

  /**
   * Load the Uris of the photos from the trip
   *
   * @param tripId the Id of the trip
   */
  fun loadPhotos(tripId: String) {
      _uiState.value = _uiState.value.copy(isLoading = true, error = false, toastMessage = "")
      viewModelScope.launch {
          try {
              val trip = tripsRepository.getTrip(tripId)
              _uiState.value = _uiState.value.copy(listUri = trip.listUri, isLoading = false, toastMessage = "Successfully loaded photos")
          } catch (e: Exception) {
              setErrorMessage("Could not load the photos")
          }
      }
  }

  /**
   * Add the uris of the photos to the PhotosUiState
   *
   * @param uris the uris of the photos to add to the state
   */
  fun addUris(uris: List<Uri>) {
    _uiState.value = _uiState.value.copy(listUri = uris + _uiState.value.listUri)
  }

  /**
   * Add or remove the index of a photos already added from the state
   *
   * @param index the index of the photo to remove or add to the selected photos of the state
   */
  fun selectToRemove(index: Int) {
    val current = _uiState.value.uriSelected.toMutableList()
    if (current.contains(index)) {
      current.remove(index)
    } else {
      current.add(index)
    }
    _uiState.value = _uiState.value.copy(uriSelected = current)
  }

  /**
   * Remove the photos from the repository depending on the photos selected on edit mode
   *
   * @param tripId the uid of the trip
   */
  fun removePhotos(tripId: String) {
    // Done with AI
      val oldState = _uiState.value
    val selected = _uiState.value.uriSelected.toSet()

    val newList = _uiState.value.listUri.filterIndexed { index, _ -> index !in selected }

    _uiState.value = _uiState.value.copy(listUri = newList, uriSelected = emptyList())
    viewModelScope.launch {
        try {
            val oldTrip = tripsRepository.getTrip(tripId)
            val newTrip = oldTrip.copy(listUri = _uiState.value.listUri)
            tripsRepository.editTrip(tripId, newTrip)
        } catch (e: Exception) {
            _uiState.value = oldState
            setErrorMessage("Could not remove photos")
        }
    }
  }

    /**
     * Reset the error state of the ui state
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = false, toastMessage = "")
    }

    private fun setErrorMessage(message: String) {
        _uiState.value = _uiState.value.copy(error = true, toastMessage = message, isLoading = false)
    }
}
