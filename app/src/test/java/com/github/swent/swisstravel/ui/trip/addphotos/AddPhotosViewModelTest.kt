@file:OptIn(ExperimentalCoroutinesApi::class)

package com.github.swent.swisstravel.ui.trip.addphotos

import androidx.core.net.toUri
import com.github.swent.swisstravel.model.trip.Trip
import com.github.swent.swisstravel.model.trip.TripProfile
import com.github.swent.swisstravel.model.trip.TripsRepository
import com.github.swent.swisstravel.ui.trip.tripinfos.photos.AddPhotosViewModel
import com.google.firebase.Timestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import kotlin.test.assertEquals

/**
 * Unit tests for AddPhotosViewModel
 */
class AddPhotosViewModelTest {
    // Fake values
    private val fakeUris = listOf("uri1".toUri(), "uri2".toUri())
    // Fake trip
    private val fakeTrip = Trip(
        uid = "1",
        name = "fake trip",
        ownerId = "100",
        locations = emptyList(),
        routeSegments = emptyList(),
        activities = emptyList(),
        tripProfile = TripProfile(
            startDate = Timestamp.now(),
            endDate = Timestamp.now()
        ),
        isFavorite = true,
        isCurrentTrip = true,
        listUri = fakeUris
    )

    @Test
    fun `test loadPhotos correctly load the photos`() = runTest {

        try {
            // Set correctly the dispatcher
            val testDispatcher = UnconfinedTestDispatcher(testScheduler)
            Dispatchers.setMain(testDispatcher)

            // Set the model
            val tripRepository = mock(TripsRepository::class.java)
            val addPhotosViewModel = AddPhotosViewModel(tripRepository)

            // Set up the mock to return the fake trip
            `when`(tripRepository.getTrip("0")).thenReturn(fakeTrip)

            // Load photos and verify that it is correctly done
            addPhotosViewModel.loadPhotos("0")
            assertEquals(fakeUris, addPhotosViewModel.uiState.value.listUri)

        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `test addUri correctly adds new uri to the list`() = runTest {

        try {
            // Set correctly the dispatcher
            val testDispatcher = UnconfinedTestDispatcher(testScheduler)
            Dispatchers.setMain(testDispatcher)

            // Set the model
            val tripRepository = mock(TripsRepository::class.java)
            val addPhotosViewModel = AddPhotosViewModel(tripRepository)

            // Set up the mock to return the fake trip
            `when`(tripRepository.getTrip("0")).thenReturn(fakeTrip)

            // Load the state
            addPhotosViewModel.loadPhotos("0")

            // Add new photos to the state
            val newUris = listOf("newUri1".toUri(), "newUri2".toUri())
            addPhotosViewModel.addUri(newUris)

            // Verify that the new list of Uris is the concatenation of the
            val expectedList = fakeUris + newUris
            assertEquals(expectedList, addPhotosViewModel.uiState.value.listUri)

        } finally {
            Dispatchers.resetMain()
        }
    }
}