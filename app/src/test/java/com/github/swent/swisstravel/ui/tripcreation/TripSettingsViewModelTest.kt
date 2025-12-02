package com.github.swent.swisstravel.ui.tripcreation

import android.content.Context
import android.content.res.Resources
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.model.trip.Coordinate
import com.github.swent.swisstravel.model.trip.Location
import com.github.swent.swisstravel.model.trip.TripsRepository
import com.github.swent.swisstravel.model.user.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class TripSettingsViewModelTest {

    private lateinit var viewModel: TripSettingsViewModel
    private lateinit var tripsRepository: TripsRepository
    private lateinit var userRepository: UserRepository
    private lateinit var context: Context
    private lateinit var resources: Resources

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        tripsRepository = mockk(relaxed = true)
        userRepository = mockk(relaxed = true)
        context = mockk(relaxed = true)
        resources = mockk(relaxed = true)

        every { context.resources } returns resources

        viewModel = TripSettingsViewModel(tripsRepository, userRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun generateSuggestions_populatesSuggestions_whenEmpty() {
        // Given
        val grandTourArray = arrayOf(
            "Location1;46.0;6.0",
            "Location2;47.0;7.0",
            "Location3;48.0;8.0",
            "Location4;49.0;9.0",
            "Location5;50.0;10.0"
        )
        every { resources.getStringArray(R.array.grand_tour) } returns grandTourArray

        // When
        viewModel.generateSuggestions(context)

        // Then
        val suggestions = viewModel.suggestions.value
        assertEquals(5, suggestions.size)
        assertTrue(suggestions.any { it.name == "Location1" })
        assertTrue(suggestions.any { it.name == "Location5" })
    }

    @Test
    fun generateSuggestions_doesNotRegenerate_whenNotEmpty() {
        // Given
        val grandTourArray = arrayOf(
            "Location1;46.0;6.0",
            "Location2;47.0;7.0",
            "Location3;48.0;8.0",
            "Location4;49.0;9.0",
            "Location5;50.0;10.0"
        )
        every { resources.getStringArray(R.array.grand_tour) } returns grandTourArray

        // First generation
        viewModel.generateSuggestions(context)
        val firstSuggestions = viewModel.suggestions.value

        // When calling again
        viewModel.generateSuggestions(context)
        val secondSuggestions = viewModel.suggestions.value

        // Then
        assertEquals(firstSuggestions, secondSuggestions)
        verify(exactly = 1) { resources.getStringArray(R.array.grand_tour) }
    }

    @Test
    fun toggleSuggestion_addsSuggestion_whenNotSelected() {
        // Given
        val location = Location(Coordinate(46.0, 6.0), "Location1")

        // When
        viewModel.toggleSuggestion(location)

        // Then
        val selected = viewModel.selectedSuggestions.value
        assertEquals(1, selected.size)
        assertEquals(location, selected[0])
    }

    @Test
    fun toggleSuggestion_removesSuggestion_whenAlreadySelected() {
        // Given
        val location = Location(Coordinate(46.0, 6.0), "Location1")
        viewModel.toggleSuggestion(location) // Select it first

        // When
        viewModel.toggleSuggestion(location) // Deselect it

        // Then
        val selected = viewModel.selectedSuggestions.value
        assertTrue(selected.isEmpty())
    }
}
