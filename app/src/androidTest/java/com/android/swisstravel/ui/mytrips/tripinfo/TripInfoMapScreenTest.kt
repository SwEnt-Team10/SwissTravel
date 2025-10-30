package com.android.swisstravel.ui.mytrips.tripinfo

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.swent.swisstravel.ui.mytrips.tripinfos.TripInfoMapScreen
import com.github.swent.swisstravel.ui.mytrips.tripinfos.TripInfoMapTestTags
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
    composeRule.setContent { TripInfoMapScreen() }

    composeRule.onNodeWithTag(TripInfoMapTestTags.TOP_APP_BAR).assertIsDisplayed()
    composeRule.onNodeWithTag(TripInfoMapTestTags.BACK_BUTTON).assertIsDisplayed()
    composeRule.onNodeWithTag(TripInfoMapTestTags.MAP_CONTAINER).assertIsDisplayed()
  }

  @Test
  fun clickOnBackCallOnBack() {
    var called = false
    composeRule.setContent { TripInfoMapScreen(onBack = { called = true }) }

    composeRule.onNodeWithTag(TripInfoMapTestTags.BACK_BUTTON).performClick()
    assertTrue("onBack should have been called after clicking back button", called)
  }
}
