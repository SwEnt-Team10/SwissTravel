//package com.github.swent.swisstravel.e2e
//
//import androidx.compose.ui.test.assertIsDisplayed
//import androidx.compose.ui.test.junit4.createComposeRule
//import androidx.compose.ui.test.onAllNodesWithTag
//import androidx.compose.ui.test.onNodeWithTag
//import androidx.compose.ui.test.onNodeWithText
//import androidx.compose.ui.test.performClick
//import androidx.compose.ui.test.performTextInput
//import androidx.compose.ui.test.performTouchInput
//import androidx.compose.ui.test.swipeDown
//import androidx.test.espresso.action.ViewActions.swipeDown
//import com.github.swent.swisstravel.SwissTravelApp
//import com.github.swent.swisstravel.model.trip.Coordinate
//import com.github.swent.swisstravel.model.trip.Location
//import com.github.swent.swisstravel.model.trip.Trip
//import com.github.swent.swisstravel.model.trip.TripProfile
//import com.github.swent.swisstravel.model.trip.TripsRepositoryFirestore
//import com.github.swent.swisstravel.model.user.Preference
//import com.github.swent.swisstravel.model.user.UserRepositoryFirebase
//import com.github.swent.swisstravel.ui.authentication.LandingScreenTestTags.SIGN_IN_BUTTON
//import com.github.swent.swisstravel.ui.authentication.SignInScreenTestTags.GOOGLE_LOGIN_BUTTON
//import com.github.swent.swisstravel.ui.friends.AddFriendsScreenTestTags
//import com.github.swent.swisstravel.ui.friends.FriendElementTestTags
//import com.github.swent.swisstravel.ui.friends.FriendsScreenTestTags
//import com.github.swent.swisstravel.ui.navigation.NavigationTestTags
//import com.github.swent.swisstravel.ui.profile.ProfileScreenTestTags
//import com.github.swent.swisstravel.ui.profile.ProfileSettingsScreenTestTags
//import com.github.swent.swisstravel.ui.trip.tripinfos.DailyViewScreenTestTags
//import com.github.swent.swisstravel.utils.E2E_WAIT_TIMEOUT
//import com.github.swent.swisstravel.utils.FakeCredentialManager
//import com.github.swent.swisstravel.utils.FakeJwtGenerator
//import com.github.swent.swisstravel.utils.FirebaseEmulator
//import com.github.swent.swisstravel.utils.FirestoreSwissTravelTest
//import com.google.firebase.Timestamp
//import kotlinx.coroutines.runBlocking
//import org.junit.After
//import org.junit.Before
//import org.junit.Rule
//import org.junit.Test
//
///**
// * Done with the help of AI.
// *
// * End-to-end Friend Flow Test:
// * 1) Alice logs in (account creation).
// * 2) Alice logs out.
// * 3) Bob logs in.
// * 4) Add Bob's dummy trip and pins it to his profile.
// * 5) Bob sends a friend request to Alice.
// * 6) Bob logs out.
// * 7) Alice logs in again.
// * 8) Alice accepts the friend request.
// * 9) Alice clicks on Bob in the friends list.
// * 10) Alice sees Bob's pinned trip.
// */
//class E2EFriendFlowTest : FirestoreSwissTravelTest() {
//
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
//    val aliceToken = FakeJwtGenerator.createFakeGoogleIdToken(name = aliceName, email = aliceEmail)
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
//
//  @Test
//  fun user_can_send_and_accept_friend_request_and_view_pinned_trips() {
//    // --- STEP 1: Alice logs in (account creation). ---
//    loginWithGoogle()
//
//    composeTestRule.onNodeWithTag(NavigationTestTags.PROFILE_TAB).performClick()
//    waitForTag(ProfileScreenTestTags.DISPLAY_NAME)
//
//    // --- STEP 2: Alice logs out. ---
//    logout()
//
//    // --- STEP 3: Bob logs in. ---
//    loginWithGoogle()
//
//    // --- STEP 4: Add Bob's dummy trip and pins it to his profile. ---
//    // We do this while Bob is logged in so we have permission
//    runBlocking {
//      val userRepo = UserRepositoryFirebase(FirebaseEmulator.auth, FirebaseEmulator.firestore)
//      val tripsRepo = TripsRepositoryFirestore(FirebaseEmulator.firestore, FirebaseEmulator.auth)
//
//      // 1. Get Bob's User object (Wait for it to be created by the app logic if needed,
//      // usually quick, but querying by email is safe).
//      val bobUser = userRepo.getUserByNameOrEmail(bobEmail).first()
//
//      // 2. Create a dummy trip owned by Bob
//      val trip = createDummyTrip(ownerId = bobUser.uid, tripName = "bobTripName")
//      tripsRepo.addTrip(trip)
//
//      // 3. Pin the trip to Bob's profile
//      userRepo.updateUser(uid = bobUser.uid, pinnedTripsUids = listOf(trip.uid))
//    }
//
//    // --- STEP 5: Bob sends a friend request to Alice. ---
//    composeTestRule.onNodeWithTag(NavigationTestTags.FRIENDS_TAB).performClick()
//    waitForTag(FriendsScreenTestTags.ADD_FRIEND_BUTTON)
//
//    // Click Add Friend
//    composeTestRule.onNodeWithTag(FriendsScreenTestTags.ADD_FRIEND_BUTTON).performClick()
//    waitForTag(AddFriendsScreenTestTags.ADD_FRIEND_SEARCH_FIELD)
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
//    composeTestRule.onNodeWithTag(FriendElementTestTags.getTestTagForFriend(alice)).performClick()
//
//    composeTestRule.waitForIdle()
//
//    // --- STEP 6: Bob logs out. ---
//    composeTestRule.onNodeWithTag(NavigationTestTags.PROFILE_TAB).performClick()
//    waitForTag(ProfileScreenTestTags.SETTINGS_BUTTON)
//    logout()
//
//    // --- STEP 7: Alice logs in again. ---
//    loginWithGoogle()
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
//    waitForTag(FriendsScreenTestTags.PENDING_SECTION_CARD)
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
//    composeTestRule.waitUntil(E2E_WAIT_TIMEOUT) {
//      composeTestRule
//          .onAllNodesWithTag(FriendsScreenTestTags.FRIENDS_LIST)
//          .fetchSemanticsNodes()
//          .isNotEmpty()
//    }
//    composeTestRule.onNodeWithTag(FriendsScreenTestTags.FRIENDS_LIST).performTouchInput {
//      swipeDown()
//    }
//    composeTestRule.waitForIdle()
//    composeTestRule.onNodeWithText(bobName).assertIsDisplayed()
//
//    // --- STEP 9: Alice clicks on Bob in the friends list. ---
//    // Click on Bob in the friend list
//    composeTestRule
//        .onNodeWithTag(
//            FriendElementTestTags.getTestTagForFriend(
//                runBlocking { userRepo.getUserByNameOrEmail(bobName).first() }))
//        .performClick()
//
//    // Verify we are on the profile screen
//    waitForTag(ProfileScreenTestTags.DISPLAY_NAME)
//    composeTestRule.waitForIdle()
//    composeTestRule.onNodeWithTag(ProfileScreenTestTags.DISPLAY_NAME).assertExists()
//
//    // Verify Pinned Trips Title is visible
//    composeTestRule.onNodeWithTag(ProfileScreenTestTags.PINNED_TRIPS_TITLE).assertIsDisplayed()
//
//    // --- STEP 10: Alice sees Bob's pinned trip. ---
//    composeTestRule.onNodeWithText("bobTripName").assertIsDisplayed().performClick()
//
//    waitForTag(DailyViewScreenTestTags.TITLE)
//    composeTestRule.onNodeWithTag(DailyViewScreenTestTags.TITLE).assertIsDisplayed()
//  }
//
//  // -- Helper functions --
//
//  private fun loginWithGoogle() {
//    composeTestRule.onNodeWithTag(SIGN_IN_BUTTON).assertExists().performClick()
//    composeTestRule.waitForIdle()
//    composeTestRule.onNodeWithTag(GOOGLE_LOGIN_BUTTON).assertExists().performClick()
//
//    // Wait for main app to load
//    composeTestRule.waitUntil(E2E_WAIT_TIMEOUT) {
//      composeTestRule
//          .onAllNodesWithTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU)
//          .fetchSemanticsNodes()
//          .isNotEmpty()
//    }
//  }
//
//  private fun logout() {
//    waitForTag(ProfileScreenTestTags.SETTINGS_BUTTON)
//    composeTestRule.onNodeWithTag(ProfileScreenTestTags.SETTINGS_BUTTON).performClick()
//    waitForTag(ProfileSettingsScreenTestTags.LOGOUT_BUTTON)
//    composeTestRule.onNodeWithTag(ProfileSettingsScreenTestTags.LOGOUT_BUTTON).performClick()
//
//    // Wait for Landing Screen
//    composeTestRule.waitUntil(E2E_WAIT_TIMEOUT) {
//      composeTestRule.onAllNodesWithTag(SIGN_IN_BUTTON).fetchSemanticsNodes().isNotEmpty()
//    }
//  }
//
//  private fun waitForTag(tag: String) {
//    composeTestRule.waitUntil(E2E_WAIT_TIMEOUT) {
//      composeTestRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
//    }
//  }
//
//  /** Creates a valid dummy trip object for testing. */
//  private fun createDummyTrip(ownerId: String, tripName: String): Trip {
//    val loc = Location(coordinate = Coordinate(0.0, 0.0), name = "Test Location", imageUrl = null)
//
//    val now = Timestamp.now()
//
//    val profile =
//        TripProfile(
//            adults = 1,
//            children = 0,
//            departureLocation = loc,
//            arrivalLocation = loc,
//            startDate = now,
//            endDate = now,
//            preferredLocations = listOf(loc),
//            preferences = listOf(Preference.SCENIC_VIEWS))
//
//    return Trip(
//        uid = "trip_${System.currentTimeMillis()}",
//        name = tripName,
//        ownerId = ownerId,
//        locations = listOf(loc),
//        routeSegments = emptyList(),
//        activities = emptyList(),
//        tripProfile = profile,
//        isFavorite = false,
//        isCurrentTrip = false,
//        listUri = emptyList(),
//        collaboratorsId = emptyList(),
//        isRandom = false)
//  }
//}
