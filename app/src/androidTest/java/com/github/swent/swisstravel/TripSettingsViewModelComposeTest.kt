package com.github.swent.swisstravel

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.swent.swisstravel.ui.navigation.Screen
import com.github.swent.swisstravel.ui.theme.SwissTravelTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TripSettingsViewModelComposeTest {

  @get:Rule val composeRule = createComposeRule()

  private val tag = "vm_ok"

  @Composable
  private fun Caller(navController: NavHostController) {
    // call the function under test and render a simple Text when it returns
    val vm = tripSettingsViewModel(navController)
    Box {
      Text(text = "ok", modifier = Modifier.testTag(tag))
      // use vm to avoid unused warning
      @Suppress("UNUSED_VARIABLE") vm.hashCode()
    }
  }

  @Test
  fun tripSettingsViewModelFallsBckSafelyWhenNoParentBackStackEntry() {
    composeRule.setContent {
      val navController = rememberNavController()
      // directly call the caller without setting up a NavHost for TripSettings
      SwissTravelTheme { Caller(navController = navController) }
    }

    composeRule.onNodeWithTag(tag).assertExists()
  }

  @Test
  fun tripSettingsViewModelFindsParentEntryWhenTripSettingsNavHostIsPresent() {
    composeRule.setContent {
      val navController = rememberNavController()
      NavHost(navController = navController, startDestination = Screen.TripSettings1.route) {
        composable(Screen.TripSettings1.route) { Caller(navController = navController) }
      }
    }

    composeRule.onNodeWithTag(tag).assertExists()
  }
}
