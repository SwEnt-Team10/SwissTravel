package com.github.swent.swisstravel.ui.profile

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.model.authentication.AuthRepository
import com.github.swent.swisstravel.model.authentication.AuthRepositoryFirebase
import com.github.swent.swisstravel.model.user.Preference
import com.github.swent.swisstravel.ui.composable.PreferenceSelector
import com.github.swent.swisstravel.ui.composable.ProfileImage
import com.github.swent.swisstravel.ui.navigation.NavigationActions
import com.github.swent.swisstravel.ui.navigation.NavigationTestTags
import com.github.swent.swisstravel.ui.navigation.Screen
import com.github.swent.swisstravel.utils.NetworkUtils

/** Test tags for the profile settings screen. */
object ProfileSettingsScreenTestTags {
  const val CONTENT = "content"
  const val EDIT_PROFILE_PIC = "editProfilePic"
  const val PROFILE_INFO = "profileInfo"
  const val PERSONAL_INFO = "personalInfo"
  const val EMAIL = "email"
  const val PREFERENCES_LIST = "preferencesList"
  const val PREFERENCES = "preferences"
  const val PREFERENCES_TOGGLE = "preferencesToggle"
  const val LOGOUT_BUTTON = "logoutButton"
  const val PREVIEW_PFP_TEXT = "previewPfpText"
  const val PREVIEW_PFP_CONFIRM = "previewPfpConfirm"
  const val PREVIEW_PFP_CANCEL = "previewPfpCancel"
  const val PREVIEW_PFP_IMAGE = "previewPfpImage"

  /**
   * A test tag for text.
   *
   * @param prefix The prefix for the test tag.
   */
  fun text(prefix: String) = "${prefix.uppercase()}_TEXT"

  /**
   * A test tag for a label.
   *
   * @param prefix The prefix for the test tag.
   */
  fun label(prefix: String) = "${prefix.uppercase()}_LABEL"

  /**
   * A test tag for a a TextField.
   *
   * @param prefix The prefix for the test tag.
   */
  fun textField(prefix: String) = "${prefix.uppercase()}_TEXTFIELD"

  /**
   * A test tag for an edit button.
   *
   * @param prefix The prefix for the test tag.
   */
  fun editButton(prefix: String) = "${prefix.uppercase()}_EDIT"

  /**
   * A test tag for a confirm button.
   *
   * @param prefix The prefix for the test tag.
   */
  fun confirmButton(prefix: String) = "${prefix.uppercase()}_CONFIRM"

  /**
   * A test tag for a cancel button.
   *
   * @param prefix The prefix for the test tag.
   */
  fun cancelButton(prefix: String) = "${prefix.uppercase()}_CANCEL"

  /**
   * A test tag for an empty text.
   *
   * @param prefix The prefix for the test tag.
   */
  fun empty(prefix: String) = "${prefix.uppercase()}_EMPTY"
}

