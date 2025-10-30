package com.android.swisstravel.ui.edittrip

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.github.swent.swisstravel.model.trip.Location
import com.github.swent.swisstravel.model.trip.RouteSegment
import com.github.swent.swisstravel.model.trip.Trip
import com.github.swent.swisstravel.model.trip.TripProfile
import com.github.swent.swisstravel.model.trip.TripsRepository
import com.github.swent.swisstravel.model.trip.activity.Activity
import com.github.swent.swisstravel.model.user.Preference
import com.github.swent.swisstravel.ui.edittrip.EditTripScreen
import com.github.swent.swisstravel.ui.edittrip.EditTripScreenTestTags
import com.github.swent.swisstravel.ui.edittrip.EditTripScreenViewModel
import com.github.swent.swisstravel.ui.theme.SwissTravelTheme
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class EditTripScreenTest {

  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  private val tripId = "trip-123"
  private val initialTrip =
      makeTrip(
          uid = tripId,
          name = "Alpine Fun",
          adults = 2,
          children = 1,
          prefs = listOf(Preference.FOODIE, Preference.SPORTS))

  @Test
  fun showsLoading_thenContent() {
    val repo = FakeRepo(initialTrip).apply { delayGetTrip = true } // start as loading
    val vm = EditTripScreenViewModel(repo)

    var navigated = 0
    composeRule.setContent {
      SwissTravelTheme {
        EditTripScreen(
            tripId = tripId, editTripViewModel = vm, onBack = {}, onSaved = { navigated++ })
      }
    }

    // Loading visible
    composeRule.onNodeWithTag(EditTripScreenTestTags.LOADING).assertIsDisplayed()

    // Finish the suspended load deterministically
    repo.delayGetTrip = false
    repo.releaseGet() // <-- this is the key line

    // Let UI settle
    composeRule.waitForIdle()

    // Title visible
    composeRule
        .onNodeWithTag(EditTripScreenTestTags.TITLE)
        .assertIsDisplayed()
        .assertTextContains("Editing Alpine Fun")
  }

  @Test
  fun confirmSavesAndNavigates() {
    val repo = FakeRepo(initialTrip)
    val vm = EditTripScreenViewModel(repo)

    var navigated = 0
    composeRule.setContent {
      SwissTravelTheme {
        EditTripScreen(
            tripId = tripId, editTripViewModel = vm, onBack = {}, onSaved = { navigated++ })
      }
    }

    // Wait for content
    composeRule.onNodeWithTag(EditTripScreenTestTags.TITLE).assertIsDisplayed()

    composeRule.onNodeWithTag(EditTripScreenTestTags.CONFIRM).performClick()

    // save called exactly once and navigation fired
    assertEquals(1, repo.editCalls)
    assertEquals(1, navigated)
  }

  @Test
  fun deleteFlowShowsDialog_cancelDismisses_confirmDeletesAndNavigates() {
    val repo = FakeRepo(initialTrip)
    val vm = EditTripScreenViewModel(repo)

    var navigated = 0
    composeRule.setContent {
      SwissTravelTheme {
        EditTripScreen(
            tripId = tripId, editTripViewModel = vm, onBack = {}, onSaved = { navigated++ })
      }
    }

    // Ensure content visible
    composeRule.onNodeWithTag(EditTripScreenTestTags.TITLE).assertIsDisplayed()

    // If the Delete button is off-screen, scroll to it first.
    composeRule.onNodeWithTag(EditTripScreenTestTags.DELETE).performScrollTo().performClick()

    // The AlertDialog is often in a separate window; query with unmerged tree.
    composeRule
        .onNodeWithTag(EditTripScreenTestTags.DELETE_DIALOG, useUnmergedTree = true)
        .assertExists()
        .assertIsDisplayed()

    // Cancel closes the dialog
    composeRule
        .onNodeWithTag(EditTripScreenTestTags.DELETE_CANCEL, useUnmergedTree = true)
        .performClick()

    // Wait for it to disappear
    composeRule.waitUntil(timeoutMillis = 5_000) {
      composeRule
          .onAllNodesWithTag(EditTripScreenTestTags.DELETE_DIALOG, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isEmpty()
    }

    // Open again and confirm
    composeRule.onNodeWithTag(EditTripScreenTestTags.DELETE).performScrollTo().performClick()
    composeRule
        .onNodeWithTag(EditTripScreenTestTags.DELETE_CONFIRM, useUnmergedTree = true)
        .performClick()

    assertEquals(1, repo.deleteCalls)
    assertEquals(1, navigated)
  }

  // -------- helpers --------

  private fun makeTrip(
      uid: String,
      name: String,
      adults: Int,
      children: Int,
      prefs: List<Preference>
  ): Trip {
    val now = Timestamp.now()
    val profile =
        TripProfile(
            startDate = now,
            endDate = now,
            preferredLocations = emptyList(),
            preferences = prefs,
            adults = adults,
            children = children)
    return Trip(
        uid = uid,
        name = name,
        ownerId = "owner-1",
        locations = emptyList<Location>(),
        routeSegments = emptyList<RouteSegment>(),
        activities = emptyList<Activity>(),
        tripProfile = profile)
  }

  // inside your EditTripScreenTest.kt
  private class FakeRepo(private val trip: Trip) : TripsRepository {
    // Keep your old flags + counters
    var delayGetTrip: Boolean = false
    var editCalls: Int = 0
    var deleteCalls: Int = 0

    // Gate to deterministically hold getTrip() when delayGetTrip = true
    private var gate = kotlinx.coroutines.CompletableDeferred<Unit>()

    override fun getNewUid(): String = "new"

    override suspend fun getAllTrips(): List<Trip> = listOf(trip)

    override suspend fun getTrip(tripId: String): Trip {
      if (delayGetTrip) {
        // Suspend until released
        gate.await()
      }
      return trip
    }

    override suspend fun addTrip(trip: Trip) {
      /* no-op */
    }

    override suspend fun editTrip(tripId: String, updatedTrip: Trip) {
      editCalls++
    }

    override suspend fun deleteTrip(tripId: String) {
      deleteCalls++
    }

    // Call this to release a pending getTrip() suspension
    fun releaseGet() {
      if (!gate.isCompleted) gate.complete(Unit)
    }

    // Optional: reset the gate if you want to hold again later
    fun resetGate() {
      gate = kotlinx.coroutines.CompletableDeferred()
    }
  }
}
