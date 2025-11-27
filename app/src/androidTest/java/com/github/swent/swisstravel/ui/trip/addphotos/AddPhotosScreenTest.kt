@file:OptIn(ExperimentalCoroutinesApi::class)

package com.github.swent.swisstravel.ui.trip.addphotos

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.swent.swisstravel.model.trip.Trip
import com.github.swent.swisstravel.model.trip.TripProfile
import com.github.swent.swisstravel.model.trip.TripRepositoryLocal
import com.github.swent.swisstravel.model.trip.TripsRepository
import com.github.swent.swisstravel.model.trip.TripsRepositoryProvider
import com.github.swent.swisstravel.ui.trip.tripinfos.photos.AddPhotosScreen
import com.github.swent.swisstravel.ui.trip.tripinfos.photos.AddPhotosViewModel
import com.github.swent.swisstravel.utils.SwissTravelTest
import com.google.firebase.Timestamp
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.runner.RunWith
import kotlin.test.Test

@RunWith(AndroidJUnit4::class)
class AddPhotosScreenTest : SwissTravelTest() {
    @get:Rule
    val composeTestRule = createComposeRule()

    override fun createInitializedRepository(): TripsRepository {
        return TripRepositoryLocal()
    }

    @Test
    fun checkAllComponentsAreDisplayedWithNoImage() = runTest {

        val fakeTrip = Trip(
            uid = "10",
            name = "Amazing trip",
            ownerId = "1274218746",
            locations = emptyList(),
            routeSegments = emptyList(),
            activities = emptyList(),
            tripProfile = TripProfile(
                startDate = Timestamp.now(),
                endDate = Timestamp.now()
            ),
            isFavorite = true,
            isCurrentTrip = true
        )
        TripsRepositoryProvider.repository.addTrip(fakeTrip)
        val fakeModel = AddPhotosViewModel()
        composeTestRule.setContent {
            AddPhotosScreen(tripId = "10", viewModel = fakeModel)
        }
        composeTestRule.addPhotosScreenIsDisplayed()
    }
}