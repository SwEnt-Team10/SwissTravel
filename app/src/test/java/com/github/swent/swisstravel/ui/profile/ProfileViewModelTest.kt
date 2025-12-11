package com.github.swent.swisstravel.ui.profile

import android.graphics.Bitmap
import com.github.swent.swisstravel.model.image.Image
import com.github.swent.swisstravel.model.image.ImageHelper
import com.github.swent.swisstravel.model.image.ImageRepository
import com.github.swent.swisstravel.model.trip.TripsRepository
import com.github.swent.swisstravel.model.user.User
import com.github.swent.swisstravel.model.user.UserRepository
import com.github.swent.swisstravel.model.user.UserStats
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

/**
 * A test class for the [ProfileViewModel].
 *
 * These tests were made with the help of AI.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

  private val testDispatcher = StandardTestDispatcher()
  private lateinit var userRepository: UserRepository
  private lateinit var tripsRepository: TripsRepository
  private lateinit var imageRepository: ImageRepository
  private lateinit var viewModel: ProfileViewModel

  private val fakeUser =
      User(
          uid = "mahito444",
          name = "Itadori Yuji",
          email = "hnd@shrine.jp",
          biography = "I dislike curses",
          profilePicUrl = "http://pic.url",
          preferences = emptyList(),
          friends = emptyList(),
          stats = UserStats(),
          pinnedTripsUids = emptyList(),
          pinnedPicturesUids = emptyList(),
          favoriteTripsUids = emptyList())

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    userRepository = mockk()
    tripsRepository = mockk(relaxed = true)
    imageRepository = mockk()

    mockkObject(ImageHelper)

    coEvery { userRepository.updateUserStats(any(), any()) } just Runs
  }

  @After
  fun tearDown() {
    unmockkAll()
    Dispatchers.resetMain()
  }

  @Test
  fun initLoadsProfileSuccessfully() = runTest {
    coEvery { userRepository.getCurrentUser() } returns fakeUser
    coEvery { userRepository.getUserByUid(fakeUser.uid) } returns fakeUser

    viewModel = ProfileViewModel(userRepository, tripsRepository, imageRepository, fakeUser.uid)
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    Assert.assertEquals(fakeUser.uid, state.uid)
    Assert.assertEquals(fakeUser.name, state.name)
    Assert.assertTrue(state.isOwnProfile)
    Assert.assertNull(state.errorMsg)
  }

  @Test
  fun refreshStats_doesNothingWhenOffline() = runTest {
    coEvery { userRepository.getCurrentUser() } returns fakeUser
    coEvery { userRepository.getUserByUid(fakeUser.uid) } returns fakeUser

    viewModel = ProfileViewModel(userRepository, tripsRepository, imageRepository, fakeUser.uid)
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.refreshStats(isOnline = false)
    testDispatcher.scheduler.advanceUntilIdle()

    coVerify(exactly = 0) { userRepository.updateUserStats(any(), any()) }
  }

  @Test
  fun refreshStats_updatesStatsWhenOnlineAndOwnProfile() = runTest {
    coEvery { userRepository.getCurrentUser() } returns fakeUser
    coEvery { userRepository.getUserByUid(fakeUser.uid) } returns fakeUser
    coEvery { tripsRepository.getAllTrips() } returns emptyList()

    viewModel = ProfileViewModel(userRepository, tripsRepository, imageRepository, fakeUser.uid)
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.refreshStats(isOnline = true)
    testDispatcher.scheduler.advanceUntilIdle()

    coVerify(exactly = 1) { userRepository.updateUserStats(fakeUser.uid, any()) }
  }

  @Test
  fun refreshStats_doesNotUpdateStatsWhenNotOwnProfile() = runTest {
    val otherUserUid = "456"
    val otherUser = fakeUser.copy(uid = otherUserUid)
    coEvery { userRepository.getCurrentUser() } returns fakeUser
    coEvery { userRepository.getUserByUid(otherUserUid) } returns otherUser

    viewModel = ProfileViewModel(userRepository, tripsRepository, imageRepository, otherUserUid)
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.refreshStats(isOnline = true)
    testDispatcher.scheduler.advanceUntilIdle()

    coVerify(exactly = 0) { userRepository.updateUserStats(any(), any()) }
  }

  @Test
  fun setErrorMsgAndClearErrorMsgWorkCorrectly() = runTest {
    viewModel = ProfileViewModel(userRepository, tripsRepository, imageRepository, fakeUser.uid)

    viewModel.setErrorMsg("Test Error")
    var state = viewModel.uiState.value
    Assert.assertEquals("Test Error", state.errorMsg)

    viewModel.clearErrorMsg()
    state = viewModel.uiState.value
    Assert.assertNull(state.errorMsg)
  }

  @Test
  fun viewingOtherUsersProfileLoadsCorrectlyAndIsOwnProfileIsFalse() = runTest {
    // Given: another existing user
    val otherUser =
        fakeUser.copy(
            uid = "bumAndFraud",
            name = "Fushiguro Megumi",
            biography = "I like dogs and being a bum.",
            email = "mahoraga@saveme.com",
            profilePicUrl = "http://bum.url")

    coEvery { userRepository.getUserByUid("bumAndFraud") } returns otherUser
    coEvery { userRepository.getCurrentUser() } returns fakeUser

    // When: loading another user's profile
    viewModel = ProfileViewModel(userRepository, tripsRepository, imageRepository, "bumAndFraud")
    testDispatcher.scheduler.advanceUntilIdle()

    // Then: UI state loads their data, but isOwnProfile is false
    val state = viewModel.uiState.value

    Assert.assertEquals(otherUser.uid, state.uid)
    Assert.assertEquals(otherUser.name, state.name)
    Assert.assertEquals(otherUser.biography, state.biography)
    Assert.assertEquals(otherUser.profilePicUrl, state.profilePicUrl)

    Assert.assertFalse(state.isOwnProfile)
    Assert.assertNull(state.errorMsg)
  }

  @Test
  fun initRemovesPinnedTripsThatNoLongerExistInTripRepository() = runTest {
    coEvery { userRepository.getCurrentUser() } returns fakeUser
    coEvery { userRepository.getUserByUid(fakeUser.uid) } returns fakeUser
    // Given: user has two pinned trip UIDs
    val validTripUid = "trip123"
    val deletedTripUid = "tripDeleted"

    val userWithPinnedTrips = fakeUser.copy(pinnedTripsUids = listOf(validTripUid, deletedTripUid))

    coEvery { userRepository.getUserByUid(fakeUser.uid) } returns userWithPinnedTrips
    // TripsRepository returns a trip for existingTripUid, throws for deletedTripUid
    coEvery { tripsRepository.getTrip(validTripUid) } returns mockk(relaxed = true)
    coEvery { tripsRepository.getTrip(deletedTripUid) } throws Exception("Trip not found")

    coEvery { userRepository.updateUser(uid = fakeUser.uid, pinnedTripsUids = any()) } just Runs
    // When: initializing the ViewModel
    viewModel = ProfileViewModel(userRepository, tripsRepository, imageRepository, fakeUser.uid)
    testDispatcher.scheduler.advanceUntilIdle()

    // And: UI state pinnedTrips contains only the existing trip
    val state = viewModel.uiState.value
    Assert.assertEquals(1, state.pinnedTrips.size)
  }

  @Test
  fun fetchPinnedPictures_loadsValidImagesSuccessfully() = runTest {
    // Given: A user with one pinned picture UID
    val picUid = "pic-123"
    val base64String = "fakeBase64"
    val userWithPic = fakeUser.copy(pinnedPicturesUids = listOf(picUid))
    val mockBitmap = mockk<Bitmap>()

    coEvery { userRepository.getCurrentUser() } returns userWithPic
    coEvery { userRepository.getUserByUid(fakeUser.uid) } returns userWithPic

    coEvery { imageRepository.getImage(picUid) } returns Image(picUid, fakeUser.uid, base64String)

    coEvery { ImageHelper.base64ToBitmap(base64String) } returns mockBitmap

    // When: ViewModel is initialized
    viewModel = ProfileViewModel(userRepository, tripsRepository, imageRepository, fakeUser.uid)
    testDispatcher.scheduler.advanceUntilIdle()

    // Then: The UI state should contain the bitmap
    val state = viewModel.uiState.value
    Assert.assertEquals(1, state.pinnedBitmaps.size)
    Assert.assertEquals(mockBitmap, state.pinnedBitmaps[0])
    Assert.assertFalse(state.isLoadingImages)
  }

  @Test
  fun fetchPinnedPictures_removesInvalidUidsAndUpdatesUser() = runTest {
    // Given: User has one VALID uid and one INVALID uid
    val validUid = "valid-uid"
    val invalidUid = "invalid-uid"
    val userWithPics = fakeUser.copy(pinnedPicturesUids = listOf(validUid, invalidUid))
    val validBase64 = "validBase64"
    val mockBitmap = mockk<Bitmap>()

    coEvery { userRepository.getCurrentUser() } returns userWithPics
    coEvery { userRepository.getUserByUid(fakeUser.uid) } returns userWithPics

    // 1. Valid image returns successfully
    coEvery { imageRepository.getImage(validUid) } returns
        Image(validUid, fakeUser.uid, validBase64)
    // 2. Invalid image throws an exception (simulating 404/not found)
    coEvery { imageRepository.getImage(invalidUid) } throws Exception("Image not found")

    coEvery { ImageHelper.base64ToBitmap(validBase64) } returns mockBitmap

    // Mock User Update (should be called to cleanup the invalid ID)
    coEvery { userRepository.updateUser(uid = any(), pinnedPicturesUids = any()) } just Runs

    // When: ViewModel is initialized
    viewModel = ProfileViewModel(userRepository, tripsRepository, imageRepository, fakeUser.uid)
    testDispatcher.scheduler.advanceUntilIdle()

    // Then 1: UI should only show the 1 valid bitmap
    val state = viewModel.uiState.value
    Assert.assertEquals(1, state.pinnedBitmaps.size)
    Assert.assertEquals(mockBitmap, state.pinnedBitmaps[0])

    // Then 2: updateUser should be called with only the valid UID
    coVerify {
      userRepository.updateUser(
          uid = fakeUser.uid,
          pinnedPicturesUids = listOf(validUid) // 'invalidUid' should be removed
          )
    }
  }
}
