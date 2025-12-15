package com.github.swent.swisstravel.e2e

import com.github.swent.swisstravel.utils.FirestoreSwissTravelTest

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

  //  @get:Rule val composeTestRule = createComposeRule()
  //
  //  private val aliceName = "Alice Tester"
  //  private val aliceEmail = "alice.tester@example.com"
  //
  //  private val bobName = "Bob Builder"
  //  private val bobEmail = "bob.builder@example.com"
  //
  //  @Before
  //  override fun setUp() {
  //    super.setUp()
  //    FirebaseEmulator.auth.signOut()
  //    FirebaseEmulator.clearAuthEmulator()
  //
  //    val aliceToken = FakeJwtGenerator.createFakeGoogleIdToken(name = aliceName, email =
  // aliceEmail)
  //    val bobToken = FakeJwtGenerator.createFakeGoogleIdToken(name = bobName, email = bobEmail)
  //
  //    // Sequence: Alice (init), Bob (setup), Alice (accept & view)
  //    val fakeCredentialManager = FakeCredentialManager.sequence(aliceToken, bobToken, aliceToken)
  //
  //    // Start app
  //    composeTestRule.setContent { SwissTravelApp(credentialManager = fakeCredentialManager) }
  //  }
  //
  //  @After
  //  override fun tearDown() {
  //    super.tearDown()
  //    FirebaseEmulator.clearFirestoreEmulator()
  //  }

  //  @Test
  //  fun user_can_send_and_accept_friend_request_and_view_pinned_trips() {
  //    // --- STEP 1: Alice logs in (account creation). ---
  //    composeTestRule.loginWithGoogle(true)
  //
  //    composeTestRule.onNodeWithTag(NavigationTestTags.PROFILE_TAB).performClick()
  //    composeTestRule.waitForTag(ProfileScreenTestTags.DISPLAY_NAME)
  //
  //    // --- STEP 2: Alice logs out. ---
  //    composeTestRule.logout()
  //
  //    // --- STEP 3: Bob logs in. ---
  //    composeTestRule.loginWithGoogle(true)
  //
  //    // --- STEP 4: Add Bob's dummy trip and pins it to his profile. ---
  //    // We do this while Bob is logged in so we have permission
  //    runBlocking {
  //      val userRepo = UserRepositoryFirebase(FirebaseEmulator.auth, FirebaseEmulator.firestore)
  //      val tripsRepo = TripsRepositoryFirestore(FirebaseEmulator.firestore,
  // FirebaseEmulator.auth)
  //
  //      // 1. Get Bob's User object
  //      val bobUser = userRepo.getUserByNameOrEmail(bobEmail).first()
  //
  //      // 2. Create dummy trip owned by Bob
  //      val trip =
  //          createTestTrip(
  //              uid = "trip_${System.currentTimeMillis()}",
  //              name = "bobTripName",
  //              ownerId = bobUser.uid,
  //              locations = listOf(dummyLocation),
  //              departureLocation = dummyLocation,
  //              arrivalLocation = dummyLocation,
  //              preferredLocations = listOf(dummyLocation),
  //              preferences = listOf(Preference.SCENIC_VIEWS),
  //              adults = 1,
  //              children = 0)
  //      tripsRepo.addTrip(trip)
  //      // 3. Pin the trip to Bob's profile
  //      userRepo.updateUser(uid = bobUser.uid, pinnedTripsUids = listOf(trip.uid))
  //    }
  //
  //    // --- STEP 5: Bob sends a friend request to Alice. ---
  //    composeTestRule.onNodeWithTag(NavigationTestTags.FRIENDS_TAB).performClick()
  //    composeTestRule.waitForTag(FriendsScreenTestTags.ADD_FRIEND_BUTTON)
  //
  //    // Click Add Friend
  //    composeTestRule.onNodeWithTag(FriendsScreenTestTags.ADD_FRIEND_BUTTON).performClick()
  //    composeTestRule.waitForTag(AddFriendsScreenTestTags.ADD_FRIEND_SEARCH_FIELD)
  //
  //    // Search for Alice
  //    composeTestRule
  //        .onNodeWithTag(AddFriendsScreenTestTags.ADD_FRIEND_SEARCH_FIELD)
  //        .performTextInput(aliceName)
  //
  //    // Wait for results
  //    composeTestRule.waitUntil(E2E_WAIT_TIMEOUT) {
  //      composeTestRule
  //          .onAllNodesWithTag(AddFriendsScreenTestTags.ADD_FRIEND_RESULTS_LIST)
  //          .fetchSemanticsNodes()
  //          .isNotEmpty()
  //    }
  //
  //    // Click on Alice in the list
  //    val userRepo = UserRepositoryFirebase(FirebaseEmulator.auth, FirebaseEmulator.firestore)
  //    val alice = runBlocking { userRepo.getUserByNameOrEmail(aliceName).first() }
  //
  // composeTestRule.onNodeWithTag(FriendElementTestTags.getTestTagForFriend(alice)).performClick()
  //
  //    composeTestRule.waitForIdle()
  //
  //    // --- STEP 6: Bob logs out. ---
  //    composeTestRule.onNodeWithTag(NavigationTestTags.PROFILE_TAB).performClick()
  //    composeTestRule.waitForTag(ProfileScreenTestTags.SETTINGS_BUTTON)
  //    composeTestRule.logout()
  //
  //    // --- STEP 7: Alice logs in again. ---
  //    composeTestRule.loginWithGoogle(true)
  //
  //    // --- STEP 8: Alice accepts the friend request. ---
  //    composeTestRule
  //        .onNodeWithTag(NavigationTestTags.FRIENDS_TAB, useUnmergedTree = true)
  //        .performClick()
  //
  //    // Refresh to see the new friend request
  //    composeTestRule.onNodeWithTag(FriendsScreenTestTags.FRIENDS_LIST).performTouchInput {
  //      swipeDown()
  //    }
  //
  //    composeTestRule.waitForTag(FriendsScreenTestTags.PENDING_SECTION_CARD)
  //
  //    // Open Pending Section
  //    composeTestRule.onNodeWithTag(FriendsScreenTestTags.PENDING_SECTION_HEADER).performClick()
  //
  //    // Verify Bob is in pending list
  //    composeTestRule.onNodeWithText(bobName).assertIsDisplayed()
  //
  //    // Accept Bob
  //    composeTestRule.onNodeWithTag(FriendElementTestTags.ACCEPT_BUTTON).performClick()
  //    composeTestRule.waitForIdle()
  //
  //    composeTestRule.waitForTag(FriendsScreenTestTags.FRIENDS_LIST)
  //
  //    composeTestRule.onNodeWithTag(FriendsScreenTestTags.FRIENDS_LIST).performTouchInput {
  //      swipeDown()
  //    }
  //
  //    val bobTag =
  //        FriendElementTestTags.getTestTagForFriend(
  //            runBlocking { userRepo.getUserByNameOrEmail(bobName).first() })
  //    composeTestRule.waitForTag(bobTag)
  //
  //    // --- STEP 9: Alice clicks on Bob in the friends list. ---
  //    // Click on Bob in the friend list
  //    composeTestRule.onNodeWithTag(bobTag).performClick()
  //
  //    // Verify we are on the profile screen
  //    composeTestRule.waitForTag(ProfileScreenTestTags.DISPLAY_NAME)
  //    composeTestRule.waitForIdle()
  //    composeTestRule.onNodeWithTag(ProfileScreenTestTags.DISPLAY_NAME).assertExists()
  //
  //    // Verify Pinned Trips Title is visible
  //    composeTestRule.waitForTag(ProfileScreenTestTags.PINNED_TRIPS_TITLE)
  //    composeTestRule.onNodeWithTag(ProfileScreenTestTags.PINNED_TRIPS_TITLE).assertIsDisplayed()
  //
  //    // --- STEP 10: Alice sees Bob's pinned trip. ---
  //    composeTestRule.waitForText("bobTripName")
  //    composeTestRule.onNodeWithText("bobTripName").assertIsDisplayed().performClick()
  //
  //    composeTestRule.waitForTag(DailyViewScreenTestTags.TITLE)
  //    composeTestRule.onNodeWithTag(DailyViewScreenTestTags.TITLE).assertIsDisplayed()
  //  }
}
