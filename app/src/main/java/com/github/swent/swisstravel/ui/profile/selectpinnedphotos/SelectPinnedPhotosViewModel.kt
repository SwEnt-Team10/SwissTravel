package com.github.swent.swisstravel.ui.profile.selectpinnedphotos

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.swent.swisstravel.model.user.User
import com.github.swent.swisstravel.model.user.UserRepository
import com.github.swent.swisstravel.model.user.UserRepositoryFirebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** UI State for the selectPinnedPhotosScreen */
data class SelectPinnedPhotosUIState(val listUri: List<Uri> = emptyList())

/** ViewModel for the selectPinnedPhotosScreen */
class SelectPinnedPhotosViewModel(
    private val userRepository: UserRepository = UserRepositoryFirebase()
) : ViewModel() {
  private val _uiState = MutableStateFlow(SelectPinnedPhotosUIState())
  val uiState: StateFlow<SelectPinnedPhotosUIState> = _uiState.asStateFlow()

  private var currentUser: User? = null

  init {
    viewModelScope.launch {
      try {
        val user = userRepository.getCurrentUser()
        currentUser = user
        addUri(user.pinnedImagesUris)
      } catch (e: Exception) {
        _uiState.value = uiState.value.copy(errorMsg = "Error fetching user data: ${e.message}")
      } finally {
        _uiState.update { it.copy(isLoading = false) }
      }
    }
  }

  /**
   * Add the uris of photos to the state
   *
   * @param uris the uris of photos to add
   */
  fun addUri(uris: List<Uri>) {
    _uiState.value = _uiState.value.copy(listUri = _uiState.value.listUri + uris)
  }

  fun savePhotos() {
    viewModelScope.launch {
      val oldTrip = tripsRepository.getTrip(tripId)
      val newTrip = oldTrip.copy(listUri = _uiState.value.listUri)
      tripsRepository.editTrip(tripId, newTrip)
    }
  }

  fun loadPhotos() {
    viewModelScope.launch {
      val trip = tripsRepository.getTrip(tripId)
      _uiState.value = AddPhotosUIState(listUri = trip.listUri)
    }
  }
}
