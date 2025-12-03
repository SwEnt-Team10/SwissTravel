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
data class SelectPinnedPhotosUIState(
    val listUri: List<Uri> = emptyList(),
    var errorMsg: String? = null
)

/** ViewModel for the selectPinnedPhotosScreen */
class SelectPinnedPhotosViewModel(
    private val userRepository: UserRepository = UserRepositoryFirebase()
) : ViewModel() {
  private val _uiState = MutableStateFlow(SelectPinnedPhotosUIState())
  val uiState: StateFlow<SelectPinnedPhotosUIState> = _uiState.asStateFlow()

  private var currentUser: User? = null

  // Loads the user's photos into the UI state
  init {
    viewModelScope.launch {
      try {
        val user = userRepository.getCurrentUser()
        currentUser = user
        addUri(user.pinnedImagesUris)
      } catch (e: Exception) {
        _uiState.value = uiState.value.copy(errorMsg = "Error fetching user images: ${e.message}")
      }
    }
  }

  /**
   * Adds a list of URIs to the UI state.
   *
   * @param uris The list of URIs to add to the UI state.
   */
  fun addUri(uris: List<Uri>) {
    _uiState.value = _uiState.value.copy(listUri = _uiState.value.listUri + uris)
  }

  /** Saves the selected photos to the user's profile. */
  fun savePhotos() {
    viewModelScope.launch {
      val user = currentUser ?: return@launch
      try {
        userRepository.updateUser(uid = user.uid, pinnedImagesUris = _uiState.value.listUri)
      } catch (e: Exception) {
        _uiState.update { it.copy(errorMsg = "Error saving image(s): ${e.message}") }
      }
    }
  }

  /** Clears the error message in the UI state. */
  fun clearErrorMsg() {
    _uiState.update { it.copy(errorMsg = null) }
  }
}
