package com.github.swent.swisstravel.ui.trip.tripinfos.photos

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.swent.swisstravel.model.trip.Coordinate
import com.github.swent.swisstravel.model.trip.Location
import com.github.swent.swisstravel.model.trip.TripsRepository
import com.github.swent.swisstravel.model.trip.TripsRepositoryProvider
import com.github.swent.swisstravel.utils.photos.getPhotoLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** The messages that the toast can have. */
object ToastMessages {
  const val PHOTOS_SAVED = "Photos saved"
  const val PHOTO_SAVED = "Photo saved"
  const val ERROR_SAVING_PHOTOS = "Could not save the photos"
  const val ERROR_SAVING_PHOTO = "Could not save the photo"
  const val LOAD_PHOTOS_SUCCESS = "Successfully loaded the photos"
  const val LOAD_PHOTO_SUCCESS = "Successfully loaded the photo"
  const val REMOVE_PHOTOS_SUCCESS = "Photos removed"
  const val REMOVE_PHOTO_SUCCESS = "Photo removed"
  const val REMOVE_PHOTOS_FAIL = "Could not remove the photos"
  const val REMOVE_PHOTO_FAIL = "Could not remove the photo"
}

/** UI State for the AddPhotosScreen */
data class PhotosUIState(
    val uriLocation: Map<Uri, Location> = emptyMap(),
    val uriSelected: List<Int> = emptyList(),
    val errorLoading: Boolean = false,
    val toastMessage: String = "",
    val isLoading: Boolean = true,
    val numberNew: Int = 0
)

/**
 * ViewModel for the AddPhotosScreen Note: all the part with the loading has been done with AI.
 *
 * @param tripsRepository the repository that the model use
 */
class PhotosViewModel(
    private val tripsRepository: TripsRepository = TripsRepositoryProvider.repository,
) : ViewModel() {
  private val _uiState = MutableStateFlow(PhotosUIState())
  val uiState: StateFlow<PhotosUIState> = _uiState.asStateFlow()
    private val defaultLocation = Location(
        Coordinate(
            latitude = 0.0,
            longitude = 0.0
        ),
        name = "No location"
    )

  /**
   * Add photos to the trip and save the trip on the repo.
   *
   * @param tripId the uid of the trip
   */
  fun savePhotos(tripId: String) {
    viewModelScope.launch {
      try {
        val oldTrip = tripsRepository.getTrip(tripId)
        val newTrip = oldTrip.copy(uriLocation = _uiState.value.uriLocation)
        tripsRepository.editTrip(tripId, newTrip)
        if (_uiState.value.numberNew > 1) {
          setToastMessage(ToastMessages.PHOTOS_SAVED)
        } else {
          setToastMessage(ToastMessages.PHOTO_SAVED)
        }
        _uiState.value = _uiState.value.copy(numberNew = 0)
      } catch (e: Exception) {
        if (_uiState.value.numberNew > 1) {
          setToastMessage(ToastMessages.ERROR_SAVING_PHOTOS)
        } else {
          setToastMessage(ToastMessages.ERROR_SAVING_PHOTO)
        }
      }
    }
  }

  /**
   * Load the Uris of the photos from the trip.
   *
   * @param tripId the Id of the trip
   */
  fun loadPhotos(tripId: String) {
    _uiState.value = _uiState.value.copy(isLoading = true, errorLoading = false, toastMessage = "")
    viewModelScope.launch {
      try {
        val trip = tripsRepository.getTrip(tripId)
        if (trip.uriLocation.size > 1) {

          _uiState.value =
              _uiState.value.copy(
                  uriLocation = trip.uriLocation,
                  isLoading = false,
                  toastMessage = ToastMessages.LOAD_PHOTOS_SUCCESS,
                  )
        } else {
          _uiState.value =
              _uiState.value.copy(
                  uriLocation = trip.uriLocation,
                  isLoading = false,
                  toastMessage = ToastMessages.LOAD_PHOTO_SUCCESS)
        }
      } catch (e: Exception) {
        _uiState.value = _uiState.value.copy(isLoading = false, errorLoading = true)
      }
    }
  }

  /**
   * Add the uris of the photos to the PhotosUiState.
   *
   * @param uris the uris of the photos to add to the state
   */
  fun addUris(uris: List<Uri>, context: Context, tripId: String) {
      val newEntries = uris.associateWith { uri ->
          context.getPhotoLocation(uri, "Photo") ?: defaultLocation
      }
      _uiState.value = _uiState.value.copy(uriLocation = _uiState.value.uriLocation + newEntries)
  }

  /**
   * Add or remove the index of a photos already added from the state.
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
   * Remove the photos from the repository depending on the photos selected on edit mode.
   *
   * @param tripId the uid of the trip
   */
  fun removePhotos(tripId: String) {
    // Done with AI
    val oldState = _uiState.value
    val selected = _uiState.value.uriSelected.toSet()

      val currentKeys = oldState.uriLocation.keys.toList()
      val newMap = oldState.uriLocation.filterKeys { uri ->
          val index = currentKeys.indexOf(uri)
          index !in selected
      }

    _uiState.value = _uiState.value.copy(uriLocation = newMap, uriSelected = emptyList())
    viewModelScope.launch {
      try {
        val oldTrip = tripsRepository.getTrip(tripId)
        val newTrip = oldTrip.copy(uriLocation = _uiState.value.uriLocation)
        tripsRepository.editTrip(tripId, newTrip)
        if (oldState.uriSelected.size > 1) {
          setToastMessage(ToastMessages.REMOVE_PHOTOS_SUCCESS)
        } else {
          setToastMessage(ToastMessages.REMOVE_PHOTO_SUCCESS)
        }
      } catch (e: Exception) {
        _uiState.value = oldState
        if (_uiState.value.uriSelected.size > 1) {
          setToastMessage(ToastMessages.REMOVE_PHOTOS_FAIL)
        } else {
          setToastMessage(ToastMessages.REMOVE_PHOTO_FAIL)
        }
      }
    }
  }

  /**
   * Set the toast message of the state with a given message.
   *
   * @param message the message to set on the state
   */
  private fun setToastMessage(message: String) {
    _uiState.value = _uiState.value.copy(toastMessage = message)
  }

  /** Reset the toast message on the state. */
  fun clearToastMessage() {
    _uiState.value = _uiState.value.copy(toastMessage = "")
  }
}
