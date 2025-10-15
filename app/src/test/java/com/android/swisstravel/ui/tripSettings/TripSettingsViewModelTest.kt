package com.android.swisstravel.ui.tripSettings

import com.github.swent.swisstravel.model.trip.Trip
import com.github.swent.swisstravel.model.trip.TripsRepository
import com.github.swent.swisstravel.model.user.RatedPreferences
import com.github.swent.swisstravel.model.user.UserPreference
import com.github.swent.swisstravel.ui.tripSettings.TripPreferences
import com.github.swent.swisstravel.ui.tripSettings.TripSettingsViewModel
import com.github.swent.swisstravel.ui.tripSettings.ValidationEvent
import java.time.LocalDate
import junit.framework.TestCase
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
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

class TripSettingsViewModelTest {

  private val testDispatcher = StandardTestDispatcher()
  private lateinit var fakeRepo: FakeTripsRepository
  private lateinit var viewModel: TripSettingsViewModel

  @OptIn(ExperimentalCoroutinesApi::class)
  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    fakeRepo = FakeTripsRepository()
    viewModel = TripSettingsViewModel(tripsRepository = fakeRepo)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `updateDates should update the date in the trip settings`() {
    val startDate = LocalDate.of(2025, 1, 1)
    val endDate = LocalDate.of(2025, 1, 1)

    viewModel.updateDates(startDate, endDate)

    val newDate = viewModel.tripSettings.value.date
    TestCase.assertEquals(startDate, newDate.startDate)
    TestCase.assertEquals(endDate, newDate.endDate)
  }

  @Test
  fun `onNextFromDateScreen should emit error when end date is before start date`() = runTest {
    // Arrange
    val startDate = LocalDate.of(2025, 1, 2)
    val endDate = LocalDate.of(2025, 1, 1)
    viewModel.updateDates(startDate, endDate)

    // Act
    viewModel.onNextFromDateScreen()

    // Assert
    val event = viewModel.validationEvents.first()
    TestCase.assertEquals(ValidationEvent.EndDateIsBeforeStartDateError, event)
  }

  @Test
  fun `onNextFromDateScreen should emit proceed when dates are valid`() = runTest {
    // Arrange
    val startDate = LocalDate.of(2025, 1, 1)
    val endDate = LocalDate.of(2025, 1, 2)
    viewModel.updateDates(startDate, endDate)

    // Act
    viewModel.onNextFromDateScreen()

    // Assert
    val event = viewModel.validationEvents.first()
    TestCase.assertEquals(ValidationEvent.Proceed, event)
  }

  @Test
  fun `updateTravelers should update the travelers in the trip settings`() {
    val adults = 2
    val children = 1

    viewModel.updateTravelers(adults, children)

    val newTravelers = viewModel.tripSettings.value.travelers
    TestCase.assertEquals(adults, newTravelers.adults)
    TestCase.assertEquals(children, newTravelers.children)
  }

  @Test
  fun `updatePreferences should update the preferences in the trip settings`() {
    val preferences =
        TripPreferences(
            quickTraveler = true,
            sportyLevel = true,
            foodyLevel = true,
            museumInterest = true,
            hasHandicap = true)

    viewModel.updatePreferences(preferences)

    val newPreferences = viewModel.tripSettings.value.preferences
    TestCase.assertEquals(preferences, newPreferences)
  }

  @Test
  fun `saveTrip should add trip and emit SaveSuccess with mapped rated preferences`() = runTest {
    // Arrange: set dates, travelers and preferences that should map to 3 rated prefs
    val start = LocalDate.of(2025, 1, 1)
    val end = LocalDate.of(2025, 1, 2)
    viewModel.updateDates(start, end)
    viewModel.updateTravelers(adults = 2, children = 1)
    val prefs =
        TripPreferences(
            quickTraveler = true,
            sportyLevel = false,
            foodyLevel = true,
            museumInterest = true,
            hasHandicap = false)
    viewModel.updatePreferences(prefs)

    // Act
    viewModel.saveTrip()

    // Assert: receive event and inspect saved trip
    val event = viewModel.validationEvents.first()
    assertEquals(ValidationEvent.SaveSuccess, event)

    val added = fakeRepo.addedTrip
    assertNotNull(added, "Trip should have been added to repository")

    assertEquals("fake-uid", added.uid)
    assertEquals(2, added.tripProfile.adults)
    assertEquals(1, added.tripProfile.children)

    val expectedPrefs =
        listOf(
            RatedPreferences(UserPreference.QUICK, 5),
            RatedPreferences(UserPreference.FOODIE, 5),
            RatedPreferences(UserPreference.MUSEUMS, 5))
    // ensure all expected rated prefs are present (order not guaranteed)
    expectedPrefs.forEach { expected ->
      assertTrue(
          added.tripProfile.preferences.contains(expected),
          "Expected rated preference $expected to be present")
    }
    assertEquals(3, added.tripProfile.preferences.size)
  }

  @Test
  fun `saveTrip should emit SaveError when repository throws`() = runTest {
    // Arrange: cause repository to throw
    fakeRepo.shouldThrow = true
    viewModel.updateDates(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 2))

    // Act
    viewModel.saveTrip()

    // Assert
    val event = viewModel.validationEvents.first()
    when (event) {
      is ValidationEvent.SaveError -> {
        assertTrue(event.message.contains("boom"))
      }
      else -> throw AssertionError("Expected SaveError but got $event")
    }
  }

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
  }
}
