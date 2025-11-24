package com.github.swent.swisstravel.ui.profile

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.swent.swisstravel.model.trip.Trip
import com.github.swent.swisstravel.model.trip.TripsRepository
import com.github.swent.swisstravel.model.trip.TripsRepositoryFirestore
import com.github.swent.swisstravel.model.trip.isPast
import com.github.swent.swisstravel.model.user.StatsCalculator
import com.github.swent.swisstravel.model.user.User
import com.github.swent.swisstravel.model.user.UserRepository
import com.github.swent.swisstravel.model.user.UserStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUIState(
    val uid: String = "",
    val isLoading: Boolean = true,
    val isOwnProfile: Boolean = false,
    val profilePicUrl: String = "",
    val name: String = "",
    val biography: String = "",
    val stats: UserStats = UserStats(),
    val pinnedTrips: List<Trip> = emptyList(),
    val pinnedImages: List<Uri> = emptyList(),
    var errorMsg: String? = null
)

class ProfileViewModel(
    private val userRepository: UserRepository,
    private val tripsRepository: TripsRepository = TripsRepositoryFirestore(),
) : ViewModel() {

  private val _uiState = MutableStateFlow(ProfileUIState())
  private var currentUser: User? = null
  val uiState: StateFlow<ProfileUIState> = _uiState.asStateFlow()

  init {
    viewModelScope.launch {
      try {
        val user = userRepository.getCurrentUser()
        currentUser = user
        autoFill(user)
        refreshStatsForUser(user)
      } catch (e: Exception) {
        setErrorMsg("Error fetching user data: ${e.message}")
      } finally {
        _uiState.update { it.copy(isOwnProfile = it.uid == currentUser?.uid) }
        _uiState.update { it.copy(isLoading = false) }
      }
    }
  }

  /**
   * Automatically fills the UI state with the user's data.
   *
   * @param loggedIn The user to fill the UI state with.
   */
  fun autoFill(loggedIn: User) {
    _uiState.value =
        ProfileUIState(
            profilePicUrl = loggedIn.profilePicUrl,
            name = loggedIn.name,
            biography = loggedIn.biography,
            stats = loggedIn.stats,
            // todo pinnedTripsUids = loggedIn.pinnedTripsUids,
            // todo pinnedImages = loggedIn.pinnedImages
        )
  }

  /**
   * Loads the profile information for the given user ID
   *
   * @param uid the unique identifier of the user
   */
  fun loadProfileInfo(uid: String?) {
    if (uid.isNullOrBlank()) {
      Log.e("ProfileViewModel", "User ID is null or blank")
      setErrorMsg("User ID is invalid")
      return
    }
    viewModelScope.launch {
      try {
        val profile = userRepository.getUserById(uid) // todo
        val pinnedTrips = profile.pinnedTripsUids.mapNotNull { uid -> tripsRepository.getTrip(uid) }
        _uiState.value =
            ProfileUIState(
                profilePicUrl = profile.profilePicUrl,
                name = profile.name,
                biography = profile.biography,
                stats = profile.stats,
                pinnedTrips = pinnedTrips,
                pinnedImages = profile.pinnedImages)
      } catch (e: Exception) {
        Log.e("ProfileViewModel", "Error loading profile info", e)
        setErrorMsg("Failed to load profile info: ${e.message}")
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
   * Sets the error message in the UI state
   *
   * @param errorMsg the error message to set
   */
  fun setErrorMsg(errorMsg: String) {
    _uiState.value = _uiState.value.copy(errorMsg = errorMsg)
  }

  /** Clears the error message in the UI state. */
  fun clearErrorMsg() {
    _uiState.update { it.copy(errorMsg = null) }
  }
}
