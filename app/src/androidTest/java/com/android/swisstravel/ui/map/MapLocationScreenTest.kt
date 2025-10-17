package com.android.swisstravel.ui.map

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.github.swent.swisstravel.ui.map.MapLocationScreen
import com.github.swent.swisstravel.ui.map.MapLocationScreenTags
import com.github.swent.swisstravel.ui.map.MapLocationViewModel
import com.github.swent.swisstravel.ui.theme.SwissTravelTheme
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class MapLocationScreenTest {
  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun showsMapIfPermissionGranted() = runTest {
    val viewModel = MapLocationViewModel()
    viewModel.setPermissionGranted(true)

    composeTestRule.setContent { SwissTravelTheme { MapLocationScreen(viewModel = viewModel) } }
    composeTestRule.onNodeWithTag(MapLocationScreenTags.MAP).assertIsDisplayed()
  }

  @Test
  fun doesNotShowMapIfPermissionNotGranted() = runTest {
    val viewModel = MapLocationViewModel()
    viewModel.setPermissionGranted(false)

    composeTestRule.setContent { SwissTravelTheme { MapLocationScreen(viewModel = viewModel) } }
    composeTestRule.onNodeWithTag(MapLocationScreenTags.MAP).assertDoesNotExist()
  }

  @Test
  fun showsMapWhenPermissionBecomesGranted() = runTest {
    val viewModel = MapLocationViewModel()
    viewModel.setPermissionGranted(false)

    composeTestRule.setContent { SwissTravelTheme { MapLocationScreen(viewModel = viewModel) } }
    composeTestRule.onNodeWithTag(MapLocationScreenTags.MAP).assertDoesNotExist()

    viewModel.setPermissionGranted(true)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(MapLocationScreenTags.MAP).assertIsDisplayed()
  }
}
