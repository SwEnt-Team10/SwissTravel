package com.github.swent.swisstravel.ui.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.swent.swisstravel.model.user.User
import com.github.swent.swisstravel.model.user.UserRepository
import com.github.swent.swisstravel.model.user.UserRepositoryFirebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FriendsListScreenUIState(
    val friends: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val errorMsg: String? = null,
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val searchResults: List<User> = emptyList(),
)

class FriendsViewModel(private val userRepository: UserRepository = UserRepositoryFirebase()) :
    ViewModel() {

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

  val friendsToDisplay =
      if (_uiState.value.searchQuery.isBlank()) {
        _uiState.value.friends
      } else {
        _uiState.value.friends.filter { user ->
          val q = _uiState.value.searchQuery.trim()
          user.name.contains(q, ignoreCase = true) || (user.email.contains(q, ignoreCase = true))
        }
      }

  /** Clears the current error message. */
  fun clearErrorMsg() {
    _uiState.value = uiState.value.copy(errorMsg = null)
  }

  fun updateSearchQuery(query: String) {
    _uiState.update { it.copy(searchQuery = query) }
  }

  fun toggleSearch() {
    _uiState.update { it.copy(isSearching = !it.isSearching, searchQuery = "") }
  }

  fun searchUsersGlobal(query: String) {
    if (query.isBlank()) {
      _uiState.update { it.copy(searchResults = emptyList()) }
      return
    }

    viewModelScope.launch {
      val results = userRepository.getUserByNameOrEmail(query)
      _uiState.update { it.copy(searchResults = results) }
    }
  }

  fun sendFriendRequest(toUid: String) {
    viewModelScope.launch {
      val currentUserId = userRepository.getCurrentUser().uid
      try {
        userRepository.sendFriendRequest(currentUserId, toUid)
      } catch (e: Exception) {
        _uiState.update { it.copy(errorMsg = "Error sending friend request: ${e.message}") }
      }
    }
  }
}
