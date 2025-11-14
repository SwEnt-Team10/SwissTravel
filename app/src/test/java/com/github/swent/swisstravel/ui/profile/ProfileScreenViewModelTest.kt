package com.github.swent.swisstravel.ui.profile

import com.github.swent.swisstravel.model.user.Preference
import com.github.swent.swisstravel.model.user.User
import com.github.swent.swisstravel.model.user.UserRepositoryFirebase
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

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileScreenViewModelTest {

  private val testDispatcher = StandardTestDispatcher()
  private lateinit var repository: UserRepositoryFirebase
  private lateinit var viewModel: ProfileScreenViewModel

  private val fakeUser =
      User(
          uid = "123",
          name = "Lionel Hegetschweiler",
          email = "lionel@example.com",
          profilePicUrl = "http://example.com/pic.jpg",
          preferences = listOf(Preference.HIKE, Preference.FOODIE))

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    repository = mockk()
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun initLoadsUserDataSuccessfully() = runTest {
    coEvery { repository.getCurrentUser() } returns fakeUser

    viewModel = ProfileScreenViewModel(repository)
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    Assert.assertEquals(fakeUser.name, state.name)
    Assert.assertEquals(fakeUser.email, state.email)
    Assert.assertEquals(fakeUser.profilePicUrl, state.profilePicUrl)
    Assert.assertEquals(fakeUser.preferences, state.selectedPreferences)
    Assert.assertNull(state.errorMsg)
  }

  @Test
  fun initSetsErrorMessageOnFailure() = runTest {
    coEvery { repository.getCurrentUser() } throws Exception("Network error")

    viewModel = ProfileScreenViewModel(repository)
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    Assert.assertTrue(state.errorMsg!!.contains("Network error"))
  }

  @Test
  fun autoFillUpdatesUiStateCorrectly() = runTest {
    coEvery { repository.getCurrentUser() } returns fakeUser
    viewModel = ProfileScreenViewModel(repository)

    viewModel.autoFill(fakeUser)

    val state = viewModel.uiState.value
    Assert.assertEquals(fakeUser.email, state.email)
    Assert.assertEquals(fakeUser.preferences, state.selectedPreferences)
  }

  @Test
  fun clearErrorMsgSetsErrorMsgToNull() = runTest {
    coEvery { repository.getCurrentUser() } throws Exception("Oops")
    viewModel = ProfileScreenViewModel(repository)
    testDispatcher.scheduler.advanceUntilIdle()

    Assert.assertNotNull(viewModel.uiState.value.errorMsg)
    viewModel.clearErrorMsg()
    Assert.assertNull(viewModel.uiState.value.errorMsg)
  }

  @Test
  fun savePreferencesUpdatesRepositoryAndUiState() = runTest {
    val updatedPrefs = listOf(Preference.MUSEUMS, Preference.FOODIE)
    coEvery { repository.getCurrentUser() } returns fakeUser
    coEvery { repository.updateUserPreferences(fakeUser.uid, updatedPrefs) } just Runs

    viewModel = ProfileScreenViewModel(repository)
    testDispatcher.scheduler.advanceUntilIdle()
    viewModel.savePreferences(updatedPrefs)
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    Assert.assertEquals(updatedPrefs, state.selectedPreferences)
    coVerify(exactly = 1) { repository.updateUserPreferences(fakeUser.uid, updatedPrefs) }
  }

  @Test
  fun savePreferencesSetsErrorMsgWhenUpdateFails() = runTest {
    val updatedPrefs = listOf(Preference.HIKE)
    coEvery { repository.getCurrentUser() } returns fakeUser
    coEvery { repository.updateUserPreferences(any(), any()) } throws Exception("Firestore error")

    viewModel = ProfileScreenViewModel(repository)
    testDispatcher.scheduler.advanceUntilIdle()
    viewModel.savePreferences(updatedPrefs)
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    Assert.assertTrue(state.errorMsg!!.contains("Firestore error"))
  }

  @Test
  fun savePreferences_showsErrorWhenUserIsNull() = runTest {
    // Arrange — repository throws so currentUser remains null
    coEvery { repository.getCurrentUser() } throws Exception("No user")

    viewModel = ProfileScreenViewModel(repository)
    testDispatcher.scheduler.advanceUntilIdle()

    // TripActivity — try saving preferences with no user
    viewModel.savePreferences(listOf(Preference.SCENIC_VIEWS))
    testDispatcher.scheduler.advanceUntilIdle()

    // Assert
    val state = viewModel.uiState.value
    Assert.assertEquals("You must be signed in to save preferences.", state.errorMsg)
  }

  @Test
  fun savePreferences_showsErrorWhenUserIsGuest() = runTest {
    // Arrange — simulate guest user
    val guestUser =
        User(
            uid = "guest",
            name = "Guest",
            email = "Not signed in",
            profilePicUrl = "",
            preferences = emptyList())
    coEvery { repository.getCurrentUser() } returns guestUser

    viewModel = ProfileScreenViewModel(repository)
    testDispatcher.scheduler.advanceUntilIdle()

    // TripActivity
    viewModel.savePreferences(listOf(Preference.HIKE))
    testDispatcher.scheduler.advanceUntilIdle()

    // Assert
    val state = viewModel.uiState.value
    Assert.assertEquals("You must be signed in to save preferences.", state.errorMsg)
  }
}
