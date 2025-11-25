package com.github.swent.swisstravel.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.swent.swisstravel.model.trip.Trip
import com.github.swent.swisstravel.model.trip.TripsRepository
import com.github.swent.swisstravel.model.trip.TripsRepositoryFirestore
import com.github.swent.swisstravel.model.trip.isPast
import com.github.swent.swisstravel.model.user.Preference
import com.github.swent.swisstravel.model.user.PreferenceRules
import com.github.swent.swisstravel.model.user.StatsCalculator
import com.github.swent.swisstravel.model.user.User
import com.github.swent.swisstravel.model.user.UserRepository
import com.github.swent.swisstravel.model.user.UserRepositoryFirebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * A data class representing the state of the profile settings screen.
 *
 * @property isLoading Whether the screen is currently loading.
 * @property profilePicUrl The URL of the user's profile picture.
 * @property name The user's name.
 * @property biography The user's biography.
 * @property email The user's email
 * @property selectedPreferences The user's selected preferences.
 * @property errorMsg The error message to display.
 */
data class ProfileSettingsUIState(
    val isLoading: Boolean = true,
    val profilePicUrl: String = "",
    val name: String = "",
    val biography: String = "",
    val email: String = "",
    var selectedPreferences: List<Preference> = emptyList(),
    var errorMsg: String? = null
)

/**
 * A view model for the profile settings screen.
 *
 * @param userRepository The repository for users.
 * @param tripsRepository The repository for trips.
 */
class ProfileSettingsViewModel(
    private val userRepository: UserRepository = UserRepositoryFirebase(),
    private val tripsRepository: TripsRepository = TripsRepositoryFirestore(),
) : ViewModel() {

  private val _uiState = MutableStateFlow(ProfileSettingsUIState())
  private var currentUser: User? = null
  val uiState: StateFlow<ProfileSettingsUIState> = _uiState.asStateFlow()

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

  /**
   * Automatically fills the UI state with the user's data.
   *
   * @param loggedIn The user to fill the UI state with.
   */
  fun autoFill(loggedIn: User) {
    val sanitized = PreferenceRules.enforceMutualExclusivity(loggedIn.preferences)
    _uiState.value =
        ProfileSettingsUIState(
            profilePicUrl = loggedIn.profilePicUrl,
            name = loggedIn.name,
            email = loggedIn.email,
            selectedPreferences = sanitized)
  }

  /** Clears the error message in the UI state. */
  fun clearErrorMsg() {
    _uiState.update { it.copy(errorMsg = null) }
  }

  /**
   * Saves the user's preferences.
   *
   * @param selected The list of selected preferences.
   */
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
