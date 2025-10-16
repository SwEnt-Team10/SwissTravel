package com.android.swisstravel.data.trips

import com.github.swent.swisstravel.model.trip.Coordinate
import com.github.swent.swisstravel.model.trip.Location
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
          description = "Description")

  @Test
  fun testGetName() {
    assertEquals("Jet d'eau de Genève", activity.getName())
  }

  @Test
  fun testEstimatedTime() {
    assertEquals(60, activity.estimatedTime())
  }
}
