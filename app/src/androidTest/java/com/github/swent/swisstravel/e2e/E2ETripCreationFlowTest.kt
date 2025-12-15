package com.github.swent.swisstravel.e2e

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.rule.GrantPermissionRule
import com.github.swent.swisstravel.SwissTravelApp
import com.github.swent.swisstravel.utils.FakeCredentialManager
import com.github.swent.swisstravel.utils.FakeJwtGenerator
import com.github.swent.swisstravel.utils.FirebaseEmulator
import com.github.swent.swisstravel.utils.FirestoreSwissTravelTest
import org.junit.After
import org.junit.Before
import org.junit.Rule

/**
 * End-to-end trip creation flow:
 * 1) Start logged out, see login button
 * 2) Click Google sign-in (mocked)
 * 3) See bottom navigation and should be on current trip screen
 * 4) Navigate to Profile
 * 5) Change preferences in profile
 * 6) Navigate to My Trips which should be empty
 * 7) Click on the create trip button (bottom right)
 * 8) Fill the trip creation form
 * 9) Submit
 * 10) Check that the trip is on My Trips
 * 11) Long click on the trip
 * 12) Favorite the trip
 * 13) Click on the trip
 * 14) Unfavorite trip
 * 15) Click on edit trip
 * 16) Change name
 * 17) Save and go back to trip info
 * 18) Go back to My Trips
 * 19) Go to Profile
 * 20) Log out
 * 21) Check we are on landing screen
 * 22) Log back in
 * 23) Check that we are on current trip screen
 * 24) Go to my trips
 * 25) Check that the trip is there
 * 26) Open selection mode and delete the trip
 * 27) Verify that the trip is deleted (To find all the steps in the code you can do ctrl + f with
 *     "step_number)")
 *
 * Made with the help of AI
 */
class E2ETripCreationFlowTest : FirestoreSwissTravelTest() {

  @get:Rule val composeTestRule = createComposeRule()

  @get:Rule
  val permissionRule: GrantPermissionRule =
      GrantPermissionRule.grant(
          android.Manifest.permission.ACCESS_FINE_LOCATION,
          android.Manifest.permission.ACCESS_COARSE_LOCATION)

  @Before
  override fun setUp() {
    super.setUp()
    FirebaseEmulator.auth.signOut()
    FirebaseEmulator.clearAuthEmulator()
    val fakeGoogleIdToken =
        FakeJwtGenerator.createFakeGoogleIdToken(name = "Test User", email = "test@example.com")
    val fakeCredentialManager = FakeCredentialManager.fake(fakeGoogleIdToken)

    // Start app logged out
    composeTestRule.setContent { SwissTravelApp(credentialManager = fakeCredentialManager) }
  }

