package com.github.swent.swisstravel.ui.composable

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.model.trip.Trip
import com.github.swent.swisstravel.model.user.Preference
import com.github.swent.swisstravel.model.user.PreferenceCategories
import com.github.swent.swisstravel.ui.mytrips.TripElementTestTags
import com.github.swent.swisstravel.ui.mytrips.TripSortType
import com.github.swent.swisstravel.ui.theme.SwissTravelTheme
import com.github.swent.swisstravel.utils.SwissTravelTest
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test

class ComposableTests : SwissTravelTest() {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun counterTest() {
    val count = mutableStateOf(0)
    composeTestRule.setContent {
      Counter(
          label = "test",
          count = count.value,
          onIncrement = { count.value++ },
          onDecrement = { count.value-- },
          enableButton = count.value > 0)
    }
    composeTestRule.onNodeWithTag("test" + CounterTestTags.COUNTER).assertIsDisplayed()
    composeTestRule.onNodeWithTag(CounterTestTags.DECREMENT).performClick()
    assertEquals(0, count.value)
    composeTestRule.onNodeWithTag(CounterTestTags.INCREMENT).performClick()
    assertEquals(1, count.value)
    composeTestRule.onNodeWithTag(CounterTestTags.COUNT).assertIsDisplayed()
  }

  @Test
  fun dateSelectorTest() {
    val clicked = mutableStateOf(false)
    composeTestRule.setContent {
      DateSelectorRow(label = "test", dateText = "test", onClick = { clicked.value = true })
    }
    composeTestRule.onNodeWithTag("test" + DateSelectorTestTags.DATE_SELECTOR).assertIsDisplayed()
    composeTestRule.onNodeWithTag(DateSelectorTestTags.DATE).performClick()
    assertTrue(clicked.value)
  }

  @Test
  fun preferenceSelectorTest() {
    val selected = mutableStateOf(listOf(Preference.FOODIE, Preference.SPORTS))

    composeTestRule.setContent {
      MaterialTheme {
        PreferenceSelector(
            isChecked = { pref -> selected.value.contains(pref) },
            onCheckedChange = { pref ->
              selected.value =
                  if (selected.value.contains(pref)) selected.value.filter { it != pref }
                  else selected.value + pref
            })
      }
    }

    assertTrue(selected.value.contains(Preference.FOODIE))
    assertTrue(selected.value.contains(Preference.SPORTS))

    composeTestRule
        .onNodeWithTag(PreferenceSelectorTestTags.PREFERENCE_SELECTOR)
        .assertIsDisplayed()
    for (preference in Preference.values().filter { it != Preference.WHEELCHAIR_ACCESSIBLE }) {
      composeTestRule
          .onNodeWithTag(PreferenceSelectorTestTags.getTestTagButton(preference))
          .assertIsDisplayed()
    }
    // Should never have preferences in the default category
    composeTestRule
        .onNodeWithTag(
            PreferenceSelectorTestTags.getTestTagCategory(PreferenceCategories.Category.DEFAULT))
        .assertIsNotDisplayed()

    composeTestRule
        .onNodeWithTag(PreferenceSelectorTestTags.getTestTagButton(Preference.FOODIE))
        .performClick()
    assertFalse(selected.value.contains(Preference.FOODIE))

    composeTestRule
        .onNodeWithTag(PreferenceSelectorTestTags.getTestTagButton(Preference.WELLNESS))
        .performClick()
    assertTrue(selected.value.contains(Preference.WELLNESS))

    composeTestRule
        .onNodeWithTag(PreferenceSelectorTestTags.getTestTagButton(Preference.SPORTS))
        .performClick()
    assertFalse(selected.value.contains(Preference.SPORTS))

    composeTestRule
        .onNodeWithTag(PreferenceSelectorTestTags.getTestTagButton(Preference.FOODIE))
        .performClick()
    assertTrue(selected.value.contains(Preference.FOODIE))
  }

  @Test
  fun preferenceSelectorPreviewContentDisplays() {
    composeTestRule.setContent { PreviewContentPreferenceSelector() }
    composeTestRule
        .onNodeWithTag(PreferenceSelectorTestTags.PREFERENCE_SELECTOR)
        .assertIsDisplayed()
  }

  @Test
  fun sliderTest() {
    val value = mutableStateOf(0)
    composeTestRule.setContent {
      PreferenceSlider(label = "test", value = value.value, onValueChange = { value.value = it })
    }
    composeTestRule.onNodeWithTag("test" + SliderTestTags.SLIDER).assertIsDisplayed()
  }

