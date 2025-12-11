package com.github.swent.swisstravel.ui.trip.addphotos

import android.content.Context
import android.net.Uri
import com.github.swent.swisstravel.model.trip.Coordinate
import com.github.swent.swisstravel.model.trip.Location
import com.github.swent.swisstravel.model.trip.Trip
import com.github.swent.swisstravel.model.trip.TripProfile
import com.github.swent.swisstravel.model.trip.TripRepositoryLocal
import com.github.swent.swisstravel.model.trip.TripsRepository
import com.github.swent.swisstravel.ui.trip.tripinfos.photos.PhotosViewModel
import com.google.firebase.Timestamp
import kotlin.test.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

/** Unit tests for AddPhotosViewModel */
@OptIn(ExperimentalCoroutinesApi::class)
class PhotosViewModelTest {
    // Fake values
    // Utilisation de mocks pour éviter les problèmes de toUri() qui renvoie null en test unitaire
    private val fakeUri1 = mock(Uri::class.java)
    private val fakeUri2 = mock(Uri::class.java)
    private val fakeLocation1 = Location(Coordinate(0.0, 1.0), "No location1")
    private val fakeLocation2 = Location(Coordinate(0.0, 0.0), "No location2")

    // Cette location correspond à la valeur par défaut dans le ViewModel
    private val defaultLocation = Location(Coordinate(0.0, 0.0), "No location")

    private val fakeUriLocation = mapOf(fakeUri1 to fakeLocation1, fakeUri2 to fakeLocation2)

