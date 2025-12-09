package com.github.swent.swisstravel.ui.trip.addphotos

import android.net.Uri
import androidx.core.net.toUri
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
  private val fakeUris = listOf("uri1".toUri(), "uri2".toUri())
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
          listUri = fakeUris,
          collaboratorsId = emptyList())

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
    `when`(tripRepository.getTrip("0")).thenReturn(fakeTrip)

    // Load photos and verify that it is correctly done
    photosViewModel.loadPhotos("0")
    assertEquals(fakeUris, photosViewModel.uiState.value.listUri)
    assertEquals(false, photosViewModel.uiState.value.isLoading)
    assertEquals("Successfully loaded the photos", photosViewModel.uiState.value.toastMessage)
  }

  @Test
  fun `test loadPhotos correctly load the photo`() = runTest {

    // Set the model
    val tripRepository = mock(TripsRepository::class.java)
    val photosViewModel = PhotosViewModel(tripRepository)

    val fakeTripNoPhoto = fakeTrip.copy(listUri = emptyList())
    // Set up the mock to return the fake trip
    `when`(tripRepository.getTrip(fakeTripNoPhoto.uid)).thenReturn(fakeTripNoPhoto)

    // Load photos and verify that it is correctly done
    photosViewModel.loadPhotos(fakeTripNoPhoto.uid)
    val state = photosViewModel.uiState
    assertEquals(fakeTripNoPhoto.listUri, state.value.listUri)
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

    // Set up the mock to return the fake trip
    `when`(tripRepository.getTrip("0")).thenReturn(fakeTrip)

    // Load the state
    photosViewModel.loadPhotos("0")

    // Add new photos to the state
    val newUris = listOf("newUri1".toUri(), "newUri2".toUri())
    photosViewModel.addUris(newUris)

    // Verify that the new list of Uris is the concatenation of the
    val expectedList = fakeUris + newUris
    assertEquals(expectedList, photosViewModel.uiState.value.listUri)
    assertEquals(newUris.size + fakeTrip.listUri.size, photosViewModel.uiState.value.listUri.size)
  }

  @Test
  fun `test savePhotos correctly adds new uris to the repository`() = runTest {

    // Set the model
    val fakeRepo = TripRepositoryLocal()
    fakeRepo.addTrip(fakeTrip)
    val photosViewModel = PhotosViewModel(fakeRepo)

    // Load the state
    photosViewModel.loadPhotos("1")

    // Add new photos to the state
    val newUris = listOf("newUri1".toUri(), "newUri2".toUri())
    photosViewModel.addUris(newUris)

    // Save to the repository
    photosViewModel.savePhotos("1")
    val expectedList = fakeUris + newUris

    // Verify
    assertEquals(expectedList, fakeRepo.getTrip("1").listUri)
    assertEquals("Photos saved", photosViewModel.uiState.value.toastMessage)
  }

  @Test
  fun `test savePhotos correctly adds new uri to the repository`() = runTest {

    // Set the model
    val fakeRepo = TripRepositoryLocal()
    fakeRepo.addTrip(fakeTrip)
    val photosViewModel = PhotosViewModel(fakeRepo)

    // Load the state
    photosViewModel.loadPhotos("1")

    // Add new photos to the state
    val newUris = listOf("newUri1".toUri())
    photosViewModel.addUris(newUris)

    // Save to the repository
    photosViewModel.savePhotos("1")
    val expectedList = fakeUris + newUris

    // Verify
    assertEquals(expectedList, fakeRepo.getTrip("1").listUri)
    assertEquals("Photo saved", photosViewModel.uiState.value.toastMessage)
  }

  @Test
  fun `savePhotos correctly set the toast message when getTrip throws exception`() = runTest {
    // Set up
    val mockRepo = mock(TripsRepository::class.java)
    val photosViewModel = PhotosViewModel(mockRepo)

    // New Uris
    photosViewModel.addUris(listOf("newUri1".toUri(), "newUri2".toUri()))

    // Get trip will fail
    `when`(mockRepo.getTrip("0")).thenThrow(RuntimeException("Could not get the trip"))

    // Try to save
    photosViewModel.savePhotos("0")

    // Verify
    assertEquals("Could not save the photos", photosViewModel.uiState.value.toastMessage)
  }

  @Test
  fun `savePhotos correctly sets toast message when getTrip throws exception with one photo`() =
      runTest {

        // Set up
        val mockRepo = mock(TripsRepository::class.java)
        val photosViewModel = PhotosViewModel(mockRepo)

        // The trip has no photo
        val fakeTripNoPhoto = fakeTrip.copy(listUri = emptyList())

        // Add one uri
        val newUri = listOf("newUri1".toUri())

        // getTrip will fail
        `when`(mockRepo.getTrip(fakeTripNoPhoto.uid))
            .thenThrow(RuntimeException("Could not get the trip"))

        photosViewModel.addUris(newUri)

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
    `when`(fakeRepo.getTrip("0")).thenReturn(fakeTrip)

    // Load the state
    photosViewModel.loadPhotos("0")

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
    `when`(fakeRepo.getTrip("0")).thenReturn(fakeTrip)

    // Load the state
    photosViewModel.loadPhotos("0")

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
    photosViewModel.loadPhotos("1")

    // Add photos to remove
    photosViewModel.selectToRemove(0)
    photosViewModel.selectToRemove(1)

    // Remove photos
    photosViewModel.removePhotos("1")

    // Verify
    val expected = emptyList<Uri>()
    assertEquals(expected, photosViewModel.uiState.value.listUri)
    assertEquals(expected, fakeRepo.getTrip("1").listUri)
    assertEquals("Photos removed", photosViewModel.uiState.value.toastMessage)
  }

  @Test
  fun `removePhotos correctly remove selected photo to the state`() = runTest {

    // Set the model
    val fakeRepo = TripRepositoryLocal()
    fakeRepo.addTrip(fakeTrip)
    val photosViewModel = PhotosViewModel(fakeRepo)

    // Load the state
    photosViewModel.loadPhotos("1")

    // Add photos to remove
    photosViewModel.selectToRemove(1)

    // Remove photos
    photosViewModel.removePhotos("1")

    // Verify
    val expected = listOf(fakeTrip.listUri[0])
    assertEquals(expected, photosViewModel.uiState.value.listUri)
    assertEquals(expected, fakeRepo.getTrip("1").listUri)
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
