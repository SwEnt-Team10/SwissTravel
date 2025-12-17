package com.github.swent.swisstravel.ui.profile

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.swent.swisstravel.model.image.ImageHelper
import com.github.swent.swisstravel.model.image.ImageRepository
import com.github.swent.swisstravel.model.image.ImageRepositoryFirebase
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
import com.github.swent.swisstravel.model.user.UserUpdate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * A data class representing the state of the profile settings screen.
 *
 * @property isLoading Whether the screen is currently loading.
 * @property profilePicUrl The URL or UID of the user's profile picture.
 * @property pendingProfilePicUri A temporary URI for the image selected from the gallery (for
 *   preview).
 * @property name The user's name.
 * @property isEditingName Whether the user is currently editing their name.
 * @property biography The user's biography.
 * @property isEditingBio Whether the user is currently editing their biography.
 * @property email The user's email
 * @property selectedPreferences The user's selected preferences.
 * @property errorMsg The error message to display.
 */
data class ProfileSettingsUIState(
    val isLoading: Boolean = true,
    val profilePicUrl: String = "",
    val pendingProfilePicUri: Uri? = null,
    val name: String = "",
    val isEditingName: Boolean = false,
    val biography: String = "",
    val isEditingBio: Boolean = false,
    val email: String = "",
    var selectedPreferences: List<Preference> = emptyList(),
    var errorMsg: String? = null,
)

/**
 * A view model for the profile settings screen.
 *
 * @param userRepository The repository for users.
 * @param tripsRepository The repository for trips.
 * @param imageRepository The repository for images.
 */
class ProfileSettingsViewModel(
    private val userRepository: UserRepository = UserRepositoryFirebase(),
    private val tripsRepository: TripsRepository = TripsRepositoryFirestore(),
    private val imageRepository: ImageRepository = ImageRepositoryFirebase()
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
      } catch (e: Exception) {
        _uiState.value = uiState.value.copy(errorMsg = "Error fetching user data.")
        Log.e("ProfileSettingsViewModel", "Error fetching user data.", e)
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
    if (!isOnline) return

    viewModelScope.launch {
      val user = currentUser ?: return@launch
      refreshStatsForUser(user)
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
      _uiState.update { it.copy(errorMsg = it.errorMsg ?: "Error updating stats.") }
      Log.e("ProfileSettingsViewModel", "Error updating stats.", e)
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
            biography = loggedIn.biography,
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
        _uiState.value = uiState.value.copy(errorMsg = "Error saving preferences.")
        Log.e("ProfileSettingsViewModel", "Error saving preferences.", e)
      }
    }
  }

  /** Starts editing the user's name. */
  fun startEditingName() {
    _uiState.update { it.copy(isEditingName = true) }
  }

  /** Starts editing the user's biography. */
  fun startEditingBio() {
    _uiState.update { it.copy(isEditingBio = true) }
  }

  /**
   * Saves the user's name.
   *
   * @param newName The new name to save.
   */
  fun saveName(newName: String) {
    viewModelScope.launch {
      val user = currentUser ?: return@launch
      try {
        userRepository.updateUser(uid = user.uid, UserUpdate(name = newName))
        _uiState.update { it.copy(name = newName, isEditingName = false) }
      } catch (e: Exception) {
        _uiState.update { it.copy(errorMsg = "Error updating name.") }
        Log.e("ProfileSettingsViewModel", "Error updating name.", e)
      }
    }
  }

  /**
   * Saves the user's biography.
   *
   * @param newBio The new biography to save.
   */
  fun saveBio(newBio: String) {
    viewModelScope.launch {
      val user = currentUser ?: return@launch
      try {
        userRepository.updateUser(uid = user.uid, UserUpdate(biography = newBio))
        _uiState.update { it.copy(biography = newBio, isEditingBio = false) }
      } catch (e: Exception) {
        _uiState.update { it.copy(errorMsg = "Error updating biography.") }
        Log.e("ProfileSettingsViewModel", "Error updating biography.", e)
      }
    }
  }

  /** Cancels editing the user's name. */
  fun cancelEditingName() {
    _uiState.update { it.copy(isEditingName = false) }
  }

  /** Cancels editing the user's biography. */
  fun cancelEditingBio() {
    _uiState.update { it.copy(isEditingBio = false) }
  }

  /**
   * Checks if the current user is signed in.
   *
   * @return True if the user is signed in, false otherwise.
   */
  fun userIsSignedIn(): Boolean {
    return currentUser != null
  }

  /**
   * Called when the user selects a picture from the gallery. Sets the pending URI to show the
   * preview dialog.
   *
   * @param uri The URI of the selected image.
   */
  fun onProfilePicSelected(uri: Uri) {
    _uiState.update { it.copy(pendingProfilePicUri = uri) }
  }

  /** Cancels the pending profile picture change. */
  fun cancelProfilePicChange() {
    _uiState.update { it.copy(pendingProfilePicUri = null) }
  }

  /**
   * Confirms the profile picture change. Compresses the image, uploads it to the repository, and
   * updates the user profile.
   *
   * @param context The context used for image compression.
   */
  fun confirmProfilePicChange(context: Context) {
    val uri = uiState.value.pendingProfilePicUri ?: return

    viewModelScope.launch {
      val user = currentUser ?: return@launch
      // Set loading state and clear the pending URI to dismiss the dialog
      _uiState.update { it.copy(isLoading = true, pendingProfilePicUri = null) }

      try {
        val base64 =
            ImageHelper.uriCompressedToBase64(context, uri)
                ?: throw Exception("Failed to process image")

        // Upload to ImageRepository
        val newImageUid = imageRepository.addImage(base64)

        // Update User Profile with the new UID
        userRepository.updateUser(uid = user.uid, UserUpdate(profilePicUrl = newImageUid))

        // Update UI state with the new UID
        _uiState.update { it.copy(profilePicUrl = newImageUid, isLoading = false) }
      } catch (e: Exception) {
        _uiState.update { it.copy(isLoading = false, errorMsg = "Error updating picture.") }
        Log.e("ProfileSettingsViewModel", "Error updating picture.", e)
      }
    }
  }
}
