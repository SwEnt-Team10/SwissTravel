package com.github.swent.swisstravel.e2e

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.github.swent.swisstravel.SwissTravelApp
import com.github.swent.swisstravel.model.trip.TripsRepositoryFirestore
import com.github.swent.swisstravel.model.user.UserRepositoryFirebase
import com.github.swent.swisstravel.model.user.UserUpdate
import com.github.swent.swisstravel.ui.navigation.NavigationTestTags
import com.github.swent.swisstravel.ui.profile.ProfileScreenTestTags
import com.github.swent.swisstravel.ui.trip.edittrip.EditTripScreenTestTags
import com.github.swent.swisstravel.ui.trip.tripinfos.DailyViewScreenTestTags
import com.github.swent.swisstravel.utils.FakeCredentialManager
import com.github.swent.swisstravel.utils.FakeJwtGenerator
import com.github.swent.swisstravel.utils.FirebaseEmulator
import com.github.swent.swisstravel.utils.FirestoreSwissTravelTest
import com.google.firebase.Timestamp
import java.time.LocalDateTime.now
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * End-to-end tests for the collaboration flow in SwissTravel.
 *
 * This test verifies the complete lifecycle of trip sharing, including:
 * - User setup (Alice and Bob)
 * - Friend request process
 * - Trip creation and sharing by Owner (Alice)
 * - Collaborator (Bob) accessing the shared trip
 * - Owner modifying the trip and Collaborator seeing updates
 * - Owner removing Collaborator
 * - Collaborator losing access
 *
 * Parts of this code was written with the help of AI, but was mostly done by hand and reviewed
 * manually.
 */
class E2ECollaboratorsTest : FirestoreSwissTravelTest() {

  @get:Rule val composeTestRule = createComposeRule()

  private val aliceName = "Alice Owner"
  private val aliceEmail = "alice.owner@example.com"
  private var aliceUid = "alice_uid"

  private val bobName = "Bob Collaborator"
  private val bobEmail = "bob.collab@example.com"
  private var bobUid = "bob_uid"

  @Before
  override fun setUp() {
    super.setUp()
    FirebaseEmulator.auth.signOut()
    FirebaseEmulator.clearAuthEmulator()
    FirebaseEmulator.clearFirestoreEmulator()
    // 1. Setup Tokens
    val aliceToken = FakeJwtGenerator.createFakeGoogleIdToken(aliceName, aliceEmail)
    val bobToken = FakeJwtGenerator.createFakeGoogleIdToken(bobName, bobEmail)

    // 2. Define Login Sequence
    // Bob (Setup) -> Alice (Setup & Share) -> Bob (Accept) -> Alice (Share) -> Bob (Check)
    val fakeCredentialManager =
        FakeCredentialManager.sequence(
            bobToken,
            aliceToken,
            bobToken,
            aliceToken,
            bobToken,
            aliceToken,
            bobToken,
            aliceToken,
            bobToken)

    composeTestRule.setContent { SwissTravelApp(credentialManager = fakeCredentialManager) }
  }

  @After
  override fun tearDown() {
    super.tearDown()
    FirebaseEmulator.clearFirestoreEmulator()
  }

