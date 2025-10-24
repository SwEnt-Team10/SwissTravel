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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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

  const val LOGOUT_BUTTON = "logoutButton"
  const val LOGIN_BUTTON = "loginButton"

  fun preferenceSwitchTag(title: String): String = "preferenceSwitch:$title"
}

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
        InfoSection(
            title = stringResource(R.string.travel_pref),
            modifier = Modifier.testTag(ProfileScreenTestTags.PREFERENCES_LIST)) {
              Text(
                  text = stringResource(R.string.default_pref_info),
                  style =
                      MaterialTheme.typography.bodyMedium.copy(
                          color = MaterialTheme.colorScheme.onSurfaceVariant),
                  modifier = Modifier.padding(bottom = 12.dp))

              profileScreenViewModel.allPreferences.forEach { pref ->
                val isSelected = uiState.selectedPreferences.contains(pref)
                PreferenceToggle(
                    title = pref,
                    checked = isSelected,
                    onCheckedChange = { checked ->
                      val newSelection =
                          if (checked) uiState.selectedPreferences + pref
                          else uiState.selectedPreferences - pref
                      profileScreenViewModel.savePreferences(newSelection)
                    },
                    modifier = Modifier.testTag(ProfileScreenTestTags.PREFERENCES))
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
                    .testTag(
                        if (isSignedIn) ProfileScreenTestTags.LOGOUT_BUTTON
                        else ProfileScreenTestTags.LOGIN_BUTTON),
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

@Composable
fun PreferenceToggle(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier
) {
  Row(
      modifier = modifier.fillMaxWidth().padding(vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            text = title,
            style =
                MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(ProfileScreenTestTags.preferenceSwitchTag(title)))
      }
}
