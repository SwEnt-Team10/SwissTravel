package com.github.swent.swisstravel.ui.activities

import android.Manifest
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.github.swent.swisstravel.MainActivity
import com.github.swent.swisstravel.model.trip.TripsRepository
import com.github.swent.swisstravel.ui.composable.fakeActivity
import com.github.swent.swisstravel.ui.trip.tripinfo.FakeTripInfoViewModel
import com.github.swent.swisstravel.ui.tripcreation.TripCreationTests
import com.github.swent.swisstravel.utils.SwissTravelTest
import kotlin.test.Test
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule

class LikedActivitiesScreenTest : SwissTravelTest() {
  @get:Rule val composeTestRule = createComposeRule()
  @get:Rule
  val grantPermissionRule: GrantPermissionRule =
      GrantPermissionRule.grant(
          Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

  val vm = FakeTripInfoViewModel()
  // fakeActivity() creates a single activity with default values (the function is defined in
  // ActivityInfosTest.kt)
  val fakeActivity = fakeActivity()

  override fun createInitializedRepository(): TripsRepository {
    // update this to a different list of trips if needed
    return TripCreationTests.FakeTripsRepository(listOf())
  }

  @Before
  fun setup() {
    val context = InstrumentationRegistry
      .getInstrumentation()
      .targetContext
    vm.setActivities(listOf(fakeActivity))
    composeTestRule.setContent { LikedActivitiesScreen(tripInfoVM = vm, onUnlike = {vm.unlikeSelectedActivities()}, onSchedule = {vm.scheduleSelectedActivities(
context    )}) }
  }

  @Test
  fun likedActivitiesScreenComponentsAreDisplayed() {
    composeTestRule.checkLikedActivitiesScreenIsDisplayed()
  }

  @Test
  fun emptyTextIsDisplayedIfNoLikedActivities() {
    assertTrue(vm.uiState.value.likedActivities.isEmpty())
    composeTestRule.onNodeWithTag(LikedActivitiesScreenTestTags.EMPTY_TEXT).assertIsDisplayed()
  }

  @Test
  fun listIsDisplayedWhenNotEmpty() {
    assertTrue(vm.uiState.value.likedActivities.isEmpty())
    vm.likeActivities(listOf(fakeActivity))
    assertTrue(vm.uiState.value.likedActivities.isNotEmpty())
    composeTestRule
        .onNodeWithTag(LikedActivitiesScreenTestTags.LIKED_ACTIVITIES_LIST)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(LikedActivitiesScreenTestTags.SELECT_LIKED_ACTIVITY + "_${fakeActivity.getName()}")
        .assertIsDisplayed()
  }

  @Test
  fun selectLikedActivityWorks() {
    likeAndSelectFakeActivity()
  }

  @Test
  fun unlikeButtonWorks() {
    likeAndSelectFakeActivity()

    // click unlike button
    composeTestRule
        .onNodeWithTag(LikedActivitiesScreenTestTags.UNLIKE_BUTTON)
        .assertIsDisplayed()
        .performClick()

    // wait for the state to update
    composeTestRule.mainClock.advanceTimeBy(100)
    assertTrue(vm.uiState.value.likedActivities.isEmpty())
    assertTrue(vm.uiState.value.selectedLikedActivities.isEmpty())
  }

  @Test
  fun scheduleButtonWorks() {

    likeAndSelectFakeActivity()

    // click schedule button
    composeTestRule
        .onNodeWithTag(LikedActivitiesScreenTestTags.SCHEDULE_BUTTON)
        .assertIsDisplayed()
        .performClick()

    // Since the scheduling functionality is not implemented yet, we just check that no changes
    // happened to the liked or selected activities
    composeTestRule.mainClock.advanceTimeBy(100)
    // even after the implementation of the scheduling logic, it should not remove the liked activities
    assertTrue(vm.uiState.value.likedActivities.isNotEmpty())
  }

  // helper to like and select an activity
  fun likeAndSelectFakeActivity() {

    // like activity
    assertTrue(vm.uiState.value.likedActivities.isEmpty())
    vm.likeActivities(listOf(fakeActivity))
    assertTrue(vm.uiState.value.likedActivities.contains(fakeActivity))

    // select activity
    assertTrue(vm.uiState.value.selectedLikedActivities.isEmpty())
    composeTestRule
        .onNodeWithTag(LikedActivitiesScreenTestTags.SELECT_LIKED_ACTIVITY + "_${fakeActivity.getName()}")
        .assertIsDisplayed()
        .performClick()
    composeTestRule.mainClock.advanceTimeBy(1000)
    assertTrue(vm.uiState.value.selectedLikedActivities.contains(fakeActivity))
  }
}
