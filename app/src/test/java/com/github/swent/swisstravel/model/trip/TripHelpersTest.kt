package com.github.swent.swisstravel.model.trip

import com.google.firebase.Timestamp
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TripHelpersTest {

  private fun tsFromInstant(inst: Instant) = Timestamp(Date.from(inst))

  @Test
  fun `toZonedDateTime converts timestamp preserving instant`() {
    val instant = Instant.now()
    val ts = tsFromInstant(instant)
    val zdt = ts.toZonedDateTime()

    // compare instants truncated to seconds to avoid nanosecond instability
    assertEquals(
        instant.truncatedTo(ChronoUnit.SECONDS), zdt.toInstant().truncatedTo(ChronoUnit.SECONDS))
  }

  @Test
  fun `isUpcoming returns true when startDate is in the future`() {
    val start = Instant.now().plus(1, ChronoUnit.DAYS)
    val end = start.plus(1, ChronoUnit.DAYS)
    val tripProfile =
        TripProfile(
            startDate = tsFromInstant(start),
            endDate = tsFromInstant(end),
            preferredLocations = emptyList(),
            preferences = emptyList())
    val trip =
        Trip(
            uid = "testUid",
            name = "testName",
            ownerId = "testOwner",
            locations = emptyList(),
            routeSegments = emptyList(),
            activities = emptyList(),
            tripProfile = tripProfile,
            isFavorite = false,
            isCurrentTrip = false,
            listUri = emptyList(),
            collaboratorsId = emptyList())

    assertTrue(trip.isUpcoming())
    assertFalse(trip.isCurrent())
    assertFalse(trip.isPast())
  }

  @Test
  fun `isCurrent returns true when it is set as such`() {
    val start = Instant.now().minus(1, ChronoUnit.DAYS)
    val end = Instant.now().plus(1, ChronoUnit.DAYS)
    val tripProfile =
        TripProfile(
            startDate = tsFromInstant(start),
            endDate = tsFromInstant(end),
            preferredLocations = emptyList(),
            preferences = emptyList())
    val trip =
        Trip(
            uid = "testUid",
            name = "testName",
            ownerId = "testOwner",
            locations = emptyList(),
            routeSegments = emptyList(),
            activities = emptyList(),
            tripProfile = tripProfile,
            isFavorite = false,
            isCurrentTrip = true,
            listUri = emptyList(),
            collaboratorsId = emptyList())

    assertTrue(trip.isCurrent())
    assertFalse(trip.isUpcoming())
    assertFalse(trip.isPast())
  }

  @Test
  fun `isPast returns true when endDate is before now`() {
    val end = Instant.now().minus(1, ChronoUnit.DAYS)
    val start = end.minus(2, ChronoUnit.DAYS)
    val tripProfile =
        TripProfile(
            startDate = tsFromInstant(start),
            endDate = tsFromInstant(end),
            preferredLocations = emptyList(),
            preferences = emptyList())
    val trip =
        Trip(
            uid = "testUid",
            name = "testName",
            ownerId = "testOwner",
            locations = emptyList(),
            routeSegments = emptyList(),
            activities = emptyList(),
            tripProfile = tripProfile,
            isFavorite = false,
            isCurrentTrip = false,
            listUri = emptyList(),
            collaboratorsId = emptyList())

    assertTrue(trip.isPast())
    assertFalse(trip.isCurrent())
    assertFalse(trip.isUpcoming())
  }

  @Test
  fun `isOwner returns true only for ownerId`() {
    val trip =
        Trip(
            uid = "testUid",
            name = "testName",
            ownerId = "owner-123",
            locations = emptyList(),
            routeSegments = emptyList(),
            activities = emptyList(),
            tripProfile =
                TripProfile(
                    startDate = tsFromInstant(Instant.now()),
                    endDate = tsFromInstant(Instant.now()),
                    preferredLocations = emptyList(),
                    preferences = emptyList()),
            isFavorite = false,
            isCurrentTrip = false,
            listUri = emptyList(),
            collaboratorsId = listOf("collab-1", "collab-2"))

    assertTrue(trip.isOwner("owner-123"))
    assertFalse(trip.isOwner("collab-1"))
    assertFalse(trip.isOwner("random-user"))
  }

  @Test
  fun `canEdit returns true for owner and collaborators only`() {
    val trip =
        Trip(
            uid = "testUid",
            name = "testName",
            ownerId = "owner-123",
            locations = emptyList(),
            routeSegments = emptyList(),
            activities = emptyList(),
            tripProfile =
                TripProfile(
                    startDate = tsFromInstant(Instant.now()),
                    endDate = tsFromInstant(Instant.now()),
                    preferredLocations = emptyList(),
                    preferences = emptyList()),
            isFavorite = false,
            isCurrentTrip = false,
            listUri = emptyList(),
            collaboratorsId = listOf("collab-1", "collab-2"))

    assertTrue(trip.canEdit("owner-123")) // owner can edit
    assertTrue(trip.canEdit("collab-1")) // collaborator can edit
    assertTrue(trip.canEdit("collab-2")) // collaborator can edit
    assertFalse(trip.canEdit("random-user")) // others cannot edit
  }
}
