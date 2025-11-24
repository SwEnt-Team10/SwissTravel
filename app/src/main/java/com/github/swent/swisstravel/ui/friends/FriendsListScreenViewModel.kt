package com.github.swent.swisstravel.ui.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.swent.swisstravel.model.user.User
import com.github.swent.swisstravel.model.user.UserRepository
import com.github.swent.swisstravel.model.user.UserRepositoryFirebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FriendsListScreenUIState(
    val friends: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val errorMsg: String? = null
)

class FriendsListScreenViewModel(
    private val userRepository: UserRepository = UserRepositoryFirebase()
) : ViewModel() {

  private val _uiState = MutableStateFlow(FriendsListScreenUIState())
  val uiState = _uiState.asStateFlow()

  init {
    viewModelScope.launch { refreshFriends() }
  }

  /** Refreshes the friends list from the repository. */
  suspend fun refreshFriends() {
    _uiState.value = uiState.value.copy(isLoading = true, errorMsg = null)
    try {
      val currentUser = userRepository.getCurrentUser()
      val friends =
          currentUser.friends.mapNotNull { friend -> userRepository.getUserByUid(friend.uid) }
      _uiState.value = uiState.value.copy(friends = friends, isLoading = false)
    } catch (e: Exception) {
      _uiState.value =
          uiState.value.copy(isLoading = false, errorMsg = "Error fetching friends: ${e.message}")
    }
  }

  /** Clears the current error message. */
  fun clearErrorMsg() {
    _uiState.value = uiState.value.copy(errorMsg = null)
  }
}
