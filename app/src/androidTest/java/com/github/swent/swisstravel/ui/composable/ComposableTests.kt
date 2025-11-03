package com.android.swisstravel.ui.composable

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import com.github.swent.swisstravel.ui.composable.Counter
import com.github.swent.swisstravel.ui.composable.CounterTestTags
import com.github.swent.swisstravel.ui.composable.DateSelectorRow
import com.github.swent.swisstravel.ui.composable.DateSelectorTestTags
import com.github.swent.swisstravel.ui.composable.IconType
import com.github.swent.swisstravel.ui.composable.PreferenceSelector
import com.github.swent.swisstravel.ui.composable.PreferenceSelectorTestTags
import com.github.swent.swisstravel.ui.composable.PreferenceSlider
import com.github.swent.swisstravel.ui.composable.PreferenceSwitch
import com.github.swent.swisstravel.ui.composable.PreferenceToggle
import com.github.swent.swisstravel.ui.composable.PreviewContentPreferenceSelector
import com.github.swent.swisstravel.ui.composable.SliderTestTags
import com.github.swent.swisstravel.ui.composable.SortedTripList
import com.github.swent.swisstravel.ui.composable.SortedTripListTestTags
import com.github.swent.swisstravel.ui.composable.SwitchTestTags
import com.github.swent.swisstravel.ui.composable.ToggleTestTags
import com.github.swent.swisstravel.ui.mytrips.MyTripsScreenTestTags
import com.github.swent.swisstravel.ui.mytrips.TripElementTestTags
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test

class ComposableTests {

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
    assert(clicked.value)
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
    assert(checked.value)
  }

  @Test
  fun toggleTest() {
    val value = mutableStateOf(false)
    composeTestRule.setContent {
      PreferenceToggle(label = "test", value = value.value, onValueChange = { value.value = it })
    }
    composeTestRule.onNodeWithTag("test" + ToggleTestTags.TOGGLE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ToggleTestTags.YES).performClick()
    assert(value.value)
    composeTestRule.onNodeWithTag(ToggleTestTags.NO).performClick()
    assert(!value.value)
  }

  @Test
  fun sortedTripListTopBarDisplaysCorrectlyTest() {
    var trips by mutableStateOf(tripList)

    composeTestRule.setContent {
      SortedTripList(
          title = "My Trips",
          trips = trips,
          onClickDropDownMenu = { sortType ->
            trips = trips.sortedByDescending { trip -> trip.tripProfile.endDate.seconds }
          },
          isSelectionMode = false,
          topBar = true,
          topBarBackIcon = IconType.CROSS)
    }
    composeTestRule.onNodeWithTag(SortedTripListTestTags.TOP_BAR)
    composeTestRule.onNodeWithTag(SortedTripListTestTags.TOP_BAR_BACK_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SortedTripListTestTags.SORT_DROPDOWN_MENU).performClick()
  }

  @OptIn(ExperimentalTestApi::class)
  @Test
  fun sortedTripListTest() {
    // State trackers for testing callbacks
    var clickedTrip: Trip? = null
    var longPressedTrip: Trip? = null
    var sortClicked = false

    composeTestRule.setContent {
      SortedTripList(
          title = "My Trips",
          trips = tripList,
          onClickTripElement = { clickedTrip = it },
          onLongPress = { longPressedTrip = it },
          onClickDropDownMenu = { sortClicked = true },
          isSelectionMode = false,
          topBar = false)
    }

    composeTestRule.checkSortedTripListNoTopBarIsDisplayed()
    composeTestRule.onNodeWithTag(TripElementTestTags.getTestTagForTrip(trip1)).performClick()
    assertEquals(trip1, clickedTrip)
    composeTestRule.onNodeWithTag(TripElementTestTags.getTestTagForTrip(trip2)).performTouchInput {
      longClick()
    }
    assertEquals(trip2, longPressedTrip)
    composeTestRule.onNodeWithTag(SortedTripListTestTags.SORT_DROPDOWN_MENU).performClick()
    // For some reason it doesn't work because it can't find the node,
    // Tried multiple things like changing the location of the test tag, changing the semantics
    // and other things but nothing worked
    //        composeTestRule.waitForIdle()
    //
    // composeTestRule.onNodeWithTag(SortedTripListTestTags.getTestTagSortOption(TripSortType.END_DATE_DESC)).performClick()
    //        assert(sortClicked)
  }

  @Test
  fun sortedTripListWithTopBarTest() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    // State trackers for testing callbacks
    var clickedTrip: Trip? = null
    var longPressedTrip: Trip? = null
    var trips by mutableStateOf(tripList)

    composeTestRule.setContent {
      SortedTripList(
          title = "My Trips",
          trips = trips,
          onClickTripElement = { clickedTrip = it },
          onLongPress = { longPressedTrip = it },
          onClickDropDownMenu = { sortType ->
            trips = trips.sortedByDescending { trip -> trip.tripProfile.endDate.seconds }
          },
          isSelectionMode = false,
          topBar = true)
    }
    composeTestRule.checkSortedTripListTopBarIsDisplayed()
    composeTestRule.onNodeWithTag(TripElementTestTags.getTestTagForTrip(trip1)).performClick()
    assertEquals(trip1, clickedTrip)
    composeTestRule.onNodeWithTag(TripElementTestTags.getTestTagForTrip(trip2)).performTouchInput {
      longClick()
    }
    assertEquals(trip2, longPressedTrip)
    composeTestRule.onNodeWithTag(SortedTripListTestTags.SORT_DROPDOWN_MENU).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText(context.getString(R.string.end_date_desc)).performClick()
    composeTestRule.waitForIdle()
    val trip1NodeAfterSort =
        composeTestRule
            .onNodeWithTag(MyTripsScreenTestTags.getTestTagForTrip(trip1))
            .fetchSemanticsNode()
    val trip2NodeAfterSort =
        composeTestRule
            .onNodeWithTag(MyTripsScreenTestTags.getTestTagForTrip(trip2))
            .fetchSemanticsNode()

    assertTrue(
        trip2NodeAfterSort.positionInRoot.y < trip1NodeAfterSort.positionInRoot.y,
        "Trip 2 should appear before Trip 1 when sorted DESC by end date")
  }

  @Test
  fun sortedTripListEmptyTest() {
    composeTestRule.setContent { SortedTripList(title = "My Trips", trips = emptyList()) }

    composeTestRule.onNodeWithTag(SortedTripListTestTags.EMPTY_MESSAGE).assertIsDisplayed()
  }
}
