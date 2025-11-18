package com.github.swent.swisstravel.ui.authentication

/** This file is largely adapted from the bootcamp solution. */

// import androidx.compose.ui.tooling.preview.Preview // <-- Uncomment this line to enable preview
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.ui.navigation.TopBar
import com.github.swent.swisstravel.ui.tripcreation.TripFirstDestinationsTestTags.RETURN_BUTTON

/** Test tags for integration and UI tests */
object SignInScreenTestTags {
  const val APP_LOGO = "appLogo"
  const val EMAIL_FIELD = "emailField"
  const val PASSWORD_FIELD = "passwordField"
  const val LOGIN_BUTTON = "loginButton"
  const val GOOGLE_LOGIN_BUTTON = "googleLoginButton"
  const val LOADING_INDICATOR = "loadingIndicator"
  const val NAME = "name"
}

enum class GoogleButtonType {
  SIGN_UP,
  SIGN_IN
}

/**
 * The Authentication screen.
 *
 * @param authViewModel ViewModel to handle the authentication logic.
 * @param credentialManager The Credential Manager instance to use for authentication.
 * @param onSignedIn Callback to be invoked when the user has successfully signed in.
 */
@Composable
fun SignInScreen(
    authViewModel: SignInViewModel = viewModel(),
    credentialManager: CredentialManager = CredentialManager.create(LocalContext.current),
    onSignedIn: () -> Unit = {},
    onPrevious: () -> Unit = {}
) {
  val context = LocalContext.current
  val uiState by authViewModel.uiState.collectAsState()
  var email by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }

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
      topBar = {
        TopBar(
            onClick = { onPrevious() }, modifier = Modifier.fillMaxWidth().testTag(RETURN_BUTTON))
      },
      modifier = Modifier.fillMaxSize(),
      content = { padding ->
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .padding(padding)
                    .padding(dimensionResource(R.dimen.medium_padding)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
          // App Logo Image
          Image(
              painter = painterResource(id = R.drawable.swisstravel),
              contentDescription = stringResource(R.string.app_logo_desc),
              modifier =
                  Modifier.size(dimensionResource(R.dimen.sign_in_logo_size))
                      .clip(RoundedCornerShape(dimensionResource(R.dimen.main_logo_corner)))
                      .testTag(SignInScreenTestTags.APP_LOGO))

          Spacer(modifier = Modifier.height(dimensionResource(R.dimen.small_spacer)))

          // App Name
          Text(
              modifier = Modifier.testTag(SignInScreenTestTags.NAME),
              text = stringResource(R.string.name_lower),
              style =
                  MaterialTheme.typography.headlineLarge.copy(fontSize = 45.sp, lineHeight = 52.sp),
              fontWeight = FontWeight.Bold,
              textAlign = TextAlign.Center)

          Spacer(modifier = Modifier.height(dimensionResource(R.dimen.mid_spacer)))

          Text(
              text = stringResource(R.string.sign_in_account),
              style = MaterialTheme.typography.titleMedium,
              textAlign = TextAlign.Center,
              modifier = Modifier.fillMaxWidth())

          Spacer(modifier = Modifier.height(dimensionResource(R.dimen.mid_spacer)))

          OutlinedTextField(
              value = email,
              onValueChange = { email = it },
              label = { Text(stringResource(R.string.email)) },
              modifier = Modifier.fillMaxWidth().testTag(SignInScreenTestTags.EMAIL_FIELD))

          Spacer(modifier = Modifier.height(dimensionResource(R.dimen.small_spacer)))

          OutlinedTextField(
              value = password,
              onValueChange = { password = it },
              label = { Text(stringResource(R.string.password)) },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
              visualTransformation = PasswordVisualTransformation(),
              modifier = Modifier.fillMaxWidth().testTag(SignInScreenTestTags.PASSWORD_FIELD))

          Spacer(modifier = Modifier.height(dimensionResource(R.dimen.mid_spacer)))

          // Authenticate With Google Button
          if (uiState.isLoading) {
            CircularProgressIndicator(
                modifier =
                    Modifier.size(dimensionResource(R.dimen.sign_in_loading))
                        .testTag(SignInScreenTestTags.LOADING_INDICATOR))
          } else {
            Button(
                onClick = { authViewModel.signInWithEmailPassword(email, password, context) },
                modifier =
                    Modifier.fillMaxWidth()
                        .height(dimensionResource(R.dimen.medium_button_height))
                        .testTag(SignInScreenTestTags.LOGIN_BUTTON)) {
                  Text(stringResource(R.string.sign_in), fontSize = 16.sp)
                }
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.mid_spacer)))
            Text(
                stringResource(R.string.or),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                color = Color.Gray)
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.mid_spacer)))
            GoogleSignInButton(
                type = GoogleButtonType.SIGN_IN,
                onSignInClick = { authViewModel.signInWithGoogle(context, credentialManager) })
          }
        }
      })
}

/**
 * The google button used to sign-in.
 *
 * @param Callback to be invoked when the user click on the button.
 */
@Composable
fun GoogleSignInButton(type: GoogleButtonType, onSignInClick: () -> Unit) {
  Button(
      onClick = onSignInClick,
      colors =
          ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.surfaceVariant,
              contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
      shape = RoundedCornerShape(50),
      border = BorderStroke(1.dp, Color.LightGray),
      modifier =
          Modifier.fillMaxWidth()
              .height(dimensionResource(R.dimen.medium_button_height))
              .testTag(SignInScreenTestTags.GOOGLE_LOGIN_BUTTON)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center) {
              // Load the 'Google logo' from resources
              Image(
                  painter = painterResource(id = R.drawable.google_logo),
                  contentDescription = stringResource(R.string.app_logo_desc),
                  modifier =
                      Modifier.size(dimensionResource(R.dimen.google_button_logo))
                          .padding(end = dimensionResource(R.dimen.google_button_padding)))

              // Text for the button
              Text(
                  text =
                      stringResource(
                          id =
                              when (type) {
                                GoogleButtonType.SIGN_IN -> R.string.google_sign_in
                                GoogleButtonType.SIGN_UP -> R.string.google_sign_up
                              }),
                  color = Color.Gray,
                  fontSize = 16.sp,
                  fontWeight = FontWeight.Medium)
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