/**
 * A screen that shows the user's profile information.
 *
 * @param profileSettingsViewModel The view model for this screen.
 * @param onBack The callback for when the back button is clicked.
 * @param navigationActions The navigation actions for this screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSettingsScreen(
    profileSettingsViewModel: ProfileSettingsViewModel = viewModel(),
    onBack: () -> Unit = {},
    navigationActions: NavigationActions? = null,
) {
  val context = LocalContext.current
  val uiState = profileSettingsViewModel.uiState.collectAsState().value

  // Image Picker
  val photoPickerLauncher =
      rememberLauncherForActivityResult(
          contract = ActivityResultContracts.PickVisualMedia(),
          onResult = { uri -> if (uri != null) profileSettingsViewModel.onProfilePicSelected(uri) })

  // Error Handling
  LaunchedEffect(uiState.errorMsg) {
    uiState.errorMsg
        ?.takeIf { it.isNotBlank() }
        ?.let {
          Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
          profileSettingsViewModel.clearErrorMsg()
        }
  }

  val isOnline = NetworkUtils.isOnline(context)
  LaunchedEffect(isOnline) { profileSettingsViewModel.refreshStats(isOnline) }

  // Preview Dialog for new Profile Picture
  if (uiState.pendingProfilePicUri != null) {
    AlertDialog(
        onDismissRequest = { profileSettingsViewModel.cancelProfilePicChange() },
        title = {
          Text(
              text = stringResource(R.string.preview_profile_pic_title),
              modifier = Modifier.testTag(ProfileSettingsScreenTestTags.PREVIEW_PFP_TEXT))
        },
        text = {
          Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              modifier = Modifier.fillMaxWidth()) {
                AsyncImage(
                    model = uiState.pendingProfilePicUri,
                    contentDescription = stringResource(R.string.preview_profile_pic_title),
                    contentScale = ContentScale.Crop,
                    modifier =
                        Modifier.size(dimensionResource(R.dimen.profile_logo_size))
                            .clip(CircleShape)
                            .testTag(ProfileSettingsScreenTestTags.PREVIEW_PFP_IMAGE))
              }
        },
        confirmButton = {
          TextButton(
              onClick = { profileSettingsViewModel.confirmProfilePicChange(context) },
              modifier = Modifier.testTag(ProfileSettingsScreenTestTags.PREVIEW_PFP_CONFIRM)) {
                Text(stringResource(R.string.preview_profile_pic_confirm))
              }
        },
        dismissButton = {
          TextButton(
              onClick = { profileSettingsViewModel.cancelProfilePicChange() },
              modifier = Modifier.testTag(ProfileSettingsScreenTestTags.PREVIEW_PFP_CANCEL)) {
                Text(stringResource(R.string.cancel))
              }
        })
  }

  Scaffold(
      topBar = {
        TopAppBar(
            title = { Text(stringResource(R.string.settings)) },
            navigationIcon = {
              IconButton(
                  onClick = onBack,
                  modifier = Modifier.testTag(NavigationTestTags.TOP_BAR_BUTTON)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back_to_profile),
                        tint = MaterialTheme.colorScheme.onBackground)
                  }
            },
            modifier = Modifier.testTag(NavigationTestTags.TOP_BAR))
      }) { pd ->
        if (uiState.isLoading) {
          Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
          }
        } else {
          val profileSettingsContentState =
              ProfileSettingsContentState(
                  uiState = uiState,
                  profileSettingsViewModel = profileSettingsViewModel,
                  modifier = Modifier.padding(pd),
                  onEditProfilePic = {
                    handleOfflineClick(
                        context = context,
                        isOnline = isOnline,
                        action = {
                          photoPickerLauncher.launch(
                              PickVisualMediaRequest(
                                  ActivityResultContracts.PickVisualMedia.ImageOnly))
                        })
                  },
                  navigationActions = navigationActions,
                  context = context,
                  isOnline = isOnline)
          ProfileSettingsContent(profileSettingsContentState)
        }
      }
}

/**
 * The state of the profile settings screen.
 *
 * @param uiState The state of the screen.
 * @param profileSettingsViewModel The view model for this screen.
 * @param modifier The modifier for the content.
 * @param authRepository The repository for authentication.
 * @param navigationActions The navigation actions for this screen.
 * @param context The context.
 * @param isOnline Whether the user is online.
 */
data class ProfileSettingsContentState(
    val uiState: ProfileSettingsUIState,
    val profileSettingsViewModel: ProfileSettingsViewModel,
    val modifier: Modifier,
    val onEditProfilePic: () -> Unit,
    val authRepository: AuthRepository = AuthRepositoryFirebase(),
    val navigationActions: NavigationActions? = null,
    val context: Context,
    val isOnline: Boolean
)

/**
 * The content of the profile screen.
 *
 * @param state The state of the screen.
 */
