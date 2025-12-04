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

  private val currentUser =
      User(
          uid = "mahitoH8er999",
          name = "Itadori Yuji",
          email = "poop@poop.com",
          biography = "I hate curses",
          profilePicUrl = "http://pic.url",
          preferences = emptyList(),
          friends = emptyList(),
          stats = UserStats(),
          pinnedTripsUids = emptyList(),
          pinnedPicturesUris = emptyList())

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    userRepository = mockk()
    tripsRepository = mockk(relaxed = true)

    // Default mocks
    coEvery { userRepository.getCurrentUser() } returns currentUser
    coEvery { userRepository.getUserByUid(any()) } returns currentUser
    coEvery { userRepository.updateUserStats(any(), any()) } just Runs
    coEvery { userRepository.removeFriend(any(), any()) } just Runs
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  // Profile Loading
  @Test
  fun initLoadsProfileSuccessfully() = runTest {
    viewModel = ProfileViewModel(userRepository, tripsRepository, currentUser.uid)
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    Assert.assertEquals(currentUser.uid, state.uid)
    Assert.assertEquals(currentUser.name, state.name)
    Assert.assertEquals(currentUser.biography, state.biography)
    Assert.assertEquals(currentUser.profilePicUrl, state.profilePicUrl)
    Assert.assertTrue(state.isOwnProfile)
    Assert.assertNull(state.errorMsg)
  }

  @Test
  fun initWithBlankUidSetsErrorMessage() = runTest {
    viewModel = ProfileViewModel(userRepository, tripsRepository, "")
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    Assert.assertEquals("User ID is invalid", state.errorMsg)
  }

  @Test
  fun initWithNonExistingUserSetsErrorMessage() = runTest {
    coEvery { userRepository.getUserByUid("nonexistent") } returns null

    viewModel = ProfileViewModel(userRepository, tripsRepository, "nonexistent")
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    Assert.assertTrue(state.errorMsg?.contains("User with uid nonexistent not found") == true)
  }

  // Stats Refresh (indirect via init)
  @Test
  fun initCallsUpdateUserStatsForOwnProfile() = runTest {
    viewModel = ProfileViewModel(userRepository, tripsRepository, currentUser.uid)
    testDispatcher.scheduler.advanceUntilIdle()

    coVerify { userRepository.updateUserStats(currentUser.uid, any()) }
  }

  @Test
  fun initDoesNotCallUpdateUserStatsForOtherProfile() = runTest {
    val otherUser = currentUser.copy(uid = "otherUid")
    coEvery { userRepository.getUserByUid("otherUid") } returns otherUser

    viewModel = ProfileViewModel(userRepository, tripsRepository, "otherUid")
    testDispatcher.scheduler.advanceUntilIdle()

    coVerify(exactly = 0) { userRepository.updateUserStats(any(), any()) }
  }

  // Error Message
  @Test
  fun setErrorMsgAndClearErrorMsgWorkCorrectly() = runTest {
    viewModel = ProfileViewModel(userRepository, tripsRepository, currentUser.uid)

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
        currentUser.copy(
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
