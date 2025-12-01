package com.github.swent.swisstravel.ui.trip.tripinfos.addphotos

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
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.github.swent.swisstravel.R

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
 * @param viewModel the viewmodel used by the screen.
 * @param tripId the uid of the trip.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPhotosScreen(
    onBack: () -> Unit = {},
    viewModel: AddPhotosViewModel = viewModel(),
    tripId: String
) {
  val context = LocalContext.current
  LaunchedEffect(tripId) { viewModel.loadPhotos(tripId) }
  // AI helped for the picker
  val picker =
      rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
        if (uris.isNotEmpty()) {
          uris.forEach { uri ->
            context.contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
          }
          viewModel.addUri(uris)
        }
      }
  val addPhotosUIState by viewModel.uiState.collectAsState()
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
            })
      },
      bottomBar = {
        Row(
            modifier = Modifier.fillMaxWidth().testTag(AddPhotosScreenTestTags.BOTTOM_BAR),
            horizontalArrangement = Arrangement.Center) {
              // Add photos to the UI state button
              Button(
                  modifier = Modifier.testTag(AddPhotosScreenTestTags.ADD_PHOTOS_BUTTON),
                  onClick = {
                    picker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                  }) {
                    Text(text = stringResource(R.string.add_photos_button))
                  }
              Spacer(modifier = Modifier.width(dimensionResource(R.dimen.save_add_button_padding)))
              // Save photos button
              Button(
                  modifier = Modifier.testTag(AddPhotosScreenTestTags.SAVE_BUTTON),
                  onClick = {
                    viewModel.savePhotos(tripId)
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
              itemsIndexed(addPhotosUIState.listUri) { index, uri ->
                AsyncImage(
                    modifier = Modifier.testTag(AddPhotosScreenTestTags.getTestTagForUri(index)),
                    model = uri,
                    contentDescription = null)
              }
            }
      }
}
