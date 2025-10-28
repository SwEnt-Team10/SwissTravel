package com.android.swisstravel.utils

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.github.swent.swisstravel.HttpClientProvider
import com.github.swent.swisstravel.ui.currenttrip.CurrentTripScreenTestTags
import com.github.swent.swisstravel.ui.mytrips.MyTripsScreenTestTags
import com.github.swent.swisstravel.ui.navigation.NavigationTestTags
import com.github.swent.swisstravel.ui.profile.ProfileScreenTestTags
import com.google.firebase.auth.FirebaseUser
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Before

const val UI_WAIT_TIMEOUT = 15_000L
const val E2E_WAIT_TIMEOUT = 15_000L

/**
 * Base class for all SwissTravel tests, providing common setup and utility functions.
 *
 * It also handles automatic sign-in when required, which should be at all time.
 *
 * You should always make sure that the emulator is running before the tests, otherwise the tests
 * will fail.
 *
 * This test is mainly taken from the swent-EPFl repo.
 */
abstract class SwissTravelTest {

  // TODO : Implement the repository here
  // abstract fun createInitializedRepository(): SwissTravelRepository

  open fun initializeHTTPClient(): OkHttpClient = FakeHttpClient.getClient()

  //    val repository: SwissTravelRepository
  //        get() = SwissTravelRepository.repository

  val httpClient
    get() = HttpClientProvider.client

  val shouldSignInAnonymously: Boolean = FirebaseEmulator.isRunning

  val currentUser: FirebaseUser
    get() {
      return FirebaseEmulator.auth.currentUser!!
    }

  init {
    assert(FirebaseEmulator.isRunning) { "FirebaseEmulator must be running when running the tests" }
  }

  @Before
  // TODO : Set up repository when it is implemented
  open fun setUp() {
    // SwissTravelRepository.repository = createInitializedRepository()
    HttpClientProvider.client = initializeHTTPClient()
    if (shouldSignInAnonymously) {
      runTest { FirebaseEmulator.auth.signInAnonymously().await() }
    }
  }

  @After
  open fun tearDown() {
    if (FirebaseEmulator.isRunning) {
      FirebaseEmulator.auth.signOut()
      FirebaseEmulator.clearAuthEmulator()
    }
  }

  // TODO : Declare ComposeTestRules here

  fun ComposeTestRule.checkMyTripsScreenIsDisplayed() {
    onNodeWithTag(MyTripsScreenTestTags.PAST_TRIPS_BUTTON).assertIsDisplayed()
    onNodeWithTag(MyTripsScreenTestTags.UPCOMING_TRIPS_TITLE)
        .assertIsDisplayed()
        .assertTextContains("Upcoming Trip", substring = false, ignoreCase = true)
    onNodeWithTag(MyTripsScreenTestTags.CURRENT_TRIP_TITLE)
        .assertIsDisplayed()
        .assertTextContains("Current Trip", substring = false, ignoreCase = true)
  }

  fun ComposeTestRule.checkMyTripsScreenIsNotDisplayed() {
    onNodeWithTag(MyTripsScreenTestTags.PAST_TRIPS_BUTTON).assertDoesNotExist()
    onNodeWithTag(MyTripsScreenTestTags.UPCOMING_TRIPS_TITLE).assertDoesNotExist()
    onNodeWithTag(MyTripsScreenTestTags.CURRENT_TRIP_TITLE).assertDoesNotExist()
  }

  fun ComposeTestRule.checkCurrentTripScreenIsDisplayed() {
    onNodeWithTag(CurrentTripScreenTestTags.CREATE_TRIP_TEXT)
        .assertIsDisplayed()
        .assertTextContains("Create a trip", substring = false, ignoreCase = true)
    onNodeWithTag(CurrentTripScreenTestTags.CREATE_TRIP_BUTTON).assertIsDisplayed()
  }

  fun ComposeTestRule.checkCurrentTripScreenIsNotDisplayed() {
    onNodeWithTag(CurrentTripScreenTestTags.CREATE_TRIP_TEXT).assertDoesNotExist()
    onNodeWithTag(CurrentTripScreenTestTags.CREATE_TRIP_BUTTON).assertDoesNotExist()
  }

  fun ComposeTestRule.checkProfileScreenIsDisplayed() {
    onNodeWithTag(ProfileScreenTestTags.DISPLAY_NAME).assertIsDisplayed()
    onNodeWithTag(ProfileScreenTestTags.PREFERENCES_LIST).assertIsDisplayed()
    onNodeWithTag(ProfileScreenTestTags.EMAIL).assertIsDisplayed()
    onNodeWithTag(ProfileScreenTestTags.GREETING).assertIsDisplayed()
  }

  fun ComposeTestRule.checkProfileScreenIsNotDisplayed() {
    onNodeWithTag(ProfileScreenTestTags.PREFERENCES_LIST).assertDoesNotExist()
    onNodeWithTag(ProfileScreenTestTags.DISPLAY_NAME).assertDoesNotExist()
    onNodeWithTag(ProfileScreenTestTags.EMAIL).assertDoesNotExist()
    onNodeWithTag(ProfileScreenTestTags.GREETING).assertDoesNotExist()
  }

  fun ComposeTestRule.checkNavigationMenuIsDisplayed() {
    onNodeWithTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU).assertIsDisplayed()
  }

  fun ComposeTestRule.checkTopBarIsDisplayed() {
    onNodeWithTag(NavigationTestTags.TOP_BAR).assertIsDisplayed()
    onNodeWithTag(NavigationTestTags.TOP_BAR_BUTTON).assertIsDisplayed()
  }

  fun <A : ComponentActivity> AndroidComposeTestRule<ActivityScenarioRule<A>, A>
      .checkActivityStateOnPressBack(shouldFinish: Boolean) {
    activityRule.scenario.onActivity { activity ->
      activity.onBackPressedDispatcher.onBackPressed()
    }
    waitUntil { activity.isFinishing == shouldFinish }
    assertEquals(shouldFinish, activity.isFinishing)
  }

  // TODO : Create helper/companions functions here

}
