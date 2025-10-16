package com.android.swisstravel.ui.map

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.github.swent.swisstravel.ui.map.NavigationMapScreen
import com.github.swent.swisstravel.ui.map.NavigationMapScreenTestTags
import com.github.swent.swisstravel.ui.mytrips.MyTripsScreen
import com.github.swent.swisstravel.ui.navigation.NavigationActions
import com.github.swent.swisstravel.ui.navigation.Screen
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class CurrentTripScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Before
  fun setup() {
    composeTestRule.setContent {
      val navController = rememberNavController()

      NavHost(navController = navController, startDestination = Screen.MyTrips.route) {
        composable(Screen.MyTrips.route) {
          MyTripsScreen(navigationActions = NavigationActions(navController))
        }
        composable(Screen.SelectedTripMap.route) {
          NavigationMapScreen(navigationActions = NavigationActions(navController))
        }
      }
    }
  }

  @Test
  fun canEnterNavigationMapFromCurrentTrip() = runTest {
    composeTestRule
        .onNodeWithTag(NavigationMapScreenTestTags.ENTER_MAP_BUTTON)
        .assertIsDisplayed()
        .performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(NavigationMapScreenTestTags.EXIT_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(NavigationMapScreenTestTags.MAP).assertIsDisplayed()
  }

  @Test
  fun canEnterAndExitNavigationMap() = runTest {
    composeTestRule.onNodeWithTag(NavigationMapScreenTestTags.ENTER_MAP_BUTTON).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(NavigationMapScreenTestTags.MAP).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(NavigationMapScreenTestTags.EXIT_BUTTON)
        .assertIsDisplayed()
        .performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(NavigationMapScreenTestTags.ENTER_MAP_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(NavigationMapScreenTestTags.MAP).assertIsNotDisplayed()
  }

  @Test
  fun routeIsRenderedWhenEnteringMap() = runTest {
    composeTestRule.onNodeWithTag(NavigationMapScreenTestTags.ENTER_MAP_BUTTON).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(NavigationMapScreenTestTags.MAP).assertIsDisplayed()
  }
}
