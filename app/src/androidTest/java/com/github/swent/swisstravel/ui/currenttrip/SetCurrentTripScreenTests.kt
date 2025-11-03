package com.github.swent.swisstravel.ui.currenttrip

import android.content.Context
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.core.app.ApplicationProvider
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.model.trip.Trip
import com.github.swent.swisstravel.ui.composable.SortedTripListTestTags
import com.github.swent.swisstravel.ui.mytrips.MyTripsScreenTestTags
import com.github.swent.swisstravel.ui.mytrips.TripElementTestTags
import com.github.swent.swisstravel.ui.theme.SwissTravelTheme
import com.github.swent.swisstravel.utils.SwissTravelTest
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test

class SetCurrentTripScreenTests : SwissTravelTest() {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun checkScreenDisplaysCorrectly() {
    val title = "Set Current Trip"
    composeTestRule.setContent {
      SwissTravelTheme { SetCurrentTripScreen(title = title, trips = tripList) }
    }
    composeTestRule.checkSetCurrentTripIsDisplayed()
    composeTestRule.onNodeWithText(title).assertIsDisplayed()
  }

  @Test
  fun checkScreenFunctionality() {
    var clickedTrip: Trip? = null
    var longPress: Trip? = null
    var backClicked = false
    composeTestRule.setContent {
      SwissTravelTheme {
        SetCurrentTripScreen(
            trips = tripList,
            onClickTripElement = { trip -> clickedTrip = trip },
            onLongPress = { trip -> longPress = trip },
            onBackClick = { backClicked = true },
        )
      }
    }

    composeTestRule
        .onNodeWithTag(TripElementTestTags.getTestTagForTrip(trip2))
        .performSemanticsAction(SemanticsActions.OnLongClick)
    assert(longPress == trip2)
    composeTestRule.onNodeWithTag(TripElementTestTags.getTestTagForTrip(trip1)).performClick()
    assert(clickedTrip == trip1)
    composeTestRule.onNodeWithTag(SetCurrentTripScreenTestTags.TOP_BAR_BACK_BUTTON).performClick()
    assert(backClicked)
  }

  @Test
  fun checkSetCurrentTripSorting() {
    val context = ApplicationProvider.getApplicationContext<Context>()

    composeTestRule.setContent {
      SwissTravelTheme {
        SetCurrentTripScreen(
            title = "Set Current Trip",
            trips = tripList,
        )
      }
    }

    composeTestRule.onNodeWithTag(SortedTripListTestTags.SORT_DROPDOWN_MENU).performClick()
    composeTestRule.onNodeWithText(context.getString(R.string.start_date_desc)).performClick()

    composeTestRule.waitForIdle()

    // New order (START_DATE_DESC expected)
    val trip1NodeAfterSort =
        composeTestRule
            .onNodeWithTag(MyTripsScreenTestTags.getTestTagForTrip(trip1))
            .fetchSemanticsNode()
    val trip2NodeAfterSort =
        composeTestRule
            .onNodeWithTag(MyTripsScreenTestTags.getTestTagForTrip(trip2))
            .fetchSemanticsNode()
    assertTrue(
        trip1NodeAfterSort.positionInRoot.y < trip2NodeAfterSort.positionInRoot.y,
        "Trip 1 should appear before Trip 2 when sorted DESC by start date")
  }

  @Test
  fun checkSetCurrentTripCanHaveASelectedTrip() {
    val trips = mutableListOf(trip1, trip2)
    var isSelectionMode = false
    val selectedTrips = mutableListOf<Trip>()
    composeTestRule.setContent {
      SwissTravelTheme {
        SetCurrentTripScreen(
            title = "Set Current Trip",
            trips = trips,
            onClickTripElement = { trip ->
              isSelectionMode = true
              selectedTrips.add(trip!!)
            },
            isSelected = { trip -> selectedTrips.contains(trip) },
            isSelectionMode = isSelectionMode,
        )
      }
    }

    composeTestRule.onNodeWithTag(TripElementTestTags.getTestTagForTrip(trip1)).performClick()
    assert(selectedTrips.contains(trip1))
    assert(isSelectionMode)
  }
}
