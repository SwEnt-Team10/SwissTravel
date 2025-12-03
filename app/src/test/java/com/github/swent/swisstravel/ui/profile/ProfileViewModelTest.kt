package com.github.swent.swisstravel.ui.profile

import com.github.swent.swisstravel.model.trip.TripsRepository
import com.github.swent.swisstravel.model.user.User
import com.github.swent.swisstravel.model.user.UserRepository
import com.github.swent.swisstravel.model.user.UserStats
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
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
  private lateinit var viewModel: ProfileViewModel

  private val fakeUser =
      User(
          uid = "123",
          name = "Test User",
          biography = "Bio",
          email = "test@example.com",
          profilePicUrl = "http://example.com/pic.jpg",
          preferences = emptyList(),
          friends = emptyList(),
          stats = UserStats(),
          pinnedTripsUids = emptyList(),
          pinnedImagesUris = emptyList())

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    userRepository = mockk()
    tripsRepository = mockk(relaxed = true)

    coEvery { userRepository.updateUserStats(any(), any()) } just Runs
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun initLoadsProfileSuccessfully() = runTest {
    coEvery { userRepository.getCurrentUser() } returns fakeUser
    coEvery { userRepository.getUserByUid(fakeUser.uid) } returns fakeUser

    viewModel = ProfileViewModel(userRepository, tripsRepository, fakeUser.uid)
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

    viewModel = ProfileViewModel(userRepository, tripsRepository, fakeUser.uid)
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

    viewModel = ProfileViewModel(userRepository, tripsRepository, fakeUser.uid)
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

    viewModel = ProfileViewModel(userRepository, tripsRepository, otherUserUid)
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.refreshStats(isOnline = true)
    testDispatcher.scheduler.advanceUntilIdle()

    coVerify(exactly = 0) { userRepository.updateUserStats(any(), any()) }
  }

  @Test
  fun setErrorMsgAndClearErrorMsgWorkCorrectly() = runTest {
    viewModel = ProfileViewModel(userRepository, tripsRepository, fakeUser.uid)

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

    // When: loading another user's profile
    viewModel = ProfileViewModel(userRepository, tripsRepository, "bumAndFraud")
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
}
