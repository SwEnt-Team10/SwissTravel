package com.github.swent.swisstravel.ui.profile.selectpinnedpictures

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.ui.composable.PhotoGrid

object SelectPinnedPicturesScreenTestTags {
  const val MAIN_SCREEN = "mainScreen"
  const val TOP_APP_BAR = "topAppBar"
  const val TOP_APP_BAR_TITLE = "topAppBarTitle"
  const val BACK_BUTTON = "backButton"
  const val BOTTOM_BAR = "bottomBar"
  const val SAVE_BUTTON = "saveButton"
  const val ADD_PICTURE_BUTTON = "addPictureButton"
  const val LOADING_INDICATOR = "loadingIndicator"
  const val EDIT_BUTTON = "editButton"
  const val REMOVE_BUTTON = "removeButton"
  const val CANCEL_BUTTON = "cancelButton"
  const val PHOTO_GRID = "photoGrid"
}

/**
 * A screen that allows the user to select pinned photos.
 *
 * @param onBack The callback for when the back button is clicked.
 * @param selectPinnedPicturesViewModel The view model for this screen.
 * @param launchPickerOverride The override for the picker launcher.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectPinnedPicturesScreen(
    onBack: () -> Unit = {},
    selectPinnedPicturesViewModel: SelectPinnedPicturesViewModel = viewModel(),
    launchPickerOverride: ((PickVisualMediaRequest) -> Unit)? = null
) {
  val context = LocalContext.current
  val uiState by selectPinnedPicturesViewModel.uiState.collectAsState()

  // Local state to toggle between "View/Add" mode and "Edit/Remove" mode
  var isEditMode by remember { mutableStateOf(false) }

  // AI helped for the picker
  val pickerLauncher =
      rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
        // Context needed cause the ViewModel needs it to resolve URIs to Bytes
        if (uris.isNotEmpty()) {
          selectPinnedPicturesViewModel.addNewImages(context, uris)
        }
      }

  val launchPicker: (PickVisualMediaRequest) -> Unit =
      launchPickerOverride ?: { request -> pickerLauncher.launch(request) }

  // Handle Errors
  LaunchedEffect(uiState.errorMsg) {
    uiState.errorMsg
        ?.takeIf { it.isNotBlank() }
        ?.let { msg ->
          Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
          selectPinnedPicturesViewModel.clearErrorMsg()
        }
  }

  Scaffold(
      modifier = Modifier.testTag(SelectPinnedPicturesScreenTestTags.MAIN_SCREEN),
      topBar = {
        TopAppBar(
            modifier = Modifier.testTag(SelectPinnedPicturesScreenTestTags.TOP_APP_BAR),
            title = {
              Text(
                  modifier = Modifier.testTag(SelectPinnedPicturesScreenTestTags.TOP_APP_BAR_TITLE),
                  text =
                      stringResource(
                          if (isEditMode) R.string.edit_top_bar_title
                          else R.string.select_pinned_pictures_title),
                  style = MaterialTheme.typography.titleLarge)
            },
            navigationIcon = {
              if (isEditMode) {
                // Cancel Button
                IconButton(
                    onClick = { isEditMode = false },
                    modifier = Modifier.testTag(SelectPinnedPicturesScreenTestTags.CANCEL_BUTTON)) {
                      Icon(
                          Icons.Default.Close,
                          contentDescription = stringResource(R.string.cancel_edit))
                    }
              } else {
                // Back Button
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag(SelectPinnedPicturesScreenTestTags.BACK_BUTTON)) {
                      Icon(
                          Icons.Default.Close,
                          contentDescription = stringResource(R.string.back_to_profile))
                    }
              }
            },
            actions = {
              // Edit Button
              if (!isEditMode && uiState.images.isNotEmpty()) {
                IconButton(
                    onClick = { isEditMode = true },
                    modifier = Modifier.testTag(SelectPinnedPicturesScreenTestTags.EDIT_BUTTON)) {
                      Icon(
                          Icons.Outlined.Edit,
                          contentDescription = stringResource(R.string.edit_button_description))
                    }
              }
            })
      },
      bottomBar = {
        Row(
            modifier =
                Modifier.fillMaxWidth().testTag(SelectPinnedPicturesScreenTestTags.BOTTOM_BAR),
            horizontalArrangement = Arrangement.Center) {
              if (isEditMode) {
                // Delete button
                Button(
                    modifier = Modifier.testTag(SelectPinnedPicturesScreenTestTags.REMOVE_BUTTON),
                    onClick = {
                      selectPinnedPicturesViewModel.removeSelectedImages()
                      isEditMode = false
                    },
                    enabled = uiState.selectedIndices.isNotEmpty()) {
                      Text(text = stringResource(R.string.remove_button))
                    }
              } else {
                // Add button
                Button(
                    modifier =
                        Modifier.testTag(SelectPinnedPicturesScreenTestTags.ADD_PICTURE_BUTTON),
                    onClick = {
                      launchPicker(
                          PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    enabled = !uiState.isLoading) {
                      Text(text = stringResource(R.string.add_photos_button))
                    }

                Spacer(modifier = Modifier.width(dimensionResource(R.dimen.small_spacer)))
                // Save button
                Button(
                    modifier = Modifier.testTag(SelectPinnedPicturesScreenTestTags.SAVE_BUTTON),
                    onClick = {
                      selectPinnedPicturesViewModel.savePictures(onSuccess = { onBack() })
                    },
                    enabled = !uiState.isLoading) {
                      Text(text = stringResource(R.string.save))
                    }
              }
            }
      }) { pd ->
        Box(modifier = Modifier.padding(pd).fillMaxSize()) {
          if (uiState.isLoading && uiState.images.isEmpty()) {
            CircularProgressIndicator(
                modifier =
                    Modifier.align(Alignment.Center)
                        .testTag(SelectPinnedPicturesScreenTestTags.LOADING_INDICATOR))
          } else {
            PhotoGrid(
                items = uiState.images,
                modifier =
                    Modifier.fillMaxSize().testTag(SelectPinnedPicturesScreenTestTags.PHOTO_GRID),
                onClick =
                    if (isEditMode)
                        { index -> selectPinnedPicturesViewModel.toggleSelection(index) }
                    else null,
                isSelected =
                    if (isEditMode) { index -> uiState.selectedIndices.contains(index) } else null,
                modelMapper = { it.bitmap })
          }

          // Loading Overlay when saving/compressing
          if (uiState.isLoading && uiState.images.isNotEmpty()) {
            CircularProgressIndicator(
                modifier =
                    Modifier.align(Alignment.Center)
                        .testTag(SelectPinnedPicturesScreenTestTags.LOADING_INDICATOR))
          }
        }
      }
}
