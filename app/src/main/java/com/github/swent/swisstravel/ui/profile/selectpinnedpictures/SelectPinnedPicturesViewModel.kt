package com.github.swent.swisstravel.ui.profile.selectpinnedpictures

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.swent.swisstravel.model.image.ImageHelper
import com.github.swent.swisstravel.model.image.ImageRepository
import com.github.swent.swisstravel.model.image.ImageRepositoryFirebase
import com.github.swent.swisstravel.model.user.User
import com.github.swent.swisstravel.model.user.UserRepository
import com.github.swent.swisstravel.model.user.UserRepositoryFirebase
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Helper data class to manage images in the UI.
 *
 * @property uid The Firestore UID. If null, it means this is a NEW image not yet saved.
 * @property bitmap The visual representation for the UI.
 * @property base64String The raw data. Only needed for NEW images to upload them.
 */
data class PinnedImage(
    val uid: String? = null,
    val bitmap: Bitmap,
    val base64String: String? = null
)

/**
 * UI State for the selectPinnedPicturesScreen.
 *
 * @property images The list of Pinned Images.
 * @property selectedIndices The indices of images selected in Edit Mode.
 * @property isLoading Whether the screen is currently loading.
 * @property errorMsg The error message, if any.
 */
data class SelectPinnedPicturesUIState(
    val images: List<PinnedImage> = emptyList(),
    val selectedIndices: List<Int> = emptyList(),
    val isLoading: Boolean = false,
    val errorMsg: String? = null
)

/**
 * ViewModel for the selectPinnedPicturesScreen. Some of these functions were made with the help of
 * AI.
 *
 * @param userRepository The repository for the user.
 * @param imageRepository The repository for the images.
 */
class SelectPinnedPicturesViewModel(
    private val userRepository: UserRepository = UserRepositoryFirebase(),
    private val imageRepository: ImageRepository = ImageRepositoryFirebase()
) : ViewModel() {
  private val _uiState = MutableStateFlow(SelectPinnedPicturesUIState())
  val uiState: StateFlow<SelectPinnedPicturesUIState> = _uiState.asStateFlow()

  private var currentUser: User? = null

  // Loads the user's pictures into the UI state
  init {
    loadExistingImages()
  }

  /** Loads existing images from the repository into the UI state. */
  private fun loadExistingImages() {
    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true) }
      try {
        val user = userRepository.getCurrentUser()
        currentUser = user

        // Fetch all existing images in parallel
        val loadedImages =
            user.pinnedPicturesUids
                .map { uid ->
                  async {
                    try {
                      val imageObj = imageRepository.getImage(uid)
                      val bitmap = ImageHelper.base64ToBitmap(imageObj.base64)
                      if (bitmap != null) {
                        PinnedImage(uid = uid, bitmap = bitmap)
                      } else {
                        null
                      }
                    } catch (e: Exception) {
                      Log.e("SelectPinnedPictures", "Failed to load image $uid", e)
                      null
                    }
                  }
                }
                .awaitAll()
                .filterNotNull()

        _uiState.update { it.copy(images = loadedImages, isLoading = false) }
      } catch (e: Exception) {
        _uiState.update {
          it.copy(isLoading = false, errorMsg = "Error loading profile: ${e.message}")
        }
      }
    }
  }

  /**
   * Processes new URIs selected from the gallery. Compresses them immediately so they are ready for
   * upload.
   *
   * @param context The application context.
   * @param uris The list of URIs to process.
   */
  fun addNewImages(context: Context, uris: List<Uri>) {
    if (uris.isEmpty()) return

    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true) }

      val newImages =
          uris
              .map { uri ->
                async {
                  try {
                    // Compress & Convert to Base64
                    val base64 =
                        ImageHelper.uriCompressedToBase64(context, uri)
                            ?: throw Exception("Failed to process image")

                    // Convert back to Bitmap for preview
                    val bitmap =
                        ImageHelper.base64ToBitmap(base64)
                            ?: throw Exception("Failed to decode processed image")

                    PinnedImage(uid = null, bitmap = bitmap, base64String = base64)
                  } catch (e: Exception) {
                    Log.e("SelectPinnedPictures", "Error adding image", e)
                    null
                  }
                }
              }
              .awaitAll()
              .filterNotNull()

      _uiState.update { it.copy(images = it.images + newImages, isLoading = false) }
    }
  }

  /**
   * Uploads new images to Firestore and updates the User profile with the list of IDs.
   *
   * @param onSuccess Callback to be executed when the operation is successful.
   */
  fun savePictures(onSuccess: () -> Unit) {
    val user = currentUser ?: return
    val currentImages = _uiState.value.images

    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true) }
      try {
        val finalUids = mutableListOf<String>()

        // Process all images
        currentImages.forEach { image ->
          if (image.uid != null) {
            // Already exists, just keep the ID
            finalUids.add(image.uid)
          } else if (image.base64String != null) {
            // New image! Upload it.
            val newUid = imageRepository.addImage(image.base64String)
            finalUids.add(newUid)
          }
        }

        // Update profile
        userRepository.updateUser(uid = user.uid, pinnedPicturesUids = finalUids)

        _uiState.update { it.copy(isLoading = false) }
        onSuccess()
      } catch (e: Exception) {
        _uiState.update { it.copy(isLoading = false, errorMsg = "Failed to save: ${e.message}") }
      }
    }
  }

  /** Clears the current error message from the UI state. */
  fun clearErrorMsg() {
    _uiState.update { it.copy(errorMsg = null) }
  }

  /** Toggles the selection status of an image at [index]. */
  fun toggleSelection(index: Int) {
    _uiState.update { state ->
      val currentSelected = state.selectedIndices.toMutableList()
      if (currentSelected.contains(index)) {
        currentSelected.remove(index)
      } else {
        currentSelected.add(index)
      }
      state.copy(selectedIndices = currentSelected)
    }
  }

  /**
   * Removes all currently selected images from the UI and deletes them from the database
   * immediately.
   */
  fun removeSelectedImages() {
    val state = _uiState.value
    val selectedSet = state.selectedIndices.toSet()
    val keptImages = mutableListOf<PinnedImage>()
    val imagesToDelete = mutableListOf<PinnedImage>()

    state.images.forEachIndexed { index, image ->
      if (index in selectedSet) {
        imagesToDelete.add(image)
      } else {
        keptImages.add(image)
      }
    }

    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true) }

      // Delete marked images from Firestore in parallel
      imagesToDelete
          .map { image ->
            async {
              if (image.uid != null) {
                try {
                  imageRepository.deleteImage(image.uid)
                } catch (e: Exception) {
                  Log.e("SelectPinnedPictures", "Failed to delete image: ${image.uid}", e)
                }
              }
            }
          }
          .awaitAll()

      _uiState.update {
        it.copy(images = keptImages, selectedIndices = emptyList(), isLoading = false)
      }
    }
  }
}
