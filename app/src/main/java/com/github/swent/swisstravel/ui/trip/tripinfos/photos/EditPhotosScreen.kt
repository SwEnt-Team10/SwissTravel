package com.github.swent.swisstravel.ui.trip.tripinfos.photos

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.github.swent.swisstravel.R

/** Test tags for the edit screen photos */
object EditPhotosScreenTestTags {
  const val EDIT_SCAFFOLD = "editScaffold"
  const val EDIT_TOP_BAR = "editTopBar"
  const val EDIT_TOP_BAR_TITLE = "editTopBarTitle"
  const val EDIT_CANCEL_BUTTON = "editCancelButton"
  const val EDIT_BOTTOM_BAR = "editBottomBar"
  const val EDIT_REMOVE_BUTTON = "editRemoveButton"
  const val EDIT_VERTICAL_GRID = "editVerticalGrid"
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
  // Start by loading the photos from the repository
  LaunchedEffect(tripId) { photosViewModel.loadPhotos(tripId) }
  val uiState by photosViewModel.uiState.collectAsState()
  Scaffold(
      modifier = Modifier.testTag(EditPhotosScreenTestTags.EDIT_SCAFFOLD),
      topBar = {
        EditTopBar(
            onCancel = {
              // Save then quit
              photosViewModel.savePhotos(tripId)
              onCancel()
            })
      },
      bottomBar = {
        EditBottomBar(
            onRemove = {
              // Remove from the state
              photosViewModel.removePhotos()
            },
            uiState = uiState)
      }) { pd ->
        // Done with AI
        LazyVerticalGrid(
            columns = GridCells.Fixed(integerResource(R.integer.images_on_grid)),
            modifier = Modifier.padding(pd).testTag(EditPhotosScreenTestTags.EDIT_VERTICAL_GRID)) {
              itemsIndexed(uiState.listUri) { index, uri ->
                Box(modifier = Modifier.clickable { photosViewModel.selectToRemove(index) }) {
                  AsyncImage(
                      model = uri,
                      contentDescription = null,
                  )
                  // the veil added when a photo is selected
                  if (uiState.uriSelected.contains(index)) {
                    Box(
                        modifier =
                            Modifier.matchParentSize()
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)))
                  }
                }
              }
            }
      }
}
/**
 * The top app bar of the edit mode. It contains only a button to quit the mode.
 *
 * @param onCancel the function to call when you want to quit the mode
 */
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
 * A button that can exit the edit mode
 *
 * @param onCancel the function to call when you click on the button
 */
@Composable
private fun CancelButton(onCancel: () -> Unit = {}) {
  IconButton(
      modifier = Modifier.testTag(EditPhotosScreenTestTags.EDIT_CANCEL_BUTTON),
      onClick = { onCancel() }) {
        Icon(
            imageVector = Icons.Filled.Cancel,
            contentDescription = stringResource(R.string.cancel_edit),
            tint = MaterialTheme.colorScheme.onBackground)
      }
}

/**
 * The bottom bar of the edit mode
 *
 * @param onRemove the function to call when you want to remove selected photos
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
