package com.github.swent.swisstravel.ui.trip.tripinfos.photos

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.ui.trip.tripinfos.photos.add.PhotosUIState
import com.github.swent.swisstravel.ui.trip.tripinfos.photos.add.PhotosViewModel

object AddPhotosScreenTestTags {
  const val MAIN_SCREEN = "mainScreen"
  const val TOP_APP_BAR = "topAppBar"
  const val TOP_APP_BAR_TITLE = "topAppBarTitle"
  const val BACK_BUTTON = "backButton"
  const val BOTTOM_BAR = "bottomBar"
  const val SAVE_BUTTON = "saveButton"
  const val ADD_PHOTOS_BUTTON = "addPhotosButton"
  const val VERTICAL_GRID = "verticalGrid"

  fun getTestTagForUri(index: Int): String = "UriIndex$index"
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
    photosViewModel: PhotosViewModel = viewModel(),
    tripId: String,
    launchPickerOverride: ((PickVisualMediaRequest) -> Unit)? = null,
    uiState: PhotosUIState
) {
  val context = LocalContext.current
  LaunchedEffect(tripId) { photosViewModel.loadPhotos(tripId) }
  // AI helped for the picker
  val pickerLauncher =
      rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
        if (uris.isNotEmpty()) {
          uris.forEach { uri ->
            context.contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
          }
          photosViewModel.addUri(uris)
        }
      }
  val launchPicker: (PickVisualMediaRequest) -> Unit =
      launchPickerOverride ?: { request -> pickerLauncher.launch(request) }

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
              // Back button
              IconButton(
                  onClick = onBack,
                  modifier = Modifier.testTag(AddPhotosScreenTestTags.BACK_BUTTON)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back_to_my_trips),
                        tint = MaterialTheme.colorScheme.onBackground)
                  }
            },
            actions = {
                EditButton(photosViewModel)
            }
            )
      },
      bottomBar = {
        Row(
            modifier = Modifier.fillMaxWidth().testTag(AddPhotosScreenTestTags.BOTTOM_BAR),
            horizontalArrangement = Arrangement.Center) {
              // Add photos to the UI state button
              Button(
                  modifier = Modifier.testTag(AddPhotosScreenTestTags.ADD_PHOTOS_BUTTON),
                  onClick = {
                    launchPicker(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                  }) {
                    Text(text = stringResource(R.string.add_photos_button))
                  }
              Spacer(modifier = Modifier.width(dimensionResource(R.dimen.save_add_button_padding)))
              // Save photos button
              Button(
                  modifier = Modifier.testTag(AddPhotosScreenTestTags.SAVE_BUTTON),
                  onClick = {
                    photosViewModel.savePhotos(tripId)
                    onBack()
                  }) {
                    Text(text = stringResource(R.string.add_photos_save_button))
                  }
            }
      }) { pd ->
        // Display a grid with the images
        LazyVerticalGrid(
            columns = GridCells.Fixed(integerResource(R.integer.images_on_grid)),
            modifier = Modifier.padding(pd).testTag(AddPhotosScreenTestTags.VERTICAL_GRID)) {
              // AI helped for the itemsIndexed
              itemsIndexed(uiState.listUri) { index, uri ->
                AsyncImage(
                    modifier = Modifier.testTag(AddPhotosScreenTestTags.getTestTagForUri(index)),
                    model = uri,
                    contentDescription = null)
              }
            }
      }
}

@Composable
private fun EditButton(
    photosViewModel: PhotosViewModel
) {
    IconButton(
        onClick = {
            photosViewModel.switchOnEditMode()
        }
    ) {
        Icon(
            imageVector = Icons.Filled.Edit,
            contentDescription = "Edit Mode",
            tint = MaterialTheme.colorScheme.onBackground
        )
    }
}