  /**
   * Verifies the complete collaboration flow between two users (Alice and Bob).
   *
   * Phases:
   * 0. Bob Setup: Log in, create account.
   * 1. Alice Setup & Friend Request: Log in, create account, add Bob. 1.5. Bob Accepts: Bob accepts
   *    friend request.
   * 2. Alice Creates & Shares: Alice creates a trip, shares it with Bob.
   * 3. Bob Accesses: Bob logs in, sees the trip in 'My Trips', can view details (but not edit).
   * 4. Alice Modifies: Alice edits trip name.
   * 5. Bob Verifies: Bob sees the updated name.
   * 6. Alice Removes: Alice removes Bob as collaborator.
   * 7. Bob Check: Bob no longer sees the trip.
   */
  @Test
  fun complete_collaboration_flow() {
    // Use Real Repository connected to Emulator
    val userRepo = UserRepositoryFirebase(FirebaseEmulator.auth, FirebaseEmulator.firestore)
    val tripsRepo = TripsRepositoryFirestore(FirebaseEmulator.firestore, FirebaseEmulator.auth)

    // =================================================================================
    // PHASE 0: BOB SETUP (Create Account)
    // =================================================================================
    composeTestRule.loginWithGoogle(isE2E = true) // Login as Bob

    runBlocking {
      // Ensure Bob exists in Firestore
      val bobUser = userRepo.getUserByNameOrEmail(bobEmail).first()
      bobUid = bobUser.uid
      userRepo.updateUser(bobUser.uid, UserUpdate(name = bobName))

      composeTestRule.logout()
    }

    // =================================================================================
    // PHASE 1: ALICE SETUP & FRIEND REQUEST
    // =================================================================================
    composeTestRule.loginWithGoogle(isE2E = true) // Login as Alice

    runBlocking {
      // Ensure Alice exists in Firestore
      val aliceUser = userRepo.getUserByNameOrEmail(aliceEmail).first()
      aliceUid = aliceUser.uid
      userRepo.updateUser(aliceUser.uid, UserUpdate(name = aliceName))

      // Send friend request to Bob
      userRepo.sendFriendRequest(aliceUid, bobUid)
    }

    composeTestRule.logout()

    // =================================================================================
    // PHASE 1.5: BOB ACCEPTS FRIEND REQUEST
    // =================================================================================
    composeTestRule.loginWithGoogle(isE2E = true) // Login as Bob again

    runBlocking { userRepo.acceptFriendRequest(bobUid, aliceUid) }

    composeTestRule.logout()

    // =================================================================================
    // PHASE 2: ALICE CREATES TRIP & SHARES
    // =================================================================================
    composeTestRule.loginWithGoogle(isE2E = true) // Login as Alice

    var tripName = "Alice's Grand Tour"

    runBlocking {
      // Create Trip for Alice

      val currentAuthUid = FirebaseEmulator.auth.currentUser!!.uid

      // Ensure the trip is UPCOMING so it appears in My Trips (endDate > now)
      val now = Timestamp(Timestamp.now().seconds + 1800L, 0)
      val end = Timestamp(now.seconds + 3600, 0)

      val trip =
          createTestTrip(
              uid = "alice_trip_1",
              name = tripName,
              ownerId = currentAuthUid,
              startDate = now,
              endDate = end,
              departureLocation = dummyLocation,
              arrivalLocation = dummyLocation,
              preferredLocations = listOf(dummyLocation))
      tripsRepo.addTrip(trip)
    }

    // 1. Force Refresh: Go to Profile then My Trips
    composeTestRule.onNodeWithTag(NavigationTestTags.PROFILE_TAB).performClick()
    composeTestRule.waitForTag(ProfileScreenTestTags.PROFILE_PIC)
    composeTestRule.onNodeWithTag(NavigationTestTags.MY_TRIPS_TAB).performClick()

    // 2. Open the Trip
    composeTestRule.waitForText(tripName)
    composeTestRule.onNodeWithText(tripName).performClick()

    // 3. Share with Bob
    composeTestRule.waitForTag(DailyViewScreenTestTags.SHARE_BUTTON)
    composeTestRule.onNodeWithTag(DailyViewScreenTestTags.SHARE_BUTTON).performClick()

    // In the dialog, Bob should be visible as a friend.
    val bobFriendTag = "friend$bobUid"
    composeTestRule.waitForTag(bobFriendTag)
    // Add Bob
    composeTestRule.onNodeWithTag(bobFriendTag).performClick()

    // Close Dialog (Click OK)
    composeTestRule.onNodeWithText("OK").performClick()

    // Wait for Firestore update (Adding collaborator is async)
    composeTestRule.waitForTag(DailyViewScreenTestTags.BACK_BUTTON)
    composeTestRule.onNodeWithTag(DailyViewScreenTestTags.BACK_BUTTON).performClick()
    composeTestRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU).isDisplayed()
    composeTestRule.onNodeWithTag(testTag = NavigationTestTags.PROFILE_TAB).performClick()
    composeTestRule.logout()

    // =================================================================================
    // PHASE 3: BOB ACCESSES TRIP
    // =================================================================================

    // --- Bob Logs In ---
    composeTestRule.loginWithGoogle(isE2E = true) // Login as Bob

    // 1. Go to My Trips
    composeTestRule.onNodeWithTag(NavigationTestTags.MY_TRIPS_TAB).performClick()

    // 2. Verify Alice's Trip is visible
    composeTestRule.waitForText(tripName)

    // 3. Enter Trip Info
    composeTestRule.onNodeWithText(tripName).performClick()

    // 4. Verify View Access
    // Bob is a collaborator, so he should see the Trip details but NOT the Edit button (Owner only)
    composeTestRule.waitForTag(DailyViewScreenTestTags.TITLE)

