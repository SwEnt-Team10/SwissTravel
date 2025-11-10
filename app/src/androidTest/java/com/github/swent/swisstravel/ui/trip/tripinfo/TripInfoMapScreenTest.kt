package com.github.swent.swisstravel.ui.trip.tripinfo

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.swent.swisstravel.ui.trip.tripinfos.TripInfoMapScreen
import com.github.swent.swisstravel.ui.trip.tripinfos.TripInfoMapTestTags
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Test class for TripInfoMapScreen composable. */
@RunWith(AndroidJUnit4::class)
class TripInfoMapScreenTest {

  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun displayTopAppBarBackButtonAndMapContainer() {
    composeRule.setContent { TripInfoMapScreen(tripInfoViewModel = viewModel()) }

    composeRule.onNodeWithTag(TripInfoMapTestTags.TOP_APP_BAR).assertIsDisplayed()
    composeRule.onNodeWithTag(TripInfoMapTestTags.BACK_BUTTON).assertIsDisplayed()
    composeRule.onNodeWithTag(TripInfoMapTestTags.MAP_CONTAINER).assertIsDisplayed()
  }

  @Test
  fun clickOnBackCallsOnBackAfterMapHides() {
    var called = false

    composeRule.setContent {
      TripInfoMapScreen(onBack = { called = true }, tripInfoViewModel = viewModel())
    }

    // Perform click on back button
    composeRule.onNodeWithTag(TripInfoMapTestTags.BACK_BUTTON).performClick()

    // Advance until all recompositions / coroutines are done
    composeRule.waitForIdle()

    // Or, safer with coroutines that use LaunchedEffect:
    composeRule.mainClock.advanceTimeByFrame()

    assertTrue("onBack should have been called after clicking back button", called)
  }
}