@Composable
private fun ProfileSettingsContent(state: ProfileSettingsContentState) {
  val scrollState = rememberScrollState()
  val isSignedIn = state.profileSettingsViewModel.userIsSignedIn()

  Column(
      modifier =
          state.modifier
              .fillMaxSize()
              .padding(dimensionResource(R.dimen.mid_padding))
              .verticalScroll(scrollState)
              .testTag(ProfileSettingsScreenTestTags.CONTENT),
      horizontalAlignment = Alignment.CenterHorizontally) {
        ProfileSettingsHeader(
            profilePicUrl = state.uiState.profilePicUrl, onEditClick = state.onEditProfilePic)

        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.mid_spacer)))

        ProfileInfoSection(
            uiState = state.uiState,
            viewModel = state.profileSettingsViewModel,
            context = state.context,
            isOnline = state.isOnline)

        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.mid_spacer)))

        PersonalInfoSection(email = state.uiState.email)

        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.mid_spacer)))

        PreferencesSection(
            selected = state.uiState.selectedPreferences,
            onToggle = { pref ->
              val sel = state.uiState.selectedPreferences
              state.profileSettingsViewModel.savePreferences(
                  if (pref in sel) sel - pref else sel + pref)
            })

        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.mid_spacer)))

        AuthButton(
            isSignedIn = isSignedIn,
            onClick = {
              if (isSignedIn) state.authRepository.signOut()
              state.navigationActions?.navigateTo(Screen.Landing, true)
            })
      }
}

/**
 * The profile header section of the profile settings screen.
 *
 * @param profilePicUrl The URL or UID of the user's profile picture.
 * @param onEditClick The callback for when the user clicks the edit button.
 */
@Composable
private fun ProfileSettingsHeader(profilePicUrl: String, onEditClick: () -> Unit) {
  Box(
      contentAlignment = Alignment.Center,
      modifier =
          Modifier.size(dimensionResource(R.dimen.profile_logo_size))
              .clip(CircleShape)
              .clickable(onClick = onEditClick)
              .testTag(ProfileSettingsScreenTestTags.EDIT_PROFILE_PIC)) {
        ProfileImage(urlOrUid = profilePicUrl, modifier = Modifier.fillMaxSize())

        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)))

        Icon(
            imageVector = Icons.Outlined.Edit,
            contentDescription = stringResource(R.string.edit_profile_pic),
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(dimensionResource(R.dimen.profile_edit_button)))
      }
}

/**
 * The profile information section of the profile settings screen.
 *
 * @param uiState The state of the screen.
 * @param viewModel The view model for this screen.
 * @param context The context.
 * @param isOnline Whether the user is online.
 */
@Composable
private fun ProfileInfoSection(
    uiState: ProfileSettingsUIState,
    viewModel: ProfileSettingsViewModel,
    context: Context,
    isOnline: Boolean
) {
  InfoSection(
      title = stringResource(R.string.profile_info),
      modifier = Modifier.testTag(ProfileSettingsScreenTestTags.PROFILE_INFO)) {
        EditableField(
            label = stringResource(R.string.name),
            text = uiState.name,
            isEditing = uiState.isEditingName,
            onStartEdit = { handleOfflineClick(context, isOnline, viewModel::startEditingName) },
            onSave = { viewModel.saveName(it) },
            onCancel = { viewModel.cancelEditingName() },
            testTagPrefix = "NAME")
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.tiny_spacer)))
        EditableField(
            label = stringResource(R.string.biography),
            text = uiState.biography,
            isEditing = uiState.isEditingBio,
            onStartEdit = { handleOfflineClick(context, isOnline, viewModel::startEditingBio) },
            onSave = { viewModel.saveBio(it) },
            onCancel = { viewModel.cancelEditingBio() },
            testTagPrefix = "BIOGRAPHY")
      }
}

/**
 * A personal information section of the profile settings screen.
 *
 * @param email The email of the user.
 */
@Composable
private fun PersonalInfoSection(email: String) {
  InfoSection(
      title = stringResource(R.string.personal_info),
      modifier = Modifier.testTag(ProfileSettingsScreenTestTags.PERSONAL_INFO)) {
        InfoItem(
            label = stringResource(R.string.email),
            value = email,
            modifier = Modifier.testTag(ProfileSettingsScreenTestTags.EMAIL))
      }
}

/**
 * A section of the profile settings screen that shows the user's preferences.
 *
 * @param selected The list of selected preferences.
 * @param onToggle The callback for when a preference is toggled.
 */
