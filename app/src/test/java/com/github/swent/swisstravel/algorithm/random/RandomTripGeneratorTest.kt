package com.github.swent.swisstravel.algorithm.random

import android.content.Context
import android.content.res.Resources
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.model.trip.Coordinate
import com.github.swent.swisstravel.model.trip.Location
import com.github.swent.swisstravel.ui.tripcreation.TripArrivalDeparture
import com.github.swent.swisstravel.ui.tripcreation.TripDate
import com.github.swent.swisstravel.ui.tripcreation.TripSettings
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// Tests done with AI
class RandomTripGeneratorTest {

    private lateinit var context: Context
    private lateinit var resources: Resources

    // Fake Grand Tour Data
    private val grandTourData = arrayOf(
        "Genève;46.204391;6.143158",
        "Lausanne;46.519653;6.632273",
        "Zurich;47.376887;8.541694",
        "Bern;46.947974;7.447447",
        "Basel;47.559599;7.588576",
        "Lugano;46.003678;8.951052",
        "Luzern;47.050168;8.309307",
        "Zermatt;46.020713;7.749117"
    )

    @Before
    fun setUp() {
        context = mockk()
        resources = mockk()

        every { context.resources } returns resources
        every { resources.getStringArray(R.array.grand_tour) } returns grandTourData
    }

    @Test
    fun `generator provides distinct start and end when no arrival is set`() {
        val settings = TripSettings()

        val (start, end, _) = RandomTripGenerator.generateRandomDestinations(context, settings)

        assertNotNull(start)
        assertNotNull(end)
        assertNotEquals("Start and end locations should be different", start.name, end.name)
    }

    @Test
    fun `generator uses provided arrival location as start`() {
        val arrivalLocation = Location(
            name = "Genève",
            coordinate = Coordinate(46.204391, 6.143158)
        )
        val settings = TripSettings(
            arrivalDeparture = TripArrivalDeparture(arrivalLocation = arrivalLocation)
        )

        val (start, end, _) = RandomTripGenerator.generateRandomDestinations(context, settings)

        assertEquals("Start location should be the provided arrival location", "Genève", start.name)
        assertNotNull(end)
        assertNotEquals("End location should be different from start", "Genève", end.name)
    }

    @Test
    fun `generator creates correct number of intermediate stops for a short trip`() {
        // 3 days: Duration calculation in Generator is (end - start) + 1
        // 3-1 = 2 days diff + 1 = 3 days total
        // Logic: 3 / 2 = 1.5 -> 1 stop
        val settings = TripSettings(
            date = TripDate(
                startDate = LocalDate.of(2025, 7, 1),
                endDate = LocalDate.of(2025, 7, 3)
            )
        )

        val (_, _, intermediate) = RandomTripGenerator.generateRandomDestinations(context, settings)

        assertEquals("A 3-day trip should have 1 intermediate destination", 1, intermediate.size)
    }

    @Test
    fun `generator creates correct number of intermediate stops for a medium trip`() {
        // 5 days total: 5 / 2 = 2.5 -> 2 stops
        val settings = TripSettings(
            date = TripDate(
                startDate = LocalDate.of(2025, 7, 1),
                endDate = LocalDate.of(2025, 7, 5)
            )
        )

        val (_, _, intermediate) = RandomTripGenerator.generateRandomDestinations(context, settings)

        assertEquals("A 5-day trip should have 2 intermediate destinations", 2, intermediate.size)
    }

    @Test
    fun `generator creates correct number of intermediate stops for a long trip`() {
        // 8 days total: 8 / 2 = 4 -> Coerced at most 3
        val settings = TripSettings(
            date = TripDate(
                startDate = LocalDate.of(2025, 7, 1),
                endDate = LocalDate.of(2025, 7, 8)
            )
        )

        val (_, _, intermediate) = RandomTripGenerator.generateRandomDestinations(context, settings)

        assertEquals("An 8-day trip should have the max (3) intermediate destinations", 3, intermediate.size)
    }

    @Test
    fun `generator creates zero intermediate stops for a very short trip`() {
        // 1 day total: 1 / 2 = 0
        val settings = TripSettings(
            date = TripDate(
                startDate = LocalDate.of(2025, 7, 1),
                endDate = LocalDate.of(2025, 7, 1)
            )
        )

        val (_, _, intermediate) = RandomTripGenerator.generateRandomDestinations(context, settings)

        assertTrue("A 1-day trip should have 0 intermediate destinations", intermediate.isEmpty())
    }

    @Test
    fun `generator with a seed produces deterministic results`() {
        val settings = TripSettings(
            date = TripDate(
                startDate = LocalDate.of(2025, 1, 1),
                endDate = LocalDate.of(2025, 1, 5)
            )
        )
        val seed = 42

        val (start1, end1, intermediate1) = RandomTripGenerator.generateRandomDestinations(context, settings, seed)
        val (start2, end2, intermediate2) = RandomTripGenerator.generateRandomDestinations(context, settings, seed)

        assertEquals("Start location should be the same for the same seed", start1.name, start2.name)
        assertEquals("End location should be the same for the same seed", end1.name, end2.name)
        assertEquals(
            "Intermediate locations should be the same for the same seed",
            intermediate1.map { it.name },
            intermediate2.map { it.name }
        )
    }

    @Test
    fun `intermediate stops are distinct from start and end`() {
        val settings = TripSettings(
            date = TripDate(
                startDate = LocalDate.of(2025, 7, 1),
                endDate = LocalDate.of(2025, 7, 8)
            )
        )

        // Run multiple times to ensure randomness doesn't accidentally pick duplicates
        repeat(10) {
            val (start, end, intermediate) = RandomTripGenerator.generateRandomDestinations(context, settings)
            val intermediateNames = intermediate.map { it.name }

            assertFalse("Intermediate stops should not contain start", intermediateNames.contains(start.name))
            assertFalse("Intermediate stops should not contain end", intermediateNames.contains(end.name))
            assertNotEquals("Start and End should be different", start.name, end.name)
        }
    }
}