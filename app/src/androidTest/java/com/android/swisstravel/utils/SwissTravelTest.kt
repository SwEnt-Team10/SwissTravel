package com.android.swisstravel.utils

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.github.swent.swisstravel.HttpClientProvider
import com.github.swent.swisstravel.ui.CurrentTripScreen
import com.github.swent.swisstravel.ui.CurrentTripScreenTestTags
import com.github.swent.swisstravel.ui.ProfileScreenTestTags
import com.github.swent.swisstravel.ui.navigation.NavigationTestTags
import com.google.firebase.auth.FirebaseUser
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Before

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

  //    val repository: ToDosRepository
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
//    onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE)
//      .assertIsDisplayed()
//     .assertTextContains("My Trips", substring = false, ignoreCase = true)
//    // TODO what defines the MyTripScreen that is different from others like the top bar
  }

  fun ComposeTestRule.checkMyTripsScreenIsNotDisplayed() {
//    onNodeWithTag(ProfileScreenTestTags.DROPDOWN_PREFERENCES)
//      .assertDoesNotExist()
  // TODO
  }

  fun ComposeTestRule.checkCurrentTripScreenIsDisplayed() {
    onNodeWithTag(CurrentTripScreenTestTags.TEMPORARY_TEST_TAG)
      .assertIsDisplayed()
      .assertTextContains("Current Trip", substring = false, ignoreCase = true)
    // TODO what defines the CurrentTrip that is different from others like the top bar
  }

  fun ComposeTestRule.checkCurrentTripScreenIsNotDisplayed() {
    onNodeWithTag(CurrentTripScreenTestTags.TEMPORARY_TEST_TAG)
      .assertDoesNotExist()
    // TODO Change this
  }

  fun ComposeTestRule.checkProfileScreenIsDisplayed() {
    onNodeWithTag(ProfileScreenTestTags.DISPLAY_NAME)
      .assertIsDisplayed()
    onNodeWithTag(ProfileScreenTestTags.DROPDOWN_PREFERENCES)
      .assertIsDisplayed()
    onNodeWithTag(ProfileScreenTestTags.EMAIL)
      .assertIsDisplayed()
    onNodeWithTag(ProfileScreenTestTags.GREETING)
      .assertIsDisplayed()
  }

  fun ComposeTestRule.checkProfileScreenIsNotDisplayed() {
    onNodeWithTag(ProfileScreenTestTags.DROPDOWN_PREFERENCES)
      .assertDoesNotExist()
    onNodeWithTag(ProfileScreenTestTags.DISPLAY_NAME)
      .assertDoesNotExist()
    onNodeWithTag(ProfileScreenTestTags.EMAIL)
      .assertDoesNotExist()
    onNodeWithTag(ProfileScreenTestTags.GREETING)
      .assertDoesNotExist()
  }

  fun ComposeTestRule.checkNavigationMenuIsDisplayed() {
    onNodeWithTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU)
      .assertIsDisplayed()
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
