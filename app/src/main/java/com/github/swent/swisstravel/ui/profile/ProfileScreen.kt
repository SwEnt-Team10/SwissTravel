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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.github.swent.swisstravel.ui.navigation.BottomNavigationMenu
import com.github.swent.swisstravel.ui.navigation.NavigationActions
import com.github.swent.swisstravel.ui.navigation.NavigationTestTags
import com.github.swent.swisstravel.ui.navigation.Screen
import com.github.swent.swisstravel.ui.navigation.Tab
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
 * A composable that represents the profile screen.
 *
 * @param profileScreenViewModel The view model for the profile screen.
 * @param navigationActions The navigation actions for the app.
 */
@Composable
fun ProfileScreen(
    profileScreenViewModel: ProfileScreenViewModel = viewModel(),
    navigationActions: NavigationActions? = null,
) {
  val context = LocalContext.current
  val uiState = profileScreenViewModel.uiState.collectAsState().value

  LaunchedEffect(uiState.errorMsg) {
    val msg = uiState.errorMsg
    if (!msg.isNullOrBlank()) {
      Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
      profileScreenViewModel.clearErrorMsg()
    }
  }

  Scaffold(
      topBar = {
        Text(
            text = stringResource(R.string.my_profile),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp))
      },
      bottomBar = {
        BottomNavigationMenu(
            selectedTab = Tab.Profile,
            onTabSelected = { tab -> navigationActions?.navigateTo(tab.destination) },
            modifier = Modifier.testTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU))
      },
      content = { pd ->
        if (uiState.isLoading) {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
          }
        } else {
          ProfileScreenContent(
              uiState = uiState,
              profileScreenViewModel = profileScreenViewModel,
              modifier = Modifier.padding(pd),
              navigationActions = navigationActions)
        }
      })
}

/**
 * A composable that represents the content of the profile screen.
 *
 * @param uiState The UI state of the profile screen.
 * @param profileScreenViewModel The view model for the profile screen.
 * @param modifier The modifier to apply to the content.
 * @param authRepository The authentication repository.
 * @param navigationActions The navigation actions for the app.
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
        AsyncImage(
            model = uiState.profilePicUrl.ifBlank { R.drawable.default_profile_pic },
            contentDescription = stringResource(R.string.profile_pic_desc),
            modifier =
                Modifier.size(100.dp).clip(CircleShape).testTag(ProfileScreenTestTags.PROFILE_PIC))

        Text(
            text = "${stringResource(R.string.hi)} ${uiState.name.ifBlank { "User" }}!",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(top = 12.dp).testTag(ProfileScreenTestTags.GREETING))

        Spacer(modifier = Modifier.height(24.dp))

        // Personal Info Section
        InfoSection(
            title = stringResource(R.string.personal_info),
            modifier = Modifier.testTag(ProfileScreenTestTags.PERSONAL_INFO)) {
              InfoItem(
                  label = stringResource(R.string.name),
                  value = uiState.name,
                  modifier = Modifier.testTag(ProfileScreenTestTags.DISPLAY_NAME))
              InfoItem(
                  label = stringResource(R.string.email),
                  value = uiState.email,
                  modifier = Modifier.testTag(ProfileScreenTestTags.EMAIL))
            }

        Spacer(modifier = Modifier.height(24.dp))

        // Travel Preferences Section
        // --- Travel Preferences (collapsible) ---
        var prefsExpanded by remember { mutableStateOf(false) }

        Column(
            modifier =
                Modifier.fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(16.dp)
                    .testTag(ProfileScreenTestTags.PREFERENCES_LIST)) {
              // Header row with title + chevron
              Row(
                  modifier = Modifier.fillMaxWidth().testTag(ProfileScreenTestTags.PREFERENCES),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = stringResource(R.string.travel_pref),
                        style =
                            MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    IconButton(
                        onClick = { prefsExpanded = !prefsExpanded },
                        modifier = Modifier.testTag(ProfileScreenTestTags.PREFERENCES_TOGGLE)) {
                          Icon(
                              imageVector =
                                  if (prefsExpanded) Icons.Default.ExpandLess
                                  else Icons.Default.ExpandMore,
                              contentDescription = if (prefsExpanded) "Collapse" else "Expand",
                              tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                  }

              if (prefsExpanded) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.default_pref_info),
                    style =
                        MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant),
                )
                Spacer(Modifier.height(12.dp))

                val selected: List<Preference> = uiState.selectedPreferences
                PreferenceSelector(
                    isChecked = { pref -> pref in selected },
                    onCheckedChange = { pref ->
                      profileScreenViewModel.savePreferences(
                          if (pref in selected) selected - pref else selected + pref)
                    },
                    textStyle = MaterialTheme.typography.bodyLarge)
              }
            }
        Button(
            onClick = {
              if (isSignedIn) {
                authRepository.signOut()
              }
              navigationActions?.navigateTo(Screen.Auth)
            },
            modifier =
                Modifier.fillMaxWidth(0.5f)
                    .height(50.dp)
                    .testTag(ProfileScreenTestTags.LOGOUT_BUTTON),
            shape = CircleShape,
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                )) {
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
}

/**
 * A composable that represents an info section.
 *
 * @param title The title of the section.
 * @param modifier The modifier to apply to the section.
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
 * A composable that represents an info item.
 *
 * @param label The label of the item.
 * @param value The value of the item.
 * @param modifier The modifier to apply to the item.
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
