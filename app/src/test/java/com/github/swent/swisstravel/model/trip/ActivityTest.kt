package com.github.swent.swisstravel.model.trip

import com.github.swent.swisstravel.model.trip.activity.Activity
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Test

class ActivityTest {
  private val activity =
      Activity(
          startDate = Timestamp(1734000000, 0),
          endDate = Timestamp(1734003600, 0),
          location =
              Location(name = "Jet d'eau de Genève", coordinate = Coordinate(46.2074, 6.1551)),
          description = "Description",
          imageUrls = emptyList(),
          estimatedTime = 3600)

  @Test
  fun testGetName() {
    assertEquals("Jet d'eau de Genève", activity.getName())
  }

  @Test
  fun testEstimatedTime() {
    assertEquals(60, activity.estimatedTime())
  }

  @Test
  fun testIsValid() {
    val blacklistedNames = setOf("Banned Activity")
    val invalidDescription = "No description available"

    // Valid activity
    assertEquals(true, activity.isValid(blacklistedNames, invalidDescription))

    // Blacklisted name
    val blacklistedActivity =
        activity.copy(location = activity.location.copy(name = "Banned Activity"))
    assertEquals(false, blacklistedActivity.isValid(blacklistedNames, invalidDescription))

    // Blank description
    val blankDescriptionActivity = activity.copy(description = "")
    assertEquals(false, blankDescriptionActivity.isValid(blacklistedNames, invalidDescription))

    // Invalid description
    val invalidDescriptionActivity = activity.copy(description = invalidDescription)
    assertEquals(false, invalidDescriptionActivity.isValid(blacklistedNames, invalidDescription))

    // Non-positive estimated time
    val nonPositiveEstimatedTimeActivity = activity.copy(estimatedTime = 0)
    assertEquals(
        false, nonPositiveEstimatedTimeActivity.isValid(blacklistedNames, invalidDescription))
  }
}
