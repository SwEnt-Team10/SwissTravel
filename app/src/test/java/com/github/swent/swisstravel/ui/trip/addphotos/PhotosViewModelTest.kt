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
  // Use mocks to avoid issues with toUri() returning null in unit tests (done by AI)
  private val fakeUri1 = mock(Uri::class.java)
  private val fakeUri2 = mock(Uri::class.java)
  private val fakeLocation1 = Location(Coordinate(0.0, 1.0), "No location1")
  private val fakeLocation2 = Location(Coordinate(0.0, 0.0), "No location2")

  // This location matches the default value in the ViewModel
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
          collaboratorsId = emptyList(),
          uriLocation = fakeUriLocation)

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
    // Initialize ViewModel
    val tripRepository = mock(TripsRepository::class.java)
    val photosViewModel = PhotosViewModel(tripRepository)

    // Mock repository response
    `when`(tripRepository.getTrip(fakeTrip.uid)).thenReturn(fakeTrip)

    // Load photos and verify state
    photosViewModel.loadPhotos(fakeTrip.uid)
    assertEquals(fakeUriLocation, photosViewModel.uiState.value.uriLocation)
    assertEquals(false, photosViewModel.uiState.value.isLoading)
  }

  @Test
  fun `test loadPhotos correctly load the photo`() = runTest {
    // Initialize ViewModel
    val tripRepository = mock(TripsRepository::class.java)
    val photosViewModel = PhotosViewModel(tripRepository)

    val fakeTripNoPhoto = fakeTrip.copy(uriLocation = emptyMap())
    // Mock repository response
    `when`(tripRepository.getTrip(fakeTripNoPhoto.uid)).thenReturn(fakeTripNoPhoto)

    // Load photos and verify state
    photosViewModel.loadPhotos(fakeTripNoPhoto.uid)
    val state = photosViewModel.uiState
    assertEquals(fakeTripNoPhoto.uriLocation, state.value.uriLocation)
    assertEquals(false, state.value.isLoading)
  }

  @Test
  fun `loadPhotos will fail on getTrip for a trip with photos`() = runTest {
    // Initialize ViewModel
    val fakeRepository = mock(TripsRepository::class.java)
    val photosViewModel = PhotosViewModel(fakeRepository)
    `when`(fakeRepository.getTrip(fakeTrip.uid)).thenThrow(RuntimeException("Could not get trip"))

    // Attempt to load photos
    photosViewModel.loadPhotos(fakeTrip.uid)

    // Verify error state
    assertEquals(false, photosViewModel.uiState.value.isLoading)
    assert(photosViewModel.uiState.value.errorLoading)
  }

  // Done by AI
  @Test
  fun `test addUri correctly adds new uri to the list`() = runTest {
    // Initialize ViewModel and Context mock
    val tripRepository = mock(TripsRepository::class.java)
    val photosViewModel = PhotosViewModel(tripRepository)
    val mockContext = mock(Context::class.java)

    // Mock repository response
    `when`(tripRepository.getTrip(fakeTrip.uid)).thenReturn(fakeTrip)

    // Load initial state
    photosViewModel.loadPhotos(fakeTrip.uid)

    val newUri1 = mock(Uri::class.java)
    val newUri2 = mock(Uri::class.java)
    val newUris = listOf(newUri1, newUri2)

    photosViewModel.addUris(newUris, mockContext, fakeTrip.uid)

    // Verify new URIs are appended with default location
    val expectedMap = fakeUriLocation + newUris.associateWith { defaultLocation }

    assertEquals(expectedMap, photosViewModel.uiState.value.uriLocation)
    assertEquals(expectedMap.size, photosViewModel.uiState.value.uriLocation.size)
  }

  // Done by AI
  @Test
  fun `test savePhotos correctly adds new uris to the repository`() = runTest {
    // Initialize ViewModel with local repo
    val fakeRepo = TripRepositoryLocal()
    fakeRepo.addTrip(fakeTrip)
    val photosViewModel = PhotosViewModel(fakeRepo)
    val mockContext = mock(Context::class.java)

    // Load initial state
    photosViewModel.loadPhotos(fakeTrip.uid)

    // Add new mock URIs
    val newUri1 = mock(Uri::class.java)
    val newUri2 = mock(Uri::class.java)
    val newUris = listOf(newUri1, newUri2)

    photosViewModel.addUris(newUris, mockContext, fakeTrip.uid)

    // Trigger save
    photosViewModel.savePhotos(fakeTrip.uid)

    // Verify the repository reflects the merged map
    val expectedMap = fakeUriLocation + newUris.associateWith { defaultLocation }

    assertEquals(expectedMap, fakeRepo.getTrip(fakeTrip.uid).uriLocation)
    assertEquals("Photo saved", photosViewModel.uiState.value.toastMessage)
  }

  // Done by AI
  @Test
  fun `test savePhotos correctly adds new uri to the repository`() = runTest {
    // Initialize ViewModel
    val fakeRepo = TripRepositoryLocal()
    fakeRepo.addTrip(fakeTrip)
    val photosViewModel = PhotosViewModel(fakeRepo)
    val mockContext = mock(Context::class.java)

    // Load initial state
    photosViewModel.loadPhotos(fakeTrip.uid)

    // Add single mock URI
    val newUri = mock(Uri::class.java)
    val newUris = listOf(newUri)

    photosViewModel.addUris(newUris, mockContext, fakeTrip.uid)

    // Trigger save
    photosViewModel.savePhotos(fakeTrip.uid)
    val expectedMap = fakeUriLocation + newUris.associateWith { defaultLocation }

    // Verify
    assertEquals(expectedMap, fakeRepo.getTrip(fakeTrip.uid).uriLocation)
    assertEquals("Photo saved", photosViewModel.uiState.value.toastMessage)
  }
  // Done by AI
  @Test
  fun `savePhotos correctly set the toast message when getTrip throws exception`() = runTest {
    // Initialize ViewModel
    val mockRepo = mock(TripsRepository::class.java)
    val photosViewModel = PhotosViewModel(mockRepo)
    val mockContext = mock(Context::class.java)

    // Prepare new mock URIs
    val newUri1 = mock(Uri::class.java)
    val newUri2 = mock(Uri::class.java)
    val newUris = listOf(newUri1, newUri2)

    photosViewModel.addUris(newUris, mockContext, fakeTrip.uid)

    // Simulate getTrip failure
    `when`(mockRepo.getTrip(fakeTrip.uid)).thenThrow(RuntimeException("Could not get the trip"))

    // Attempt to save
    photosViewModel.savePhotos(fakeTrip.uid)

    // Verify error message
    assertEquals("Could not save the photo", photosViewModel.uiState.value.toastMessage)
  }

  // Done by AI
  @Test
  fun `savePhotos correctly sets toast message when getTrip throws exception with one photo`() =
      runTest {
        // Initialize ViewModel
        val mockRepo = mock(TripsRepository::class.java)
        val photosViewModel = PhotosViewModel(mockRepo)
        val mockContext = mock(Context::class.java)

        // Trip with no photos
        val fakeTripNoPhoto = fakeTrip.copy(uriLocation = emptyMap())

        // Add single mock URI
        val newUri = mock(Uri::class.java)
        val newUris = listOf(newUri)

        // Simulate getTrip failure
        `when`(mockRepo.getTrip(fakeTripNoPhoto.uid))
            .thenThrow(RuntimeException("Could not get the trip"))

        photosViewModel.addUris(newUris, mockContext, fakeTripNoPhoto.uid)

        photosViewModel.savePhotos(fakeTripNoPhoto.uid)

        // Verify error message
        assertEquals("Could not save the photo", photosViewModel.uiState.value.toastMessage)
      }

  @Test
  fun `selectToRemove correctly add index when there is not the index`() = runTest {
    // Initialize ViewModel
    val fakeRepo = mock(TripsRepository::class.java)
    val photosViewModel = PhotosViewModel(fakeRepo)

    // Mock repository
    `when`(fakeRepo.getTrip(fakeTrip.uid)).thenReturn(fakeTrip)

    // Load state
    photosViewModel.loadPhotos(fakeTrip.uid)

    // Select first photo for removal
    photosViewModel.selectToRemove(0)

    // Verify selection
    val expected = listOf(0)
    assertEquals(expected, photosViewModel.uiState.value.uriSelected)
  }

  @Test
  fun `selectToRemove correctly remove index when the index is already there`() = runTest {
    // Initialize ViewModel
    val fakeRepo = mock(TripsRepository::class.java)
    val photosViewModel = PhotosViewModel(fakeRepo)

    // Mock repository
    `when`(fakeRepo.getTrip(fakeTrip.uid)).thenReturn(fakeTrip)

    // Load state
    photosViewModel.loadPhotos(fakeTrip.uid)

    // Select first photo
    photosViewModel.selectToRemove(0)

    // Deselect first photo
    photosViewModel.selectToRemove(0)

    // Verify empty selection
    val expected = emptyList<Int>()
    assertEquals(expected, photosViewModel.uiState.value.uriSelected)
  }

  @Test
  fun `removePhotos correctly remove selected photos to the state`() = runTest {
    // Initialize ViewModel
    val fakeRepo = TripRepositoryLocal()
    fakeRepo.addTrip(fakeTrip)
    val photosViewModel = PhotosViewModel(fakeRepo)

    // Load state
    photosViewModel.loadPhotos(fakeTrip.uid)

    // Mark photos for removal
    photosViewModel.selectToRemove(0)
    photosViewModel.selectToRemove(1)

    // Execute removal
    photosViewModel.removePhotos(fakeTrip.uid)

    // Verify removal
    val expected = emptyMap<Uri, Location>()
    assertEquals(expected, photosViewModel.uiState.value.uriLocation)
    assertEquals(expected, fakeRepo.getTrip(fakeTrip.uid).uriLocation)
    assertEquals("Photos removed", photosViewModel.uiState.value.toastMessage)
  }

  @Test
  fun `removePhotos correctly remove selected photo to the state`() = runTest {
    // Initialize ViewModel
    val fakeRepo = TripRepositoryLocal()
    fakeRepo.addTrip(fakeTrip)
    val photosViewModel = PhotosViewModel(fakeRepo)

    // Load state
    photosViewModel.loadPhotos(fakeTrip.uid)

    // Mark only the second photo for removal
    photosViewModel.selectToRemove(1)

    // Execute removal
    photosViewModel.removePhotos(fakeTrip.uid)

    // Verify first photo remains
    val expected = mapOf(fakeUri1 to fakeLocation1)
    assertEquals(expected, photosViewModel.uiState.value.uriLocation)
    assertEquals(expected, fakeRepo.getTrip(fakeTrip.uid).uriLocation)
    assertEquals("Photo removed", photosViewModel.uiState.value.toastMessage)
  }

  @Test
  fun `removePhotos fails on getTrip case several photos`() = runTest {
    // Initialize ViewModel
    val fakeRepo = mock(TripsRepository::class.java)
    val photosViewModel = PhotosViewModel(fakeRepo)

    // Initial load
    `when`(fakeRepo.getTrip(fakeTrip.uid)).thenReturn(fakeTrip)
    photosViewModel.loadPhotos(fakeTrip.uid)

    // Select photos to remove
    photosViewModel.selectToRemove(0)
    photosViewModel.selectToRemove(1)
    val selected = listOf(0, 1)

    // Attempt removal with simulated failure
    `when`(fakeRepo.getTrip(fakeTrip.uid)).thenThrow(RuntimeException("Could not get trip"))
    photosViewModel.removePhotos(fakeTrip.uid)

    // Verify error state
    val state = photosViewModel.uiState
    assertEquals("Could not remove the photos", state.value.toastMessage)
    assertEquals(selected, state.value.uriSelected)
  }

  @Test
  fun `removePhotos fails on getTrip case one photo`() = runTest {
    // Initialize ViewModel
    val fakeRepo = mock(TripsRepository::class.java)
    val photosViewModel = PhotosViewModel(fakeRepo)

    // Initial load
    `when`(fakeRepo.getTrip(fakeTrip.uid)).thenReturn(fakeTrip)
    photosViewModel.loadPhotos(fakeTrip.uid)

    // Select photo to remove
    photosViewModel.selectToRemove(0)
    val selected = listOf(0)

    // Attempt removal with simulated failure
    `when`(fakeRepo.getTrip(fakeTrip.uid)).thenThrow(RuntimeException("Could not get trip"))
    photosViewModel.removePhotos(fakeTrip.uid)

    // Verify error state
    val state = photosViewModel.uiState
    assertEquals("Could not remove the photo", state.value.toastMessage)
    assertEquals(selected, state.value.uriSelected)
  }
}
