package com.github.swent.swisstravel.ui.authentication

/** This file is largely adapted from the bootcamp solution. */

// import androidx.compose.ui.tooling.preview.Preview    // <-- Uncomment this line to enable
// preview
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.swent.swisstravel.R

object SignInScreenTestTags {
  const val APP_LOGO = "appLogo"
  const val LOGIN_BUTTON = "loginButton"
  const val LOADING_INDICATOR = "loadingIndicator"
}

@Composable
fun SignInScreen(
    authViewModel: SignInViewModel = viewModel(),
    credentialManager: CredentialManager = CredentialManager.create(LocalContext.current),
    onSignedIn: () -> Unit = {}
) {
  val context = LocalContext.current
  val uiState by authViewModel.uiState.collectAsState()

  // Show error message if login fails
  LaunchedEffect(uiState.errorMsg) {
    uiState.errorMsg?.let {
      Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
      authViewModel.clearErrorMsg()
    }
  }

  // Navigate to overview screen on successful login
  LaunchedEffect(uiState.user) {
    uiState.user?.let {
      Toast.makeText(context, R.string.login_success, Toast.LENGTH_SHORT).show()
      onSignedIn()
    }
  }

  // The main container for the screen
  // A surface container using the 'background' color from the theme
  Scaffold(
      modifier = Modifier.fillMaxSize(),
      content = { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
          // App Logo Image
          Image(
              painter = painterResource(id = R.drawable.swisstravel_logo),
              contentDescription = "App Logo",
              modifier = Modifier.size(250.dp).testTag(SignInScreenTestTags.APP_LOGO))

          Spacer(modifier = Modifier.height(48.dp))

          // Authenticate With Google Button
          if (uiState.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp).testTag(SignInScreenTestTags.LOADING_INDICATOR))
          } else {
            GoogleSignInButton(onSignInClick = { authViewModel.signIn(context, credentialManager) })
          }
        }
      })
}

@Composable
fun GoogleSignInButton(onSignInClick: () -> Unit) {
  Button(
      onClick = onSignInClick,
      colors =
          ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.surfaceVariant,
              contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
      shape = RoundedCornerShape(50),
      border = BorderStroke(1.dp, Color.Companion.LightGray),
      modifier = Modifier.padding(8.dp).height(48.dp).testTag(SignInScreenTestTags.LOGIN_BUTTON)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.Companion.fillMaxWidth()) {
              // Load the 'Google logo' from resources
              Image(
                  painter = painterResource(id = R.drawable.google_logo),
                  contentDescription = "Google Logo",
                  modifier = Modifier.size(30.dp).padding(end = 8.dp))

              // Text for the button
              Text(
                  text = stringResource(R.string.google_sign_in),
                  color = Color.Companion.Gray,
                  fontSize = 16.sp,
                  fontWeight = FontWeight.Companion.Medium)
            }
      }
}

/**
 * Preview function for the SignInScreen, commented out to avoid unchecked coverage.
 *
 * Do not forget to uncomment 'import androidx.compose.ui.tooling.preview.Preview' at the beginning
 * of this file
 */
// @Preview
// @Composable
// fun SignInScreenPreview() {
//  SignInScreen()
// }
