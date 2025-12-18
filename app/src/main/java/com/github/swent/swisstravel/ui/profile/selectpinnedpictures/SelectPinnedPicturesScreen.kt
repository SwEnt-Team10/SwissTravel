package com.github.swent.swisstravel.ui.profile.selectpinnedpictures

import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.ui.composable.CancelButton
import com.github.swent.swisstravel.ui.composable.DeleteDialog
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
  const val PHOTO_GRID = "photoGrid"
}

/**
 * Wrapper for all UI actions to reduce argument count.
 *
 * @property onBack The callback for when the back button is clicked.
 * @property onToggleEditMode The callback for when the edit button is clicked.
 * @property onDeleteRequest The callback for when the delete button is clicked.
 * @property onAddPhotos The callback for when the add button is clicked
 * @property onSave The callback for when the save button is clicked.
 * @property onImageClick The callback for when an image is clicked.
 */
data class SelectPinnedPicturesActions(
    val onBack: () -> Unit,
    val onToggleEditMode: (Boolean) -> Unit,
    val onDeleteRequest: () -> Unit,
    val onAddPhotos: () -> Unit,
    val onSave: () -> Unit,
    val onImageClick: (Int) -> Unit
)

/**
 * A screen that allows the user to select pinned photos.
 *
 * @param onBack The callback for when the back button is clicked.
 * @param selectPinnedPicturesViewModel The view model for this screen.
 * @param launchPickerOverride The override for the picker launcher.
 */
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
  var showDeleteDialog by remember { mutableStateOf(false) }

  // AI helped for the picker
  val pickerLauncher =
      rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
        // Context needed cause the ViewModel needs it to resolve URIs to Bytes
        if (uris.isNotEmpty()) {
          selectPinnedPicturesViewModel.addNewImages(context, uris)
        }
      }
  val launchPicker = launchPickerOverride ?: { request -> pickerLauncher.launch(request) }

  BackHandler { if (isEditMode) isEditMode = false else onBack() }

  LaunchedEffect(uiState.errorMsg) {
    uiState.errorMsg
        ?.takeIf { it.isNotBlank() }
        ?.let { msg ->
          Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
          selectPinnedPicturesViewModel.clearErrorMsg()
        }
  }

  if (showDeleteDialog) {
    val selectedCount = uiState.selectedIndices.size
    DeleteDialog(
        onConfirm = {
          selectPinnedPicturesViewModel.removeSelectedImages()
          isEditMode = false
          showDeleteDialog = false
        },
        onCancel = { showDeleteDialog = false },
        title =
            pluralStringResource(
                R.plurals.confirm_delete_title_images, selectedCount, selectedCount))
  }

  val actions =
      SelectPinnedPicturesActions(
          onBack = onBack,
          onToggleEditMode = { isEditMode = it },
          onDeleteRequest = { showDeleteDialog = true },
          onAddPhotos = {
            launchPicker(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
          },
          onSave = { selectPinnedPicturesViewModel.savePictures(onSuccess = { onBack() }) },
          onImageClick = { index -> selectPinnedPicturesViewModel.toggleSelection(index) })

  SelectPinnedPicturesContent(uiState = uiState, isEditMode = isEditMode, actions = actions)
}

/**
 * Structural component: Assembles the Scaffold, TopBar, and BottomBar.
 *
 * @param uiState The current UI state.
 * @param isEditMode Whether the screen is in edit mode.
 * @param actions The actions to perform.
 */
