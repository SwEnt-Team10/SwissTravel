package com.github.swent.swisstravel.ui.authentication

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.ui.theme.SwissTravelTheme

/** Test tags for the LandingScreen. */
object LandingScreenTestTags {
  const val APP_LOGO = "appLogo"
  const val APP_NAME = "appName"
  const val SIGN_UP_BUTTON = "signUpButton"
  const val SIGN_IN_BUTTON = "signInButton"
}

/**
 * The initial screen of the application, presenting the user with sign-in and sign-up options.
 *
 * @param onSignInClick Callback invoked when the user clicks the "Sign In" button.
 * @param onSignUpClick Callback invoked when the user clicks the "Sign Up" button.
 */
@Composable
fun LandingScreen(onSignInClick: () -> Unit = {}, onSignUpClick: () -> Unit = {}) {
  Scaffold(modifier = Modifier.fillMaxSize()) { paddingValues ->
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
                  .testTag(LandingScreenTestTags.APP_LOGO))

      Spacer(modifier = Modifier.height(16.dp))

      Text(
          modifier = Modifier.testTag(LandingScreenTestTags.APP_NAME),
          text = stringResource(R.string.name_lower),
          style = MaterialTheme.typography.headlineLarge.copy(fontSize = 45.sp, lineHeight = 52.sp),
          fontWeight = FontWeight.Bold,
          textAlign = TextAlign.Center)

      Spacer(modifier = Modifier.height(60.dp))

      // Sign Up button
      Button(
          onClick = onSignUpClick,
          modifier =
              Modifier.fillMaxWidth().height(48.dp).testTag(LandingScreenTestTags.SIGN_UP_BUTTON)) {
            Text(stringResource(R.string.sign_up_landing), fontSize = 16.sp)
          }

      Spacer(modifier = Modifier.height(16.dp))

      // Sign In button
      TextButton(
          onClick = onSignInClick,
          modifier = Modifier.testTag(LandingScreenTestTags.SIGN_IN_BUTTON)) {
            Text(stringResource(R.string.sign_in_landing))
          }
    }
  }
}

@Preview(showBackground = true)
@Composable
fun LandingScreenPreview() {
  SwissTravelTheme { LandingScreen() }
}
