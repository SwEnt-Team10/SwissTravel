package com.github.swent.swisstravel.ui.activities

import android.Manifest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.rule.GrantPermissionRule
import com.github.swent.swisstravel.ui.composable.fakeActivity
import com.github.swent.swisstravel.ui.trip.tripinfo.FakeTripInfoViewModel
import kotlin.test.Test
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule

class LikedActivitiesScreenTest {
  @get:Rule val composeTestRule = createComposeRule()
  @get:Rule
  val grantPermissionRule: GrantPermissionRule =
    GrantPermissionRule.grant(
      Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

  val vm = FakeTripInfoViewModel()
  // fakeActivity() creates a single activity with default values (the function is defined in
  // ActivityInfosTest.kt)
  val fakeActivity = fakeActivity()

  @Before
  fun setup() {
    vm.setActivities(listOf(fakeActivity))
    composeTestRule.setContent { LikedActivitiesScreen(tripInfoViewModel = vm) }
  }

  @Test
  fun likedActivitiesScreenComponentsAreDisplayed() {
    composeTestRule.onNodeWithTag(LikedActivitiesScreenTestTags.SCREEN_TITLE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(LikedActivitiesScreenTestTags.BACK_BUTTON).assertIsDisplayed()
  }

  @Test
  fun emptyTextIsDisplayedIfNoLikedActivities() {
    assertTrue(vm.uiState.value.likedActivities.isEmpty())
    composeTestRule.onNodeWithTag(LikedActivitiesScreenTestTags.EMPTY_TEXT).assertIsDisplayed()
  }

  @Test
  fun listIsDisplayedWhenNotEmpty() {
    assertTrue(vm.uiState.value.likedActivities.isEmpty())
    vm.likeActivity(fakeActivity)
    assertTrue(vm.uiState.value.likedActivities.isNotEmpty())
    composeTestRule
        .onNodeWithTag(LikedActivitiesScreenTestTags.LIKED_ACTIVITIES_LIST)
        .assertIsDisplayed()
  }
}
