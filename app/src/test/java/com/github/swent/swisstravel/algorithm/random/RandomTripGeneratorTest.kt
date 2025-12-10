package com.github.swent.swisstravel.algorithm.random

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.model.trip.Location
import com.github.swent.swisstravel.ui.tripcreation.TripArrivalDeparture
import com.github.swent.swisstravel.ui.tripcreation.TripDate
import com.github.swent.swisstravel.ui.tripcreation.TripSettings
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RandomTripGeneratorTest {

  private lateinit var context: Context
  private lateinit var grandTourCities: List<String>

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    // Load the array to know what to expect
    grandTourCities = context.resources.getStringArray(R.array.grand_tour).toList()
    assertTrue("Grand Tour city list should not be empty", grandTourCities.isNotEmpty())
  }

  @Test
  fun `generator provides distinct start and end when no arrival is set`() {
    // Given a trip setting with no arrival or departure
    val settings = TripSettings()

    // When we generate destinations
    val (start, end, _) = RandomTripGenerator.generateRandomDestinations(context, settings)

    // Then start and end locations should be different
    assertNotNull(start)
    assertNotNull(end)
    assertNotEquals("Start and end locations should be different", start.name, end.name)
  }

  @Test
  fun `generator uses provided arrival location as start`() {
    // Given a specific arrival location
    val arrivalLocation =
        Location(
            name = "Genève",
            coordinate = com.github.swent.swisstravel.model.trip.Coordinate(46.204391, 6.143158))
    val settings =
        TripSettings(arrivalDeparture = TripArrivalDeparture(arrivalLocation = arrivalLocation))

    // When we generate destinations
    val (start, end, _) = RandomTripGenerator.generateRandomDestinations(context, settings)

    // Then the start location must be the one we provided
    assertEquals("Start location should be the provided arrival location", "Genève", start.name)
    assertNotNull(end)
    assertNotEquals("End location should be different from start", "Genève", end.name)
  }

  @Test
  fun `generator creates correct number of intermediate stops for a short trip`() {
    // 3-day trip (duration 2) -> 1 intermediate stop
    val settings =
        TripSettings(
            date =
                TripDate(startDate = LocalDate.of(2025, 7, 1), endDate = LocalDate.of(2025, 7, 3)))

    val (start, end, intermediate) =
        RandomTripGenerator.generateRandomDestinations(context, settings)

    assertNotNull(start)
    assertNotNull(end)
    assertEquals("A 3-day trip should have 1 intermediate destination", 1, intermediate.size)
  }

  @Test
  fun `generator creates correct number of intermediate stops for a medium trip`() {
    // 5-day trip (duration 4) -> 2 intermediate stops
    val settings =
        TripSettings(
            date =
                TripDate(startDate = LocalDate.of(2025, 7, 1), endDate = LocalDate.of(2025, 7, 5)))

    val (start, end, intermediate) =
        RandomTripGenerator.generateRandomDestinations(context, settings)

    assertNotNull(start)
    assertNotNull(end)
    assertEquals("A 5-day trip should have 2 intermediate destinations", 2, intermediate.size)
  }

  @Test
  fun `generator creates correct number of intermediate stops for a long trip`() {
    // 8-day trip (duration 7) -> 3 intermediate stops (max)
    val settings =
        TripSettings(
            date =
                TripDate(startDate = LocalDate.of(2025, 7, 1), endDate = LocalDate.of(2025, 7, 8)))

    val (start, end, intermediate) =
        RandomTripGenerator.generateRandomDestinations(context, settings)

    assertNotNull(start)
    assertNotNull(end)
    assertEquals(
        "An 8-day trip should have the max (3) intermediate destinations", 3, intermediate.size)
  }

  @Test
  fun `generator creates zero intermediate stops for a very short trip`() {
    // 1-day trip (duration 0) -> 0 intermediate stops
    val settings =
        TripSettings(
            date =
                TripDate(startDate = LocalDate.of(2025, 7, 1), endDate = LocalDate.of(2025, 7, 1)))

    val (start, end, intermediate) =
        RandomTripGenerator.generateRandomDestinations(context, settings)

    assertNotNull(start)
    assertNotNull(end)
    assertTrue("A 1-day trip should have 0 intermediate destinations", intermediate.isEmpty())
  }

  @Test
  fun `generator with a seed produces deterministic results`() {
    val settings =
        TripSettings(
            date =
                TripDate(startDate = LocalDate.of(2025, 1, 1), endDate = LocalDate.of(2025, 1, 5)))
    val seed = 42

    // When generating destinations twice with the same seed
    val (start1, end1, intermediate1) =
        RandomTripGenerator.generateRandomDestinations(context, settings, seed)
    val (start2, end2, intermediate2) =
        RandomTripGenerator.generateRandomDestinations(context, settings, seed)

    // Then the results should be identical
    assertEquals("Start location should be the same for the same seed", start1.name, start2.name)
    assertEquals("End location should be the same for the same seed", end1.name, end2.name)
    assertEquals(
        "Intermediate locations should be the same for the same seed",
        intermediate1.map { it.name },
        intermediate2.map { it.name })
  }

  @Test
  fun `intermediate stops are distinct from start and end`() {
    val settings =
        TripSettings(
            date =
                TripDate(startDate = LocalDate.of(2025, 7, 1), endDate = LocalDate.of(2025, 7, 8)))

    val (start, end, intermediate) =
        RandomTripGenerator.generateRandomDestinations(context, settings)

    val intermediateNames = intermediate.map { it.name }
    assertFalse(
        "Intermediate stops should not contain the start location",
        intermediateNames.contains(start.name))
    assertFalse(
        "Intermediate stops should not contain the end location",
        intermediateNames.contains(end.name))
  }
}
