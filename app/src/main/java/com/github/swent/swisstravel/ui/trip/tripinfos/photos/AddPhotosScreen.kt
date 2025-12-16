package com.github.swent.swisstravel.ui.trip.tripinfos.photos

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.ui.composable.BackButton
import com.github.swent.swisstravel.ui.composable.ErrorScreen
import com.github.swent.swisstravel.ui.composable.PhotoGrid

/** Test tags for the add photos screen */
object AddPhotosScreenTestTags {
  const val MAIN_SCREEN = "mainScreen"
  const val TOP_APP_BAR = "topAppBar"
  const val TOP_APP_BAR_TITLE = "topAppBarTitle"
  const val BOTTOM_BAR = "bottomBar"
  const val ADD_PHOTOS_BUTTON = "addPhotosButton"
  const val ADD_PHOTO_GRID = "addPhotoGrid"
  const val EDIT_BUTTON = "editButton"
}

/**
 * A screen that shows the photos associated to a trip. You can add photos too.
 *
 * @param onBack a function that is call when the user click on the back button.
 * @param photosViewModel the viewmodel used by the screen.
 * @param tripId the uid of the trip.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPhotosScreen(
    onBack: () -> Unit = {},
    onEdit: () -> Unit = {},
    photosViewModel: PhotosViewModel = viewModel(),
    tripId: String,
    launchPickerOverride: ((String) -> Unit)? = null
) {
  val context = LocalContext.current
  LaunchedEffect(tripId) { photosViewModel.loadPhotos(tripId) }
  // AI helped for the picker
  val pickerLauncher =
      rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) {
          uris.forEach { uri ->
            context.contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
          }
          photosViewModel.addUris(uris, context, tripId)
          photosViewModel.savePhotos(tripId)
        }
      }
  val launchPicker: (String) -> Unit =
      launchPickerOverride ?: { type -> pickerLauncher.launch(arrayOf(type)) }

  val uiState by photosViewModel.uiState.collectAsState()
  val stringPicker = stringResource(R.string.image_picker)

  // AI gave this part
  LaunchedEffect(uiState.toastMessage) {
    if (uiState.toastMessage.isNotEmpty()) {
      Toast.makeText(context, uiState.toastMessage, Toast.LENGTH_SHORT).show()
      photosViewModel.clearToastMessage()
    }
  }

  // AI gave the structure with the when
  // Choose which screen to display depending on the state of the app
  when {
    uiState.isLoading -> LoadingPhotosScreen()
    uiState.errorLoading ->
        ErrorScreen(
            message = stringResource(R.string.error_loading),
            topBarTitle = stringResource(R.string.add_photos_title),
            backButtonDescription = stringResource(R.string.back_add_photos),
            onRetry = { photosViewModel.loadPhotos(tripId) },
            onBack = { onBack() })
    else -> {
      Scaffold(
          modifier = Modifier.testTag(AddPhotosScreenTestTags.MAIN_SCREEN),
          topBar = {
            TopAppBar(
                modifier = Modifier.testTag(AddPhotosScreenTestTags.TOP_APP_BAR),
                title = {
                  Text(
                      modifier = Modifier.testTag(AddPhotosScreenTestTags.TOP_APP_BAR_TITLE),
                      text = stringResource(R.string.add_photos_title),
                      style = MaterialTheme.typography.titleLarge,
                      color = MaterialTheme.colorScheme.onBackground)
                },
                navigationIcon = {
                  BackButton(
                      onBack = { onBack() },
                      contentDescription = stringResource(R.string.back_add_photos))
                },
                actions = { EditButton(onEdit = { onEdit() }) })
          },
          bottomBar = {
            Row(
                modifier = Modifier.fillMaxWidth().testTag(AddPhotosScreenTestTags.BOTTOM_BAR),
                horizontalArrangement = Arrangement.Center) {
                  // Add photos to the UI state button
                  Button(
                      modifier = Modifier.testTag(AddPhotosScreenTestTags.ADD_PHOTOS_BUTTON),
                      // AI give the function onClick
                      onClick = { launchPicker(stringPicker) }) {
                        Text(text = stringResource(R.string.add_photos_button))
                      }
                }
          }) { pd ->
            PhotoGrid(
                items = uiState.uriLocation.keys.toList(),
                modifier = Modifier.padding(pd).testTag(AddPhotosScreenTestTags.ADD_PHOTO_GRID),
                onClick = null, // No click action in view mode
                modelMapper = { it } // already a Uri
                )
          }
    }
  }
}

/**
 * A button that go on edit mode when you click on it.
 *
 * @param onEdit the function called when you click on the button
 */
@Composable
private fun EditButton(onEdit: () -> Unit = {}) {
  IconButton(
      modifier = Modifier.testTag(AddPhotosScreenTestTags.EDIT_BUTTON), onClick = { onEdit() }) {
        Icon(
            imageVector = Icons.Outlined.Edit,
            contentDescription = stringResource(R.string.edit_button_description),
            tint = MaterialTheme.colorScheme.onBackground)
      }
}
