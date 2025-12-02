package com.github.swent.swisstravel.ui.trip.addphotos

import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.core.net.toUri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.swent.swisstravel.model.trip.Trip
import com.github.swent.swisstravel.model.trip.TripProfile
import com.github.swent.swisstravel.model.trip.TripRepositoryLocal
import com.github.swent.swisstravel.model.trip.TripsRepository
import com.github.swent.swisstravel.model.trip.TripsRepositoryProvider
import com.github.swent.swisstravel.ui.trip.tripinfos.addphotos.AddPhotosScreen
import com.github.swent.swisstravel.ui.trip.tripinfos.addphotos.AddPhotosScreenTestTags
import com.github.swent.swisstravel.ui.trip.tripinfos.addphotos.AddPhotosViewModel
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
            listUri = emptyList())
    TripsRepositoryProvider.repository.addTrip(fakeTrip)
    val fakeModel = AddPhotosViewModel()
    // UI testing
    composeTestRule.setContent { AddPhotosScreen(tripId = fakeTrip.uid, viewModel = fakeModel) }
    composeTestRule.addPhotosScreenIsDisplayed()
  }

  @Test
  fun checkAllComponentsAreDisplayedWithImages() = runTest {
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
            listUri = listOf("Uri1".toUri(), "AmazingUri2".toUri()))

    TripsRepositoryProvider.repository.addTrip(fakeTrip)
    val fakeModel = AddPhotosViewModel()
    // UI testing
    composeTestRule.setContent { AddPhotosScreen(tripId = fakeTrip.uid, viewModel = fakeModel) }
    composeTestRule.addPhotosScreenIsDisplayed()
    composeTestRule.onNodeWithTag(AddPhotosScreenTestTags.VERTICAL_GRID).isDisplayed()
    for (i in fakeTrip.listUri.indices) {
      composeTestRule.onNodeWithTag(AddPhotosScreenTestTags.getTestTagForUri(i)).isDisplayed()
    }
  }

  @Test
  fun pickerSimulation_addsImagesToGrid() = runTest {
    val fakeTrip =
        Trip(
            uid = "10",
            name = "Amazing trip",
            ownerId = "1274218746",
            locations = emptyList(),
            routeSegments = emptyList(),
            activities = emptyList(),
            tripProfile = TripProfile(Timestamp.now(), Timestamp.now()),
            isFavorite = true,
            isCurrentTrip = true,
            listUri = emptyList())
    TripsRepositoryProvider.repository.addTrip(fakeTrip)
    val fakeModel = AddPhotosViewModel()

    // Compose content
    composeTestRule.setContent { AddPhotosScreen(tripId = fakeTrip.uid, viewModel = fakeModel) }

    // Simulate picker returning 2 URIs
    val fakeUris = listOf("FakeUri1".toUri(), "FakeUri2".toUri())
    composeTestRule.runOnUiThread { fakeModel.addUri(fakeUris) }

    // Assert grid shows the images
    fakeUris.forEachIndexed { index, _ ->
      composeTestRule.onNodeWithTag(AddPhotosScreenTestTags.getTestTagForUri(index)).isDisplayed()
    }
  }
}
