package com.github.swent.swisstravel.ui.composable

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.model.trip.Coordinate
import com.github.swent.swisstravel.model.trip.Location
import com.github.swent.swisstravel.model.trip.activity.Activity
import com.google.firebase.Timestamp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ActivityInfosTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun title_is_displayed() {
    val activity = fakeActivity(name = "Notre-Dame Cathedral")

    composeRule.setContent {
      ActivityInfos(
          activity = activity,
          onBack = { /* no-op for this test */},
          mapContent = { Box(modifier = Modifier.fillMaxSize().testTag("TEST_MAP_TAG")) })
    }

    composeRule.onNodeWithTag(ActivityInfosTestTag.TOP_BAR).assertIsDisplayed()

    composeRule
        .onNodeWithTag(ActivityInfosTestTag.TITLE)
        .assertIsDisplayed()
        .assert(hasText("Notre-Dame Cathedral"))
  }

  @Test
  fun description_is_shown_when_not_blank() {
    val description = "A magical place."
    val activity = fakeActivity(description = description)

    composeRule.setContent {
      ActivityInfos(
          activity = activity,
          mapContent = { Box(modifier = Modifier.fillMaxSize().testTag("TEST_MAP_TAG")) })
    }

    // First ensure the container exists
    composeRule.onNodeWithTag(ActivityInfosTestTag.DESCRIPTION).assertIsDisplayed()

    // Then look for the exact description text
    composeRule.onNodeWithText(description, substring = false).assertIsDisplayed()
  }

  @Test
  fun description_is_hidden_when_blank() {
    val activity = fakeActivity(description = "")

    composeRule.setContent {
      ActivityInfos(
          activity = activity,
          mapContent = { Box(modifier = Modifier.fillMaxSize().testTag("TEST_MAP_TAG")) })
    }

    // Tag is only added when description is not blank, so it must not exist
    composeRule
        .onNodeWithTag(ActivityInfosTestTag.DESCRIPTION, useUnmergedTree = true)
        .assertDoesNotExist()
  }

  @Test
  fun estimated_time_chip_displays_formatted_duration() {
    // estimatedTime() = estimatedTimeSeconds / 60
    // here 3h30 => 210 minutes => "3h 30 min"
    val activity = fakeActivity(estimatedTimeSeconds = 210 * 60)

    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val expectedDurationText = "3h 30 min"
    val expectedChipText = context.getString(R.string.estimated_time, expectedDurationText)

    composeRule.setContent {
      ActivityInfos(
          activity = activity,
          mapContent = { Box(modifier = Modifier.fillMaxSize().testTag("TEST_MAP_TAG")) })
    }

    composeRule
        .onNodeWithTag(ActivityInfosTestTag.ESTIMATED_TIME)
        .assertIsDisplayed()
        .assert(hasText(expectedChipText))
  }
  //  This test was momentarily commented because of some issues with the Wikipedia Library.
  //    TODO() : Turn this test back once wikipedia works again

  //  @Test
  //  fun when_wikipedia_returns_images_images_section_is_shown() {
  //    // Use a very generic, well-known place name to maximize chance of images
  //    val activity = fakeActivity(name = "Bern")
  //
  //    composeRule.setContent {
  //      ActivityInfos(
  //          activity = activity,
  //          onBack = { /* no-op */},
  //          mapContent = { Box(modifier = Modifier.fillMaxSize().testTag("TEST_MAP_TAG")) })
  //    }
  //
  //    // Let composition start
  //    composeRule.waitForIdle()
  //
  //    // Now wait until the IMAGES container appears.
  //    // This will only happen if activityUrls.isNotEmpty()
  //    composeRule.waitUntil(timeoutMillis = 10_000) {
  //      try {
  //        composeRule
  //            .onAllNodesWithTag(ActivityInfosTestTag.IMAGES, useUnmergedTree = true)
  //            .fetchSemanticsNodes()
  //            .isNotEmpty()
  //      } catch (e: AssertionError) {
  //        false
  //      }
  //    }
  //
  //    // Finally assert that the images section is actually visible
  //    composeRule.onNodeWithTag(ActivityInfosTestTag.IMAGES).assertIsDisplayed()
  //  }

  @Test
  fun tip_text_is_displayed() {
    val activity = fakeActivity()
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val expectedTip = context.getString(R.string.activity_tip)

    composeRule.setContent {
      ActivityInfos(
          activity = activity,
          mapContent = { Box(modifier = Modifier.fillMaxSize().testTag("TEST_MAP_TAG")) })
    }

    composeRule
        .onNodeWithTag(ActivityInfosTestTag.TIP, useUnmergedTree = true)
        .assertIsDisplayed()
        .assert(hasText(expectedTip))
  }
}

fun fakeActivity(
    name: String = "Some Activity",
    description: String = "Nice place to visit.",
    estimatedTimeSeconds: Int = 7200,
): Activity {
  val now = Timestamp.now()
  val loc =
      Location(
          coordinate = Coordinate(0.0, 0.0),
          name = name,
      )

  return Activity(
      startDate = now,
      endDate = now,
      location = loc,
      description = description,
      imageUrls = emptyList(),
      estimatedTime = estimatedTimeSeconds,
  )
}