  //  @Test
  //  fun user_can_create_a_trip_and_edit_it() {
  //    val context = ApplicationProvider.getApplicationContext<Context>()
  //    val fakeUserRepo = FakeUserRepository()
  //
  //    /* 1), 2) */
  //    composeTestRule.loginWithGoogle(true)
  //
  //    /* 3) */
  //    // Verify bottom navigation visible
  //    composeTestRule.checkNavigationMenuIsDisplayed()
  //    composeTestRule.onNodeWithTag(NavigationTestTags.CURRENT_TRIP_TAB).performClick()
  //    composeTestRule.checkCurrentTripScreenEmptyIsDisplayed()
  //
  //    /* 4) */
  //    // Verify that we are on the profile screen
  //    composeTestRule.onNodeWithTag(NavigationTestTags.PROFILE_TAB).performClick()
  //    composeTestRule.waitUntil(E2E_WAIT_TIMEOUT) {
  //      composeTestRule
  //          .onAllNodesWithTag(ProfileScreenTestTags.PROFILE_PIC)
  //          .fetchSemanticsNodes()
  //          .isNotEmpty()
  //    }
  //    composeTestRule.checkProfileScreenIsDisplayed()
  //
  //    /* 5) */
  //    // Open preferences
  //    composeTestRule.onNodeWithTag(ProfileScreenTestTags.SETTINGS_BUTTON).performClick()
  //    composeTestRule.waitForTag(ProfileSettingsScreenTestTags.EMAIL)
  //    composeTestRule
  //        .onNodeWithTag(ProfileSettingsScreenTestTags.PREFERENCES)
  //        .performScrollToNode(hasTestTag(ProfileSettingsScreenTestTags.PREFERENCES_TOGGLE))
  //
  // composeTestRule.onNodeWithTag(ProfileSettingsScreenTestTags.PREFERENCES_TOGGLE).performClick()
  //
  //    // Preferences to change
  //    val preferences =
  //        listOf(
  //            Preference.MUSEUMS,
  //            Preference.HIKE,
  //            Preference.NIGHTLIFE,
  //            Preference.SHOPPING,
  //            Preference.WHEELCHAIR_ACCESSIBLE,
  //            Preference.NIGHT_OWL)
  //    // Assert that all categories exist
  //    for (category in
  //        PreferenceCategories.Category.values().filter {
  //          it != PreferenceCategories.Category.DEFAULT
  //        }) {
  //      val tag = PreferenceSelectorTestTags.getTestTagCategory(category)
  //      composeTestRule
  //          .onNodeWithTag(PreferenceSelectorTestTags.PREFERENCE_SELECTOR)
  //          .performScrollToNode(hasTestTag(tag))
  //      composeTestRule.onNodeWithTag(tag).assertIsDisplayed()
  //    }
  //    // Find the preferences from the list and click on them
  //    composeTestRule.performClickPreferences(preferences)
  //
  //    // Add EARLY_BIRD preference -> should remove NIGHT_OWL
  //    val tag = PreferenceSelectorTestTags.getTestTagButton(Preference.EARLY_BIRD)
  //    composeTestRule
  //        .onNodeWithTag(PreferenceSelectorTestTags.PREFERENCE_SELECTOR)
  //        .performScrollToNode(hasTestTag(tag))
  //    composeTestRule.onNodeWithTag(tag).assertIsDisplayed()
  //    composeTestRule.onNodeWithTag(tag).performClick()
  //
  //    /* 6) */
  //    // Go to my trips
  //    composeTestRule.onNodeWithTag(NavigationTestTags.TOP_BAR_BUTTON).performClick()
  //    composeTestRule.onNodeWithTag(NavigationTestTags.MY_TRIPS_TAB).performClick()
  //    composeTestRule.checkMyTripsScreenIsDisplayedWithNoTrips()
  //
  //    /* 7) */
  //    // Trip creation
  //    composeTestRule.onNodeWithTag(MyTripsScreenTestTags.CREATE_TRIP_BUTTON).performClick()
  //
  //    /* 8) */
  //    // Trip date
  //    composeTestRule.checkTripDateScreenIsDisplayed()
  //    composeTestRule.onNodeWithTag(TripDateTestTags.NEXT).performClick()
  //
  //    // Travelers
  //    val adultsLabel = context.getString(R.string.nb_adults)
  //    val childrenLabel = context.getString(R.string.nb_children)
  //    composeTestRule.checkTripTravelersIsDisplayed(adultsLabel, childrenLabel)
  //
  //    // Adults increment 2 times
  //    repeat(2) {
  //      composeTestRule.onNodeWithTag(adultsLabel + CounterTestTags.INCREMENT).performClick()
  //    }
  //
  //    // Adults decrement 1 time
  //    composeTestRule.onNodeWithTag(adultsLabel + CounterTestTags.DECREMENT).performClick()
  //
  //    // Children increment 4 times
  //    repeat(4) {
  //      composeTestRule.onNodeWithTag(childrenLabel + CounterTestTags.INCREMENT).performClick()
  //    }
  //
  //    // Children decrement 2 times
  //    repeat(2) {
  //      composeTestRule.onNodeWithTag(childrenLabel + CounterTestTags.DECREMENT).performClick()
  //    }
  //
  //    // Check the adult counter displays 2
  //    composeTestRule.onNodeWithTag(adultsLabel + CounterTestTags.COUNT).assertTextEquals("2")
  //
  //    // Check the children counter displays 2
  //    composeTestRule.onNodeWithTag(childrenLabel + CounterTestTags.COUNT).assertTextEquals("2")
  //
  //    composeTestRule.onNodeWithTag(TripTravelersTestTags.NEXT).performClick()
  //
  //    // Preferences
  //    val newPreferences =
  //        listOf(
  //            Preference.GROUP,
  //            Preference.HIKE, // Remove hike since already activated in the profile
  //            Preference.NIGHTLIFE, // Remove nightlife since already activated in the profile
  //            Preference.SHOPPING, // Remove shopping since already activated in the profile
  //            Preference.MUSEUMS, // Remove museums since already activated in the profile
  //        )
  //    composeTestRule.checkTripPreferencesIsDisplayed()
  //    // Assert that all categories exist
  //    for (category in
  //        PreferenceCategories.Category.values().filter {
  //          it != PreferenceCategories.Category.DEFAULT
  //        }) {
  //      val tagPref = PreferenceSelectorTestTags.getTestTagCategory(category)
  //      composeTestRule
  //          .onNodeWithTag(PreferenceSelectorTestTags.PREFERENCE_SELECTOR)
  //          .performScrollToNode(hasTestTag(tagPref))
  //      composeTestRule.onNodeWithTag(tagPref).assertIsDisplayed()
  //    }
  //    // Find the preferences from the list and click on them
  //    composeTestRule.performClickPreferences(newPreferences)
  //    composeTestRule
  //        .onNodeWithTag(TripPreferencesTestTags.TRIP_PREFERENCE_CONTENT)
  //        .performScrollToNode(hasTestTag(TripPreferencesTestTags.DONE))
  //    composeTestRule.onNodeWithTag(TripPreferencesTestTags.DONE).assertIsDisplayed()
  //    composeTestRule.onNodeWithTag(TripPreferencesTestTags.DONE).performClick()
  //
  //    // Arrival - destination (using nominatim)
  //    composeTestRule.checkArrivalDepartureScreenIsDisplayed()
  //    composeTestRule
  //        .onNodeWithTag(ArrivalDepartureTestTags.NEXT_BUTTON)
  //        .performClick() // should do nothing
  //    composeTestRule.checkArrivalDepartureScreenIsDisplayed() // still on the same screen
  //
  //    // Fill the text fields
  //    composeTestRule
  //        .onNodeWithTag(ArrivalDepartureTestTags.ARRIVAL_TEXTFIELD, useUnmergedTree = true)
  //        .performTextInput("epfl")
  //    composeTestRule.performClickOnLocationSuggestion()
  //
  //    composeTestRule
  //        .onNodeWithTag(ArrivalDepartureTestTags.DEPARTURE_TEXTFIELD, useUnmergedTree = true)
  //        .performTextInput("cafe de paris")
  //    composeTestRule.performClickOnLocationSuggestion()
  //
  //    composeTestRule.onNodeWithTag(ArrivalDepartureTestTags.NEXT_BUTTON).performClick()
  //
  //    composeTestRule.onNodeWithTag(TripFirstDestinationsTestTags.NEXT_BUTTON).performClick()
  //
  //    // Trip Summary
  //    composeTestRule.waitUntil(E2E_WAIT_TIMEOUT) {
  //      composeTestRule
  //          .onAllNodesWithTag(TripSummaryTestTags.TRIP_SUMMARY_SCREEN)
  //          .fetchSemanticsNodes()
  //          .isNotEmpty()
  //    }
  //    //
  // *********************************************************************************************
  //    // Recreate the trip object locally just for data verification to match the
  // "createSampleTrip"
  //    // logic
  //    val startTimestamp = Timestamp.now()
  //    val endTimestamp = Timestamp(startTimestamp.seconds + 24 * 60 * 60, 0)
  //    val arrivalLocation =
  //        Location(
  //            Coordinate(46.5191, 6.5668),
  //            "École Polytechnique Fédérale de Lausanne (EPFL), Route Cantonale, 1015 Lausanne",
  //            null)
  //    val departureLocation =
  //        Location(
  //            Coordinate(46.2095, 6.1432), "Café de Paris, Rue du Mont-Blanc 26, 1201 Genève",
  // null)
  //    val zermattLocation =
  //        Location(Coordinate(46.0207, 7.7491), "Zermatt", "https://example.com/zermatt1.jpg")
  //    val locations = listOf(departureLocation, zermattLocation, arrivalLocation)
  //
  //    var tripE2E =
  //        createTestTrip(
  //            uid = "testuid",
  //            name = "trip-E2E-1",
  //            ownerId = currentUser.uid,
  //            locations = locations,
  //            adults = 2,
  //            children = 2,
  //            departureLocation = departureLocation,
  //            arrivalLocation = arrivalLocation,
  //            startDate = startTimestamp,
  //            endDate = endTimestamp,
  //            preferredLocations = locations,
  //            preferences =
  //                listOf(Preference.GROUP, Preference.WHEELCHAIR_ACCESSIBLE,
  // Preference.EARLY_BIRD))
  //    //
  // *********************************************************************************************
  //
  //    composeTestRule.checkTripSummaryScreenIsDisplayed(
  //        expectedAdults = tripE2E.tripProfile.adults,
  //        expectedChildren = tripE2E.tripProfile.children,
  //        expectedDeparture = tripE2E.tripProfile.departureLocation?.name!!,
  //        expectedArrival = tripE2E.tripProfile.arrivalLocation?.name!!,
  //        expectedPreferences = tripE2E.tripProfile.preferences,
  //        startDate = tripE2E.tripProfile.startDate,
  //        endDate = tripE2E.tripProfile.endDate)
  //
  //    // Write the name
  //    composeTestRule
  //        .onNodeWithTag(TripSummaryTestTags.TRIP_SUMMARY_SCREEN)
  //        .performScrollToNode(hasTestTag(TripSummaryTestTags.TRIP_NAME_FIELD))
  //    composeTestRule
  //        .onNodeWithTag(TripSummaryTestTags.TRIP_NAME_FIELD)
  //        .performTextInput(tripE2E.name)
  //    composeTestRule.onNodeWithTag(TripSummaryTestTags.TRIP_NAME_FIELD).performImeAction()
  //
  //    /* 9) */
  //    composeTestRule
  //        .onNodeWithTag(TripSummaryTestTags.TRIP_SUMMARY_SCREEN)
  //        .performScrollToNode(hasTestTag(TripSummaryTestTags.CREATE_TRIP_BUTTON))
  //    composeTestRule.onNodeWithTag(TripSummaryTestTags.CREATE_TRIP_BUTTON).performClick()
  //
  //    /* 10) */
  //    // Back to my trips
  //    composeTestRule.waitUntil(
  //        E2E_WAIT_TIMEOUT * 3) { // Algorithm can take a long time to generate the trip
  //          composeTestRule
  //              .onAllNodesWithTag(
  //                  MyTripsScreenTestTags.CREATE_TRIP_BUTTON) // random element on my trips screen
  //              .fetchSemanticsNodes()
  //              .isNotEmpty()
  //        }
  //    composeTestRule.checkMyTripsScreenIsDisplayed()
  //    val trips = runBlocking { repository.getAllTrips() }
  //    assertEquals(1, trips.size)
  //    val trip = trips.elementAt(0)
  //    tripE2E = trip
  //
  //    /* 11) */
  //    // Long click
  //    composeTestRule.waitForTag(MyTripsScreenTestTags.getTestTagForTrip(tripE2E))
  //    composeTestRule
  //        .onNodeWithTag(MyTripsScreenTestTags.getTestTagForTrip(tripE2E))
  //        .assertIsDisplayed()
  //    composeTestRule
  //        .onNodeWithTag(MyTripsScreenTestTags.getTestTagForTrip(tripE2E))
  //        .performTouchInput { longClick() }
  //
  //    // Selection mode
  //    composeTestRule.checkMyTripsInSelectionMode()
  //
  //    /* 12) */
  //    // Save as favorite
  //    composeTestRule.onNodeWithTag(MyTripsScreenTestTags.FAVORITE_SELECTED_BUTTON).performClick()
  //    composeTestRule.waitForTag(MyTripsScreenTestTags.CREATE_TRIP_BUTTON)
  //    composeTestRule.checkMyTripsNotInSelectionMode()
  //
  //    tripE2E = runBlocking { repository.getTrip(tripE2E.uid) }
  //
  //    /* 13) */
  //    // Click on the trip
  //    composeTestRule.onNodeWithTag(TripElementTestTags.getTestTagForTrip(tripE2E)).performClick()
  //    composeTestRule.waitForTag(DailyViewScreenTestTags.TITLE)
  //
  //    /* 14) */
  //    // Unfavorite trip
  //    composeTestRule.waitForTag(DailyViewScreenTestTags.FAVORITE_BUTTON)
  //    composeTestRule.onNodeWithTag(DailyViewScreenTestTags.FAVORITE_BUTTON).performClick()
  //    Thread.sleep(1500)
  //    tripE2E = runBlocking { repository.getTrip(tripE2E.uid) }
  //    assertFalse(
  //        runBlocking { fakeUserRepo.getCurrentUser().favoriteTripsUids.contains(tripE2E.uid) },
  //        "The trip is still favorited")
  //
  //    /* 15) */
  //    // Click on edit trip
  //    composeTestRule.onNodeWithTag(DailyViewScreenTestTags.EDIT_BUTTON).performClick()
  //
  //    // Edit trip screen
  //    composeTestRule.checkEditTripScreenIsDisplayed(tripE2E, adultsLabel, childrenLabel)
  //
  //    /* 16), 17) */
  //    val newName = "trip-e2e-new-name"
  //    // Change the name
  //    composeTestRule.changeTripNameAndSaveInEditTrip(newName)
  //
  //    // Wait until the UI is back to the trip info screen and the name is updated
  //    composeTestRule.waitUntil(E2E_WAIT_TIMEOUT) {
  //      composeTestRule.onAllNodesWithText(newName).fetchSemanticsNodes().isNotEmpty()
  //    }
  //
  //    // Getting the trip
  //    tripE2E = runBlocking { repository.getTrip(tripE2E.uid) }
  //    assertEquals(newName, tripE2E.name)
  //
  //    // Back to the trip info screen
  //    composeTestRule.onNodeWithTag(DailyViewScreenTestTags.BACK_BUTTON).performClick()
  //
  //    /* 18) */
  //    // Back to my trips
  //    composeTestRule.checkMyTripsScreenIsDisplayed()
  //
  //    /* 19) */
  //    // Go back to profile
  //    composeTestRule.onNodeWithTag(NavigationTestTags.PROFILE_TAB).performClick()
  //
  //    /* 20) */
  //    // Log out
  //    composeTestRule.logout()
  //
  //    /* 21) */
  //    // Check that we are on landing screen
  //    composeTestRule.checkLandingScreenIsDisplayed()
  //
  //    /* 22) */
  //    // Sign In
  //    composeTestRule.loginWithGoogle(true)
  //
  //    /* 23) */
  //    // Check that we arrive on "Current Trip" Screen
  //    // Verify bottom navigation visible
  //    composeTestRule.checkNavigationMenuIsDisplayed()
  //
  //    /* 24) */
  //    // Go to My Trips
  //    composeTestRule.onNodeWithTag(NavigationTestTags.MY_TRIPS_TAB).performClick()
  //    composeTestRule.checkMyTripsScreenIsDisplayed()
  //
  //    /* 25) */
  //    // Check that the previously created a trip is still there
  //    val trips2ndCheck = runBlocking { repository.getAllTrips() }
  //    assertEquals(1, trips.size)
  //    val trip2ndCheck = trips2ndCheck.elementAt(0)
  //    composeTestRule
  //        .onNodeWithTag(MyTripsScreenTestTags.getTestTagForTrip(trip2ndCheck))
  //        .assertIsDisplayed()
  //
  //    /* 26) */
  //    // Long click (Selection Mode)
  //    composeTestRule
  //        .onNodeWithTag(MyTripsScreenTestTags.getTestTagForTrip(trip2ndCheck))
  //        .performTouchInput { longClick() }
  //    composeTestRule.checkMyTripsInSelectionMode()
  //
  //    // Delete Trip
  //    composeTestRule.onNodeWithTag(MyTripsScreenTestTags.DELETE_SELECTED_BUTTON).performClick()
  //    // AlertDialog should appear
  //    composeTestRule
  //        .onNodeWithTag(DeleteTripDialogTestTags.CONFIRM_DELETE_BUTTON)
  //        .assertIsDisplayed()
  //
  //
  // composeTestRule.onNodeWithTag(DeleteTripDialogTestTags.CANCEL_DELETE_BUTTON).assertIsDisplayed()
  //    // Confirm deletion
  //    composeTestRule.onNodeWithTag(DeleteTripDialogTestTags.CONFIRM_DELETE_BUTTON).performClick()
  //    composeTestRule.checkMyTripsNotInSelectionMode()
  //
  //    /* 27) */
  //    // Check that the Trip is actually deleted (backend and frontend)
  //    val emptyTrips = runBlocking { repository.getAllTrips() }
  //    assertEquals(0, emptyTrips.size)
  //    composeTestRule.waitForIdle()
  //    composeTestRule.checkMyTripsScreenIsDisplayedWithNoTrips()
  //  }

  @After
  override fun tearDown() {
    super.tearDown()
  }
}
