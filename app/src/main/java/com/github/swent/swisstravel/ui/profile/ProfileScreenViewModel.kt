package com.github.swent.swisstravel.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.swent.swisstravel.model.trip.Trip
import com.github.swent.swisstravel.model.trip.TripsRepository
import com.github.swent.swisstravel.model.trip.isPast
import com.github.swent.swisstravel.model.user.Preference
import com.github.swent.swisstravel.model.user.PreferenceRules
import com.github.swent.swisstravel.model.user.StatsCalculator
import com.github.swent.swisstravel.model.user.User
import com.github.swent.swisstravel.model.user.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileScreenUIState(
    val isLoading: Boolean = true,
    val profilePicUrl: String = "",
    val name: String = "",
    val email: String = "",
    var selectedPreferences: List<Preference> = emptyList(),
    var errorMsg: String? = null
)

class ProfileScreenViewModel(
    private val userRepository: UserRepository,
    private val tripsRepository: TripsRepository,
) : ViewModel() {

  private val _uiState = MutableStateFlow(ProfileScreenUIState())
  private var currentUser: User? = null
  val uiState: StateFlow<ProfileScreenUIState> = _uiState.asStateFlow()

  init {
    viewModelScope.launch {
      try {
        val user = userRepository.getCurrentUser()
        currentUser = user
        autoFill(user)
        refreshStatsForUser(user)
      } catch (e: Exception) {
        _uiState.value = uiState.value.copy(errorMsg = "Error fetching user data: ${e.message}")
      } finally {
        _uiState.update { it.copy(isLoading = false) }
      }
    }
  }

  /**
   * Refreshes the user's stats based on their past trips.
   *
   * @param user The user to refresh stats for.
   * @throws Exception If an error occurs while refreshing the stats.
   */
  private suspend fun refreshStatsForUser(user: User) {
    if (user.uid == "guest") return

    try {
      val trips: List<Trip> = tripsRepository.getAllTrips()
      val pastTrips = trips.filter { it.isPast() }

      val stats = StatsCalculator.computeStats(pastTrips)
      userRepository.updateUserStats(user.uid, stats)
    } catch (e: Exception) {
      _uiState.update { it.copy(errorMsg = it.errorMsg ?: "Error updating stats: ${e.message}") }
    }
  }

  fun autoFill(loggedIn: User) {
    val sanitized = PreferenceRules.enforceMutualExclusivity(loggedIn.preferences)
    _uiState.value =
        ProfileScreenUIState(
            profilePicUrl = loggedIn.profilePicUrl,
            name = loggedIn.name,
            email = loggedIn.email,
            selectedPreferences = sanitized)
  }

  fun clearErrorMsg() {
    _uiState.update { it.copy(errorMsg = null) }
  }

  fun savePreferences(selected: List<Preference>) {
    viewModelScope.launch {
      val user = currentUser

      if (user == null || user.uid == "guest") {
        _uiState.update { it.copy(errorMsg = "You must be signed in to save preferences.") }
        return@launch
      }

      val sanitized = PreferenceRules.enforceMutualExclusivity(selected)
      _uiState.update { it.copy(selectedPreferences = sanitized) }

      try {
        userRepository.updateUserPreferences(user.uid, sanitized)
      } catch (e: Exception) {
        _uiState.value = uiState.value.copy(errorMsg = "Error saving preferences: ${e.message}")
      }
    }
  }
}
