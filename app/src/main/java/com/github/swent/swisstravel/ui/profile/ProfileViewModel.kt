package com.github.swent.swisstravel.ui.profile

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.swent.swisstravel.model.trip.Trip
import com.github.swent.swisstravel.model.trip.TripsRepository
import com.github.swent.swisstravel.model.trip.TripsRepositoryProvider
import com.github.swent.swisstravel.model.trip.isPast
import com.github.swent.swisstravel.model.user.Achievement
import com.github.swent.swisstravel.model.user.FriendStatus
import com.github.swent.swisstravel.model.user.StatsCalculator
import com.github.swent.swisstravel.model.user.User
import com.github.swent.swisstravel.model.user.UserRepository
import com.github.swent.swisstravel.model.user.UserRepositoryFirebase
import com.github.swent.swisstravel.model.user.UserStats
import com.github.swent.swisstravel.model.user.computeAchievements
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * A data class representing the state of the profile screen.
 *
 * @property uid The UID of the user.
 * @property isLoading Whether the screen is currently loading.
 * @property isOwnProfile Whether the user is their own profile.
 * @property profilePicUrl The URL of the user's profile picture.
 * @property name The user's name.
 * @property biography The user's biography.
 * @property stats The user's stats.
 * @property pinnedTrips The user's pinned trips.
 * @property pinnedImages The user's pinned images.
 * @property errorMsg The error message to display.
 */
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
    var errorMsg: String? = null,
    var achievements: List<Achievement> = emptyList(),
    val friendsCount: Int = 0,
)

/**
 * A view model for the profile screen.
 *
 * @param userRepository The repository for users.
 * @param tripsRepository The repository for trips.
 * @param requestedUid The UID of the profile to load.
 */
class ProfileViewModel(
    private val userRepository: UserRepository = UserRepositoryFirebase(),
    private val tripsRepository: TripsRepository = TripsRepositoryProvider.repository,
    requestedUid: String
) : ViewModel() {

  private val _uiState = MutableStateFlow(ProfileUIState())
  private var currentUser: User? = null
  val uiState: StateFlow<ProfileUIState> = _uiState.asStateFlow()

  /**
   * Loads a user profile for the given user ID and determines whether it is the current user's
   * profile.
   *
   * @param requestedUid The UID of the profile to load.
   */
  init {
    if (requestedUid.isBlank()) {
      Log.e("ProfileViewModel", "User ID is null or blank")
      setErrorMsg("User ID is invalid")
    }
    viewModelScope.launch {
      try {
        // Load current user
        currentUser = userRepository.getCurrentUser()
        // Check if current user is the same as the profile to load
        val isOwn = currentUser?.uid == requestedUid
        loadProfile(requestedUid)
        _uiState.update { it.copy(uid = requestedUid, isOwnProfile = isOwn) }
        if (isOwn) {
          refreshStatsForUser(currentUser!!)
        }
      } catch (e: Exception) {
        Log.e("ProfileViewModel", "Error loading profile", e)
        setErrorMsg("Failed to load profile: ${e.message}")
      } finally {
        _uiState.update { it.copy(isLoading = false) }
      }
    }
  }

  /**
   * Loads the profile information for the given user ID
   *
   * @param uid the unique identifier of the user
   */
  private suspend fun loadProfile(uid: String) {
    try {
      val profile =
          userRepository.getUserByUid(uid)
              ?: throw IllegalStateException("User with uid $uid not found")
      val pinnedTrips = profile.pinnedTripsUids.map { uid -> tripsRepository.getTrip(uid) }

      val friendsCount = profile.friends.filter { it.status == FriendStatus.ACCEPTED }.size

      val achievements = computeAchievements(stats = profile.stats, friendsCount = friendsCount)
      _uiState.update {
        it.copy(
            profilePicUrl = profile.profilePicUrl,
            name = profile.name,
            biography = profile.biography,
            stats = profile.stats,
            pinnedTrips = pinnedTrips,
            pinnedImages = profile.pinnedImagesUris,
            achievements = achievements,
            friendsCount = friendsCount)
      }
    } catch (e: Exception) {
      Log.e("ProfileViewModel", "Error loading profile info", e)
      setErrorMsg("Failed to load profile info: ${e.message}")
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
