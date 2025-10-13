package com.android.swisstravel.ui.tripSettings

import com.github.swent.swisstravel.ui.tripSettings.TripPreferences
import com.github.swent.swisstravel.ui.tripSettings.TripSettingsViewModel
import com.github.swent.swisstravel.ui.tripSettings.ValidationEvent
import java.time.LocalDate
import junit.framework.TestCase
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

  private lateinit var viewModel: TripSettingsViewModel

  @OptIn(ExperimentalCoroutinesApi::class)
  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    viewModel = TripSettingsViewModel()
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
}
