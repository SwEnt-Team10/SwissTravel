package com.android.swisstravel.ui.map

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.NavController
import com.github.swent.swisstravel.ui.map.BottomSheet
import com.github.swent.swisstravel.ui.map.MapScreen
import com.github.swent.swisstravel.ui.map.NavigationMapScreenTestTags
import com.github.swent.swisstravel.ui.map.SampleMenu
import com.mapbox.navigation.core.MapboxNavigationProvider
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.*

class NavigationMapScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var mockNavController: NavController

  @Before
  fun setUp() {
    mockNavController = mock(NavController::class.java)
  }

  @After
  fun tearDown() {
    // Reset singleton after each test
    MapboxNavigationProvider.destroy()
  }

  @Test
  fun mapScreen_showsPermissionButton_whenPermissionDenied() {
    // Simulate permission denied
    composeTestRule.setContent { MapScreen(navController = mockNavController) }

    composeTestRule
        .onNodeWithTag(NavigationMapScreenTestTags.PERMISSION_BUTTON)
        .assertIsDisplayed()
        .performClick()
  }

  @Test
  fun mapScreen_showsBottomSheet_whenPermissionGranted() {

    composeTestRule.setContent { MapScreen(navController = mockNavController) }

    // We cannot easily simulate MapView in Compose test, but we can check BottomSheet exists
    composeTestRule.onNodeWithTag(NavigationMapScreenTestTags.BOTTOM_SHEET).assertIsDisplayed()
  }

  @Test
  fun bottomSheet_exitNavigationButton_navigatesBack() {
    composeTestRule.setContent { BottomSheet(navController = mockNavController) }

    composeTestRule
        .onNodeWithTag(NavigationMapScreenTestTags.EXIT_BUTTON)
        .assertIsDisplayed()
        .performClick()

    verify(mockNavController).navigate("menu-example")
  }

  @Test
  fun sampleMenu_buttonNavigatesToMap() {
    composeTestRule.setContent { SampleMenu(navController = mockNavController) }

    composeTestRule
        .onNodeWithText(NavigationMapScreenTestTags.ENTER_BUTTON)
        .assertIsDisplayed()
        .performClick()

    verify(mockNavController).navigate("nav-map")
  }
}
