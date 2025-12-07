package com.github.swent.swisstravel.ui.trip.edittrip

import android.content.Context
import com.github.swent.swisstravel.algorithm.TripAlgorithm
import com.github.swent.swisstravel.algorithm.random.RandomTripGenerator
import com.github.swent.swisstravel.model.trip.Coordinate
import com.github.swent.swisstravel.model.trip.Location
import com.github.swent.swisstravel.model.trip.Trip
import com.github.swent.swisstravel.model.trip.TripProfile
import com.github.swent.swisstravel.model.trip.TripsRepository
import com.github.swent.swisstravel.model.trip.activity.ActivityRepository
import com.github.swent.swisstravel.model.user.Preference
import com.github.swent.swisstravel.ui.tripcreation.TripSettings
import com.google.firebase.Timestamp
import io.mockk.*
import kotlin.collections.emptyList
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
          prefs = listOf(Preference.FOODIE, Preference.SPORTS),
          arrivalLocation = Location(Coordinate(0.0, 0.0), "Lausanne"),
          departureLocation = Location(Coordinate(1.1, 1.1), "Geneva"),
          random = false)

  private val randomTrip =
      makeTrip(
          uid = "random-456",
          name = "Random Trip",
          adults = 1,
          children = 0,
          prefs = listOf(Preference.PUBLIC_TRANSPORT),
          arrivalLocation = Location(Coordinate(0.0, 0.0), "Zurich"),
          departureLocation = Location(Coordinate(1.1, 1.1), "Bern"),
          random = true)

  @Before
  fun setUp() {
    repo = mockk(relaxed = true)
    // Relaxed mock for ActivityRepository to avoid mocking every call in `save`
    val activityRepo: ActivityRepository = mockk(relaxed = true)
    vm = EditTripScreenViewModel(tripRepository = repo, activityRepository = activityRepo)

    // Mock the static RandomTripGenerator object
    mockkObject(RandomTripGenerator)
    every { RandomTripGenerator.generateRandomDestinations(any(), any(), any()) } returns
        Triple(
            Location(Coordinate(2.0, 2.0), "NewStart"),
            Location(Coordinate(3.0, 3.0), "NewEnd"),
            listOf(Location(Coordinate(4.0, 4.0), "NewIntermediate")))
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
    assertFalse(s.isRandom)
  }

  @Test
  fun `loadTrip on random trip sets isRandom to true`() = runTest {
    coEvery { repo.getTrip(randomTrip.uid) } returns randomTrip

    vm.loadTrip(randomTrip.uid)
    advanceUntilIdle()

    val s = vm.state.value
    assertTrue(s.isRandom)
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
  // reroll
  // -------------------------

  @Test
  fun `reroll does nothing if trip is not random`() = runTest {
    coEvery { repo.getTrip(sampleTripId) } returns sampleTrip
    vm.loadTrip(sampleTripId)
    advanceUntilIdle()

    val context = mockk<Context>(relaxed = true)
    vm.reroll(context)
    advanceUntilIdle()

    // Verify that the generator and save were NOT called
    verify { RandomTripGenerator wasNot Called }
    coVerify(exactly = 0) { repo.editTrip(any(), any()) }
  }

  @Test
  fun `reroll failure sets error message`() = runTest {
    coEvery { repo.getTrip(randomTrip.uid) } returns randomTrip
    every { RandomTripGenerator.generateRandomDestinations(any(), any(), any()) } throws
        RuntimeException("generator failed")
    vm.loadTrip(randomTrip.uid)
    advanceUntilIdle()

    val context = mockk<Context>(relaxed = true)
    vm.reroll(context)
    advanceUntilIdle()

    val state = vm.state.value
    assertFalse(state.isSaving)
    assertEquals("generator failed", state.errorMsg)
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

  @Test
  fun `editName updates state`() = runTest {
    coEvery { repo.getTrip(sampleTripId) } returns sampleTrip
    vm.loadTrip(sampleTripId)
    advanceUntilIdle()

    vm.editTripName("New Trip")

    val s = vm.state.value
    assertEquals("New Trip", s.tripName)
  }

  // -------------------------
  // save
  // -------------------------
  @Test
  fun `save merges ui state into tripProfile and calls editTrip`() = runTest {
    val mockAlgorithm = mockk<TripAlgorithm>()
    val algorithmFactory: (Context, TripSettings, ActivityRepository) -> TripAlgorithm =
        { _, _, _ ->
          mockAlgorithm
        }

    vm =
        EditTripScreenViewModel(
            tripRepository = repo,
            activityRepository = mockk(relaxed = true),
            algorithmFactory = algorithmFactory)

    coEvery { repo.getTrip(sampleTripId) } returns sampleTrip
    coEvery { repo.editTrip(any(), any()) } just Runs

    // Mock algorithm to produce no new schedule
    coEvery { mockAlgorithm.computeTrip(any(), any(), any()) } returns emptyList()

    // --- Act ---
    vm.loadTrip(sampleTripId)
    advanceUntilIdle()

    // change some values that DON'T trigger re-computation
    vm.editTripName("Alps Adventure")
    vm.setAdults(4)
    vm.setChildren(0)

    val tripSlot = slot<Trip>()
    coEvery { repo.editTrip(eq(sampleTripId), capture(tripSlot)) } just Runs

    val context = mockk<Context>(relaxed = true)
    vm.save(context)
    advanceUntilIdle()

    // Algorithm should NOT be called if only metadata changes
    coVerify(exactly = 0) { mockAlgorithm.computeTrip(any(), any(), any()) }
    coVerify(exactly = 1) { repo.editTrip(eq(sampleTripId), any()) }

    val sentTrip = tripSlot.captured
    assertEquals(4, sentTrip.tripProfile.adults)
    assertEquals(0, sentTrip.tripProfile.children)
    assertEquals("Alps Adventure", sentTrip.name)
    assertEquals(sampleTrip.uid, sentTrip.uid)
  }

  @Test
  fun `save failure sets errorMsg`() = runTest {
    coEvery { repo.getTrip(sampleTripId) } returns sampleTrip
    coEvery { repo.editTrip(any(), any()) } throws RuntimeException("save failed")

    vm.loadTrip(sampleTripId)
    advanceUntilIdle()

    val context = mockk<Context>(relaxed = true)
    vm.save(context)
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
    coEvery { repo.getTrip(sampleTripId) } throws RuntimeException("x")
    vm.loadTrip(sampleTripId)
    advanceUntilIdle()

    // Error is now set
    assertNotNull(vm.state.value.errorMsg)

    // Clear it
    vm.clearErrorMsg()
    assertNull(vm.state.value.errorMsg)
  }

  // ------------ helpers ------------

  private fun makeTrip(
      uid: String,
      name: String,
      adults: Int,
      children: Int,
      prefs: List<Preference>,
      arrivalLocation: Location,
      departureLocation: Location,
      random: Boolean
  ): Trip {
    val now = Timestamp.now()
    val profile =
        TripProfile(
            startDate = now,
            endDate = now,
            preferredLocations = emptyList(),
            preferences = prefs,
            adults = adults,
            children = children,
            arrivalLocation = arrivalLocation,
            departureLocation = departureLocation)
    return Trip(
        uid = uid,
        name = name,
        ownerId = "owner-1",
        locations = emptyList(),
        routeSegments = emptyList(),
        activities = emptyList(),
        tripProfile = profile,
        isFavorite = false,
        isCurrentTrip = false,
        listUri = emptyList(),
        random = random)
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
