package com.android.swisstravel.ui.composable

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.github.swent.swisstravel.ui.authentication.GoogleSignInButton
import com.github.swent.swisstravel.ui.theme.SwissTravelTheme
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito

class GoogleSignInButtonTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun googleSignInButton_displaysCorrectly_and_isClickable() {
    // Given
    val onSignInClick: () -> Unit = Mockito.mock(Function0::class.java) as () -> Unit

    // When
    composeTestRule.setContent {
      SwissTravelTheme { GoogleSignInButton(onSignInClick = onSignInClick) }
    }

    // Then
    composeTestRule.onNodeWithText("Sign in with Google").assertExists()
    composeTestRule.onNodeWithText("Sign in with Google").performClick()
    Mockito.verify(onSignInClick).invoke()
  }
}
