package com.github.swent.swisstravel.ui.trip.tripinfo

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.model.trip.Coordinate
import com.github.swent.swisstravel.model.trip.Location
import com.github.swent.swisstravel.ui.trip.tripinfos.StepLocationCard
import com.github.swent.swisstravel.ui.trip.tripinfos.StepLocationCardTestTags
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Instrumented tests for the StepLocationCard composable. */
@RunWith(AndroidJUnit4::class)
class StepLocationTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun stepLabelAndLocationNameAreDisplayed() {
    val stepNumber = 2
    val location = Location(name = "Lausanne", coordinate = Coordinate(46.5197, 6.6323))

    composeTestRule.setContent {
      StepLocationCard(
          stepNumber = stepNumber,
          title = location.name,
          timeRange = "12:00–13:00",
          modifier = Modifier)
    }

    val expectedStep = composeTestRule.activity.getString(R.string.step_info, stepNumber)

    composeTestRule.onNodeWithTag(StepLocationCardTestTags.CARD).assertIsDisplayed()

    composeTestRule
        .onNodeWithTag(StepLocationCardTestTags.STEP_LABEL, useUnmergedTree = true)
        .assertTextEquals(expectedStep)

    composeTestRule
        .onNodeWithTag(StepLocationCardTestTags.LOCATION_NAME, useUnmergedTree = true)
        .assertTextEquals("Lausanne")
  }

  @Test
  fun stepAndLocationAreDisplayedUsingTags() {
    val stepNumber = 2
    val location = Location(name = "Lausanne", coordinate = Coordinate(46.5197, 6.6323))

    composeTestRule.setContent {
      StepLocationCard(
          stepNumber = stepNumber,
          title = location.name,
          timeRange = "12:00–13:00",
          modifier = Modifier)
    }

    val expectedStep = composeTestRule.activity.getString(R.string.step_info, stepNumber)

    composeTestRule.onNodeWithTag(StepLocationCardTestTags.CARD).assertIsDisplayed()

    composeTestRule
        .onNodeWithTag(StepLocationCardTestTags.STEP_LABEL, useUnmergedTree = true)
        .assertTextEquals(expectedStep)

    composeTestRule
        .onNodeWithTag(StepLocationCardTestTags.LOCATION_NAME, useUnmergedTree = true)
        .assertTextEquals(location.name)
  }

  @Test
  fun multipleCardsRenderAndHaveCorrectCounts() {
    composeTestRule.setContent {
      Column {
        StepLocationCard(
            stepNumber = 1, title = "A", timeRange = "12:00-13:00", modifier = Modifier)
        StepLocationCard(
            stepNumber = 2, title = "B", timeRange = "12:00-13:00", modifier = Modifier)
      }
    }

    composeTestRule.onAllNodesWithTag(StepLocationCardTestTags.STEP_LABEL).assertCountEquals(2)
    composeTestRule.onAllNodesWithTag(StepLocationCardTestTags.LOCATION_NAME).assertCountEquals(2)
  }

  @Test
  fun emptyLocationNameRendersEmptyTextNode() {
    val location = Location(name = "", coordinate = Coordinate(0.0, 0.0))
    composeTestRule.setContent {
      StepLocationCard(
          stepNumber = 0, title = location.name, timeRange = "12:00-13:00", modifier = Modifier)
    }

    composeTestRule.onNodeWithTag(StepLocationCardTestTags.LOCATION_NAME).assertTextEquals("")
  }
}
