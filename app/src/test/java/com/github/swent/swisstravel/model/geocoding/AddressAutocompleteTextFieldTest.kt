package com.github.swent.swisstravel.model.geocoding

import com.github.swent.swisstravel.model.map.LocationRepository
import com.github.swent.swisstravel.model.trip.Coordinate
import com.github.swent.swisstravel.model.trip.Location
import com.github.swent.swisstravel.ui.geocoding.AddressTextFieldViewModel
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddressTextFieldViewModelTest {

    private lateinit var viewModel: AddressTextFieldViewModel
    private lateinit var repository: LocationRepository

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
        viewModel = AddressTextFieldViewModel().apply {
            val field = AddressTextFieldViewModel::class.java.getDeclaredField("locationRepository")
            field.isAccessible = true
            field.set(this, repository)
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun setLocationShouldUpdateSelectedLocationAndQuery() {
        val location = Location(Coordinate(46.5197, 6.6323), "Lausanne")
        viewModel.setLocation(location)

        val state = viewModel.addressState.value
        assertEquals(location, state.selectedLocation)
        assertEquals("Lausanne", state.locationQuery)
    }

    @Test
    fun setLocationQueryShouldFetchSuggestionsFromRepository() = runTest {
        val fakeResults = listOf(Location(Coordinate(46.2, 6.1), "Geneva"))
        coEvery { repository.search("Gen") } returns fakeResults

        viewModel.setLocationQuery("Gen")
        advanceUntilIdle()

        val state = viewModel.addressState.value
        assertEquals(fakeResults, state.locationSuggestions)
    }

    @Test
    fun setLocationQueryShouldHandleEmptyQuery() = runTest {
        viewModel.setLocationQuery("")

        val state = viewModel.addressState.value
        assertTrue(state.locationSuggestions.isEmpty())
    }

    @Test
    fun setLocationQueryShouldClearSuggestionsOnException() = runTest {
        coEvery { repository.search("Paris") } throws RuntimeException("Network error")

        viewModel.setLocationQuery("Paris")
        advanceUntilIdle()

        val state = viewModel.addressState.value
        assertTrue(state.locationSuggestions.isEmpty())
    }
}
