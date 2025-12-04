package com.github.swent.swisstravel.ui.tripcreation

import android.content.Context
import android.content.res.Resources
import android.net.Uri
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.algorithm.TripAlgorithm
import com.github.swent.swisstravel.model.trip.Coordinate
import com.github.swent.swisstravel.model.trip.Location
import com.github.swent.swisstravel.model.trip.Trip
import com.github.swent.swisstravel.model.trip.TripsRepository
import com.github.swent.swisstravel.model.user.Preference
import com.github.swent.swisstravel.model.user.User
import com.github.swent.swisstravel.model.user.UserRepository
import com.github.swent.swisstravel.model.user.UserStats
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
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
import org.mockito.Mockito.mock

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
  fun `randomTrip should pick locations and save the trip`() = runTest {
    // Mocks
    val mockAlgorithm = mockk<TripAlgorithm>()
    val mockContext = mockk<Context>()
    val mockResources = mockk<Resources>()

    // A factory that returns our mock algorithm
    val algorithmFactory: (Context, TripSettings) -> TripAlgorithm = { _, _ -> mockAlgorithm }

    val viewModel =
        TripSettingsViewModel(
            tripsRepository = fakeRepo,
            userRepository = fakeUserRepo,
            activityRepository = fakeActivityRepo,
            algorithmFactory = algorithmFactory,
        )

    // Mocking the resource loading
    val cities =
        arrayOf(
            "Zurich;47.3769;8.5417",
            "Geneva;46.2044;6.1432",
            "Bern;46.9480;7.4474",
            "Lucerne;47.0502;8.3093")
    every { mockContext.resources } returns mockResources
    every { mockResources.getStringArray(R.array.grand_tour) } returns cities

    // Mock the algorithm response
    coEvery { mockAlgorithm.computeTrip(any(), any(), any()) } returns emptyList()

    // Setup initial state
    viewModel.updateDates(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 4)) // 4-day trip
    viewModel.setRandomTrip(true) // Simulate being in random mode

    // Use a seed for predictable "randomness"
    viewModel.randomTrip(mockContext, seed = 123)

    // Assertions
    val settings = viewModel.tripSettings.value

    // Check that trip name is updated
    assertEquals("Random Swiss Adventure", settings.name)

    // Check that arrival/departure are set and different
    assertNotNull(settings.arrivalDeparture.arrivalLocation)
    assertNotNull(settings.arrivalDeparture.departureLocation)
    assertNotEquals(
        settings.arrivalDeparture.arrivalLocation, settings.arrivalDeparture.departureLocation)

    // Check that intermediate destinations are selected
    assertEquals(4, settings.destinations.size)
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
  fun saveTripShouldFailIfNoArrivalLocation() = runTest {
    viewModel.updateDates(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 2))

    // Act
    val fakeContext: Context = mock()
    viewModel.saveTrip(fakeContext)

    // Assert
    when (val event = viewModel.validationEvents.first()) {
      is ValidationEvent.SaveError -> {
        assertEquals("Arrival location must not be null", event.message)
      }
      else -> throw AssertionError("Expected SaveError but got $event")
    }
  }

  @Test
  fun saveTripShouldFailIfNoDestinationLocation() = runTest {
    viewModel.updateDates(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 2))
    viewModel.updateArrivalLocation(Location(Coordinate(1.0, 1.0), "loc1"))

    // Act
    val fakeContext: Context = mock()
    viewModel.saveTrip(fakeContext)

    // Assert
    when (val event = viewModel.validationEvents.first()) {
      is ValidationEvent.SaveError -> {
        assertEquals("Departure location must not be null", event.message)
      }
      else -> throw AssertionError("Expected SaveError but got $event")
    }
  }

  // Refactored using AI
  @Test
  fun saveTripShouldAddTripAndEmitSaveSuccess() = runTest {
    val mockAlgorithm = mockk<TripAlgorithm>()
    val fakeContext: Context = mockk()

    // A factory that returns our mock algorithm
    val algorithmFactory: (Context, TripSettings) -> TripAlgorithm = { _, _ -> mockAlgorithm }

    val viewModel =
        TripSettingsViewModel(
            tripsRepository = fakeRepo,
            userRepository = fakeUserRepo,
            activityRepository = fakeActivityRepo,
            algorithmFactory = algorithmFactory,
        )

    // Algorithm returns empty schedule → no activities or route segments
    coEvery { mockAlgorithm.computeTrip(any(), any(), any()) } returns emptyList()

    viewModel.updateDates(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 2))
    viewModel.updateTravelers(2, 1)
    viewModel.updatePreferences(listOf(Preference.QUICK, Preference.FOODIE))
    viewModel.updateArrivalLocation(Location(Coordinate(0.0, 0.0), "Arrival", ""))
    viewModel.updateDepartureLocation(Location(Coordinate(0.0, 0.0), "Departure", ""))

    viewModel.saveTrip(fakeContext)

    val event = viewModel.validationEvents.first()
    assertEquals(ValidationEvent.SaveSuccess, event)

    val added = fakeRepo.addedTrip
    assertNotNull(added)
    assertEquals(2, added.tripProfile.adults)
    assertEquals(1, added.tripProfile.children)
  }

  // Refactored using AI
  @Test
  fun saveTripShouldEmitSaveErrorWhenRepositoryThrows() = runTest {
    val mockAlgorithm = mockk<TripAlgorithm>()
    val fakeContext: Context = mockk()

    // The fake repo will throw when saving
    fakeRepo.shouldThrow = true

    val algorithmFactory: (Context, TripSettings) -> TripAlgorithm = { _, _ -> mockAlgorithm }

    val viewModel =
        TripSettingsViewModel(
            tripsRepository = fakeRepo,
            userRepository = fakeUserRepo,
            activityRepository = fakeActivityRepo,
            algorithmFactory = algorithmFactory,
        )

    // Algorithm still returns empty schedule
    coEvery { mockAlgorithm.computeTrip(any(), any(), any()) } returns emptyList()

    viewModel.updateDates(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 2))
    viewModel.updateTravelers(2, 1)
    viewModel.updatePreferences(listOf(Preference.QUICK, Preference.FOODIE))
    viewModel.updateArrivalLocation(Location(Coordinate(0.0, 0.0), "Arrival", ""))
    viewModel.updateDepartureLocation(Location(Coordinate(0.0, 0.0), "Departure", ""))

    // Act
    viewModel.saveTrip(fakeContext)

    // Assert
    val event = viewModel.validationEvents.first()
    when (event) {
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
          friends = emptyList(),
          stats = UserStats(),
          pinnedTripsUids = emptyList(),
          pinnedPicturesUris = emptyList())
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

    override suspend fun updateUser(
        uid: String,
        name: String?,
        biography: String?,
        profilePicUrl: String?,
        preferences: List<Preference>?,
        pinnedTripsUids: List<String>?,
        pinnedPicturesUrls: List<Uri>?
    ) {
      // no-op for testing
    }
  }
}
