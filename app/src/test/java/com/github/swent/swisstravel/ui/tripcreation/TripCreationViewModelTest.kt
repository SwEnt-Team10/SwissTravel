package com.github.swent.swisstravel.ui.tripcreation

import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.model.trip.Trip
import com.github.swent.swisstravel.model.trip.TripsRepository
import com.github.swent.swisstravel.model.user.Preference
import com.github.swent.swisstravel.model.user.User
import com.github.swent.swisstravel.model.user.UserRepository
import com.github.swent.swisstravel.model.user.UserStats
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

class TripCreationViewModelTest {

  private val testDispatcher = StandardTestDispatcher()
  private lateinit var fakeRepo: FakeTripsRepository
  private lateinit var fakeUserRepo: FakeUserRepository
  private lateinit var fakeActivityRepo: FakeActivityRepository
  private lateinit var viewModel: TripSettingsViewModel

  @OptIn(ExperimentalCoroutinesApi::class)
  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    fakeRepo = FakeTripsRepository()
    fakeUserRepo = FakeUserRepository()
    fakeActivityRepo = FakeActivityRepository()
    viewModel =
        TripSettingsViewModel(
            tripsRepository = fakeRepo,
            userRepository = fakeUserRepo,
            activityRepository = fakeActivityRepo)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun updateDatesShouldUpdateTheDateInTheTripSettings() {
    val startDate = LocalDate.of(2025, 1, 1)
    val endDate = LocalDate.of(2025, 1, 1)

    viewModel.updateDates(startDate, endDate)

    val newDate = viewModel.tripSettings.value.date
    assertEquals(startDate, newDate.startDate)
    assertEquals(endDate, newDate.endDate)
  }

  @Test
  fun onNextFromDateScreenShouldEmitErrorWhenEndDateIsBeforeStartDate() = runTest {
    // Arrange
    val startDate = LocalDate.of(2025, 1, 2)
    val endDate = LocalDate.of(2025, 1, 1)
    viewModel.updateDates(startDate, endDate)

    // Act
    viewModel.onNextFromDateScreen()

    // Assert
    val event = viewModel.validationEvents.first()
    assertEquals(ValidationEvent.EndDateIsBeforeStartDateError, event)
  }

  @Test
  fun onNextFromDateScreenShouldEmitProceedWhenDatesAreValid() = runTest {
    // Arrange
    val startDate = LocalDate.of(2025, 1, 1)
    val endDate = LocalDate.of(2025, 1, 2)
    viewModel.updateDates(startDate, endDate)

    // Act
    viewModel.onNextFromDateScreen()

    // Assert
    val event = viewModel.validationEvents.first()
    assertEquals(ValidationEvent.Proceed, event)
  }

  @Test
  fun updateTravelersShouldUpdateTheTravelersInTheTripSettings() {
    val adults = 2
    val children = 1

    viewModel.updateTravelers(adults, children)

    val newTravelers = viewModel.tripSettings.value.travelers
    assertEquals(adults, newTravelers.adults)
    assertEquals(children, newTravelers.children)
  }

  @Test
  fun updatePreferencesShouldUpdateThePreferencesInTheTripSettings() {
    val preferences =
        listOf(
            Preference.QUICK,
            Preference.FOODIE,
            Preference.MUSEUMS,
            Preference.WHEELCHAIR_ACCESSIBLE,
            Preference.SPORTS)

    viewModel.updatePreferences(preferences)

    val newPreferences = viewModel.tripSettings.value.preferences
    assertEquals(preferences, newPreferences)
  }

  @Test
  fun saveTripShouldAddTripAndEmitSaveSuccess() = runTest {
    // Arrange
    val start = LocalDate.of(2025, 1, 1)
    val end = LocalDate.of(2025, 1, 2)
    viewModel.updateDates(start, end)
    viewModel.updateTravelers(adults = 2, children = 1)
    val prefs = listOf(Preference.QUICK, Preference.FOODIE, Preference.MUSEUMS)
    viewModel.updatePreferences(prefs)

    // Act
    viewModel.saveTrip()

    // Assert
    val event = viewModel.validationEvents.first()
    assertEquals(ValidationEvent.SaveSuccess, event)

    val added = fakeRepo.addedTrip
    assertNotNull(added, "Trip should have been added to repository")

    // Check basic trip info
    assertEquals("fake-uid", added.uid)
    assertEquals(2, added.tripProfile.adults)
    assertEquals(1, added.tripProfile.children)

    // Check preferences
    assertEquals(prefs, added.tripProfile.preferences)
    assertEquals(3, added.tripProfile.preferences.size)
  }

  @Test
  fun saveTripShouldEmitSaveErrorWhenRepositoryThrows() = runTest {
    // Arrange: cause repository to throw
    fakeRepo.shouldThrow = true
    viewModel.updateDates(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 2))

    // Act
    viewModel.saveTrip()

    // Assert
    when (val event = viewModel.validationEvents.first()) {
      is ValidationEvent.SaveError -> {
        assertTrue(event.message.contains("boom"))
      }
      else -> throw AssertionError("Expected SaveError but got $event")
    }
  }

  @Test
  fun updateNameWithDifferentString() = runTest {
    val emptyString = ""
    viewModel.updateName(emptyString)
    assertEquals(viewModel.tripSettings.value.invalidNameMsg, R.string.name_empty)
    val fakeName = "Test Trip"
    viewModel.updateName(fakeName)
    assertNull(viewModel.tripSettings.value.invalidNameMsg)
  }

  /** A fake repository that records added trips and can be made to throw on addTrip. */
  private class FakeTripsRepository : TripsRepository {
    var addedTrip: Trip? = null
    var shouldThrow: Boolean = false

    // match the interface: non-suspending
    override fun getNewUid(): String = "fake-uid"

    override suspend fun addTrip(trip: Trip) {
      if (shouldThrow) throw Exception("boom")
      addedTrip = trip
    }

    // minimal stubs required by the interface
    override suspend fun getAllTrips(): List<Trip> = emptyList()

    override suspend fun getTrip(tripId: String): Trip =
        throw NotImplementedError("getTrip not needed for these tests")

    override suspend fun deleteTrip(tripId: String) {
      /* no-op for tests */
    }

    override suspend fun editTrip(tripId: String, updatedTrip: Trip) {
      /* no-op for tests */
    }
  }

  private class FakeUserRepository : UserRepository {
    override suspend fun getCurrentUser(): User {
      return User(
          uid = "test-user",
          name = "Test User",
          biography = "Test bio",
          email = "test@example.com",
          profilePicUrl = "",
          preferences = listOf(Preference.FOODIE),
          friends = emptyList())
    }

    override suspend fun getUserByUid(uid: String): User? {
      // no-op in tests
      return null
    }

    override suspend fun getUserByNameOrEmail(query: String): List<User> {
      // no-op in tests
      return emptyList()
    }

    override suspend fun updateUserPreferences(uid: String, preferences: List<Preference>) {}

    override suspend fun updateUserStats(uid: String, stats: UserStats) {
      // no-op for testing
    }

    override suspend fun sendFriendRequest(fromUid: String, toUid: String) {
      // no-op for testing
    }

    override suspend fun acceptFriendRequest(currentUid: String, fromUid: String) {
      // no-op for testing
    }

    override suspend fun removeFriend(uid: String, friendUid: String) {
      // no-op for testing
    }
  }
}
