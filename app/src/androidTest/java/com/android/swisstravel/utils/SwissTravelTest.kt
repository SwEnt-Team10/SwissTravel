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

/**
 * Base class for all Bootcamp tests, providing common setup and utility functions.
 *
 * It also handles gracefully automatic sign-in when required by the milestone.
 *
 * For the B1 tests, it is quite tricky. During the first week, emulators are not set up, so we
 * can't simply sign-in anonymously. However, during week 3, B1 tests won't pass if we do not
 * sign-in automatically. Hence, to detect that we are running B1 tests during the first week, we
 * check if the Firebase emulators are running. If they are not running *by mistake*, B2 and B3
 * tests will fail, notifying the user that they need to start the emulators.
 */
abstract class SwissTravelTest {

    //TODO : Implement the repository here
    //abstract fun createInitializedRepository(): SwissTravelRepository

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
        assert(FirebaseEmulator.isRunning) {
            "FirebaseEmulator must be running when running the tests"
        }
    }

    @Before
    open fun setUp() {
        //uncomment this line when repository is set up
        //SwissTravelRepository.repository = createInitializedRepository()
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

        //TODO : Declare ComposeTestRules here

        fun <A : ComponentActivity> AndroidComposeTestRule<ActivityScenarioRule<A>, A>
                .checkActivityStateOnPressBack(shouldFinish: Boolean) {
            activityRule.scenario.onActivity { activity ->
                activity.onBackPressedDispatcher.onBackPressed()
            }
            waitUntil { activity.isFinishing == shouldFinish }
            assertEquals(shouldFinish, activity.isFinishing)
        }

        //TODO : Create helper/companions functions here

}
