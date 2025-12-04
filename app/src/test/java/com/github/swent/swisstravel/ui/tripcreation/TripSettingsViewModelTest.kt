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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Some tests were written with the help of AI. */
@ExperimentalCoroutinesApi
class TripSettingsViewModelTest {

  private lateinit var viewModel: TripSettingsViewModel
  private lateinit var tripsRepository: TripsRepository
  private lateinit var userRepository: UserRepository
  private lateinit var context: Context
  private lateinit var resources: Resources

  private val testDispatcher = StandardTestDispatcher()

  // Test data
  private val geneva = Location(Coordinate(46.2044, 6.1432), "Geneva", "")
  private val lausanne = Location(Coordinate(46.5197, 6.6323), "Lausanne", "") // ~54km from Geneva
  private val bern = Location(Coordinate(46.9480, 7.4474), "Bern", "") // ~95km from Lausanne
  private val fribourg = Location(Coordinate(46.8065, 7.1616), "Fribourg", "") // ~28km from Bern
  private val interlaken =
      Location(Coordinate(46.6863, 7.8632), "Interlaken", "") // ~45km from Bern
  private val zurich = Location(Coordinate(47.3769, 8.5417), "Zurich", "")

  private val grandTourLocations = listOf(geneva, lausanne, bern, fribourg, interlaken, zurich)
  private val grandTourStrings =
      grandTourLocations
          .map { "${it.name};${it.coordinate.latitude};${it.coordinate.longitude}" }
          .toTypedArray()

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    tripsRepository = mockk(relaxed = true)
    userRepository = mockk(relaxed = true)
    context = mockk(relaxed = true)
    resources = mockk(relaxed = true)

    every { context.resources } returns resources
    every { resources.getStringArray(R.array.grand_tour) } returns grandTourStrings

    viewModel = TripSettingsViewModel(tripsRepository, userRepository)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun generateSuggestions_populatesSuggestions_whenEmpty() {
    // When
    viewModel.generateSuggestions(context)

    // Then
    val suggestions = viewModel.suggestions.value
    assertEquals(5, suggestions.size)
    assertTrue(suggestions.all { grandTourLocations.contains(it) })
  }

  @Test
  fun generateSuggestions_doesNotRegenerate_whenNotEmpty() {
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
  fun generateSuggestions_providesIdealSuggestions_withinRange() {
    // Given a user has selected Bern
    viewModel.updateArrivalLocation(bern)

    // When
    viewModel.generateSuggestions(context)

    // Then suggestions should be in the ideal range (15-50km) from Bern
    // Fribourg (~28km) and Interlaken (~45km) are ideal.
    val suggestions = viewModel.suggestions.value
    assertTrue(suggestions.isNotEmpty())
    assertTrue(suggestions.any { it.name == "Fribourg" })
    assertTrue(suggestions.any { it.name == "Interlaken" })
    assertFalse(suggestions.any { it.name == "Bern" }) // Should not suggest itself

    suggestions.forEach {
      val distance = it.haversineDistanceTo(bern)
      assertTrue("Suggestion ${it.name} is not in ideal range", distance > 15 && distance < 50)
    }
  }

  @Test
  fun generateSuggestions_fallsBackToNearest_whenNoIdealSuggestionsFound() {
    // Given a user has selected Geneva. The nearest location is Lausanne (~54km),
    // which is outside the ideal range of <50km.
    viewModel.updateArrivalLocation(geneva)

    // When
    viewModel.generateSuggestions(context)

    // Then the suggestions should be the nearest locations, sorted by distance
    val suggestions = viewModel.suggestions.value
    assertTrue(suggestions.isNotEmpty())
    // The closest location in our test set to Geneva is Lausanne.
    assertEquals("Lausanne", suggestions.first().name)
  }

  @Test
  fun generateSuggestions_excludesAlreadySelectedLocations() {
    // Given user has selected Bern as arrival and Fribourg as a destination
    viewModel.updateArrivalLocation(bern)
    viewModel.updateDepartureLocation(fribourg) // Add Fribourg to selected suggestions

    // When
    viewModel.generateSuggestions(context)

    // Then suggestions should not include Bern or Fribourg
    val suggestions = viewModel.suggestions.value
    assertFalse(suggestions.any { it.name == "Bern" })
    assertFalse(suggestions.any { it.name == "Fribourg" })

    // The only remaining ideal location near Bern is Interlaken
    assertTrue(suggestions.any { it.name == "Interlaken" })
    assertEquals(1, suggestions.size)
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
