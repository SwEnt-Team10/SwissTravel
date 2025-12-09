package com.github.swent.swisstravel.ui.trip.edittrip

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.performTextInput
import com.github.swent.swisstravel.model.trip.Trip
import com.github.swent.swisstravel.model.trip.TripProfile
import com.github.swent.swisstravel.model.trip.TripsRepository
import com.github.swent.swisstravel.model.user.Preference
import com.github.swent.swisstravel.ui.navigation.NavigationTestTags
import com.github.swent.swisstravel.ui.theme.SwissTravelTheme
import com.google.firebase.Timestamp
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
          prefs = listOf(Preference.FOODIE, Preference.SPORTS),
          random = false)

  private val randomTrip =
      makeTrip(
          uid = "trip-random",
          name = "Random Adventure",
          adults = 1,
          children = 0,
          prefs = listOf(Preference.PUBLIC_TRANSPORT),
          random = true)

  @Test
  fun showsLoading_thenContent() {
    val repo = FakeRepo(initialTrip).apply { delayGetTrip = true } // start as loading
    val vm = EditTripScreenViewModel(repo)

    composeRule.setContent {
      SwissTravelTheme {
        EditTripScreen(
            tripId = tripId, editTripViewModel = vm, onBack = {}, onSaved = {}, onDelete = {})
      }
    }

    // Loading visible
    composeRule.onNodeWithTag(EditTripScreenTestTags.LOADING).assertIsDisplayed()

    // Finish the suspended load deterministically
    repo.releaseGet()

    // Let UI settle
    composeRule.waitForIdle()

    // Text field visible
    composeRule
        .onNodeWithTag(EditTripScreenTestTags.TRIP_NAME)
        .assertIsDisplayed()
        .assertTextContains(initialTrip.name)
  }

  @Test
  fun confirmSavesAndNavigates() {
    val repo = FakeRepo(initialTrip)
    val vm = EditTripScreenViewModel(repo)
    var saved = false
    composeRule.setContent {
      SwissTravelTheme {
        EditTripScreen(
            tripId = tripId,
            editTripViewModel = vm,
            onBack = {},
            onSaved = { saved = true },
            onDelete = {})
      }
    }

    // Wait for content
    composeRule.onNodeWithTag(EditTripScreenTestTags.TRIP_NAME).assertIsDisplayed()

    composeRule.onNodeWithTag(EditTripScreenTestTags.CONFIRM_TOP_BAR).performClick()

    composeRule.waitUntil { repo.editCalls > 0 }
    assertTrue(saved)
  }

  @Test
  fun deleteFlowShowsDialog_cancelDismisses_confirmDeletesAndNavigates() {
    val repo = FakeRepo(initialTrip)
    val vm = EditTripScreenViewModel(repo)

    var navigated = 0
    composeRule.setContent {
      SwissTravelTheme {
        EditTripScreen(
            tripId = tripId,
            editTripViewModel = vm,
            onBack = {},
            onSaved = { navigated++ },
            onDelete = { navigated++ })
      }
    }

    // Ensure content visible
    composeRule.onNodeWithTag(EditTripScreenTestTags.TRIP_NAME).assertIsDisplayed()

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

  @Test
  fun backButtonCallsOnBack() {
    val repo = FakeRepo(initialTrip)
    val vm = EditTripScreenViewModel(repo)

    var backCalls = 0
    composeRule.setContent {
      SwissTravelTheme {
        EditTripScreen(
            tripId = tripId,
            editTripViewModel = vm,
            onBack = { backCalls++ },
            onSaved = {},
            onDelete = {})
      }
    }

    // Ensure content visible so app bar is there
    composeRule.onNodeWithTag(EditTripScreenTestTags.TRIP_NAME).assertIsDisplayed()

    // Click the top bar close button
    composeRule.onNodeWithTag(NavigationTestTags.TOP_BAR_BUTTON).performClick()

    assertEquals(1, backCalls)
  }

  @Test
  fun confirmDisabledWhileLoading_thenEnabledAfterLoad() {
    val repo = FakeRepo(initialTrip).apply { delayGetTrip = true }
    val vm = EditTripScreenViewModel(repo)

    composeRule.setContent {
      SwissTravelTheme {
        EditTripScreen(
            tripId = tripId, editTripViewModel = vm, onBack = {}, onSaved = {}, onDelete = {})
      }
    }

    // Spinner shown, confirm disabled
    composeRule.onNodeWithTag(EditTripScreenTestTags.LOADING).assertIsDisplayed()
    composeRule.onNodeWithTag(EditTripScreenTestTags.CONFIRM_TOP_BAR).assertIsNotEnabled()

    // Finish load
    repo.releaseGet()
    composeRule.waitForIdle()

    // Text field visible, confirm enabled
    composeRule.onNodeWithTag(EditTripScreenTestTags.TRIP_NAME).assertIsDisplayed()
    composeRule.onNodeWithTag(EditTripScreenTestTags.CONFIRM_TOP_BAR).assertIsEnabled()
  }

  @Test
  fun editingTripName_isPersistedOnSave() {
    val repo = FakeRepo(initialTrip)
    val vm = EditTripScreenViewModel(repo)

    composeRule.setContent {
      SwissTravelTheme {
        EditTripScreen(
            tripId = tripId, editTripViewModel = vm, onBack = {}, onSaved = {}, onDelete = {})
      }
    }

    // Wait for content
    composeRule.onNodeWithTag(EditTripScreenTestTags.TRIP_NAME).assertIsDisplayed()

    // Type a new name
    val newName = "Alpine Fun X"
    val field = composeRule.onNodeWithTag(EditTripScreenTestTags.TRIP_NAME)
    field.performClick() // ensure focus
    field.performTextClearance()
    field.performTextInput(newName)

    // Save
    composeRule.onNodeWithTag(EditTripScreenTestTags.CONFIRM_TOP_BAR).performClick()

    composeRule.waitUntil { repo.lastEditedTrip != null }
    assertEquals(newName, repo.lastEditedTrip?.name)
  }

  // Trips were generated by AI
  @Test
  fun rerollButton_isVisibleForRandomTripsOnly1() {
    // Case 1: Non-random trip
    val repo = FakeRepo(initialTrip)
    val vm = EditTripScreenViewModel(repo)
    composeRule.setContent {
      EditTripScreen(
          tripId = tripId, editTripViewModel = vm, onBack = {}, onSaved = {}, onDelete = {})
    }
    composeRule.waitForIdle()
    composeRule.onNodeWithTag(EditTripScreenTestTags.REROLL).assertDoesNotExist()
  }

  @Test
  fun rerollButton_isVisibleForRandomTripsOnly2() {
    // Case 2: Random trip
    val randomRepo = FakeRepo(randomTrip)
    val randomVm = EditTripScreenViewModel(randomRepo)
    composeRule.setContent {
      SwissTravelTheme {
        EditTripScreen(
            tripId = randomTrip.uid,
            editTripViewModel = randomVm,
            onBack = {},
            onSaved = {},
            onDelete = {})
      }
    }
    composeRule.waitForIdle()
    composeRule.onNodeWithTag(EditTripScreenTestTags.REROLL).assertExists()
  }

  // -------- helpers --------

  private fun makeTrip(
      uid: String,
      name: String,
      adults: Int,
      children: Int,
      prefs: List<Preference>,
      random: Boolean
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
        locations = emptyList(),
        routeSegments = emptyList(),
        activities = emptyList(),
        tripProfile = profile,
        isFavorite = false,
        isCurrentTrip = false,
        listUri = emptyList(),
        isRandom = random)
  }

  private class FakeRepo(private var trip: Trip) : TripsRepository {
    var delayGetTrip: Boolean = false
    var editCalls: Int = 0
    var deleteCalls: Int = 0
    private var gate = CompletableDeferred<Unit>()

    override fun getNewUid(): String = "new"

    override suspend fun getAllTrips(): List<Trip> = listOf(trip)

    override suspend fun getTrip(tripId: String): Trip {
      if (delayGetTrip) {
        gate.await()
      }
      return trip
    }

    override suspend fun addTrip(trip: Trip) {
      /* no-op */
    }

    var lastEditedTripId: String? = null
    var lastEditedTrip: Trip? = null

    override suspend fun editTrip(tripId: String, updatedTrip: Trip) {
      editCalls++
      lastEditedTripId = tripId
      lastEditedTrip = updatedTrip
      // a reroll might change the trip instance
      this.trip = updatedTrip
    }

    override suspend fun deleteTrip(tripId: String) {
      deleteCalls++
    }

    fun releaseGet() {
      if (!gate.isCompleted) gate.complete(Unit)
    }
  }
}
