package com.github.swent.swisstravel.ui.profile

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.ui.navigation.NavigationActions
import com.github.swent.swisstravel.ui.navigation.NavigationTestTags
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

object ProfileScreenTestTags {
  const val PROFILE_PIC = "profilePic"
  const val SETTINGS_BUTTON = "settingsButton"
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
 * @param profileViewModel The view model for this screen.
 * @param onSettings The callback to navigate to the settings screen.
 * @param navigationActions The navigation actions for this screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    profileViewModel: ProfileViewModel = viewModel(),
    onSettings: () -> Unit = {},
    navigationActions: NavigationActions? = null,
) {
  val context = LocalContext.current
  val uiState = profileViewModel.uiState.collectAsState().value

  LaunchedEffect(uiState.errorMsg) {
    uiState.errorMsg
        ?.takeIf { it.isNotBlank() }
        ?.let {
          Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
          profileViewModel.clearErrorMsg()
        }
  }

  Scaffold(
      topBar = {
        TopAppBar(
            title = { Text(stringResource(R.string.my_profile)) },
            actions = {
              IconButton(
                  onClick = onSettings,
                  modifier = Modifier.testTag(ProfileScreenTestTags.SETTINGS_BUTTON)) {
                    Icon(
                        Icons.Outlined.Settings,
                        contentDescription = stringResource(R.string.settings))
                  }
            },
            modifier = Modifier.testTag(NavigationTestTags.TOP_BAR))
      }) { pd ->
        if (uiState.isLoading) {
          Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
          }
        } else {
          ProfileScreenContent(
              uiState = uiState,
              profileScreenViewModel = profileViewModel,
              modifier = Modifier.padding(pd))
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
    uiState: ProfileUIState,
    profileScreenViewModel: ProfileViewModel,
    modifier: Modifier
) {
  val scrollState = rememberScrollState()
  val isSignedIn = Firebase.auth.currentUser != null

  Column(
      modifier =
          modifier
              .fillMaxSize()
              .padding(dimensionResource(R.dimen.mid_padding))
              .verticalScroll(scrollState),
      horizontalAlignment = Alignment.CenterHorizontally) {}
}
