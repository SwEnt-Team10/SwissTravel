package com.android.swisstravel.utils

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.github.swent.swisstravel.HttpClientProvider
import com.google.firebase.auth.FirebaseUser
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Before

const val UI_WAIT_TIMEOUT = 5_000L

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
