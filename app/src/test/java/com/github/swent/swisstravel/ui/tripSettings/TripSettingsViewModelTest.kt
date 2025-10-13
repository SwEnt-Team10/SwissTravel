package com.github.swent.swisstravel.ui.tripSettings

import java.time.LocalDate
import junit.framework.TestCase.assertEquals
import org.junit.Before
import org.junit.Test

class TripSettingsViewModelTest {

  private lateinit var viewModel: TripSettingsViewModel

  @Before
  fun setUp() {
    viewModel = TripSettingsViewModel()
  }

  @Test
  fun `updateDates should update the date in the trip settings`() {
    val startDate = LocalDate.of(2025, 1, 1)
    val endDate = LocalDate.of(2025, 1, 1)

    viewModel.updateDates(startDate, endDate)

    val newDate = viewModel.tripSettings.value.date
    assertEquals(startDate, newDate.startDate)
    assertEquals(endDate, newDate.endDate)
  }

  @Test
  fun `updateTravelers should update the travelers in the trip settings`() {
    val adults = 2
    val children = 1

    viewModel.updateTravelers(adults, children)

    val newTravelers = viewModel.tripSettings.value.travelers
    assertEquals(adults, newTravelers.adults)
    assertEquals(children, newTravelers.children)
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
    assertEquals(preferences, newPreferences)
  }
}
