package com.github.swent.swisstravel.ui.authentication

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.ui.theme.SwissTravelTheme

/** Test tags for the SignUpScreen. */
object SignUpScreenTestTags {
  const val FIRST_NAME_FIELD = "firstNameField"
  const val LAST_NAME_FIELD = "lastNameField"
  const val EMAIL_FIELD = "emailField"
  const val PASSWORD_FIELD = "passwordField"
  const val SIGN_UP_BUTTON = "signUpButton"
  const val LOADING_INDICATOR = "loadingIndicator"
}

/**
 * A screen for users to fill in their details to sign up for an account.
 *
 * @param signUpViewModel The ViewModel that manages the state and logic for this screen.
 * @param onSignUpSuccess A callback to be invoked when the user successfully signs up.
 */
@Composable
fun SignUpScreen(
    signUpViewModel: SignUpViewModel = viewModel(),
    credentialManager: CredentialManager = CredentialManager.create(LocalContext.current),
    onSignUpSuccess: () -> Unit = {}
) {
  val context = LocalContext.current
  val uiState by signUpViewModel.uiState.collectAsState()
  var firstName by remember { mutableStateOf("") }
  var lastName by remember { mutableStateOf("") }
  var email by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }

  // Show error messages as Toasts
  LaunchedEffect(uiState.errorMsg) {
    uiState.errorMsg?.let {
      Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
      signUpViewModel.clearErrorMsg()
    }
  }

  // Navigate on successful sign-up
  LaunchedEffect(uiState.user) {
    uiState.user?.let {
      Toast.makeText(context, R.string.signup_success, Toast.LENGTH_SHORT).show()
      onSignUpSuccess()
    }
  }

  Scaffold(modifier = Modifier.fillMaxSize()) { paddingValues ->
    Column(
        modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
      Text(
          text = "Create Your Account",
          style = MaterialTheme.typography.headlineMedium,
          fontWeight = FontWeight.Bold,
          textAlign = TextAlign.Center,
          modifier = Modifier.fillMaxWidth())

      Spacer(modifier = Modifier.height(24.dp))

      // First Name Field
      OutlinedTextField(
          value = firstName,
          onValueChange = { firstName = it },
          label = { Text("First Name") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag(SignUpScreenTestTags.FIRST_NAME_FIELD))

      Spacer(modifier = Modifier.height(16.dp))

      // Last Name Field
      OutlinedTextField(
          value = lastName,
          onValueChange = { lastName = it },
          label = { Text("Last Name") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag(SignUpScreenTestTags.LAST_NAME_FIELD))

      Spacer(modifier = Modifier.height(16.dp))

      // Email Field
      OutlinedTextField(
          value = email,
          onValueChange = { email = it },
          label = { Text("Email") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag(SignUpScreenTestTags.EMAIL_FIELD))

      Spacer(modifier = Modifier.height(16.dp))

      // Password Field
      OutlinedTextField(
          value = password,
          onValueChange = { password = it },
          label = { Text("Password") },
          visualTransformation = PasswordVisualTransformation(),
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag(SignUpScreenTestTags.PASSWORD_FIELD))

      Spacer(modifier = Modifier.height(32.dp))

      if (uiState.isLoading) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp).testTag(SignUpScreenTestTags.LOADING_INDICATOR))
      } else {
        Button(
            onClick = {
              signUpViewModel.signUpWithEmailPassword(
                  email = email.trim(),
                  password = password,
                  firstName = firstName.trim(),
                  lastName = lastName.trim(),
                  context = context)
            },
            modifier =
                Modifier.fillMaxWidth()
                    .height(48.dp)
                    .testTag(SignUpScreenTestTags.SIGN_UP_BUTTON)) {
              Text("Sign Up", fontSize = 16.sp)
            }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "OR",
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
            color = Color.Gray)
        Spacer(modifier = Modifier.height(24.dp))
        GoogleSignInButton(
            type = GoogleButtonType.SIGN_UP,
            onSignInClick = { signUpViewModel.signUpWithGoogle(context, credentialManager) })
      }
    }
  }
}

@Preview(showBackground = true)
@Composable
fun SignUpScreenPreview() {
  SwissTravelTheme { LandingScreen() }
}
