package com.github.swent.swisstravel.e2e

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import com.github.swent.swisstravel.SwissTravelApp
import com.github.swent.swisstravel.model.trip.TripsRepositoryFirestore
import com.github.swent.swisstravel.model.user.Preference
import com.github.swent.swisstravel.model.user.UserRepositoryFirebase
import com.github.swent.swisstravel.ui.friends.AddFriendsScreenTestTags
import com.github.swent.swisstravel.ui.friends.FriendElementTestTags
import com.github.swent.swisstravel.ui.friends.FriendsScreenTestTags
import com.github.swent.swisstravel.ui.navigation.NavigationTestTags
import com.github.swent.swisstravel.ui.profile.ProfileScreenTestTags
import com.github.swent.swisstravel.ui.trip.tripinfos.DailyViewScreenTestTags
import com.github.swent.swisstravel.utils.E2E_WAIT_TIMEOUT
import com.github.swent.swisstravel.utils.FakeCredentialManager
import com.github.swent.swisstravel.utils.FakeJwtGenerator
import com.github.swent.swisstravel.utils.FirebaseEmulator
import com.github.swent.swisstravel.utils.FirestoreSwissTravelTest
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Done with the help of AI.
 *
 * End-to-end Friend Flow Test:
 * 1) Alice logs in (account creation).
 * 2) Alice logs out.
 * 3) Bob logs in.
 * 4) Add Bob's dummy trip and pins it to his profile.
 * 5) Bob sends a friend request to Alice.
 * 6) Bob logs out.
 * 7) Alice logs in again.
 * 8) Alice accepts the friend request.
 * 9) Alice clicks on Bob in the friends list.
 * 10) Alice sees Bob's pinned trip.
 */
class E2EFriendsTest : FirestoreSwissTravelTest() {

  @get:Rule val composeTestRule = createComposeRule()

  private val aliceName = "Alice Tester"
  private val aliceEmail = "alice.tester@example.com"

  private val bobName = "Bob Builder"
  private val bobEmail = "bob.builder@example.com"

  // We define Charlie here so we can access his ID later
  private val charlieName = "Charlie"
  private val charlieEmail = "charlie.test@example.com"
  private lateinit var charlieUid: String

  @Before
  override fun setUp() {
    super.setUp()
    FirebaseEmulator.auth.signOut()
    FirebaseEmulator.clearAuthEmulator()

    val aliceToken = FakeJwtGenerator.createFakeGoogleIdToken(name = aliceName, email = aliceEmail)
    val bobToken = FakeJwtGenerator.createFakeGoogleIdToken(name = bobName, email = bobEmail)
    val charlieToken =
        FakeJwtGenerator.createFakeGoogleIdToken(name = charlieName, email = charlieEmail)

    // Sequence: Alice (init), Bob (setup), Alice (accept & view)
    val fakeCredentialManager = FakeCredentialManager.sequence(aliceToken, bobToken, aliceToken)

    // Pre-create Charlie in Firestore properly by signing in as him
    runBlocking {
      val credential = GoogleAuthProvider.getCredential(charlieToken, null)
      FirebaseEmulator.auth.signInWithCredential(credential).await()
      charlieUid = FirebaseEmulator.auth.currentUser!!.uid

      val charlieData =
          hashMapOf(
              "uid" to charlieUid,
              "name" to charlieName,
              "email" to charlieEmail,
              "friends" to listOf<Any>())
      FirebaseEmulator.firestore.collection("users").document(charlieUid).set(charlieData).await()
      FirebaseEmulator.auth.signOut()
    }

    // Start app
    composeTestRule.setContent { SwissTravelApp(credentialManager = fakeCredentialManager) }
  }

  @After
  override fun tearDown() {
    super.tearDown()
    FirebaseEmulator.clearFirestoreEmulator()
  }

