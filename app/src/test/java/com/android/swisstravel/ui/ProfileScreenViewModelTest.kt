package com.android.swisstravel.ui

import com.github.swent.swisstravel.model.user.*
import com.github.swent.swisstravel.ui.ProfileScreenViewModel
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileScreenViewModelTest {

  private val testDispatcher = StandardTestDispatcher()
  private lateinit var repository: UserRepository
  private lateinit var viewModel: ProfileScreenViewModel

  private val fakeUser =
      User(
          uid = "123",
          name = "Lionel Hegetschweiler",
          email = "lionel@example.com",
          profilePicUrl = "http://example.com/pic.jpg",
          preferences = listOf(UserPreference.HIKING, UserPreference.FOODIE))

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
    assertEquals(fakeUser.name, state.name)
    assertEquals(fakeUser.email, state.email)
    assertEquals(fakeUser.profilePicUrl, state.profilePicUrl)
    assertEquals(fakeUser.preferences.map { it.displayString() }, state.selectedPreferences)
    assertNull(state.errorMsg)
  }

  @Test
  fun initSetsErrorMessageOnFailure() = runTest {
    coEvery { repository.getCurrentUser() } throws Exception("Network error")

    viewModel = ProfileScreenViewModel(repository)
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertTrue(state.errorMsg!!.contains("Network error"))
  }

  @Test
  fun autoFillUpdatesUiStateCorrectly() = runTest {
    coEvery { repository.getCurrentUser() } returns fakeUser
    viewModel = ProfileScreenViewModel(repository)

    viewModel.autoFill(fakeUser)

    val state = viewModel.uiState.value
    assertEquals(fakeUser.email, state.email)
    assertEquals(fakeUser.preferences.map { it.displayString() }, state.selectedPreferences)
  }

  @Test
  fun clearErrorMsgSetsErrorMsgToNull() = runTest {
    coEvery { repository.getCurrentUser() } throws Exception("Oops")
    viewModel = ProfileScreenViewModel(repository)
    testDispatcher.scheduler.advanceUntilIdle()

    assertNotNull(viewModel.uiState.value.errorMsg)
    viewModel.clearErrorMsg()
    assertNull(viewModel.uiState.value.errorMsg)
  }

  @Test
  fun savePreferencesUpdatesRepositoryAndUiState() = runTest {
    val updatedPrefs = listOf("Museums", "Food & Culinary Experiences")
    coEvery { repository.getCurrentUser() } returns fakeUser
    coEvery { repository.updateUserPreferences(fakeUser.uid, updatedPrefs) } just Runs

    viewModel = ProfileScreenViewModel(repository)
    testDispatcher.scheduler.advanceUntilIdle()
    viewModel.savePreferences(updatedPrefs)
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals(updatedPrefs, state.selectedPreferences)
    coVerify(exactly = 1) { repository.updateUserPreferences(fakeUser.uid, updatedPrefs) }
  }

  @Test
  fun savePreferencesSetsErrorMsgWhenUpdateFails() = runTest {
    val updatedPrefs = listOf("Hiking & Outdoor")
    coEvery { repository.getCurrentUser() } returns fakeUser
    coEvery { repository.updateUserPreferences(any(), any()) } throws Exception("Firestore error")

    viewModel = ProfileScreenViewModel(repository)
    testDispatcher.scheduler.advanceUntilIdle()
    viewModel.savePreferences(updatedPrefs)
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertTrue(state.errorMsg!!.contains("Firestore error"))
  }
}
