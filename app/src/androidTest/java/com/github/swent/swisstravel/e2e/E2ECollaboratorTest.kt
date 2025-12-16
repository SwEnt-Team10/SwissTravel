package com.github.swent.swisstravel.e2e

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.github.swent.swisstravel.SwissTravelApp
import com.github.swent.swisstravel.model.user.UserRepositoryFirebase
import com.github.swent.swisstravel.ui.navigation.NavigationTestTags
import com.github.swent.swisstravel.ui.profile.ProfileScreenTestTags
import com.github.swent.swisstravel.ui.trip.edittrip.EditTripScreenTestTags
import com.github.swent.swisstravel.ui.trip.tripinfos.DailyViewScreenTestTags
import com.github.swent.swisstravel.utils.FakeCredentialManager
import com.github.swent.swisstravel.utils.FakeJwtGenerator
import com.github.swent.swisstravel.utils.FirebaseEmulator
import com.github.swent.swisstravel.utils.FirestoreSwissTravelTest
import com.github.swent.swisstravel.utils.SwissTravelTest.*
import com.google.firebase.Timestamp
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

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

    // 1. Setup Tokens
    val aliceToken = FakeJwtGenerator.createFakeGoogleIdToken(aliceName, aliceEmail)
    val bobToken = FakeJwtGenerator.createFakeGoogleIdToken(bobName, bobEmail)

    // 2. Define Login Sequence
    val fakeCredentialManager =
        FakeCredentialManager.sequence(
            bobToken, // Phase 0: Bob Setup
            aliceToken, // Phase 1: Alice Setup
            bobToken, // Phase 1.5: Bob Accepts
            aliceToken, // Phase 2: Alice Creates & Shares
            bobToken, // Phase 3: Bob Accesses
            aliceToken, // Phase 4: Alice Modifies
            bobToken, // Phase 5: Bob Verifies
            aliceToken, // Phase 6: Alice Removes
            bobToken // Phase 7: Bob Checks
            )

    composeTestRule.setContent { SwissTravelApp(credentialManager = fakeCredentialManager) }
  }

  @After
  override fun tearDown() {
    super.tearDown()
    FirebaseEmulator.clearFirestoreEmulator()
  }

  @Test
  fun complete_collaboration_flow() {
    val userRepo = UserRepositoryFirebase(FirebaseEmulator.auth, FirebaseEmulator.firestore)
    val tripsRepo = createInitializedRepository()

    // =================================================================================
    // PHASE 0: BOB SETUP
    // =================================================================================
    composeTestRule.loginWithGoogle(isE2E = true)
    composeTestRule.verifyLoggedInUser(bobName) // Sync barrier

    runBlocking {
      val bobUser = userRepo.getUserByNameOrEmail(bobEmail).first()
      bobUid = bobUser.uid
      userRepo.updateUser(bobUser.uid, name = bobName)
    }
    composeTestRule.logout()

    // =================================================================================
    // PHASE 1: ALICE SETUP & FRIEND REQUEST
    // =================================================================================
    composeTestRule.loginWithGoogle(isE2E = true)
    composeTestRule.verifyLoggedInUser(aliceName) // Sync barrier

    runBlocking {
      val aliceUser = userRepo.getUserByNameOrEmail(aliceEmail).first()
      aliceUid = aliceUser.uid
      userRepo.updateUser(aliceUser.uid, name = aliceName)
      userRepo.sendFriendRequest(aliceUid, bobUid)
    }
    composeTestRule.logout()

    // =================================================================================
    // PHASE 1.5: BOB ACCEPTS FRIEND REQUEST
    // =================================================================================
    composeTestRule.loginWithGoogle(isE2E = true)
    composeTestRule.verifyLoggedInUser(bobName) // Sync barrier

    runBlocking { userRepo.acceptFriendRequest(bobUid, aliceUid) }
    composeTestRule.logout()

    // =================================================================================
    // PHASE 2: ALICE CREATES TRIP & SHARES
    // =================================================================================
    composeTestRule.loginWithGoogle(isE2E = true)
    composeTestRule.verifyLoggedInUser(
        aliceName) // CRITICAL: Ensures Auth is "Alice" before tripsRepo.addTrip runs

    var tripName = "Alice's Grand Tour"

    runBlocking {
      val currentAuthUid = FirebaseEmulator.auth.currentUser!!.uid
      val now = Timestamp.now()
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

    // Navigate to My Trips
    composeTestRule.onNodeWithTag(NavigationTestTags.MY_TRIPS_TAB).performClick()

    // Open Trip
    composeTestRule.waitForText(tripName)
    composeTestRule.onNodeWithText(tripName).performClick()

    // Share with Bob
    composeTestRule.waitForTag(DailyViewScreenTestTags.SHARE_BUTTON)
    composeTestRule.onNodeWithTag(DailyViewScreenTestTags.SHARE_BUTTON).performClick()

    val bobFriendTag = "friend$bobUid"
    composeTestRule.waitForTag(bobFriendTag)
    composeTestRule.onNodeWithTag(bobFriendTag).performClick()
    composeTestRule.onNodeWithText("OK").performClick()

    // Wait/Back
    composeTestRule.waitForTag(DailyViewScreenTestTags.BACK_BUTTON)
    composeTestRule.onNodeWithTag(DailyViewScreenTestTags.BACK_BUTTON).performClick()
    composeTestRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU).isDisplayed()

    composeTestRule.logout()

    // =================================================================================
    // PHASE 3: BOB ACCESSES TRIP
    // =================================================================================
    composeTestRule.loginWithGoogle(isE2E = true)
    composeTestRule.verifyLoggedInUser(bobName)

    composeTestRule.onNodeWithTag(NavigationTestTags.MY_TRIPS_TAB).performClick()

    composeTestRule.waitForText(tripName)
    composeTestRule.onNodeWithText(tripName).performClick()

    // Verify Bob (Collaborator) cannot edit
    composeTestRule.waitForTag(DailyViewScreenTestTags.TITLE)
    composeTestRule.onNodeWithTag(DailyViewScreenTestTags.EDIT_BUTTON).assertIsNotDisplayed()

    // Logout
    composeTestRule.onNodeWithTag(DailyViewScreenTestTags.BACK_BUTTON).performClick()
    composeTestRule.logout()

    // =================================================================================
    // PHASE 4: ALICE MODIFIES TRIP
    // =================================================================================
    composeTestRule.loginWithGoogle(isE2E = true)
    composeTestRule.verifyLoggedInUser(aliceName)

    composeTestRule.onNodeWithTag(NavigationTestTags.MY_TRIPS_TAB).performClick()
    composeTestRule.waitForText(tripName)
    composeTestRule.onNodeWithText(tripName).performClick()

    // Edit
    composeTestRule.waitForTag(DailyViewScreenTestTags.EDIT_BUTTON)
    composeTestRule.onNodeWithTag(DailyViewScreenTestTags.EDIT_BUTTON).performClick()

    composeTestRule.waitForTag(EditTripScreenTestTags.TRIP_NAME)
    tripName = "Alice changed her adventure"
    composeTestRule.onNodeWithTag(EditTripScreenTestTags.TRIP_NAME).performTextClearance()
    composeTestRule.onNodeWithTag(EditTripScreenTestTags.TRIP_NAME).performTextInput(tripName)

    // Save
    composeTestRule.onNodeWithTag(EditTripScreenTestTags.CONFIRM_TOP_BAR).performClick()

    composeTestRule.waitForTag(DailyViewScreenTestTags.BACK_BUTTON)
    composeTestRule.onNodeWithTag(DailyViewScreenTestTags.BACK_BUTTON).performClick()
    composeTestRule.logout()

    // =================================================================================
    // PHASE 5: BOB VERIFIES UPDATE
    // =================================================================================
    composeTestRule.loginWithGoogle(isE2E = true)
    composeTestRule.verifyLoggedInUser(bobName)

    composeTestRule.onNodeWithTag(NavigationTestTags.MY_TRIPS_TAB).performClick()

    composeTestRule.waitForText(tripName) // Updated name should be visible
    composeTestRule.onNodeWithText(tripName).performClick()
    composeTestRule.waitForTag(DailyViewScreenTestTags.TITLE)

    composeTestRule.onNodeWithTag(DailyViewScreenTestTags.BACK_BUTTON).performClick()
    composeTestRule.logout()

    // =================================================================================
    // PHASE 6: ALICE REMOVES BOB
    // =================================================================================
    composeTestRule.loginWithGoogle(isE2E = true)
    composeTestRule.verifyLoggedInUser(aliceName)

    composeTestRule.onNodeWithTag(NavigationTestTags.MY_TRIPS_TAB).performClick()

    composeTestRule.waitForText(tripName)
    composeTestRule.onNodeWithText(tripName).performClick()

    composeTestRule.waitForTag(DailyViewScreenTestTags.SHARE_BUTTON)
    composeTestRule.onNodeWithTag(DailyViewScreenTestTags.SHARE_BUTTON).performClick()

    val removeBobTag = DailyViewScreenTestTags.getTestTagForRemoveCollaborator(bobUid)
    composeTestRule.waitForTag(removeBobTag)
    composeTestRule.onNodeWithTag(removeBobTag).performClick()

    composeTestRule.onNodeWithTag(DailyViewScreenTestTags.BACK_BUTTON).performClick()
    composeTestRule.logout()

    // =================================================================================
    // PHASE 7: BOB CHECK
    // =================================================================================
    composeTestRule.loginWithGoogle(isE2E = true)
    composeTestRule.verifyLoggedInUser(bobName)

    composeTestRule.onNodeWithTag(NavigationTestTags.MY_TRIPS_TAB).performClick()

    // Force refresh by toggling tabs (Trip list might need trigger to reload)
    composeTestRule.onNodeWithTag(NavigationTestTags.PROFILE_TAB).performClick()
    composeTestRule.waitForTag(ProfileScreenTestTags.PROFILE_PIC)
    composeTestRule.onNodeWithTag(NavigationTestTags.MY_TRIPS_TAB).performClick()

    composeTestRule.onNodeWithText(tripName).assertIsNotDisplayed()
  }
}
