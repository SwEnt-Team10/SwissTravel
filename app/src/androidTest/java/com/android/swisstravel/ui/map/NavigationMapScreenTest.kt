package com.android.swisstravel.ui.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import com.github.swent.swisstravel.ui.map.NavigationMapScreen
import com.github.swent.swisstravel.ui.map.NavigationMapScreenTestTags
import com.github.swent.swisstravel.ui.map.NavigationMapViewModel
import com.github.swent.swisstravel.ui.navigation.NavigationActions
import com.github.swent.swisstravel.ui.navigation.Screen
import org.junit.Rule
import org.junit.Test

class NavigationMapScreenTest {

  @get:Rule val composeRule = createComposeRule()

  private val MY_TRIPS_ROOT = "myTripsRoot"

  /** Map is shown by default when starting on SelectedTripMap; Exit button is visible. */
  @Test
  fun mapIsVisibleOnEntry() {
    composeRule.setContent {
      val navController = rememberNavController()
      NavHost(navController = navController, startDestination = Screen.SelectedTripMap.route) {
        composable(Screen.SelectedTripMap.route) {
          NavigationMapScreen(navigationActions = NavigationActions(navController))
        }
        // Minimal destination for MyTrips
        composable(Screen.MyTrips.route) { Box(Modifier.fillMaxSize().testTag(MY_TRIPS_ROOT)) }
      }
    }

    composeRule.onNodeWithTag(NavigationMapScreenTestTags.MAP).assertIsDisplayed()
    composeRule.onNodeWithTag(NavigationMapScreenTestTags.EXIT_BUTTON).assertIsDisplayed()
  }

  /** Tapping Exit navigates to MyTrips; the map is no longer present. */
  @Test
  fun exitNavigatesToMyTrips() {
    composeRule.setContent {
      val navController = rememberNavController()
      NavHost(navController = navController, startDestination = Screen.SelectedTripMap.route) {
        composable(Screen.SelectedTripMap.route) {
          NavigationMapScreen(navigationActions = NavigationActions(navController))
        }
        composable(Screen.MyTrips.route) { Box(Modifier.fillMaxSize().testTag(MY_TRIPS_ROOT)) }
      }
    }

    // We are on the map
    composeRule.onNodeWithTag(NavigationMapScreenTestTags.MAP).assertIsDisplayed()

    // Click Exit
    composeRule.onNodeWithTag(NavigationMapScreenTestTags.EXIT_BUTTON).performClick()

    // Landed on MyTrips, map removed
    composeRule.onNodeWithTag(MY_TRIPS_ROOT).assertIsDisplayed()
    composeRule.onNodeWithTag(NavigationMapScreenTestTags.MAP).assertDoesNotExist()
  }

  @Test
  fun isRouteRenderedIsInitiallyFalseAndSetterWorks() {
    val vm = NavigationMapViewModel(ApplicationProvider.getApplicationContext())
    assert(!vm.isRouteRendered.value)
    vm.setRouteRendered(true)
    assert(vm.isRouteRendered.value)
  }
}