  @Test
  fun switchTest() {
    val checked = mutableStateOf(false)
    composeTestRule.setContent {
      PreferenceSwitch(
          label = "test", checked = checked.value, onCheckedChange = { checked.value = it })
    }
    composeTestRule.onNodeWithTag("test" + SwitchTestTags.SWITCH_CONTAINER).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SwitchTestTags.SWITCH).performClick()
    assertTrue(checked.value)
  }

  @Test
  fun toggleTest() {
    val value = mutableStateOf(false)
    composeTestRule.setContent {
      PreferenceToggle(label = "test", value = value.value, onValueChange = { value.value = it })
    }
    composeTestRule.onNodeWithTag("test" + ToggleTestTags.TOGGLE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ToggleTestTags.YES).performClick()
    assertTrue(value.value)
    composeTestRule.onNodeWithTag(ToggleTestTags.NO).performClick()
    assertTrue(!value.value)
  }

  @Test
  fun sortMenuTest() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    var selectedSortType: TripSortType? = null

    composeTestRule.setContent { SortMenu(onClickDropDownMenu = { selectedSortType = it }) }

    composeTestRule
        .onNodeWithTag(SortMenuTestTags.SORT_DROPDOWN_MENU)
        .assertIsDisplayed()
        .performClick()
    composeTestRule.onNodeWithText(context.getString(R.string.start_date_desc)).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(SortMenuTestTags.getTestTagSortOption(TripSortType.START_DATE_ASC))
        .performClick()
    assertTrue(selectedSortType == TripSortType.START_DATE_ASC)
  }

  @Test
  fun tripListTest() {
    var clicked: Trip? = null
    var longPressed: Trip? = null

    composeTestRule.setContent {
      TripList(
          trips = tripList,
          onClickTripElement = { clicked = it },
          onLongPress = { longPressed = it })
    }

    composeTestRule.onNodeWithTag(TripListTestTags.TRIP_LIST).assertIsDisplayed()
    composeTestRule.onNodeWithTag(TripElementTestTags.getTestTagForTrip(trip1)).assertIsDisplayed()
    composeTestRule.onNodeWithTag(TripElementTestTags.getTestTagForTrip(trip2)).assertIsDisplayed()

    composeTestRule.onNodeWithTag(TripElementTestTags.getTestTagForTrip(trip1)).performClick()
    assertEquals(trip1, clicked)
    composeTestRule.onNodeWithTag(TripElementTestTags.getTestTagForTrip(trip2)).performTouchInput {
      longClick()
    }
    assertEquals(trip2, longPressed)
  }

  @OptIn(ExperimentalTestApi::class)
  @Test
  fun sortedTripListTest() {
    // State trackers for testing callbacks
    var clickedTrip: Trip? = null
    var longPressedTrip: Trip? = null
    var sortClicked = false

    composeTestRule.setContent {
      SwissTravelTheme {
        SortedTripList(
            title = "My Trips",
            trips = tripList,
            onClickTripElement = { clickedTrip = it },
            onLongPress = { longPressedTrip = it },
            onClickDropDownMenu = { sortClicked = true },
            isSelectionMode = false)
      }
    }

    composeTestRule.checkSortedTripListNotEmptyIsDisplayed()
    composeTestRule.onNodeWithTag(TripElementTestTags.getTestTagForTrip(trip1)).performClick()
    assertEquals(trip1, clickedTrip)
    composeTestRule.onNodeWithTag(TripElementTestTags.getTestTagForTrip(trip2)).performTouchInput {
      longClick()
    }
    assertEquals(trip2, longPressedTrip)
    composeTestRule.onNodeWithTag(SortedTripListTestTags.SORT_DROPDOWN_MENU).performClick()

    composeTestRule
        .onNodeWithTag(
            SortedTripListTestTags.getTestTagSortOption(TripSortType.END_DATE_DESC),
            useUnmergedTree = true)
        .performClick()
    assertTrue(sortClicked)
  }

  @Test
  fun sortedTripListEmptyTest() {
    composeTestRule.setContent {
      SortedTripList(title = "test", trips = emptyList(), emptyListString = "test")
    }

    composeTestRule.onNodeWithTag(SortedTripListTestTags.EMPTY_MESSAGE).assertIsDisplayed()
  }
}