  @Test
  fun user_can_send_and_accept_friend_request_and_view_pinned_trips() {
    // --- STEP 1: Alice logs in (account creation). ---
    composeTestRule.loginWithGoogle(true)

    composeTestRule.onNodeWithTag(NavigationTestTags.PROFILE_TAB).performClick()
    composeTestRule.waitForTag(ProfileScreenTestTags.DISPLAY_NAME)

    // --- STEP 2: Alice logs out. ---
    composeTestRule.logout()

    // --- STEP 3: Bob logs in. ---
    composeTestRule.loginWithGoogle(true)

    // Wait for Bob's profile to be fully loaded (Auth ready)
    composeTestRule.onNodeWithTag(NavigationTestTags.PROFILE_TAB).performClick()
    composeTestRule.waitForTag(ProfileScreenTestTags.DISPLAY_NAME)
    composeTestRule.waitUntil(E2E_WAIT_TIMEOUT) {
      runCatching {
            composeTestRule.onNodeWithText(bobName).assertIsDisplayed()
            true
          }
          .getOrDefault(false)
    }

    // --- STEP 4: Add Bob's dummy trip and inject Pending Friend Request ---
    runBlocking {
      val userRepo = UserRepositoryFirebase(FirebaseEmulator.auth, FirebaseEmulator.firestore)
      val tripsRepo = TripsRepositoryFirestore(FirebaseEmulator.firestore, FirebaseEmulator.auth)

      // 1. Get Bob's User object
      val bobUser = userRepo.getUserByNameOrEmail(bobEmail).first()

      // 2. Create dummy trip owned by Bob
      val trip =
          createTestTrip(
              uid = "trip_${System.currentTimeMillis()}",
              name = "bobTripName",
              ownerId = bobUser.uid,
              locations = listOf(dummyLocation),
              departureLocation = dummyLocation,
              arrivalLocation = dummyLocation,
              preferredLocations = listOf(dummyLocation),
              preferences = listOf(Preference.SCENIC_VIEWS),
              adults = 1,
              children = 0)
      tripsRepo.addTrip(trip)

      // 3. Pin the trip
      userRepo.updateUser(uid = bobUser.uid, pinnedTripsUids = listOf(trip.uid))

      // 4. Inject "Charlie" as a pending friend request into Bob's list.
      // We are logged in as Bob, so we can update Bob's document.
      val bobRef = FirebaseEmulator.firestore.collection("users").document(bobUser.uid)
      FirebaseEmulator.firestore
          .runTransaction { transaction ->
            val snapshot = transaction.get(bobRef)
            @Suppress("UNCHECKED_CAST")
            val friends =
                snapshot.get("friends") as? MutableList<Map<String, String>> ?: mutableListOf()
            // Add pending request from Charlie (created in setUp)
            friends.add(mapOf("uid" to charlieUid, "status" to "PENDING_INCOMING"))
            transaction.update(bobRef, "friends", friends)
          }
          .await()
    }

    // --- STEP 5: Bob sends a friend request to Alice. ---
    composeTestRule.onNodeWithTag(NavigationTestTags.FRIENDS_TAB).performClick()

    composeTestRule.waitForTag(FriendsScreenTestTags.FRIENDS_LIST)
    composeTestRule.onNodeWithTag(FriendsScreenTestTags.FRIENDS_LIST).performTouchInput {
      swipeDown()
    }

    // WAIT for the pending request header (from Charlie) to appear.
    // This ensures FriendsViewModel has finished refreshing and currentUserUid is set.
    composeTestRule.waitUntil(E2E_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(FriendsScreenTestTags.PENDING_SECTION_HEADER)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule.waitForTag(FriendsScreenTestTags.ADD_FRIEND_BUTTON)

    // Click Add Friend
    composeTestRule.onNodeWithTag(FriendsScreenTestTags.ADD_FRIEND_BUTTON).performClick()
    composeTestRule.waitForTag(AddFriendsScreenTestTags.ADD_FRIEND_SEARCH_FIELD)

    // Search for Alice
    composeTestRule
        .onNodeWithTag(AddFriendsScreenTestTags.ADD_FRIEND_SEARCH_FIELD)
        .performTextInput(aliceName)

    // Wait for results
    composeTestRule.waitUntil(E2E_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(AddFriendsScreenTestTags.ADD_FRIEND_RESULTS_LIST)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Click on Alice in the list
    val userRepo = UserRepositoryFirebase(FirebaseEmulator.auth, FirebaseEmulator.firestore)
    val alice = runBlocking { userRepo.getUserByNameOrEmail(aliceName).first() }
    composeTestRule.onNodeWithTag(FriendElementTestTags.getTestTagForFriend(alice)).performClick()

    composeTestRule.waitForIdle()

    val bobUid = FirebaseEmulator.auth.currentUser!!.uid
    composeTestRule.waitUntil(E2E_WAIT_TIMEOUT) {
      runBlocking {
        // Fetch Alice's fresh data to see if she received the request
        val updatedAlice = userRepo.getUserByUid(alice.uid)
        // Check if Bob's UID exists in Alice's friend list (status will be PENDING)
        updatedAlice?.friends?.any { it.uid == bobUid } == true
      }
    }

    // --- STEP 6: Bob logs out. ---
    composeTestRule.onNodeWithTag(NavigationTestTags.PROFILE_TAB).performClick()
    composeTestRule.waitForTag(ProfileScreenTestTags.SETTINGS_BUTTON)
    composeTestRule.logout()

    // --- STEP 7: Alice logs in again. ---
    composeTestRule.loginWithGoogle(true)

    // --- STEP 8: Alice accepts the friend request. ---
    composeTestRule
        .onNodeWithTag(NavigationTestTags.FRIENDS_TAB, useUnmergedTree = true)
        .performClick()

    // Refresh to see the new friend request
    composeTestRule.onNodeWithTag(FriendsScreenTestTags.FRIENDS_LIST).performTouchInput {
      swipeDown()
    }

    composeTestRule.waitForTag(FriendsScreenTestTags.PENDING_SECTION_CARD)

    // Open Pending Section
    composeTestRule.onNodeWithTag(FriendsScreenTestTags.PENDING_SECTION_HEADER).performClick()

    // Verify Bob is in pending list
    composeTestRule.onNodeWithText(bobName).assertIsDisplayed()

    // Accept Bob
    composeTestRule.onNodeWithTag(FriendElementTestTags.ACCEPT_BUTTON).performClick()
    composeTestRule.waitForIdle()

    composeTestRule.waitForTag(FriendsScreenTestTags.FRIENDS_LIST)

    composeTestRule.onNodeWithTag(FriendsScreenTestTags.FRIENDS_LIST).performTouchInput {
      swipeDown()
    }

    val bobTag =
        FriendElementTestTags.getTestTagForFriend(
            runBlocking { userRepo.getUserByNameOrEmail(bobName).first() })
    composeTestRule.waitForTag(bobTag)

    // --- STEP 9: Alice clicks on Bob in the friends list. ---
    // Click on Bob in the friend list
    composeTestRule.onNodeWithTag(bobTag).performClick()

    // Verify we are on the profile screen
    composeTestRule.waitForTag(ProfileScreenTestTags.DISPLAY_NAME)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.DISPLAY_NAME).assertExists()

    // Verify Pinned Trips Title is visible
    composeTestRule.waitForTag(ProfileScreenTestTags.PINNED_TRIPS_TITLE)
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.PINNED_TRIPS_TITLE).assertIsDisplayed()

    // --- STEP 10: Alice sees Bob's pinned trip. ---
    composeTestRule.waitForText("bobTripName")
    composeTestRule.onNodeWithText("bobTripName").assertIsDisplayed().performClick()

    composeTestRule.waitForTag(DailyViewScreenTestTags.TITLE)
    composeTestRule.onNodeWithTag(DailyViewScreenTestTags.TITLE).assertIsDisplayed()
  }
}
