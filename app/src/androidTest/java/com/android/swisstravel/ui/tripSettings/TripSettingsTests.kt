package com.android.swisstravel.ui.tripSettings

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.github.swent.swisstravel.ui.theme.SwissTravelTheme
import com.github.swent.swisstravel.ui.tripSettings.TripDateScreen
import com.github.swent.swisstravel.ui.tripSettings.TripDateTestTags
import com.github.swent.swisstravel.ui.tripSettings.TripPreferencesScreen
import com.github.swent.swisstravel.ui.tripSettings.TripPreferencesTestTags
import com.github.swent.swisstravel.ui.tripSettings.TripTravelersScreen
import com.github.swent.swisstravel.ui.tripSettings.TripTravelersTestTags
import org.junit.Rule
import org.junit.Test

class TripSettingsTests {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun tripDateScreenTest() {
    composeTestRule.setContent { SwissTravelTheme { TripDateScreen(onNext = {}) } }
    composeTestRule.onNodeWithTag(TripDateTestTags.TRIP_DATE_SCREEN).assertExists()
    composeTestRule.onNodeWithTag(TripDateTestTags.NEXT).performClick()
  }

  @Test
  fun tripPreferencesScreenTest() {
    composeTestRule.setContent { SwissTravelTheme { TripPreferencesScreen(onDone = {}) } }
    composeTestRule.onNodeWithTag(TripPreferencesTestTags.TRIP_PREFERENCES_SCREEN).assertExists()
    composeTestRule.onNodeWithTag(TripPreferencesTestTags.DONE).performClick()
  }

  @Test
  fun tripTravelersScreenTest() {
    composeTestRule.setContent { SwissTravelTheme { TripTravelersScreen(onNext = {}) } }
    composeTestRule.onNodeWithTag(TripTravelersTestTags.TRIP_TRAVELERS_SCREEN).assertExists()
    composeTestRule.onNodeWithTag(TripTravelersTestTags.NEXT).performClick()
  }
}
