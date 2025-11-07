package com.github.swent.swisstravel.ui.mytrips.tripinfo

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.swent.swisstravel.ui.mytrips.tripinfos.TripInfoZoomableMap
import com.github.swent.swisstravel.ui.mytrips.tripinfos.TripInfoZoomableMapTestTags
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Test class for TripInfoZoomableMap composable. */
@RunWith(AndroidJUnit4::class)
class TripInfoZoomableMapTest {

  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun displayMapContainerAndFullscreenButton() {
    composeRule.setContent { TripInfoZoomableMap(onFullscreenClick = {}, emptyList()) }

    composeRule.onNodeWithTag(TripInfoZoomableMapTestTags.MAP_CONTAINER).assertIsDisplayed()
    composeRule.onNodeWithTag(TripInfoZoomableMapTestTags.FULLSCREEN_BUTTON).assertIsDisplayed()
  }

  @Test
  fun clickOnFullscreenCallOnFullscreenClick() {
    var called = false
    composeRule.setContent {
      TripInfoZoomableMap(onFullscreenClick = { called = true }, emptyList())
    }

    composeRule.onNodeWithTag(TripInfoZoomableMapTestTags.FULLSCREEN_BUTTON).performClick()
    assertTrue("onFullscreenClick should have been called after clicking fullscreen button", called)
  }
}