@Composable
private fun SelectPinnedPicturesContent(
    uiState: SelectPinnedPicturesUIState,
    isEditMode: Boolean,
    actions: SelectPinnedPicturesActions
) {
  Scaffold(
      modifier = Modifier.testTag(SelectPinnedPicturesScreenTestTags.MAIN_SCREEN),
      topBar = {
        PinnedPicturesTopBar(
            isEditMode = isEditMode,
            hasImages = uiState.images.isNotEmpty(),
            onBack = actions.onBack,
            onCancelEdit = { actions.onToggleEditMode(false) },
            onEnterEdit = { actions.onToggleEditMode(true) })
      },
      bottomBar = {
        PinnedPicturesBottomBar(
            isEditMode = isEditMode,
            isLoading = uiState.isLoading,
            hasSelection = uiState.selectedIndices.isNotEmpty(),
            onDelete = actions.onDeleteRequest,
            onAdd = actions.onAddPhotos,
            onSave = actions.onSave)
      }) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
          if (!uiState.isLoading || uiState.images.isNotEmpty()) {
            PhotoGrid(
                items = uiState.images,
                modifier =
                    Modifier.fillMaxSize().testTag(SelectPinnedPicturesScreenTestTags.PHOTO_GRID),
                onClick = if (isEditMode) actions.onImageClick else null,
                isSelected =
                    if (isEditMode) { index -> uiState.selectedIndices.contains(index) } else null,
                modelMapper = { it.bitmap })
          }

          if (uiState.isLoading) {
            CircularProgressIndicator(
                modifier =
                    Modifier.align(Alignment.Center)
                        .testTag(SelectPinnedPicturesScreenTestTags.LOADING_INDICATOR))
          }
        }
      }
}

/**
 * Structural component: The top bar of the screen.
 *
 * @param isEditMode Whether the screen is in edit mode.
 * @param hasImages Whether the screen has images.
 * @param onBack The callback for when the back button is clicked.
 * @param onCancelEdit The callback for when the cancel button is clicked.
 * @param onEnterEdit The callback for when the edit button is clicked
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PinnedPicturesTopBar(
    isEditMode: Boolean,
    hasImages: Boolean,
    onBack: () -> Unit,
    onCancelEdit: () -> Unit,
    onEnterEdit: () -> Unit
) {
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
          CancelButton(
              onCancel = onCancelEdit, contentDescription = stringResource(R.string.cancel_edit))
        } else {
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
        if (!isEditMode && hasImages) {
          IconButton(
              onClick = onEnterEdit,
              modifier = Modifier.testTag(SelectPinnedPicturesScreenTestTags.EDIT_BUTTON)) {
                Icon(
                    Icons.Outlined.Edit,
                    contentDescription = stringResource(R.string.edit_button_description))
              }
        }
      })
}

/**
 * Structural component: The bottom bar of the screen.
 *
 * @param isEditMode Whether the screen is in edit mode.
 * @param isLoading Whether the screen is currently loading.
 * @param hasSelection Whether the screen has a selection.
 * @param onDelete The callback for when the delete button is clicked.
 * @param onAdd The callback for when the add button is clicked.
 * @param onSave The callback for when the save button is clicked.
 */
@Composable
private fun PinnedPicturesBottomBar(
    isEditMode: Boolean,
    isLoading: Boolean,
    hasSelection: Boolean,
    onDelete: () -> Unit,
    onAdd: () -> Unit,
    onSave: () -> Unit
) {
  Row(
      modifier = Modifier.fillMaxWidth().testTag(SelectPinnedPicturesScreenTestTags.BOTTOM_BAR),
      horizontalArrangement = Arrangement.Center) {
        if (isEditMode) {
          Button(
              modifier = Modifier.testTag(SelectPinnedPicturesScreenTestTags.REMOVE_BUTTON),
              onClick = onDelete,
              enabled = hasSelection) {
                Text(text = stringResource(R.string.remove_button))
              }
        } else {
          Button(
              modifier = Modifier.testTag(SelectPinnedPicturesScreenTestTags.ADD_PICTURE_BUTTON),
              onClick = onAdd,
              enabled = !isLoading) {
                Text(text = stringResource(R.string.add_photos_button))
              }

          Spacer(modifier = Modifier.width(dimensionResource(R.dimen.small_spacer)))

          Button(
              modifier = Modifier.testTag(SelectPinnedPicturesScreenTestTags.SAVE_BUTTON),
              onClick = onSave,
              enabled = !isLoading) {
                Text(text = stringResource(R.string.save))
              }
        }
      }
}
