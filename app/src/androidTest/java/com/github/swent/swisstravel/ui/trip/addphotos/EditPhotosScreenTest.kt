package com.github.swent.swisstravel.ui.trip.addphotos

import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.core.net.toUri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.swent.swisstravel.model.trip.Coordinate
import com.github.swent.swisstravel.model.trip.Location
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
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EditPhotosScreenTest : SwissTravelTest() {
  @get:Rule val composeTestRule = createComposeRule()

  override fun createInitializedRepository(): TripsRepository {
    return TripRepositoryLocal()
  }

  // Fake location for testing
  private val dummyLocation = Location(Coordinate(0.0, 0.0), "Test Location")

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
            isFavorite = true,
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
            isFavorite = true,
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
    composeTestRule.onNodeWithTag(EditPhotosScreenTestTags.EDIT_VERTICAL_GRID).isDisplayed()
  }

  // AI did the test
  @Test
  fun editPhotosScreenShowsErrorScreenWhenErrorLoading() {
    val viewModel = PhotosViewModel(tripsRepository = TripRepositoryLocal())

    composeTestRule.setContent { EditPhotosScreen(tripId = "0", photosViewModel = viewModel) }

    composeTestRule.onNodeWithText("Could not load the photos").assertExists()
  }
}
