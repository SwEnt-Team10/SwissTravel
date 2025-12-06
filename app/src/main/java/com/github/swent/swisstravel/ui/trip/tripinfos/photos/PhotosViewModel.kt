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
    val errorLoading: Boolean = false,
    val toastMessage: String = "",
    val isLoading: Boolean = true
)

/** ViewModel for the AddPhotosScreen
 * Note: all the part with the loading has been done with AI
 *
 * @param tripsRepository the repository that the model use
 * */
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
        if (_uiState.value.listUri.size > 1) {
            setToastMessage("Could not save the photos")
        } else {
            setToastMessage("Could not save the photo")
        }
      }
    }
  }

  /**
   * Load the Uris of the photos from the trip
   *
   * @param tripId the Id of the trip
   */
  fun loadPhotos(tripId: String) {
    _uiState.value = _uiState.value.copy(isLoading = true, errorLoading = false, toastMessage = "")
    viewModelScope.launch {
      try {
        val trip = tripsRepository.getTrip(tripId)
        _uiState.value =
            _uiState.value.copy(
                listUri = trip.listUri,
                isLoading = false,
                toastMessage = "Successfully loaded photos")
      } catch (e: Exception) {
          _uiState.value = _uiState.value.copy(isLoading = false, errorLoading = true)
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
          if (_uiState.value.uriSelected.size > 1) {
              setToastMessage("Could not remove the photos")
          } else {
              setToastMessage("Could not remove the photo")
          }
      }
    }
  }

    /**
     * Set the toast message of the state with a given message
     *
     * @param message the message to set on the state
     */
    fun setToastMessage(message: String) {
        _uiState.value = _uiState.value.copy(toastMessage = message)
    }

    /**
     * Reset the toast message on the state
     */
    fun clearToastMessage() {
        _uiState.value = _uiState.value.copy(toastMessage = "")
    }
}
