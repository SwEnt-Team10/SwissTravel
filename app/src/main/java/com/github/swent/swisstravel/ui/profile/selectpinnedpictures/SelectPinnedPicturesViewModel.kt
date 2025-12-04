package com.github.swent.swisstravel.ui.profile.selectpinnedpictures

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

/** UI State for the selectPinnedPicturesScreen */
data class SelectPinnedPicturesUIState(
    val listUri: List<Uri> = emptyList(),
    var errorMsg: String? = null
)

/** ViewModel for the selectPinnedPicturesScreen */
class SelectPinnedPicturesViewModel(
    private val userRepository: UserRepository = UserRepositoryFirebase()
) : ViewModel() {
  private val _uiState = MutableStateFlow(SelectPinnedPicturesUIState())
  val uiState: StateFlow<SelectPinnedPicturesUIState> = _uiState.asStateFlow()

  private var currentUser: User? = null

  // Loads the user's pictures into the UI state
  init {
    viewModelScope.launch {
      try {
        val user = userRepository.getCurrentUser()
        currentUser = user
        addUri(user.pinnedPicturesUris)
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
    _uiState.update { it.copy(listUri = it.listUri + uris) }
  }

  /** Saves the selected pictures to the user's profile. */
  fun savePictures() {
    viewModelScope.launch {
      val user = currentUser ?: return@launch
      try {
        userRepository.updateUser(uid = user.uid, pinnedPicturesUris = _uiState.value.listUri)
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
