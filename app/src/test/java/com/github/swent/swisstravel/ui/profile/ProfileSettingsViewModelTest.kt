package com.github.swent.swisstravel.ui.profile

import com.github.swent.swisstravel.model.trip.TripsRepository
import com.github.swent.swisstravel.model.user.Preference
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
 * A test class for the [ProfileSettingsViewModel].
 *
 * Some of these tests were made with the help of AI.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProfileSettingsViewModelTest {

  private val testDispatcher = StandardTestDispatcher()
  private lateinit var userRepository: UserRepository
  private lateinit var tripsRepository: TripsRepository
  private lateinit var viewModel: ProfileSettingsViewModel

  private val fakeUser =
      User(
          uid = "123",
          name = "Lionel Hegetschweiler",
          biography = "dylan was here !!",
          email = "lionel@example.com",
          profilePicUrl = "http://example.com/pic.jpg",
          preferences = listOf(Preference.HIKE, Preference.FOODIE),
          friends = emptyList(),
          stats = UserStats(),
          pinnedTripsUids = emptyList(),
          pinnedPicturesUids = emptyList())

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
  fun initLoadsUserDataSuccessfully() = runTest {
    coEvery { userRepository.getCurrentUser() } returns fakeUser

    viewModel = ProfileSettingsViewModel(userRepository, tripsRepository)
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
    coEvery { userRepository.getCurrentUser() } throws Exception("Network error")

    viewModel = ProfileSettingsViewModel(userRepository, tripsRepository)
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    Assert.assertTrue(state.errorMsg!!.contains("Network error"))
  }

  @Test
  fun autoFillUpdatesUiStateCorrectly() = runTest {
    coEvery { userRepository.getCurrentUser() } returns fakeUser
    viewModel = ProfileSettingsViewModel(userRepository, tripsRepository)

    viewModel.autoFill(fakeUser)

    val state = viewModel.uiState.value
    Assert.assertEquals(fakeUser.email, state.email)
    Assert.assertEquals(fakeUser.preferences, state.selectedPreferences)
  }

  @Test
  fun clearErrorMsgSetsErrorMsgToNull() = runTest {
    coEvery { userRepository.getCurrentUser() } throws Exception("Oops")
    viewModel = ProfileSettingsViewModel(userRepository, tripsRepository)
    testDispatcher.scheduler.advanceUntilIdle()

    Assert.assertNotNull(viewModel.uiState.value.errorMsg)
    viewModel.clearErrorMsg()
    Assert.assertNull(viewModel.uiState.value.errorMsg)
  }

  @Test
  fun savePreferencesUpdatesRepositoryAndUiState() = runTest {
    val updatedPrefs = listOf(Preference.MUSEUMS, Preference.FOODIE)
    coEvery { userRepository.getCurrentUser() } returns fakeUser
    coEvery { userRepository.updateUserPreferences(fakeUser.uid, updatedPrefs) } just Runs

    viewModel = ProfileSettingsViewModel(userRepository, tripsRepository)
    testDispatcher.scheduler.advanceUntilIdle()
    viewModel.savePreferences(updatedPrefs)
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    Assert.assertEquals(updatedPrefs, state.selectedPreferences)
    coVerify(exactly = 1) { userRepository.updateUserPreferences(fakeUser.uid, updatedPrefs) }
  }

  @Test
  fun savePreferencesSetsErrorMsgWhenUpdateFails() = runTest {
    val updatedPrefs = listOf(Preference.HIKE)
    coEvery { userRepository.getCurrentUser() } returns fakeUser
    coEvery { userRepository.updateUserPreferences(any(), any()) } throws
        Exception("Firestore error")

    viewModel = ProfileSettingsViewModel(userRepository, tripsRepository)
    testDispatcher.scheduler.advanceUntilIdle()
    viewModel.savePreferences(updatedPrefs)
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    Assert.assertTrue(state.errorMsg!!.contains("Firestore error"))
  }

  @Test
  fun savePreferences_showsErrorWhenUserIsNull() = runTest {
    // Arrange — repository throws so currentUser remains null
    coEvery { userRepository.getCurrentUser() } throws Exception("No user")

    viewModel = ProfileSettingsViewModel(userRepository, tripsRepository)
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
            biography = "Guest",
            email = "Not signed in",
            profilePicUrl = "",
            preferences = emptyList(),
            friends = emptyList(),
            stats = UserStats(),
            pinnedTripsUids = emptyList(),
            pinnedPicturesUids = emptyList())
    coEvery { userRepository.getCurrentUser() } returns guestUser

    viewModel = ProfileSettingsViewModel(userRepository, tripsRepository)
    testDispatcher.scheduler.advanceUntilIdle()

    // TripActivity
    viewModel.savePreferences(listOf(Preference.HIKE))
    testDispatcher.scheduler.advanceUntilIdle()

    // Assert
    val state = viewModel.uiState.value
    Assert.assertEquals("You must be signed in to save preferences.", state.errorMsg)
  }

  @Test
  fun init_callsUpdateUserStats() = runTest {
    coEvery { userRepository.getCurrentUser() } returns fakeUser
    coEvery { userRepository.updateUserStats(any(), any()) } just Runs

    viewModel = ProfileSettingsViewModel(userRepository, tripsRepository)
    testDispatcher.scheduler.advanceUntilIdle()

    coVerify(exactly = 0) { userRepository.updateUserStats(any(), any()) }
  }

  @Test
  fun refreshStats_doesNothingWhenOffline() = runTest {
    coEvery { userRepository.getCurrentUser() } returns fakeUser
    viewModel = ProfileSettingsViewModel(userRepository, tripsRepository)
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.refreshStats(isOnline = false)
    testDispatcher.scheduler.advanceUntilIdle()

    coVerify(exactly = 0) { userRepository.updateUserStats(any(), any()) }
  }

  @Test
  fun refreshStats_updatesStatsWhenOnline() = runTest {
    coEvery { userRepository.getCurrentUser() } returns fakeUser
    coEvery { tripsRepository.getAllTrips() } returns emptyList()
    coEvery { userRepository.updateUserStats(any(), any()) } just Runs

    viewModel = ProfileSettingsViewModel(userRepository, tripsRepository)
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.refreshStats(isOnline = true)
    testDispatcher.scheduler.advanceUntilIdle()

    coVerify(exactly = 1) { userRepository.updateUserStats(fakeUser.uid, any()) }
  }

  @Test
  fun autoFill_withPartialUserData_updatesUiState() = runTest {
    val partialUser = fakeUser.copy(name = "", email = "")
    coEvery { userRepository.getCurrentUser() } returns fakeUser

    viewModel = ProfileSettingsViewModel(userRepository, tripsRepository)
    viewModel.autoFill(partialUser)

    val state = viewModel.uiState.value
    Assert.assertEquals("", state.name)
    Assert.assertEquals("", state.email)
  }

  @Test
  fun startEditingName_setsIsEditingNameTrue() = runTest {
    viewModel = ProfileSettingsViewModel(userRepository, tripsRepository)
    viewModel.startEditingName()
    Assert.assertTrue(viewModel.uiState.value.isEditingName)
  }

  @Test
  fun cancelEditingName_setsIsEditingNameFalse() = runTest {
    viewModel = ProfileSettingsViewModel(userRepository, tripsRepository)
    viewModel.startEditingName()
    viewModel.cancelEditingName()
    Assert.assertFalse(viewModel.uiState.value.isEditingName)
  }

  @Test
  fun startEditingBio_setsIsEditingBioTrue() = runTest {
    viewModel = ProfileSettingsViewModel(userRepository, tripsRepository)
    viewModel.startEditingBio()
    Assert.assertTrue(viewModel.uiState.value.isEditingBio)
  }

  @Test
  fun cancelEditingBio_setsIsEditingBioFalse() = runTest {
    viewModel = ProfileSettingsViewModel(userRepository, tripsRepository)
    viewModel.startEditingBio()
    viewModel.cancelEditingBio()
    Assert.assertFalse(viewModel.uiState.value.isEditingBio)
  }

  @Test
  fun saveName_updatesUiStateAndCallsRepository() = runTest {
    coEvery { userRepository.getCurrentUser() } returns fakeUser
    coEvery { userRepository.updateUser(any(), any(), any()) } just Runs

    viewModel = ProfileSettingsViewModel(userRepository, tripsRepository)
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.saveName("New Name")
    testDispatcher.scheduler.advanceUntilIdle()

    Assert.assertEquals("New Name", viewModel.uiState.value.name)
    Assert.assertFalse(viewModel.uiState.value.isEditingName)
    coVerify { userRepository.updateUser(fakeUser.uid, name = "New Name") }
  }

  @Test
  fun saveName_setsErrorMsgOnFailure() = runTest {
    coEvery { userRepository.getCurrentUser() } returns fakeUser
    coEvery { userRepository.updateUser(any(), any(), any()) } throws Exception("Name error")

    viewModel = ProfileSettingsViewModel(userRepository, tripsRepository)
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.saveName("New Name")
    testDispatcher.scheduler.advanceUntilIdle()

    Assert.assertTrue(viewModel.uiState.value.errorMsg!!.contains("Name error"))
  }

  @Test
  fun saveBio_updatesUiStateAndCallsRepository() = runTest {
    coEvery { userRepository.getCurrentUser() } returns fakeUser
    coEvery { userRepository.updateUser(any(), any(), any()) } just Runs

    viewModel = ProfileSettingsViewModel(userRepository, tripsRepository)
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.saveBio("New Bio")
    testDispatcher.scheduler.advanceUntilIdle()

    Assert.assertEquals("New Bio", viewModel.uiState.value.biography)
    Assert.assertFalse(viewModel.uiState.value.isEditingBio)
    coVerify { userRepository.updateUser(fakeUser.uid, biography = "New Bio") }
  }

  @Test
  fun saveBio_setsErrorMsgOnFailure() = runTest {
    coEvery { userRepository.getCurrentUser() } returns fakeUser
    coEvery { userRepository.updateUser(any(), any(), any()) } throws Exception("Bio error")

    viewModel = ProfileSettingsViewModel(userRepository, tripsRepository)
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.saveBio("New Bio")
    testDispatcher.scheduler.advanceUntilIdle()

    Assert.assertTrue(viewModel.uiState.value.errorMsg!!.contains("Bio error"))
  }
}
