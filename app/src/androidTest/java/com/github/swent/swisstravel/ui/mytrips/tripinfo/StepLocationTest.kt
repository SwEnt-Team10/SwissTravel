package com.github.swent.swisstravel.ui.mytrips.tripinfo

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
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
import com.github.swent.swisstravel.ui.mytrips.tripinfos.StepLocationCard
import com.github.swent.swisstravel.ui.mytrips.tripinfos.StepLocationCardTestTags
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StepLocationTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun stepLabelAndLocationNameAreDisplayed() {
    val stepNumber = 2
    val location = Location(name = "Lausanne", coordinate = Coordinate(46.5197, 6.6323))
    composeTestRule.setContent { StepLocationCard(int = stepNumber, location = location) }

    val expectedStep = "${composeTestRule.activity.getString(R.string.step_info)} $stepNumber"
    composeTestRule.onNodeWithTag(StepLocationCardTestTags.CARD).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(StepLocationCardTestTags.STEP_LABEL)
        .assertTextEquals(expectedStep)
    composeTestRule
        .onNodeWithTag(StepLocationCardTestTags.LOCATION_NAME)
        .assertTextEquals("Lausanne")
  }

  @Test
  fun stepAndLocationAreDisplayedUsingTags() {
    val stepNumber = 2
    val location = Location(name = "Lausanne", coordinate = Coordinate(46.5197, 6.6323))
    composeTestRule.setContent { StepLocationCard(int = stepNumber, location = location) }

    val expectedStep = "${composeTestRule.activity.getString(R.string.step_info)} $stepNumber"
    composeTestRule.onNodeWithTag(StepLocationCardTestTags.CARD).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(StepLocationCardTestTags.STEP_LABEL)
        .assertTextEquals(expectedStep)
    composeTestRule
        .onNodeWithTag(StepLocationCardTestTags.LOCATION_NAME)
        .assertTextEquals("Lausanne")
  }

  @Test
  fun multipleCardsRenderAndHaveCorrectCounts() {
    composeTestRule.setContent {
      Column {
        StepLocationCard(
            int = 1, location = Location(name = "A", coordinate = Coordinate(0.0, 0.0)))
        StepLocationCard(
            int = 2, location = Location(name = "B", coordinate = Coordinate(0.0, 0.0)))
      }
    }

    composeTestRule.onAllNodesWithTag(StepLocationCardTestTags.STEP_LABEL).assertCountEquals(2)
    composeTestRule.onAllNodesWithTag(StepLocationCardTestTags.LOCATION_NAME).assertCountEquals(2)
  }

  @Test
  fun emptyLocationNameRendersEmptyTextNode() {
    val location = Location(name = "", coordinate = Coordinate(0.0, 0.0))
    composeTestRule.setContent { StepLocationCard(int = 0, location = location) }

    composeTestRule.onNodeWithTag(StepLocationCardTestTags.LOCATION_NAME).assertTextEquals("")
  }
}
