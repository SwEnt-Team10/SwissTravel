package com.github.swent.swisstravel.ui.authentication

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
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

/**
 * Defines test tags for UI elements in the sign-up flow for testing purposes.
 */
object SignUpScreenTestTags {
    const val FIRST_NAME_FIELD = "firstNameField"
    const val LAST_NAME_FIELD = "lastNameField"
    const val EMAIL_FIELD = "emailField"
    const val PASSWORD_FIELD = "passwordField"
    const val SIGN_UP_BUTTON = "signUpButton"
    const val LOADING_INDICATOR = "loadingIndicator"
    const val PENDING_VERIFICATION_SCREEN = "pendingVerificationScreen"
    const val DONE_BUTTON = "doneButton"
    const val RESEND_EMAIL_BUTTON = "resendEmailButton"
}

/**
 * The main composable for the sign-up flow.
 *
 * This screen acts as a controller that displays either the sign-up form or the
 * pending verification screen, based on the `signUpStage` from the [SignUpViewModel].
 * It handles UI state changes, displays error messages, and triggers navigation
 * upon successful email verification.
 *
 * @param signUpViewModel The ViewModel that manages the state and logic for this screen.
 * @param credentialManager The manager for handling credentials, used for Google Sign-In.
 * @param onSignUpSuccess A callback invoked when the entire sign-up and verification process is complete.
 * @param onPrevious A callback to navigate to the previous screen.
 */
@Composable
fun SignUpScreen(
    signUpViewModel: SignUpViewModel = viewModel(),
    credentialManager: CredentialManager = CredentialManager.create(LocalContext.current),
    onSignUpSuccess: () -> Unit = {},
    onPrevious: () -> Unit = {}
) {
    val context = LocalContext.current
    val uiState by signUpViewModel.uiState.collectAsState()

    // Show error messages as Toasts
    LaunchedEffect(uiState.errorMsg) {
        uiState.errorMsg?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            signUpViewModel.clearErrorMsg()
        }
    }

    // Navigate on successful email verification
    LaunchedEffect(uiState.isEmailVerified) {
        if (uiState.isEmailVerified) {
            Toast.makeText(context, R.string.signup_success, Toast.LENGTH_SHORT).show()
            onSignUpSuccess()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            when (uiState.signUpStage) {
                SignUpStage.FILLING_FORM -> {
                    TopBar(
                        onClick = { onPrevious() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(RETURN_BUTTON))
                }
                SignUpStage.PENDING_VERIFICATION -> {} // No top bar in verification stage
            }
        }) { paddingValues ->
        when (uiState.signUpStage) {
            SignUpStage.FILLING_FORM -> {
                SignUpForm(
                    modifier = Modifier.padding(paddingValues),
                    uiState = uiState,
                    onSignUp = { email, password, firstName, lastName ->
                        signUpViewModel.signUpWithEmailPassword(
                            email = email,
                            password = password,
                            firstName = firstName,
                            lastName = lastName,
                            context = context)
                    },
                    onGoogleSignUp = { signUpViewModel.signInWithGoogle(context, credentialManager) },
                )
            }
            SignUpStage.PENDING_VERIFICATION -> {
                PendingVerificationScreen(
                    modifier = Modifier.padding(paddingValues),
                    uiState = uiState,
                    onDoneClick = {
                        signUpViewModel.checkVerificationStatus()
                    },
                    onResendClick = { signUpViewModel.resendVerificationEmail() })
            }
        }
    }
}

/**
 * A private composable that displays the sign-up form with input fields.
 *
 * @param modifier The modifier to be applied to the composable.
 * @param uiState The current authentication UI state, used to show a loading indicator.
 * @param onSignUp A callback invoked when the user clicks the sign-up button, providing the form data.
 * @param onGoogleSignUp A callback invoked when the user clicks the Google Sign-Up button.
 */
@Composable
private fun SignUpForm(
    modifier: Modifier = Modifier,
    uiState: AuthUiState,
    onSignUp: (String, String, String, String) -> Unit,
    onGoogleSignUp: () -> Unit
) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.create_account),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth())

        Spacer(modifier = Modifier.height(24.dp))

        // First Name Field
        OutlinedTextField(
            value = firstName,
            onValueChange = { firstName = it },
            label = { Text(stringResource(R.string.first_name)) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(SignUpScreenTestTags.FIRST_NAME_FIELD))

        Spacer(modifier = Modifier.height(16.dp))

        // Last Name Field
        OutlinedTextField(
            value = lastName,
            onValueChange = { lastName = it },
            label = { Text(stringResource(R.string.last_name)) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(SignUpScreenTestTags.LAST_NAME_FIELD))

        Spacer(modifier = Modifier.height(16.dp))

        // Email Field
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text(stringResource(R.string.email)) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(SignUpScreenTestTags.EMAIL_FIELD))

        Spacer(modifier = Modifier.height(16.dp))

        // Password Field
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(stringResource(R.string.password)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(SignUpScreenTestTags.PASSWORD_FIELD))

        Spacer(modifier = Modifier.height(32.dp))

        if (uiState.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(48.dp)
                    .testTag(SignUpScreenTestTags.LOADING_INDICATOR))
        } else {
            Button(
                onClick = { onSignUp(email.trim(), password, firstName.trim(), lastName.trim()) },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag(SignUpScreenTestTags.SIGN_UP_BUTTON)) {
                Text(stringResource(R.string.sign_up_landing), fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                stringResource(R.string.or),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                color = Color.Gray)
            Spacer(modifier = Modifier.height(24.dp))
            GoogleSignInButton(type = GoogleButtonType.SIGN_UP, onSignInClick = { onGoogleSignUp() })
        }
    }
}

/**
 * A composable screen shown after the user has signed up, prompting them to verify their email.
 *
 * @param modifier The modifier to be applied to the composable.
 * @param uiState The current authentication UI state, used to show a loading indicator.
 * @param onDoneClick A callback invoked when the user clicks the "Done" button, signaling they have verified their email.
 * @param onResendClick A callback invoked when the user requests a new verification email.
 */
@Composable
fun PendingVerificationScreen(
    modifier: Modifier = Modifier,
    uiState: AuthUiState,
    onDoneClick: () -> Unit,
    onResendClick: () -> Unit
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp)
                .testTag(SignUpScreenTestTags.PENDING_VERIFICATION_SCREEN),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        Text(
            text = "Verify Your Email",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center)

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text =
                "We've sent a verification link to your email address. Please check your inbox and click the link to complete your sign up.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge)

        Spacer(modifier = Modifier.height(32.dp))

        if (uiState.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(48.dp)
                    .testTag(SignUpScreenTestTags.LOADING_INDICATOR))
        } else {
            Button(
                onClick = onDoneClick,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag(SignUpScreenTestTags.DONE_BUTTON)) {
                Text(stringResource(R.string.done), fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                onClick = onResendClick,
                modifier = Modifier.testTag(SignUpScreenTestTags.RESEND_EMAIL_BUTTON)) {
                Text(stringResource(R.string.no_email_received))
            }
        }
    }
}


