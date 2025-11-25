package com.github.swent.swisstravel.ui.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.swent.swisstravel.model.user.FriendStatus
import com.github.swent.swisstravel.model.user.User
import com.github.swent.swisstravel.model.user.UserRepository
import com.github.swent.swisstravel.model.user.UserRepositoryFirebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FriendsListScreenUIState(
    val friends: List<User> = emptyList(),
    val pendingFriends: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val errorMsg: String? = null,
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val searchResults: List<User> = emptyList(),
    val currentUserUid: String = "",
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

      val acceptedFriends = currentUser.friends.filter { it.status == FriendStatus.ACCEPTED }
      val pendingEntries = currentUser.friends.filter { it.status == FriendStatus.PENDING_INCOMING }

      val friends = acceptedFriends.mapNotNull { friend -> userRepository.getUserByUid(friend.uid) }
      val pendingUsers =
          pendingEntries.mapNotNull { friend -> userRepository.getUserByUid(friend.uid) }

      _uiState.value =
          uiState.value.copy(
              friends = friends,
              pendingFriends = pendingUsers,
              isLoading = false,
              currentUserUid = currentUser.uid)
    } catch (e: Exception) {
      _uiState.value =
          uiState.value.copy(isLoading = false, errorMsg = "Error fetching friends: ${e.message}")
    }
  }

  val friendsToDisplay: List<User>
    get() {
      val state = _uiState.value
      val q = state.searchQuery.trim()
      val base = state.friends.filter { it.uid != state.currentUserUid }

      return if (q.isBlank()) {
        base
      } else {
        base.filter { user ->
          user.name.contains(q, ignoreCase = true) || user.email.contains(q, ignoreCase = true)
        }
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
      val currentUid = _uiState.value.currentUserUid

      _uiState.update { it.copy(searchResults = results.filter { user -> user.uid != currentUid }) }
    }
  }

  fun sendFriendRequest(toUid: String) {
    viewModelScope.launch {
      try {
        userRepository.sendFriendRequest(_uiState.value.currentUserUid, toUid)
        refreshFriends()
      } catch (e: Exception) {
        _uiState.update { it.copy(errorMsg = "Error sending friend request: ${e.message}") }
      }
    }
  }

  fun acceptFriendRequest(toUid: String) {
    viewModelScope.launch {
      try {
        userRepository.acceptFriendRequest(_uiState.value.currentUserUid, toUid)
        refreshFriends()
      } catch (e: Exception) {
        _uiState.update { it.copy(errorMsg = "Error accepting friend request: ${e.message}") }
      }
    }
  }

  fun removeFriend(toUid: String) {
    viewModelScope.launch {
      try {
        userRepository.removeFriend(_uiState.value.currentUserUid, toUid)
      } catch (e: Exception) {
        _uiState.update { it.copy(errorMsg = "Error removing friend: ${e.message}") }
      }
    }
  }
}