    // 5. Bob logs out
    composeTestRule.onNodeWithTag(DailyViewScreenTestTags.BACK_BUTTON).performClick()
    composeTestRule.waitForTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU)
    composeTestRule.onNodeWithTag(NavigationTestTags.PROFILE_TAB).performClick()
    composeTestRule.logout()

    // ========================================================================================
    // PHASE 4 : Alice logs back in and modifies the trip
    // ========================================================================================

    // Alice logs back in and goes to the shared trip
    composeTestRule.loginWithGoogle(true)
    composeTestRule.onNodeWithTag(NavigationTestTags.MY_TRIPS_TAB).performClick()
    composeTestRule.waitForText(tripName)
    composeTestRule.onNodeWithText(tripName).performClick()

    // Alice modifies the content of the trip
    composeTestRule.waitForTag(DailyViewScreenTestTags.EDIT_BUTTON)
    composeTestRule.onNodeWithTag(DailyViewScreenTestTags.EDIT_BUTTON).performClick()
    composeTestRule.waitForTag(EditTripScreenTestTags.TRIP_NAME)
    tripName = "Alice changed her adventure"
    composeTestRule.onNodeWithTag(EditTripScreenTestTags.TRIP_NAME).performTextClearance()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(EditTripScreenTestTags.TRIP_NAME).performTextInput(tripName)

    // Alice saves her modifications
    composeTestRule.onNodeWithTag(EditTripScreenTestTags.CONFIRM_TOP_BAR).performClick()
    composeTestRule.waitForTag(DailyViewScreenTestTags.BACK_BUTTON)
    composeTestRule.onNodeWithTag(DailyViewScreenTestTags.BACK_BUTTON).performClick()

    composeTestRule.waitForTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU)
    composeTestRule.onNodeWithTag(NavigationTestTags.PROFILE_TAB).performClick()
    composeTestRule.logout()

    // =========================================================================================
    // PHASE 5. Bob logs back in and verifies that the trip changed
    // =========================================================================================
    // Bob goes back to the shared trip
    composeTestRule.loginWithGoogle(true)
    composeTestRule.waitForTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU)
    composeTestRule.onNodeWithTag(NavigationTestTags.MY_TRIPS_TAB).performClick()

    // We can assume that if the trip name can change, all other components can change

    composeTestRule.waitForText(tripName)
    composeTestRule.onNodeWithText(tripName).performClick()

    composeTestRule.waitForTag(DailyViewScreenTestTags.TITLE)

    // Bob goes back off and logs out
    composeTestRule.onNodeWithTag(DailyViewScreenTestTags.BACK_BUTTON).performClick()
    composeTestRule.waitForTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU)
    composeTestRule.onNodeWithTag(NavigationTestTags.PROFILE_TAB).performClick()
    composeTestRule.logout()

    // =========================================================================================
    // PHASE 6. Alice logs back and remove Bob from the shared trip
    // =========================================================================================

    // Alice logs back in and goes to my trips
    composeTestRule.loginWithGoogle(true)
    composeTestRule.onNodeWithTag(NavigationTestTags.MY_TRIPS_TAB).performClick()

    // She selects her shared trip and goes to the shared tab
    composeTestRule.waitForText(tripName)
    composeTestRule.onNodeWithText(tripName).performClick()
    composeTestRule.waitForTag(DailyViewScreenTestTags.SHARE_BUTTON)
    composeTestRule.onNodeWithTag(DailyViewScreenTestTags.SHARE_BUTTON).performClick()

    // Alice removes Bob from the users that have access to the shared trip
    val removeBobTag = DailyViewScreenTestTags.getTestTagForRemoveCollaborator(bobUid)
    composeTestRule.waitForTag(removeBobTag)
    composeTestRule.onNodeWithTag(removeBobTag).performClick()

    // Alice logs back out
    composeTestRule.onNodeWithTag(DailyViewScreenTestTags.BACK_BUTTON).performClick()
    composeTestRule.waitForTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU)
    composeTestRule.onNodeWithTag(NavigationTestTags.PROFILE_TAB).performClick()
    composeTestRule.logout()

    // =========================================================================================
    // Phase 7. Bob logs back in and cannot access the shared trip anymore
    // =========================================================================================

    // Bob logs back in and goes to my trips
    composeTestRule.loginWithGoogle(true)
    composeTestRule.onNodeWithTag(NavigationTestTags.MY_TRIPS_TAB).performClick()

    // Bob refreshes the screen
    composeTestRule.onNodeWithTag(NavigationTestTags.PROFILE_TAB).performClick()
    composeTestRule.waitForTag(ProfileScreenTestTags.PROFILE_PIC)
    composeTestRule.onNodeWithTag(NavigationTestTags.MY_TRIPS_TAB).performClick()
    // The shared trip should not be there
    composeTestRule.onNodeWithText(tripName).assertIsNotDisplayed()
  }
}
