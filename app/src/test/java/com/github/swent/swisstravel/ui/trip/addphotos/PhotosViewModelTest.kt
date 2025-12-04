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
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

/** Unit tests for AddPhotosViewModel */
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
          listUri = fakeUris)

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `test loadPhotos correctly load the photos`() = runTest {
    try {
      // Set correctly the dispatcher
      val testDispatcher = UnconfinedTestDispatcher(testScheduler)
      Dispatchers.setMain(testDispatcher)

      // Set the model
      val tripRepository = mock(TripsRepository::class.java)
      val photosViewModel = PhotosViewModel(tripRepository)

      // Set up the mock to return the fake trip
      `when`(tripRepository.getTrip("0")).thenReturn(fakeTrip)

      // Load photos and verify that it is correctly done
      photosViewModel.loadPhotos("0")
      assertEquals(fakeUris, photosViewModel.uiState.value.listUri)
    } finally {
      Dispatchers.resetMain()
    }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `test addUri correctly adds new uri to the list`() = runTest {
    try {
      // Set correctly the dispatcher
      val testDispatcher = UnconfinedTestDispatcher(testScheduler)
      Dispatchers.setMain(testDispatcher)

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
    } finally {
      Dispatchers.resetMain()
    }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `test saveUri correctly adds new uri to the repository`() = runTest {
    try {

      // Set the dispatcher
      val testDispatcher = UnconfinedTestDispatcher(testScheduler)
      Dispatchers.setMain(testDispatcher)

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
    } finally {
      Dispatchers.resetMain()
    }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `selectToRemove correctly add index when there is not the index`() = runTest {
    try {

      // Set the dispatcher
      val testDispatcher = UnconfinedTestDispatcher(testScheduler)
      Dispatchers.setMain(testDispatcher)

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
    } finally {
      Dispatchers.resetMain()
    }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `selectToRemove correctly remove index when the index is already there`() = runTest {
    try {

      // Set the dispatcher
      val testDispatcher = UnconfinedTestDispatcher(testScheduler)
      Dispatchers.setMain(testDispatcher)

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
    } finally {
      Dispatchers.resetMain()
    }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `removePhotos correctly remove selected photos to the state`() = runTest {
    try {

      // Set the dispatcher
      val testDispatcher = UnconfinedTestDispatcher(testScheduler)
      Dispatchers.setMain(testDispatcher)

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
    } finally {
      Dispatchers.resetMain()
    }
  }
}