    // Fake trip
    private val fakeTrip =
        Trip(
            uid = "1",
            name = "fake trip",
            ownerId = "100",
            locations = emptyList(),
            routeSegments = emptyList(),
            activities = emptyList(),
            tripProfile = TripProfile(startDate = Timestamp.now(), endDate = Timestamp.now()),
            isFavorite = true,
            isCurrentTrip = true,
            collaboratorsId = emptyList(),
            uriLocation = fakeUriLocation
        )

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test loadPhotos correctly load the photos`() = runTest {
        // Set the model
        val tripRepository = mock(TripsRepository::class.java)
        val photosViewModel = PhotosViewModel(tripRepository)

        // Set up the mock to return the fake trip
        `when`(tripRepository.getTrip(fakeTrip.uid)).thenReturn(fakeTrip)

        // Load photos and verify that it is correctly done
        photosViewModel.loadPhotos(fakeTrip.uid)
        assertEquals(fakeUriLocation, photosViewModel.uiState.value.uriLocation)
        assertEquals(false, photosViewModel.uiState.value.isLoading)
        assertEquals("Successfully loaded the photos", photosViewModel.uiState.value.toastMessage)
    }

    @Test
    fun `test loadPhotos correctly load the photo`() = runTest {
        // Set the model
        val tripRepository = mock(TripsRepository::class.java)
        val photosViewModel = PhotosViewModel(tripRepository)

        val fakeTripNoPhoto = fakeTrip.copy(uriLocation = emptyMap())
        // Set up the mock to return the fake trip
        `when`(tripRepository.getTrip(fakeTripNoPhoto.uid)).thenReturn(fakeTripNoPhoto)

        // Load photos and verify that it is correctly done
        photosViewModel.loadPhotos(fakeTripNoPhoto.uid)
        val state = photosViewModel.uiState
        assertEquals(fakeTripNoPhoto.uriLocation, state.value.uriLocation)
        assertEquals(false, state.value.isLoading)
        assertEquals("Successfully loaded the photo", state.value.toastMessage)
    }

    @Test
    fun `loadPhotos will fail on getTrip for a trip with photos`() = runTest {
        // Set the model
        val fakeRepository = mock(TripsRepository::class.java)
        val photosViewModel = PhotosViewModel(fakeRepository)
        `when`(fakeRepository.getTrip(fakeTrip.uid)).thenThrow(RuntimeException("Could not get trip"))

        // Try to load
        photosViewModel.loadPhotos(fakeTrip.uid)

        // Verify
        assertEquals(false, photosViewModel.uiState.value.isLoading)
        assert(photosViewModel.uiState.value.errorLoading)
    }

    @Test
    fun `test addUri correctly adds new uri to the list`() = runTest {
        // Set the model
        val tripRepository = mock(TripsRepository::class.java)
        val photosViewModel = PhotosViewModel(tripRepository)
        val mockContext = mock(Context::class.java)

        // Set up the mock to return the fake trip
        `when`(tripRepository.getTrip(fakeTrip.uid)).thenReturn(fakeTrip)

        // Load the state
        photosViewModel.loadPhotos(fakeTrip.uid)

        // Add new photos to the state (USING MOCKS)
        val newUri1 = mock(Uri::class.java)
        val newUri2 = mock(Uri::class.java)
        val newUris = listOf(newUri1, newUri2)

        photosViewModel.addUris(newUris, mockContext, fakeTrip.uid)

        // Verify that the new list of Uris is the concatenation
        val expectedMap = fakeUriLocation + newUris.associateWith { defaultLocation }

        assertEquals(expectedMap, photosViewModel.uiState.value.uriLocation)
        assertEquals(expectedMap.size, photosViewModel.uiState.value.uriLocation.size)
    }

    @Test
    fun `test savePhotos correctly adds new uris to the repository`() = runTest {
        // Set the model
        val fakeRepo = TripRepositoryLocal()
        fakeRepo.addTrip(fakeTrip)
        val photosViewModel = PhotosViewModel(fakeRepo)
        val mockContext = mock(Context::class.java)

        // Load the state
        photosViewModel.loadPhotos(fakeTrip.uid)

        // Add new photos to the state (USING MOCKS)
        val newUri1 = mock(Uri::class.java)
        val newUri2 = mock(Uri::class.java)
        val newUris = listOf(newUri1, newUri2)

        photosViewModel.addUris(newUris, mockContext, fakeTrip.uid)

        // Save to the repository
        photosViewModel.savePhotos(fakeTrip.uid)

        // The expected map contains old uris + new uris
        val expectedMap = fakeUriLocation + newUris.associateWith { defaultLocation }

        // Verify
        assertEquals(expectedMap, fakeRepo.getTrip(fakeTrip.uid).uriLocation)
        assertEquals("Photo saved", photosViewModel.uiState.value.toastMessage)
    }

    @Test
    fun `test savePhotos correctly adds new uri to the repository`() = runTest {
        // Set the model
        val fakeRepo = TripRepositoryLocal()
        fakeRepo.addTrip(fakeTrip)
        val photosViewModel = PhotosViewModel(fakeRepo)
        val mockContext = mock(Context::class.java)

        // Load the state
        photosViewModel.loadPhotos(fakeTrip.uid)

        // Add new photos to the state (USING MOCK)
        val newUri = mock(Uri::class.java)
        val newUris = listOf(newUri)

        photosViewModel.addUris(newUris, mockContext, fakeTrip.uid)

        // Save to the repository
        photosViewModel.savePhotos(fakeTrip.uid)
        val expectedMap = fakeUriLocation + newUris.associateWith { defaultLocation }

        // Verify
        assertEquals(expectedMap, fakeRepo.getTrip(fakeTrip.uid).uriLocation)
        assertEquals("Photo saved", photosViewModel.uiState.value.toastMessage)
    }

    @Test
    fun `savePhotos correctly set the toast message when getTrip throws exception`() = runTest {
        // Set up
        val mockRepo = mock(TripsRepository::class.java)
        val photosViewModel = PhotosViewModel(mockRepo)
        val mockContext = mock(Context::class.java)

        // New Uris (USING MOCKS)
        val newUri1 = mock(Uri::class.java)
        val newUri2 = mock(Uri::class.java)
        val newUris = listOf(newUri1, newUri2)

        photosViewModel.addUris(newUris, mockContext, fakeTrip.uid)

        // Get trip will fail
        `when`(mockRepo.getTrip(fakeTrip.uid)).thenThrow(RuntimeException("Could not get the trip"))

        // Try to save
        photosViewModel.savePhotos(fakeTrip.uid)

        // Verify
        assertEquals("Could not save the photo", photosViewModel.uiState.value.toastMessage)
    }

    @Test
    fun `savePhotos correctly sets toast message when getTrip throws exception with one photo`() =
        runTest {
            // Set up
            val mockRepo = mock(TripsRepository::class.java)
            val photosViewModel = PhotosViewModel(mockRepo)
            val mockContext = mock(Context::class.java)

            // The trip has no photo
            val fakeTripNoPhoto = fakeTrip.copy(uriLocation = emptyMap())

            // Add one uri (USING MOCK)
            val newUri = mock(Uri::class.java)
            val newUris = listOf(newUri)

            // getTrip will fail
            `when`(mockRepo.getTrip(fakeTripNoPhoto.uid))
                .thenThrow(RuntimeException("Could not get the trip"))

            photosViewModel.addUris(newUris, mockContext, fakeTripNoPhoto.uid)

            photosViewModel.savePhotos(fakeTripNoPhoto.uid)

            // Verify
            assertEquals("Could not save the photo", photosViewModel.uiState.value.toastMessage)
        }

    @Test
    fun `selectToRemove correctly add index when there is not the index`() = runTest {
        // Set the model
        val fakeRepo = mock(TripsRepository::class.java)
        val photosViewModel = PhotosViewModel(fakeRepo)

        // Set up the mock to return the fake trip
        `when`(fakeRepo.getTrip(fakeTrip.uid)).thenReturn(fakeTrip)

        // Load the state
        photosViewModel.loadPhotos(fakeTrip.uid)

        // Add first photo to remove
        photosViewModel.selectToRemove(0)

        // Verify
        val expected = listOf(0)
        assertEquals(expected, photosViewModel.uiState.value.uriSelected)
    }

    @Test
    fun `selectToRemove correctly remove index when the index is already there`() = runTest {
        // Set the model
        val fakeRepo = mock(TripsRepository::class.java)
        val photosViewModel = PhotosViewModel(fakeRepo)

        // Set up the mock to return the fake trip
        `when`(fakeRepo.getTrip(fakeTrip.uid)).thenReturn(fakeTrip)

        // Load the state
        photosViewModel.loadPhotos(fakeTrip.uid)

        // Add first photo to remove
        photosViewModel.selectToRemove(0)

        // Remove first photo to remove
        photosViewModel.selectToRemove(0)

        // Verify
        val expected = emptyList<Int>()
        assertEquals(expected, photosViewModel.uiState.value.uriSelected)
    }

    @Test
    fun `removePhotos correctly remove selected photos to the state`() = runTest {
        // Set the model
        val fakeRepo = TripRepositoryLocal()
        fakeRepo.addTrip(fakeTrip)
        val photosViewModel = PhotosViewModel(fakeRepo)

        // Load the state
        photosViewModel.loadPhotos(fakeTrip.uid)

        // Add photos to remove
        photosViewModel.selectToRemove(0)
        photosViewModel.selectToRemove(1)

        // Remove photos
        photosViewModel.removePhotos(fakeTrip.uid)

        // Verify
        val expected = emptyMap<Uri, Location>()
        assertEquals(expected, photosViewModel.uiState.value.uriLocation)
        assertEquals(expected, fakeRepo.getTrip(fakeTrip.uid).uriLocation)
        assertEquals("Photos removed", photosViewModel.uiState.value.toastMessage)
    }

    @Test
    fun `removePhotos correctly remove selected photo to the state`() = runTest {
        // Set the model
        val fakeRepo = TripRepositoryLocal()
        fakeRepo.addTrip(fakeTrip)
        val photosViewModel = PhotosViewModel(fakeRepo)

        // Load the state
        photosViewModel.loadPhotos(fakeTrip.uid)

        // Add photos to remove
        photosViewModel.selectToRemove(1)

        // Remove photos
        photosViewModel.removePhotos(fakeTrip.uid)

        // Verify
        // fakeUriLocation uses fakeUri1 (index 0) and fakeUri2 (index 1). Removing index 1 leaves fakeUri1.
        val expected = mapOf(fakeUri1 to fakeLocation1)
        assertEquals(expected, photosViewModel.uiState.value.uriLocation)
        assertEquals(expected, fakeRepo.getTrip(fakeTrip.uid).uriLocation)
        assertEquals("Photo removed", photosViewModel.uiState.value.toastMessage)
    }

    @Test
    fun `removePhotos fails on getTrip case several photos`() = runTest {
        // Set the model
        val fakeRepo = mock(TripsRepository::class.java)
        val photosViewModel = PhotosViewModel(fakeRepo)

        // Load photos
        `when`(fakeRepo.getTrip(fakeTrip.uid)).thenReturn(fakeTrip)
        photosViewModel.loadPhotos(fakeTrip.uid)

        // Select to remove
        photosViewModel.selectToRemove(0)
        photosViewModel.selectToRemove(1)
        val selected = listOf(0, 1)

        // Try to remove
        `when`(fakeRepo.getTrip(fakeTrip.uid)).thenThrow(RuntimeException("Could not get trip"))
        photosViewModel.removePhotos(fakeTrip.uid)

        // Verify
        val state = photosViewModel.uiState
        assertEquals("Could not remove the photos", state.value.toastMessage)
        assertEquals(selected, state.value.uriSelected)
    }

    @Test
    fun `removePhotos fails on getTrip case one photo`() = runTest {
        // Set the model
        val fakeRepo = mock(TripsRepository::class.java)
        val photosViewModel = PhotosViewModel(fakeRepo)

        // Load photos
        `when`(fakeRepo.getTrip(fakeTrip.uid)).thenReturn(fakeTrip)
        photosViewModel.loadPhotos(fakeTrip.uid)

        // Select to remove
        photosViewModel.selectToRemove(0)
        val selected = listOf(0)

        // Try to remove
        `when`(fakeRepo.getTrip(fakeTrip.uid)).thenThrow(RuntimeException("Could not get trip"))
        photosViewModel.removePhotos(fakeTrip.uid)

        // Verify
        val state = photosViewModel.uiState
        assertEquals("Could not remove the photo", state.value.toastMessage)
        assertEquals(selected, state.value.uriSelected)
    }
}