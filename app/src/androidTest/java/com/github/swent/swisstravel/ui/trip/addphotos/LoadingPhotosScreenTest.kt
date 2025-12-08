package com.github.swent.swisstravel.ui.trip.addphotos

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.github.swent.swisstravel.ui.trip.tripinfos.photos.LoadingPhotosScreen
import com.github.swent.swisstravel.ui.trip.tripinfos.photos.LoadingPhotosTestTags
import org.junit.Rule
import org.junit.Test

class LoadingPhotosScreenTest {
  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun loadingScreenIsDisplayed() {
    composeTestRule.setContent { LoadingPhotosScreen() }
    composeTestRule.onNodeWithTag(LoadingPhotosTestTags.LOADING_PHOTOS_SCAFFOLD).assertIsDisplayed()
    composeTestRule.onNodeWithTag(LoadingPhotosTestTags.LOADING_PHOTOS_COLUMN).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(LoadingPhotosTestTags.LOADING_PHOTOS_INDICATOR)
        .assertIsDisplayed()
  }
}
