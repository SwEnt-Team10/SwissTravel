package com.github.swent.swisstravel.ui.authentication

/** This file is largely adapted from the bootcamp solution. */
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.ui.composable.GoogleSignInButton

object SignInScreenTestTags {
  const val APP_LOGO = "appLogo"
  const val LOGIN_TITLE = "loginTitle"
  const val LOGIN_BUTTON = "loginButton"
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

          Spacer(modifier = Modifier.height(16.dp))

          // Welcome Text
          Text(
              modifier = Modifier.testTag(SignInScreenTestTags.LOGIN_TITLE),
              text = stringResource(R.string.welcome_msg),
              style =
                  MaterialTheme.typography.headlineLarge.copy(fontSize = 57.sp, lineHeight = 64.sp),
              fontWeight = FontWeight.Bold,
              textAlign = TextAlign.Center)

          Spacer(modifier = Modifier.height(48.dp))

          // Authenticate With Google Button
          if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(48.dp))
          } else {
            GoogleSignInButton(onSignInClick = { authViewModel.signIn(context, credentialManager) })
          }
        }
      })
}

@Preview
@Composable
fun SignInScreenPreview() {
  SignInScreen()
}
