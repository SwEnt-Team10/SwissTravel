package com.github.swent.swisstravel.ui.profile

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.model.authentication.AuthRepository
import com.github.swent.swisstravel.model.authentication.AuthRepositoryFirebase
import com.github.swent.swisstravel.model.user.Preference
import com.github.swent.swisstravel.ui.composable.PreferenceSelector
import com.github.swent.swisstravel.ui.navigation.NavigationActions
import com.github.swent.swisstravel.ui.navigation.NavigationTestTags
import com.github.swent.swisstravel.ui.navigation.Screen
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

object ProfileScreenTestTags {
  const val PROFILE_PIC = "profilePic"
  const val PREFERENCES_LIST = "preferencesList"
  const val DISPLAY_NAME = "displayName"
  const val EMAIL = "email"
  const val GREETING = "greeting"
  const val PERSONAL_INFO = "personalInfo"
  const val PREFERENCES = "preferences"
  const val PREFERENCES_TOGGLE = "preferencesToggle"
  const val LOGOUT_BUTTON = "logoutButton"
}

/**
 * A screen that shows the user's profile information.
 *
 * @param profileScreenViewModel The view model for this screen.
 * @param navigationActions The navigation actions for this screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    profileScreenViewModel: ProfileScreenViewModel = viewModel(),
    navigationActions: NavigationActions? = null,
) {
  val context = LocalContext.current
  val uiState = profileScreenViewModel.uiState.collectAsState().value

  LaunchedEffect(uiState.errorMsg) {
    uiState.errorMsg
        ?.takeIf { it.isNotBlank() }
        ?.let {
          Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
          profileScreenViewModel.clearErrorMsg()
        }
  }

  Scaffold(
      topBar = {
        TopAppBar(
            title = { Text(stringResource(R.string.my_profile)) },
            modifier = Modifier.testTag(NavigationTestTags.TOP_BAR))
      }) { pd ->
        if (uiState.isLoading) {
          Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
          }
        } else {
          ProfileScreenContent(
              uiState = uiState,
              profileScreenViewModel = profileScreenViewModel,
              modifier = Modifier.padding(pd),
              navigationActions = navigationActions)
        }
      }
}

/**
 * The content of the profile screen.
 *
 * @param uiState The state of the screen.
 * @param profileScreenViewModel The view model for this screen.
 */
@Composable
private fun ProfileScreenContent(
    uiState: ProfileScreenUIState,
    profileScreenViewModel: ProfileScreenViewModel,
    modifier: Modifier,
    authRepository: AuthRepository = AuthRepositoryFirebase(),
    navigationActions: NavigationActions? = null
) {
  val scrollState = rememberScrollState()
  val isSignedIn = Firebase.auth.currentUser != null

  Column(
      modifier = modifier.fillMaxSize().padding(20.dp).verticalScroll(scrollState),
      horizontalAlignment = Alignment.CenterHorizontally) {
        ProfileHeader(photoUrl = uiState.profilePicUrl, name = uiState.name)

        Spacer(Modifier.height(24.dp))

        PersonalInfoSection(name = uiState.name, email = uiState.email)

        Spacer(Modifier.height(24.dp))

        PreferencesSection(
            selected = uiState.selectedPreferences,
            onToggle = { pref ->
              val sel = uiState.selectedPreferences
              profileScreenViewModel.savePreferences(if (pref in sel) sel - pref else sel + pref)
            })

        AuthButton(
            isSignedIn = isSignedIn,
            onClick = {
              if (isSignedIn) authRepository.signOut()
              navigationActions?.navigateTo(Screen.Auth, true)
            })
      }
}

/**
 * The profile header section of the profile screen.
 *
 * @param photoUrl The URL of the profile picture.
 * @param name The name of the user.
 */
@Composable
private fun ProfileHeader(photoUrl: String, name: String) {
  AsyncImage(
      model = photoUrl.ifBlank { R.drawable.default_profile_pic },
      contentDescription = stringResource(R.string.profile_pic_desc),
      modifier = Modifier.size(100.dp).clip(CircleShape).testTag(ProfileScreenTestTags.PROFILE_PIC))
  Text(
      text = "${stringResource(R.string.hi)} ${name.ifBlank { "User" }}!",
      style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
      modifier = Modifier.padding(top = 12.dp).testTag(ProfileScreenTestTags.GREETING))
}

/**
 * A personal information section of the profile screen.
 *
 * @param name The name of the user.
 * @param email The email of the user.
 */
@Composable
private fun PersonalInfoSection(name: String, email: String) {
  InfoSection(
      title = stringResource(R.string.personal_info),
      modifier = Modifier.testTag(ProfileScreenTestTags.PERSONAL_INFO)) {
        InfoItem(
            label = stringResource(R.string.name),
            value = name,
            modifier = Modifier.testTag(ProfileScreenTestTags.DISPLAY_NAME))
        InfoItem(
            label = stringResource(R.string.email),
            value = email,
            modifier = Modifier.testTag(ProfileScreenTestTags.EMAIL))
      }
}

/**
 * A section of the profile screen that shows the user's preferences.
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
              .padding(vertical = 8.dp)
              .clip(MaterialTheme.shapes.medium)
              .background(MaterialTheme.colorScheme.secondaryContainer)
              .padding(16.dp)
              .testTag(ProfileScreenTestTags.PREFERENCES_LIST)) {
        // Header row
        Row(
            modifier = Modifier.fillMaxWidth().testTag(ProfileScreenTestTags.PREFERENCES),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
              Text(
                  text = stringResource(R.string.travel_pref),
                  style =
                      MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                  color = MaterialTheme.colorScheme.onSecondaryContainer)
              IconButton(
                  onClick = { expanded = !expanded },
                  modifier = Modifier.testTag(ProfileScreenTestTags.PREFERENCES_TOGGLE)) {
                    Icon(
                        imageVector =
                            if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer)
                  }
            }

        if (expanded) {
          Spacer(Modifier.height(12.dp))
          Text(
              text = stringResource(R.string.default_pref_info),
              style =
                  MaterialTheme.typography.bodyMedium.copy(
                      color = MaterialTheme.colorScheme.onSurfaceVariant))
          Spacer(Modifier.height(12.dp))

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
          Modifier.fillMaxWidth(0.5f).height(50.dp).testTag(ProfileScreenTestTags.LOGOUT_BUTTON),
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
            modifier = Modifier.padding(end = 8.dp))
        Text(stringResource(if (isSignedIn) R.string.sign_out else R.string.sign_in))
      }
}

/**
 * A section of the profile screen that shows information.
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
              .padding(vertical = 8.dp)
              .clip(MaterialTheme.shapes.medium)
              .background(MaterialTheme.colorScheme.secondaryContainer)
              .padding(16.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(bottom = 12.dp))
        content()
      }
}

/**
 * A single item of information in the profile screen.
 *
 * @param label The label for the item.
 * @param value The value of the item.
 * @param modifier The modifier for the item.
 */
@Composable
fun InfoItem(label: String, value: String, modifier: Modifier) {
  Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
    Text(
        text = label,
        style =
            MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)))
    Text(
        text = value.ifBlank { "-" },
        style = MaterialTheme.typography.bodyLarge,
        modifier = modifier)
  }
}
