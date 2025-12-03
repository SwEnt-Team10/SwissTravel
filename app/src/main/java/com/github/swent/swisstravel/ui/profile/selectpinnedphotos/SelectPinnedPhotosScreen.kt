package com.github.swent.swisstravel.ui.profile.selectpinnedphotos

import android.content.Intent
import android.widget.Toast
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.github.swent.swisstravel.R

object SelectPinnedPhotosScreenTestTags {
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
 * A screen that allows the user to select pinned photos.
 *
 * @param onBack The callback for when the back button is clicked.
 * @param selectPinnedPhotosViewModel The view model for this screen.
 * @param launchPickerOverride The override for the picker launcher.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectPinnedPhotosScreen(
    onBack: () -> Unit = {},
    selectPinnedPhotosViewModel: SelectPinnedPhotosViewModel = viewModel(),
    launchPickerOverride: ((PickVisualMediaRequest) -> Unit)? = null
) {
  val context = LocalContext.current

  // AI helped for the picker
  val pickerLauncher =
      rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
        if (uris.isNotEmpty()) {
          uris.forEach { uri ->
            context.contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
          }
          selectPinnedPhotosViewModel.addUri(uris)
        }
      }
  val launchPicker: (PickVisualMediaRequest) -> Unit =
      launchPickerOverride ?: { request -> pickerLauncher.launch(request) }

  val selectPinnedPhotosUIState by selectPinnedPhotosViewModel.uiState.collectAsState()

  LaunchedEffect(selectPinnedPhotosUIState.errorMsg) {
    selectPinnedPhotosUIState.errorMsg
        ?.takeIf { it.isNotBlank() }
        ?.let {
          Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
          selectPinnedPhotosViewModel.clearErrorMsg()
        }
  }

  Scaffold(
      modifier = Modifier.testTag(SelectPinnedPhotosScreenTestTags.MAIN_SCREEN),
      topBar = {
        TopAppBar(
            modifier = Modifier.testTag(SelectPinnedPhotosScreenTestTags.TOP_APP_BAR),
            title = {
              Text(
                  modifier = Modifier.testTag(SelectPinnedPhotosScreenTestTags.TOP_APP_BAR_TITLE),
                  text = stringResource(R.string.select_pinned_photos_title),
                  style = MaterialTheme.typography.titleLarge,
                  color = MaterialTheme.colorScheme.onBackground)
            },
            navigationIcon = {
              // Back button
              IconButton(
                  onClick = onBack,
                  modifier = Modifier.testTag(SelectPinnedPhotosScreenTestTags.BACK_BUTTON)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.back_to_profile),
                        tint = MaterialTheme.colorScheme.onBackground)
                  }
            })
      },
      bottomBar = {
        Row(
            modifier = Modifier.fillMaxWidth().testTag(SelectPinnedPhotosScreenTestTags.BOTTOM_BAR),
            horizontalArrangement = Arrangement.Center) {
              // Add photos to the UI state button
              Button(
                  modifier = Modifier.testTag(SelectPinnedPhotosScreenTestTags.ADD_PHOTOS_BUTTON),
                  onClick = {
                    launchPicker(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                  }) {
                    Text(text = stringResource(R.string.add_photos_button))
                  }
              Spacer(modifier = Modifier.width(dimensionResource(R.dimen.save_add_button_padding)))
              // Save photos button
              Button(
                  modifier = Modifier.testTag(SelectPinnedPhotosScreenTestTags.SAVE_BUTTON),
                  onClick = {
                    selectPinnedPhotosViewModel.savePhotos()
                    onBack()
                  }) {
                    Text(text = stringResource(R.string.add_photos_save_button))
                  }
            }
      }) { pd ->
        // Display a grid with the images
        LazyVerticalGrid(
            columns = GridCells.Fixed(integerResource(R.integer.images_on_grid)),
            modifier =
                Modifier.padding(pd).testTag(SelectPinnedPhotosScreenTestTags.VERTICAL_GRID)) {
              // AI helped for the itemsIndexed
              itemsIndexed(selectPinnedPhotosUIState.listUri) { index, uri ->
                AsyncImage(
                    modifier =
                        Modifier.testTag(SelectPinnedPhotosScreenTestTags.getTestTagForUri(index)),
                    model = uri,
                    contentDescription = null)
              }
            }
      }
}
