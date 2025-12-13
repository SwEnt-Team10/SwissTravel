package com.github.swent.swisstravel.ui.activities

import android.Manifest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.rule.GrantPermissionRule
import com.github.swent.swisstravel.model.trip.TripsRepository
import com.github.swent.swisstravel.ui.composable.fakeActivity
import com.github.swent.swisstravel.ui.trip.tripinfo.FakeTripInfoViewModel
import com.github.swent.swisstravel.ui.tripcreation.TripCreationTests
import com.github.swent.swisstravel.utils.SwissTravelTest
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule

@ExperimentalCoroutinesApi
class SwipeActivitiesScreenTest : SwissTravelTest() {

  @get:Rule val composeTestRule = createComposeRule()
  @get:Rule
  val grantPermissionRule: GrantPermissionRule =
      GrantPermissionRule.grant(
          Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

  val vm = FakeTripInfoViewModel()

  override fun createInitializedRepository(): TripsRepository {
    // update this to a different list of trips if needed
    return TripCreationTests.FakeTripsRepository(listOf())
  }

  @Before
  fun setup() {
    // fakeActivity() creates a single activity with default values (the function is defined in
    // ActivityInfosTest.kt)
    vm.setActivitiesQueue(activitiesQueue = ArrayDeque(listOf(fakeActivity())))
    composeTestRule.setContent { SwipeActivitiesScreen(tripInfoVM = vm) }
  }

  @Test
  fun swipeActivitiesScreenComponentsAreDisplayed() {
    assertTrue(vm.uiState.value.activitiesQueue.isNotEmpty())
    composeTestRule.checkSwipeActivityScreenIsDisplayed()
  }

  @Test
  fun likeButtonWorks() {
    assertTrue(vm.uiState.value.likedActivities.isEmpty())
    composeTestRule
        .onNodeWithTag(SwipeActivitiesScreenTestTags.LIKE_BUTTON)
        .assertIsDisplayed()
        .performClick()
    // wait for the animation to finish and the state to update
    composeTestRule.mainClock.advanceTimeBy(500)
    composeTestRule.waitForIdle()
    assertTrue(vm.uiState.value.likedActivities.isNotEmpty())
  }

  @Test
  fun dislikeButtonWorks() {
    assertTrue(vm.uiState.value.likedActivities.isEmpty())
    composeTestRule
        .onNodeWithTag(SwipeActivitiesScreenTestTags.DISLIKE_BUTTON)
        .assertIsDisplayed()
        .performClick()
    // wait for the animation to finish and the state to update
    composeTestRule.mainClock.advanceTimeBy(500)
    composeTestRule.waitForIdle()
    assertTrue(vm.uiState.value.likedActivities.isEmpty())
  }

  @Test
  fun backButtonIsDisplayedWhenNoActivitiesProposed() {
    assertTrue(vm.uiState.value.likedActivities.isEmpty())
    assertTrue(vm.uiState.value.activitiesQueue.size == 1)

    // back button should not be displayed since there is still an activity proposed
    composeTestRule.onNodeWithTag(SwipeActivitiesScreenTestTags.BACK_BUTTON).assertIsNotDisplayed()

    // like the activity
    composeTestRule
        .onNodeWithTag(SwipeActivitiesScreenTestTags.LIKE_BUTTON)
        .assertIsDisplayed()
        .performClick()

    // wait for the animation to finish and the state to update
    composeTestRule.mainClock.advanceTimeBy(500)
    composeTestRule.waitForIdle()

    // there should now be the back button
    composeTestRule.onNodeWithTag(SwipeActivitiesScreenTestTags.BACK_BUTTON).assertIsDisplayed()
  }
}
