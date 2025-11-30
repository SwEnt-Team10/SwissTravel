package com.github.swent.swisstravel.ui.activities

import android.Manifest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.rule.GrantPermissionRule
import com.github.swent.swisstravel.ui.composable.fakeActivity
import com.github.swent.swisstravel.ui.trip.tripinfo.FakeTripInfoViewModel
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule

@ExperimentalCoroutinesApi
class SwipeActivitiesScreenTest {

  @get:Rule val composeTestRule = createComposeRule()
    @get:Rule
    val grantPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

  val vm = FakeTripInfoViewModel()

  @Before
  fun setup() {
    // fakeActivity() creates a single activity with default values (the function is defined in
    // ActivityInfosTest.kt)
    vm.setActivities(listOf(fakeActivity()))
    composeTestRule.setContent { SwipeActivitiesScreen(tripInfoViewModel = vm) }
  }

  @Test
  fun swipeActivitiesScreenComponentsAreDisplayed() {
    assertFalse(vm.uiState.value.activities.isEmpty())
    composeTestRule
        .onNodeWithTag(SwipeActivitiesScreenTestTags.SWIPE_ACTIVITIES_SCREEN)
        .assertIsDisplayed()
    composeTestRule.onNodeWithTag(SwipeActivitiesScreenTestTags.LIKE_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SwipeActivitiesScreenTestTags.DISLIKE_BUTTON).assertIsDisplayed()
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
}
