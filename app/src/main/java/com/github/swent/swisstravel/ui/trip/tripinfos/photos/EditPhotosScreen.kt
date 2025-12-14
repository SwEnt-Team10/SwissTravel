package com.github.swent.swisstravel.ui.trip.tripinfos.photos

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.ui.composable.DeleteDialog
import com.github.swent.swisstravel.ui.composable.ErrorScreen
import com.github.swent.swisstravel.ui.composable.PhotoGrid

/** Test tags for the edit screen photos */
object EditPhotosScreenTestTags {
  const val EDIT_SCAFFOLD = "editScaffold"
  const val EDIT_TOP_BAR = "editTopBar"
  const val EDIT_TOP_BAR_TITLE = "editTopBarTitle"
  const val EDIT_CANCEL_BUTTON = "editCancelButton"
  const val EDIT_BOTTOM_BAR = "editBottomBar"
  const val EDIT_REMOVE_BUTTON = "editRemoveButton"
  const val EDIT_PHOTO_GRID = "editPhotoGrid"
}

/**
 * A Screen corresponding to the edit mode of the feature that can add photos to the trip.
 *
 * @param photosViewModel the viewModel of the screen
 * @param onCancel a function called when you want to exit the edit mode
 * @param tripId the uid of the current trip selected
 */
@Composable
fun EditPhotosScreen(
    photosViewModel: PhotosViewModel = viewModel(),
    onCancel: () -> Unit = {},
    tripId: String
) {

  val context = LocalContext.current
  // Start by loading the photos from the repository
  LaunchedEffect(tripId) { photosViewModel.loadPhotos(tripId) }
  val uiState by photosViewModel.uiState.collectAsState()
  val selectedImageCount = uiState.uriSelected.size

  // State for the confirmation dialog
  var showDeleteDialog by remember { mutableStateOf(false) }

  // AI gives this part
  LaunchedEffect(uiState.toastMessage) {
    if (uiState.toastMessage.isNotEmpty()) {
      Toast.makeText(context, uiState.toastMessage, Toast.LENGTH_SHORT).show()
      photosViewModel.clearToastMessage()
    }
  }

  if (showDeleteDialog) {
    DeleteDialog(
        onConfirm = {
          photosViewModel.removePhotos(tripId)
          showDeleteDialog = false
        },
        onCancel = { showDeleteDialog = false },
        title =
            pluralStringResource(
                R.plurals.confirm_delete_title_images, selectedImageCount, selectedImageCount))
  }

  // AI gave the structure with the when
  // Choose which screen to display depending on the state of the app
  when {
    uiState.isLoading -> LoadingPhotosScreen()
    uiState.errorLoading ->
        ErrorScreen(
            message = stringResource(R.string.error_loading),
            topBarTitle = stringResource(R.string.edit_top_bar_title),
            backButtonDescription = stringResource(R.string.back_edit_photos),
            onRetry = { photosViewModel.loadPhotos(tripId) },
            onBack = { onCancel() })
  }

  Scaffold(
      modifier = Modifier.testTag(EditPhotosScreenTestTags.EDIT_SCAFFOLD),
      topBar = { EditTopBar(onCancel = { onCancel() }) },
      bottomBar = {
        EditBottomBar(onRemove = { photosViewModel.removePhotos(tripId) }, uiState = uiState)
      }) { pd ->

        // Replaced LazyVerticalGrid with Shared PhotoGrid
        PhotoGrid(
            items = uiState.uriLocation.keys.toList(),
            modifier = Modifier.padding(pd).testTag(EditPhotosScreenTestTags.EDIT_PHOTO_GRID),
            onClick = { index -> photosViewModel.selectToRemove(index) },
            isSelected = { index -> uiState.uriSelected.contains(index) },
            modelMapper = { it })
      }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditTopBar(onCancel: () -> Unit = {}) {
  TopAppBar(
      modifier = Modifier.testTag(EditPhotosScreenTestTags.EDIT_TOP_BAR),
      title = {
        Text(
            modifier = Modifier.testTag(EditPhotosScreenTestTags.EDIT_TOP_BAR_TITLE),
            text = stringResource(R.string.edit_top_bar_title))
      },
      navigationIcon = { CancelButton(onCancel = onCancel) })
}

/**
 * A button that can exit the edit mode.
 *
 * @param onCancel the function to call when you click on the button
 */
@Composable
private fun CancelButton(onCancel: () -> Unit = {}) {
  IconButton(
      modifier = Modifier.testTag(EditPhotosScreenTestTags.EDIT_CANCEL_BUTTON),
      onClick = { onCancel() }) {
        Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = stringResource(R.string.cancel_edit),
            tint = MaterialTheme.colorScheme.onBackground)
      }
}

/**
 * The bottom bar of the edit mode
 *
 * @param onRemove the function to call when you want to remove selected photos
 * @param uiState the state of edit photos screen
 */
@Composable
private fun EditBottomBar(onRemove: () -> Unit = {}, uiState: PhotosUIState) {
  Row(
      modifier = Modifier.fillMaxWidth().testTag(EditPhotosScreenTestTags.EDIT_BOTTOM_BAR),
      horizontalArrangement = Arrangement.Center) {
        Button(
            onClick = { onRemove() },
            modifier = Modifier.testTag(EditPhotosScreenTestTags.EDIT_REMOVE_BUTTON),
            enabled = uiState.uriSelected.isNotEmpty()) {
              Text(text = stringResource(R.string.remove_button))
            }
      }
}
