package com.github.swent.swisstravel.ui

import com.github.swent.swisstravel.model.trip.Location
import com.github.swent.swisstravel.model.trip.RouteSegment
import com.github.swent.swisstravel.model.trip.Trip
import com.github.swent.swisstravel.model.trip.TripProfile
import com.github.swent.swisstravel.model.trip.TripsRepository
import com.github.swent.swisstravel.model.trip.activity.Activity
import com.github.swent.swisstravel.model.user.Preference
import com.github.swent.swisstravel.ui.edittrip.EditTripScreenViewModel
import com.google.firebase.Timestamp
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class EditTripScreenViewModelTest {

  // --- Dispatcher rule so viewModelScope uses a test dispatcher ---
  @get:Rule val mainDispatcherRule = MainDispatcherRule()

  private lateinit var repo: TripsRepository
  private lateinit var vm: EditTripScreenViewModel

  private val sampleTripId = "trip-123"
  private val sampleTrip =
      makeTrip(
          uid = sampleTripId,
          name = "Swiss Alps",
          adults = 2,
          children = 1,
          prefs = listOf(Preference.FOODIE, Preference.SPORTS))

  @Before
  fun setUp() {
    repo = mockk(relaxed = true)
    vm = EditTripScreenViewModel(tripRepository = repo)
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  // -------------------------
  // loadTrip
  // -------------------------
  @Test
  fun `loadTrip success updates state with trip data`() = runTest {
    coEvery { repo.getTrip(sampleTripId) } returns sampleTrip

    vm.loadTrip(sampleTripId)
    advanceUntilIdle()

    val s = vm.state.value
    assertFalse(s.isLoading)
    assertNull(s.errorMsg)
    assertEquals(sampleTripId, s.tripId)
    assertEquals("Swiss Alps", s.tripName)
    assertEquals(2, s.adults)
    assertEquals(1, s.children)
    assertEquals(setOf(Preference.FOODIE, Preference.SPORTS), s.selectedPrefs)
  }

  @Test
  fun `loadTrip failure sets errorMsg and stops loading`() = runTest {
    coEvery { repo.getTrip(sampleTripId) } throws RuntimeException("boom")

    vm.loadTrip(sampleTripId)
    advanceUntilIdle()

    val s = vm.state.value
    assertFalse(s.isLoading)
    assertEquals("boom", s.errorMsg)
    assertEquals(sampleTripId, s.tripId) // still set
  }

  // -------------------------
  // togglePref
  // -------------------------
  @Test
  fun `togglePref adds and removes preference`() = runTest {
    coEvery { repo.getTrip(sampleTripId) } returns sampleTrip
    vm.loadTrip(sampleTripId)
    advanceUntilIdle()

    // Initially: FOODIE, SPORTS
    assertTrue(vm.state.value.selectedPrefs.contains(Preference.FOODIE))
    assertTrue(vm.state.value.selectedPrefs.contains(Preference.SPORTS))

    // Toggle FOODIE -> remove
    vm.togglePref(Preference.FOODIE)
    assertFalse(vm.state.value.selectedPrefs.contains(Preference.FOODIE))

    // Toggle WELLNESS -> add
    vm.togglePref(Preference.WELLNESS)
    assertTrue(vm.state.value.selectedPrefs.contains(Preference.WELLNESS))
  }

  // -------------------------
  // setters
  // -------------------------
  @Test
  fun `setAdults and setChildren update state`() = runTest {
    coEvery { repo.getTrip(sampleTripId) } returns sampleTrip
    vm.loadTrip(sampleTripId)
    advanceUntilIdle()

    vm.setAdults(5)
    vm.setChildren(3)

    val s = vm.state.value
    assertEquals(5, s.adults)
    assertEquals(3, s.children)
  }

  // -------------------------
  // save
  // -------------------------
  @Test
  fun `save merges ui state into tripProfile and calls editTrip`() = runTest {
    coEvery { repo.getTrip(sampleTripId) } returns sampleTrip
    coEvery { repo.editTrip(any(), any()) } just Runs

    vm.loadTrip(sampleTripId)
    advanceUntilIdle()

    // change some values
    vm.setAdults(4)
    vm.setChildren(0)
    vm.togglePref(Preference.FOODIE) // remove FOODIE
    vm.togglePref(Preference.MUSEUMS) // add MUSEUMS

    val tripSlot = slot<Trip>()
    coEvery { repo.editTrip(eq(sampleTripId), capture(tripSlot)) } just Runs

    vm.save()
    advanceUntilIdle()

    coVerify(exactly = 1) { repo.editTrip(eq(sampleTripId), any()) }

    val sentTrip = tripSlot.captured
    assertEquals(4, sentTrip.tripProfile.adults)
    assertEquals(0, sentTrip.tripProfile.children)
    assertTrue(sentTrip.tripProfile.preferences.contains(Preference.MUSEUMS))
    assertFalse(sentTrip.tripProfile.preferences.contains(Preference.FOODIE))
    // Unchanged fields stay the same
    assertEquals(sampleTrip.uid, sentTrip.uid)
    assertEquals(sampleTrip.name, sentTrip.name)
  }

  @Test
  fun `save failure sets errorMsg`() = runTest {
    coEvery { repo.getTrip(sampleTripId) } returns sampleTrip
    coEvery { repo.editTrip(any(), any()) } throws RuntimeException("save failed")

    vm.loadTrip(sampleTripId)
    advanceUntilIdle()

    vm.save()
    advanceUntilIdle()

    assertEquals("save failed", vm.state.value.errorMsg)
  }

  // -------------------------
  // deleteTrip
  // -------------------------
  @Test
  fun `deleteTrip calls repository with original uid`() = runTest {
    coEvery { repo.getTrip(sampleTripId) } returns sampleTrip
    coEvery { repo.deleteTrip(any()) } just Runs

    vm.loadTrip(sampleTripId)
    advanceUntilIdle()

    vm.deleteTrip()
    advanceUntilIdle()

    coVerify { repo.deleteTrip(sampleTrip.uid) }
  }

  @Test
  fun `deleteTrip failure sets errorMsg`() = runTest {
    coEvery { repo.getTrip(sampleTripId) } returns sampleTrip
    coEvery { repo.deleteTrip(any()) } throws RuntimeException("delete failed")

    vm.loadTrip(sampleTripId)
    advanceUntilIdle()

    vm.deleteTrip()
    advanceUntilIdle()

    assertEquals("delete failed", vm.state.value.errorMsg)
  }

  // -------------------------
  // clearErrorMsg
  // -------------------------
  @Test
  fun `clearErrorMsg clears error state`() = runTest {
    vm.clearErrorMsg()
    assertNull(vm.state.value.errorMsg)

    // Set an error then clear
    vm.loadTrip(sampleTripId)
    coEvery { repo.getTrip(sampleTripId) } throws RuntimeException("x")
    advanceUntilIdle()

    vm.clearErrorMsg()
    assertNull(vm.state.value.errorMsg)
  }

  // ------------ helpers ------------

  private fun makeTrip(
      uid: String,
      name: String,
      adults: Int,
      children: Int,
      prefs: List<Preference>
  ): Trip {
    val now = Timestamp.now()
    val profile =
        TripProfile(
            startDate = now,
            endDate = now,
            preferredLocations = emptyList(),
            preferences = prefs,
            adults = adults,
            children = children)
    return Trip(
        uid = uid,
        name = name,
        ownerId = "owner-1",
        locations = emptyList<Location>(),
        routeSegments = emptyList<RouteSegment>(),
        activities = emptyList<Activity>(),
        tripProfile = profile)
  }
}

/** JUnit4 rule that sets the Main dispatcher to a TestDispatcher. */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(private val testDispatcher: TestDispatcher = StandardTestDispatcher()) :
    TestWatcher() {
  override fun starting(description: Description?) {
    Dispatchers.setMain(testDispatcher)
  }

  override fun finished(description: Description?) {
    Dispatchers.resetMain()
  }
}