@Composable
private fun PreferencesSection(selected: List<Preference>, onToggle: (Preference) -> Unit) {
  var expanded by remember { mutableStateOf(false) }

  Column(
      modifier =
          Modifier.fillMaxWidth()
              .padding(vertical = dimensionResource(R.dimen.tiny_spacer))
              .clip(MaterialTheme.shapes.medium)
              .background(MaterialTheme.colorScheme.onPrimary)
              .padding(dimensionResource(R.dimen.small_padding))
              .testTag(ProfileSettingsScreenTestTags.PREFERENCES_LIST)) {
        // Header row
        Row(
            modifier = Modifier.fillMaxWidth().testTag(ProfileSettingsScreenTestTags.PREFERENCES),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
              Text(
                  text = stringResource(R.string.travel_pref),
                  style = MaterialTheme.typography.titleLarge)
              IconButton(
                  onClick = { expanded = !expanded },
                  modifier = Modifier.testTag(ProfileSettingsScreenTestTags.PREFERENCES_TOGGLE)) {
                    Icon(
                        imageVector =
                            if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer)
                  }
            }

        if (expanded) {
          Spacer(Modifier.height(dimensionResource(R.dimen.smaller_spacer)))
          Text(
              text = stringResource(R.string.default_pref_info),
              style =
                  MaterialTheme.typography.bodyMedium.copy(
                      color = MaterialTheme.colorScheme.onSurfaceVariant))
          Spacer(modifier = Modifier.height(dimensionResource(R.dimen.smaller_spacer)))

          PreferenceSelector(
              isChecked = { pref -> pref in selected },
              onCheckedChange = onToggle,
              textStyle = MaterialTheme.typography.bodyLarge)
        }
      }
}

/**
 * A button that allows the user to sign in or out.
 *
 * @param isSignedIn Whether the user is signed in.
 * @param onClick The callback for when the button is clicked.
 */
@Composable
private fun AuthButton(isSignedIn: Boolean, onClick: () -> Unit) {
  Button(
      onClick = onClick,
      modifier =
          Modifier.fillMaxWidth(0.5f)
              .height(dimensionResource(R.dimen.medium_button_height))
              .testTag(ProfileSettingsScreenTestTags.LOGOUT_BUTTON),
      shape = CircleShape,
      colors =
          ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary)) {
        Icon(
            imageVector =
                if (isSignedIn) Icons.AutoMirrored.Filled.Logout
                else Icons.AutoMirrored.Filled.Login,
            contentDescription =
                stringResource(if (isSignedIn) R.string.sign_out else R.string.sign_in),
            modifier = Modifier.padding(end = dimensionResource(R.dimen.tiny_padding)))
        Text(stringResource(if (isSignedIn) R.string.sign_out else R.string.sign_in))
      }
}

/**
 * A section of the profile settings screen that shows information.
 *
 * @param title The title of the section.
 * @param modifier The modifier for the section.
 * @param content The content of the section.
 */
@Composable
fun InfoSection(
    title: String,
    modifier: Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
  Column(
      modifier =
          modifier
              .fillMaxWidth()
              .padding(vertical = dimensionResource(R.dimen.tiny_spacer))
              .clip(MaterialTheme.shapes.large)
              .background(MaterialTheme.colorScheme.onPrimary)
              .padding(dimensionResource(R.dimen.small_padding))) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = dimensionResource(R.dimen.smaller_padding)))
        content()
      }
}

/**
 * A single item of information in the profile settings screen.
 *
 * @param label The label for the item.
 * @param value The value of the item.
 * @param modifier The modifier for the item.
 */
@Composable
fun InfoItem(label: String, value: String, modifier: Modifier) {
  Column(
      modifier =
          Modifier.fillMaxWidth().padding(bottom = dimensionResource(R.dimen.smaller_spacer))) {
        Text(
            text = label,
            style =
                MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface))
        Text(
            text = value.ifBlank { stringResource(R.string.hyphen) },
            style = MaterialTheme.typography.bodyLarge,
            modifier = modifier)
      }
}

