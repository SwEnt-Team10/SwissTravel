package com.github.swent.swisstravel.ui.profile

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.swent.swisstravel.model.image.ImageHelper
import com.github.swent.swisstravel.model.image.ImageRepository
import com.github.swent.swisstravel.model.image.ImageRepositoryFirebase
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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
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
 * @property pinnedPicturesUids The user's pinned pictures.
 * @property errorMsg The error message to display.
 * @property achievements The user's achievements.
 * @property friendsCount The number of friends the user has.
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
    val pinnedPicturesUids: List<String> = emptyList(),
    var errorMsg: String? = null,
    var achievements: List<Achievement> = emptyList(),
    val friendsCount: Int = 0,
    val pinnedBitmaps: List<Bitmap> = emptyList(),
    val isLoadingImages: Boolean = false
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
    private val imageRepository: ImageRepository = ImageRepositoryFirebase(),
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
      } catch (e: Exception) {
        Log.e("ProfileViewModel", "Error loading profile", e)
        setErrorMsg("Failed to load profile: ${e.message}")
      } finally {
        _uiState.update { it.copy(isLoading = false) }
      }
    }
  }

  /**
   * Refreshes the user's stats based on their past trips.
   *
   * @param isOnline Whether the device is online.
   */
  fun refreshStats(isOnline: Boolean) {
    if (!isOnline) return // Allow fetching from cache

    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true) }
      try {
        val user = userRepository.getCurrentUser()

        // Only refresh stats if it's the user's own profile
        if (user.uid == _uiState.value.uid) {
          refreshStatsForUser(user)
        }
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

      val pinnedTrips = getValidPinnedTrips(profile)
      val friendsCount = profile.friends.filter { it.status == FriendStatus.ACCEPTED }.size
      val achievements = computeAchievements(stats = profile.stats, friendsCount = friendsCount)
      fetchPinnedPictures(profile)

      _uiState.update {
        it.copy(
            profilePicUrl = profile.profilePicUrl,
            name = profile.name,
            biography = profile.biography,
            stats = profile.stats,
            pinnedTrips = pinnedTrips,
            pinnedPicturesUids = profile.pinnedPicturesUids,
            achievements = achievements,
            friendsCount = friendsCount)
      }
    } catch (e: Exception) {
      Log.e("ProfileViewModel", "Error loading profile info", e)
      setErrorMsg("Failed to load profile info: ${e.message}")
    }
  }

  /**
   * Returns the list of valid pinned trips, removing any invalid UIDs from the user's pinned trips.
   *
   * @param user The user to get the pinned trips for.
   * @return The list of valid pinned trips.
   */
  private suspend fun getValidPinnedTrips(user: User): List<Trip> {
    val pinnedTrips = mutableListOf<Trip>()
    val invalidPinnedUids = mutableListOf<String>()

    user.pinnedTripsUids.forEach { tripUid ->
      try {
        pinnedTrips.add(tripsRepository.getTrip(tripUid))
      } catch (e: Exception) {
        Log.e("ProfileViewModel", "Pinned trip $tripUid not found, removing from user", e)
        invalidPinnedUids.add(tripUid)
      }
    }

    if (invalidPinnedUids.isNotEmpty()) {
      val updatedUids = user.pinnedTripsUids - invalidPinnedUids.toSet()
      userRepository.updateUser(uid = user.uid, pinnedTripsUids = updatedUids)
    }

    return pinnedTrips
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

      val achievements = computeAchievements(stats, _uiState.value.friendsCount)
      _uiState.update { it.copy(stats = stats, achievements = achievements) }
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

  /**
   * Fetches pinned pictures from the repository. This function was made with the help of AI.
   *
   * @param user The user to fetch Pictures for
   */
  private fun fetchPinnedPictures(user: User) {
    val uids = user.pinnedPicturesUids

    if (uids.isEmpty()) {
      _uiState.update { it.copy(pinnedBitmaps = emptyList()) }
      return
    }

    viewModelScope.launch {
      _uiState.update { it.copy(isLoadingImages = true) }

      // 1. Parallel Fetching
      val results =
          uids
              .map { uid ->
                async {
                  try {
                    val imageObj = imageRepository.getImage(uid)
                    val bitmap = ImageHelper.base64ToBitmap(imageObj.base64)
                    if (bitmap != null) uid to bitmap else uid to null
                  } catch (e: Exception) {
                    Log.e("ProfileViewModel", "Image $uid not found/invalid", e)
                    uid to null
                  }
                }
              }
              .awaitAll()

      // 2. Separate Data
      val validBitmaps = results.mapNotNull { it.second }
      val invalidUids = results.filter { it.second == null }.map { it.first }

      // 3. Update UI
      _uiState.update { it.copy(pinnedBitmaps = validBitmaps, isLoadingImages = false) }

      // 4. Cleanup UIDs with no image
      if (invalidUids.isNotEmpty()) {
        removeInvalidImageUids(user.uid, uids, invalidUids)
      }
    }
  }

  /**
   * Removes invalid image UIDs from the user's profile in Firestore.
   *
   * @param userId The ID of the user.
   * @param currentUids The current list of image UIDs.
   * @param invalidUids The list of invalid image UIDs to remove.
   */
  private fun removeInvalidImageUids(
      userId: String,
      currentUids: List<String>,
      invalidUids: List<String>
  ) {
    viewModelScope.launch {
      try {
        val updatedUids = currentUids - invalidUids.toSet()
        userRepository.updateUser(uid = userId, pinnedPicturesUids = updatedUids)
      } catch (e: Exception) {
        Log.e("ProfileViewModel", "Failed to update user profile", e)
      }
    }
  }
}

class ProfileViewModelFactory(
    private val requestedUid: String,
    private val userRepository: UserRepository = UserRepositoryFirebase(),
    private val tripsRepository: TripsRepository = TripsRepositoryProvider.repository,
    private val imageRepository: ImageRepository = ImageRepositoryFirebase()
) : androidx.lifecycle.ViewModelProvider.Factory {
  @Suppress("UNCHECKED_CAST")
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    return ProfileViewModel(userRepository, tripsRepository, imageRepository, requestedUid) as T
  }
}
