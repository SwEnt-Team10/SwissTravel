package com.github.swent.swisstravel.ui.trip.addphotos

import android.net.Uri
import androidx.core.net.toUri
import com.github.swent.swisstravel.model.trip.Trip
import com.github.swent.swisstravel.model.trip.TripProfile
import com.github.swent.swisstravel.model.trip.TripsRepository
import com.github.swent.swisstravel.ui.trip.tripinfos.photos.AddPhotosViewModel
import com.google.firebase.Timestamp
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import kotlin.test.assertEquals

class AddPhotosViewModelTest {

    // Mocked instance of TripsRepository initialization
    private lateinit var tripRepository: TripsRepository
    private lateinit var addPhotosViewModel: AddPhotosViewModel

    @Before
    fun setUp() {
        // Initialize the mock repository before each test
        tripRepository = mock(TripsRepository::class.java)
        addPhotosViewModel = AddPhotosViewModel(tripRepository)
    }

    @Test
    fun `test loadPhotos correctly load the photos`() = runTest {
        val fakeUris = listOf("uri1".toUri(), "uri2".toUri())
        val fakeTrip = Trip(
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
        `when`(tripRepository.getTrip("0")).thenReturn(fakeTrip)
        addPhotosViewModel.loadPhotos("0")
        assertEquals(fakeUris, addPhotosViewModel.uiState.value.listUri)
    }
}