/**
 * A field that can be edited in the profile settings screen.
 *
 * @param label The label for the field.
 * @param text The current text in the field.
 * @param isEditing Whether the field is currently being edited.
 * @param onStartEdit The callback for when the field is clicked.
 * @param onSave The callback for when the field is saved.
 * @param onCancel The callback for when the field is cancelled.
 * @param testTagPrefix The prefix for the test tags.
 */
@Composable
fun EditableField(
    label: String,
    text: String,
    isEditing: Boolean,
    onStartEdit: () -> Unit,
    onSave: (String) -> Unit,
    onCancel: () -> Unit,
    testTagPrefix: String
) {
  var editedText by remember(isEditing) { mutableStateOf(text) }

  Column(modifier = Modifier.fillMaxWidth()) {

    // Header Row with label and action buttons
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()) {
          Text(
              text = label,
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
              modifier = Modifier.testTag(ProfileSettingsScreenTestTags.label(testTagPrefix)))

          EditButtons(
              isEditing = isEditing,
              onStartEdit = { onStartEdit() },
              onSave = { onSave(editedText) },
              onCancel = { onCancel() },
              testTagPrefix = testTagPrefix)
        }

    // Editing mode or Display mode
    if (isEditing) {
      TextField(
          value = editedText,
          onValueChange = { editedText = it },
          colors =
              TextFieldDefaults.colors(
                  focusedContainerColor = MaterialTheme.colorScheme.onPrimary,
                  unfocusedContainerColor = MaterialTheme.colorScheme.onPrimary),
          placeholder = {
            Text(
                text = stringResource(R.string.enter_text),
                style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic))
          },
          modifier =
              Modifier.fillMaxWidth()
                  .testTag(ProfileSettingsScreenTestTags.textField(testTagPrefix)))
    } else {
      DisplayTextOrPlaceholder(text = text, testTagPrefix = testTagPrefix)
    }
  }
}

/**
 * A row of buttons for editing or saving.
 *
 * @param isEditing Whether the user is editing.
 * @param onStartEdit The callback for when the user starts editing.
 * @param onSave The callback for when the user saves.
 * @param onCancel The callback for when the user cancels.
 * @param testTagPrefix The prefix for the test tags.
 */
@Composable
private fun EditButtons(
    isEditing: Boolean,
    onStartEdit: () -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    testTagPrefix: String
) {
  Row {
    if (isEditing) {
      IconButton(
          onClick = onCancel,
          modifier = Modifier.testTag(ProfileSettingsScreenTestTags.cancelButton(testTagPrefix))) {
            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cancel))
          }
    }

    IconButton(
        onClick = { if (isEditing) onSave() else onStartEdit() },
        modifier =
            Modifier.testTag(
                if (isEditing) {
                  ProfileSettingsScreenTestTags.confirmButton(testTagPrefix)
                } else {
                  ProfileSettingsScreenTestTags.editButton(testTagPrefix)
                })) {
          Icon(
              imageVector = if (isEditing) Icons.Default.Check else Icons.Outlined.Edit,
              contentDescription =
                  if (isEditing) stringResource(R.string.save) else stringResource(R.string.edit))
        }
  }
}

/**
 * A text that is displayed or a placeholder.
 *
 * @param text The text to display.
 * @param testTagPrefix The prefix for the test tag.
 */
@Composable
private fun DisplayTextOrPlaceholder(text: String, testTagPrefix: String) {
  if (text.isBlank()) {
    Text(
        text = stringResource(R.string.press_edit_to_add),
        style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic),
        modifier = Modifier.testTag(ProfileSettingsScreenTestTags.empty(testTagPrefix)))
  } else {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.testTag(ProfileSettingsScreenTestTags.text(testTagPrefix)))
  }
}

/**
 * Handle an offline click.
 *
 * @param context The context.
 * @param isOnline Whether the user is online.
 * @param action The action to perform
 */
private fun handleOfflineClick(context: Context, isOnline: Boolean, action: () -> Unit) {
  if (isOnline) {
    action()
  } else {
    Toast.makeText(context, context.getString(R.string.requires_internet), Toast.LENGTH_SHORT)
        .show()
  }
}
