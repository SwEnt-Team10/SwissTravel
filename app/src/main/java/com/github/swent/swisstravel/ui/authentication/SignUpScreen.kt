package com.github.swent.swisstravel.ui.authentication

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.ui.theme.SwissTravelTheme

object SignUpScreenTestTags {
    const val EMAIL_FIELD = "emailField"
    const val PASSWORD_FIELD = "passwordField"
    const val SIGN_UP_BUTTON = "signUpButton"
    const val SIGN_IN_BUTTON = "signInButton"
    const val LOADING_INDICATOR = "loadingIndicator"
}

@Composable
fun SignUpScreen(
    signUpViewModel: SignUpViewModel = viewModel(),
    onSignUpSuccess: () -> Unit = {},
    onSignInClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val uiState by signUpViewModel.uiState.collectAsState()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    LaunchedEffect(uiState.errorMsg) {
        uiState.errorMsg?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            signUpViewModel.clearErrorMsg()
        }
    }

    LaunchedEffect(uiState.user) {
        uiState.user?.let {
            Toast.makeText(context, R.string.signup_success, Toast.LENGTH_SHORT).show()
            onSignUpSuccess()
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) {
        paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(id = R.drawable.swisstravel),
                contentDescription = stringResource(R.string.app_logo_desc),
                modifier =
                Modifier.size(240.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .testTag(SignInScreenTestTags.APP_LOGO)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                modifier = Modifier.testTag(SignInScreenTestTags.NAME),
                text = stringResource(R.string.name_lower),
                style =
                MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 45.sp,
                    lineHeight = 52.sp
                ),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Create an account",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth().testTag(SignUpScreenTestTags.EMAIL_FIELD)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth().testTag(SignUpScreenTestTags.PASSWORD_FIELD)
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp).testTag(SignUpScreenTestTags.LOADING_INDICATOR)
                )
            } else {
                Button(
                    onClick = { signUpViewModel.signUpWithEmailPassword(email, password, context) },
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag(SignUpScreenTestTags.SIGN_UP_BUTTON)
                ) {
                    Text("Sign Up", fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = onSignInClick) {
                    Text("Already have an account? Sign In")
                }
            }
        }
    }
}

@Preview
@Composable
fun SignUpScreenPreview(){
    SwissTravelTheme { SignUpScreen() }
}
