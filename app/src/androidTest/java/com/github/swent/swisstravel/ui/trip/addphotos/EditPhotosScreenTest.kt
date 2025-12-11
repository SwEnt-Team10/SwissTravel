package com.github.swent.swisstravel.ui.trip.addphotos

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.core.net.toUri
import com.github.swent.swisstravel.model.trip.Trip
import com.github.swent.swisstravel.model.trip.TripProfile
import com.github.swent.swisstravel.model.trip.TripRepositoryLocal
import com.github.swent.swisstravel.model.trip.TripsRepository
import com.github.swent.swisstravel.model.trip.TripsRepositoryProvider
import com.github.swent.swisstravel.ui.trip.tripinfos.photos.EditPhotosScreen
import com.github.swent.swisstravel.ui.trip.tripinfos.photos.EditPhotosScreenTestTags
import com.github.swent.swisstravel.ui.trip.tripinfos.photos.PhotosViewModel
import com.github.swent.swisstravel.utils.SwissTravelTest
import com.google.firebase.Timestamp
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

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
            listUri = emptyList(),
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
            listUri = listOf("Uri1".toUri(), "AmazingUri2".toUri()),
            collaboratorsId = emptyList())

    TripsRepositoryProvider.repository.addTrip(fakeTrip)
    val fakeModel = PhotosViewModel()

    // UI testing
    composeTestRule.setContent {
      EditPhotosScreen(tripId = fakeTrip.uid, photosViewModel = fakeModel)
    }
    composeTestRule.editPhotosScreenIsDisplayed()
    composeTestRule.onNodeWithTag(EditPhotosScreenTestTags.EDIT_VERTICAL_GRID)
  }

  // AI did the test
  @Test
  fun editPhotosScreenShowsErrorScreenWhenErrorLoading() {
    val viewModel = PhotosViewModel(tripsRepository = TripRepositoryLocal())

    composeTestRule.setContent { EditPhotosScreen(tripId = "0", photosViewModel = viewModel) }

    composeTestRule.onNodeWithText("Could not load the photos").assertExists()
  }
}
