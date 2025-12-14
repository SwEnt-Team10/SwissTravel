package com.github.swent.swisstravel.ui.trip.addphotos

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.core.net.toUri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.swent.swisstravel.model.trip.Trip
import com.github.swent.swisstravel.model.trip.TripProfile
import com.github.swent.swisstravel.model.trip.TripRepositoryLocal
import com.github.swent.swisstravel.model.trip.TripsRepository
import com.github.swent.swisstravel.model.trip.TripsRepositoryProvider
import com.github.swent.swisstravel.ui.composable.DeleteDialogTestTags
import com.github.swent.swisstravel.ui.composable.PhotoGridTestTags
import com.github.swent.swisstravel.ui.trip.tripinfos.photos.EditPhotosScreen
import com.github.swent.swisstravel.ui.trip.tripinfos.photos.EditPhotosScreenTestTags
import com.github.swent.swisstravel.ui.trip.tripinfos.photos.PhotosViewModel
import com.github.swent.swisstravel.utils.SwissTravelTest
import com.github.swent.swisstravel.utils.UI_WAIT_TIMEOUT
import com.google.firebase.Timestamp
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EditPhotosScreenTest : SwissTravelTest() {
  @get:Rule val composeTestRule = createComposeRule()

  override fun createInitializedRepository(): TripsRepository {
    return TripRepositoryLocal()
  }

  @Test
  fun checkAllComponentsAreDisplayedWithNoImage() = runTest {

    // Initialization of the fake repository and model
    val fakeTrip =
        Trip(
            uid = "10",
            name = "Amazing trip",
            ownerId = "1274218746",
            locations = emptyList(),
            routeSegments = emptyList(),
            activities = emptyList(),
            tripProfile = TripProfile(startDate = Timestamp.now(), endDate = Timestamp.now()),
            isCurrentTrip = true,
            uriLocation = emptyMap(),
            collaboratorsId = emptyList())
    TripsRepositoryProvider.repository.addTrip(fakeTrip)
    val fakeModel = PhotosViewModel()

    // UI testing
    composeTestRule.setContent {
      EditPhotosScreen(tripId = fakeTrip.uid, photosViewModel = fakeModel)
    }
    composeTestRule.editPhotosScreenIsDisplayed()
  }

  @Test
  fun checkAllComponentsAreDisplayedWithImagesNoSelection() = runTest {

    // Initialization of the fake repository and model
    val uri1 = "Uri1".toUri()
    val uri2 = "AmazingUri2".toUri()
    val fakeTrip =
        Trip(
            uid = "10",
            name = "Amazing trip",
            ownerId = "1274218746",
            locations = emptyList(),
            routeSegments = emptyList(),
            activities = emptyList(),
            tripProfile = TripProfile(startDate = Timestamp.now(), endDate = Timestamp.now()),
            isCurrentTrip = true,
            uriLocation = mapOf(uri1 to dummyLocation, uri2 to dummyLocation),
            collaboratorsId = emptyList())

    TripsRepositoryProvider.repository.addTrip(fakeTrip)
    val fakeModel = PhotosViewModel()

    // UI testing
    composeTestRule.setContent {
      EditPhotosScreen(tripId = fakeTrip.uid, photosViewModel = fakeModel)
    }
    composeTestRule.editPhotosScreenIsDisplayed()
    composeTestRule.onNodeWithTag(EditPhotosScreenTestTags.EDIT_PHOTO_GRID).isDisplayed()
  }

  // AI did the test
  @Test
  fun editPhotosScreenShowsErrorScreenWhenErrorLoading() {
    val viewModel = PhotosViewModel(tripsRepository = TripRepositoryLocal())

    composeTestRule.setContent { EditPhotosScreen(tripId = "0", photosViewModel = viewModel) }

    composeTestRule.onNodeWithText("Could not load the photos").assertExists()
  }

  // AI helped for this test
  @Test
  fun deletePhotoRemovesItFromGrid() = runTest {
    // 1. Setup Data
    val uri1 = "Uri1".toUri()
    val uri2 = "AmazingUri2".toUri()
    val fakeTrip =
        Trip(
            uid = "tripToDeletePhotos",
            name = "Delete Photos Trip",
            ownerId = "user1",
            locations = emptyList(),
            routeSegments = emptyList(),
            activities = emptyList(),
            tripProfile = TripProfile(startDate = Timestamp.now(), endDate = Timestamp.now()),
            isCurrentTrip = true,
            uriLocation = mapOf(uri1 to dummyLocation, uri2 to dummyLocation),
            collaboratorsId = emptyList())

    TripsRepositoryProvider.repository.addTrip(fakeTrip)
    val fakeModel = PhotosViewModel()

    // 2. Launch Screen
    composeTestRule.setContent {
      EditPhotosScreen(tripId = fakeTrip.uid, photosViewModel = fakeModel)
    }
    composeTestRule.waitForIdle()

    // Verify initially 2 items are present
    composeTestRule
        .onNodeWithTag(EditPhotosScreenTestTags.EDIT_PHOTO_GRID)
        .onChildren()
        .assertCountEquals(2)

    // 3. Select the first photo (index 0) using tag
    composeTestRule
        .onNodeWithTag(PhotoGridTestTags.getTestTagForPhoto(0), useUnmergedTree = true)
        .performClick()

    composeTestRule.waitForIdle()

    // 4. Click Remove Button
    composeTestRule
        .onNodeWithTag(EditPhotosScreenTestTags.EDIT_REMOVE_BUTTON)
        .assertIsEnabled()
        .performClick()

    // 5. Confirm deletion
    composeTestRule.onNodeWithTag(DeleteDialogTestTags.CONFIRM_DELETE_BUTTON).performClick()

    composeTestRule.waitForIdle()

    // 6. Verify only 1 item remains in the grid
    // The previous index 1 (uri2) should now be index 0.
    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(PhotoGridTestTags.getTestTagForPhoto(1), useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isEmpty()
    }

    // Verify one item remains (at index 0)
    composeTestRule
        .onNodeWithTag(PhotoGridTestTags.getTestTagForPhoto(0), useUnmergedTree = true)
        .assertIsDisplayed()

    // Verify total count
    composeTestRule
        .onNodeWithTag(EditPhotosScreenTestTags.EDIT_PHOTO_GRID)
        .onChildren()
        .assertCountEquals(1)
  }
}
