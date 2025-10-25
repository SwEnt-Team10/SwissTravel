package com.android.swisstravel.ui.tripSettings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.github.swent.swisstravel.model.user.Preference
import com.github.swent.swisstravel.ui.composable.PreferenceSelectorTestTags
import com.github.swent.swisstravel.ui.composable.ToggleTestTags
import com.github.swent.swisstravel.ui.theme.SwissTravelTheme
import com.github.swent.swisstravel.ui.tripsettings.TripDateScreen
import com.github.swent.swisstravel.ui.tripsettings.TripDateTestTags
import com.github.swent.swisstravel.ui.tripsettings.TripPreferencesScreen
import com.github.swent.swisstravel.ui.tripsettings.TripPreferencesTestTags
import com.github.swent.swisstravel.ui.tripsettings.TripTravelersScreen
import com.github.swent.swisstravel.ui.tripsettings.TripTravelersTestTags
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
    composeTestRule
        .onNodeWithTag(TripPreferencesTestTags.TRIP_PREFERENCES_TITLE)
        .assertIsDisplayed()
    /* Preference Selector */
    composeTestRule
        .onNodeWithTag(PreferenceSelectorTestTags.PREFERENCE_SELECTOR)
        .assertIsDisplayed()
    val preferences = Preference.values().filter { it != Preference.WHEELCHAIR_ACCESSIBLE }
    for (preference in preferences) {
      composeTestRule
          .onNodeWithTag(PreferenceSelectorTestTags.getTestTagButton(preference))
          .assertIsDisplayed()
    }
    /* Preference toggle */
    composeTestRule.onNodeWithTag(ToggleTestTags.NO).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ToggleTestTags.YES).assertIsDisplayed()
    /* Done button */
    composeTestRule.onNodeWithTag(TripPreferencesTestTags.DONE).assertIsDisplayed()
  }

  @Test
  fun tripTravelersScreenTest() {
    composeTestRule.setContent { SwissTravelTheme { TripTravelersScreen(onNext = {}) } }
    composeTestRule.onNodeWithTag(TripTravelersTestTags.TRIP_TRAVELERS_SCREEN).assertExists()
    composeTestRule.onNodeWithTag(TripTravelersTestTags.NEXT).performClick()
  }
}
