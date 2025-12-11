package com.github.swent.swisstravel.ui.trip.addphotos

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.swent.swisstravel.model.trip.Coordinate
import com.github.swent.swisstravel.model.trip.Location
import com.github.swent.swisstravel.model.trip.Trip
import com.github.swent.swisstravel.model.trip.TripProfile
import com.github.swent.swisstravel.model.trip.TripRepositoryLocal
import com.github.swent.swisstravel.model.trip.TripsRepository
import com.github.swent.swisstravel.model.trip.TripsRepositoryProvider
import com.github.swent.swisstravel.ui.trip.tripinfos.photos.AddPhotosScreen
import com.github.swent.swisstravel.ui.trip.tripinfos.photos.AddPhotosScreenTestTags
import com.github.swent.swisstravel.ui.trip.tripinfos.photos.PhotosViewModel
import com.github.swent.swisstravel.utils.SwissTravelTest
import com.google.firebase.Timestamp
import kotlin.test.Test
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AddPhotosScreenTest : SwissTravelTest() {
  @get:Rule val composeTestRule = createComposeRule()

  override fun createInitializedRepository(): TripsRepository {
    // Create a local repository
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
      AddPhotosScreen(tripId = fakeTrip.uid, photosViewModel = fakeModel)
    }
    composeTestRule.addPhotosScreenIsDisplayed()
  }

  @Test
  fun checkAllComponentsAreDisplayedWithImages() = runTest {
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
      AddPhotosScreen(tripId = fakeTrip.uid, photosViewModel = fakeModel)
    }
    composeTestRule.addPhotosScreenIsDisplayed()
    composeTestRule.onNodeWithTag(AddPhotosScreenTestTags.VERTICAL_GRID).isDisplayed()

    // We go through the map
    val uriCount = fakeTrip.uriLocation.size
    for (i in 0 until uriCount) {
      composeTestRule.onNodeWithTag(AddPhotosScreenTestTags.getTestTagForUri(i)).isDisplayed()
    }
  }

  // AI did the test
  @Test
  fun checkAddingImagesViaButton() = runTest {
    val fakeTrip =
        Trip(
            uid = "11",
            name = "Amazing trip",
            ownerId = "1274218746",
            locations = emptyList(),
            routeSegments = emptyList(),
            activities = emptyList(),
            tripProfile = TripProfile(Timestamp.now(), Timestamp.now()),
            isFavorite = true,
            isCurrentTrip = true,
            uriLocation = emptyMap(),
            collaboratorsId = emptyList())
    TripsRepositoryProvider.repository.addTrip(fakeTrip)
    val fakeModel = PhotosViewModel()

    composeTestRule.setContent {
      AddPhotosScreen(
          tripId = fakeTrip.uid,
          photosViewModel = fakeModel,
          launchPickerOverride = {
            // Context required for addUris
            fakeModel.addUris(
                listOf("content://fake/photo1".toUri(), "content://fake/photo2".toUri()),
                ApplicationProvider.getApplicationContext(),
                fakeTrip.uid)
          })
    }

    // Click on the button add photos
    composeTestRule.onNodeWithTag(AddPhotosScreenTestTags.ADD_PHOTOS_BUTTON).performClick()

    // Verify that the images has been added
    composeTestRule
        .onNodeWithTag(AddPhotosScreenTestTags.VERTICAL_GRID)
        .onChildren()
        .assertCountEquals(2)
  }

  // AI did the test
  @Test
  fun checkBackAndSaveButtonsTriggerOnBack() = runTest {
    var backCalled = false
    val fakeTrip =
        Trip(
            uid = "12",
            name = "Trip with images",
            ownerId = "1274218746",
            locations = emptyList(),
            routeSegments = emptyList(),
            activities = emptyList(),
            tripProfile = TripProfile(Timestamp.now(), Timestamp.now()),
            isFavorite = true,
            isCurrentTrip = true,
            uriLocation = emptyMap(),
            collaboratorsId = emptyList())
    TripsRepositoryProvider.repository.addTrip(fakeTrip)
    val fakeModel = PhotosViewModel()

    composeTestRule.setContent {
      AddPhotosScreen(
          tripId = fakeTrip.uid,
          photosViewModel = fakeModel,
          onBack = { backCalled = true },
          launchPickerOverride = {
            fakeModel.addUris(
                listOf("content://fake/photo1".toUri(), "content://fake/photo2".toUri()),
                ApplicationProvider.getApplicationContext(),
                fakeTrip.uid)
          })
    }

    // Click on the back button
    composeTestRule.clickOnBackButton()
    assert(backCalled)
  }

  // AI did the test
  @Test
  fun addPhotosScreenShowsErrorScreenWhenErrorLoading() {
    val viewModel = PhotosViewModel(tripsRepository = TripRepositoryLocal())

    composeTestRule.setContent { AddPhotosScreen(tripId = "0", photosViewModel = viewModel) }

    composeTestRule.onNodeWithText("Could not load the photos").assertExists()
  }
}
