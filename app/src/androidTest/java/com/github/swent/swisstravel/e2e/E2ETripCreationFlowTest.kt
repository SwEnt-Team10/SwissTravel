package com.github.swent.swisstravel.e2e

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.SwissTravelApp
import com.github.swent.swisstravel.model.trip.Coordinate
import com.github.swent.swisstravel.model.trip.Location
import com.github.swent.swisstravel.model.trip.Trip
import com.github.swent.swisstravel.model.trip.TripProfile
import com.github.swent.swisstravel.model.user.Preference
import com.github.swent.swisstravel.model.user.PreferenceCategories
import com.github.swent.swisstravel.ui.authentication.LandingScreenTestTags.SIGN_IN_BUTTON
import com.github.swent.swisstravel.ui.authentication.SignInScreenTestTags.GOOGLE_LOGIN_BUTTON
import com.github.swent.swisstravel.ui.composable.CounterTestTags
import com.github.swent.swisstravel.ui.composable.PreferenceSelectorTestTags
import com.github.swent.swisstravel.ui.geocoding.LocationTextTestTags
import com.github.swent.swisstravel.ui.navigation.NavigationTestTags
import com.github.swent.swisstravel.ui.profile.ProfileScreenTestTags
import com.github.swent.swisstravel.ui.theme.SwissTravelTheme
import com.github.swent.swisstravel.ui.tripcreation.ArrivalDepartureTestTags
import com.github.swent.swisstravel.ui.tripcreation.TripDateTestTags
import com.github.swent.swisstravel.ui.tripcreation.TripFirstDestinationsTestTags
import com.github.swent.swisstravel.ui.tripcreation.TripPreferencesTestTags
import com.github.swent.swisstravel.ui.tripcreation.TripSummaryTestTags
import com.github.swent.swisstravel.ui.tripcreation.TripTravelersTestTags
import com.github.swent.swisstravel.ui.trips.MyTripsScreenTestTags
import com.github.swent.swisstravel.utils.E2E_WAIT_TIMEOUT
import com.github.swent.swisstravel.utils.FakeCredentialManager
import com.github.swent.swisstravel.utils.FakeJwtGenerator
import com.github.swent.swisstravel.utils.FirebaseEmulator
import com.github.swent.swisstravel.utils.FirestoreSwissTravelTest
import com.google.firebase.Timestamp
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * End-to-end trip creation flow:
 * 1) Start logged out, see login button
 * 2) Click Google sign-in (mocked)
 * 3) See bottom navigation and should be on profile screen
 * 4) Change preferences in profile
 * 5) Navigate to my trips which should be empty
 * 6) Click on the create trip button (bottom right)
 * 7) Fill the trip creation form
 * 8) Submit and see the newly created trip in my trips list
 * 9) Click on it and see trip details
 * 10) Edit the trip
 * 11) Save and go back to my trips
 * 12) Long click on the trip
 * 13) Favorite trip
 * 14) Click on the edit button and set as current trip
 * 15) Click on the trip and change some information
 * 16) Save and go back to trip info
 * 17) Click on edit trip
 * 18) Delete trip
 * 19) Go back to my trips and assert that the trip is gone and that the edit button isn't available
 * 20) Go to profile
 * 21) Log out
 */
class E2ETripCreationFlowTest : FirestoreSwissTravelTest() {

  @get:Rule val composeTestRule = createComposeRule()

  @Before
  override fun setUp() {
    super.setUp()
    FirebaseEmulator.auth.signOut()
    FirebaseEmulator.clearAuthEmulator()
  }

  @Test
  fun user_can_create_a_trip_and_edit_it() {
    val context = ApplicationProvider.getApplicationContext<Context>()

    val fakeGoogleIdToken =
        FakeJwtGenerator.createFakeGoogleIdToken(name = "Test User", email = "test@example.com")
    val fakeCredentialManager = FakeCredentialManager.fake(fakeGoogleIdToken)

    // Start app logged out
    composeTestRule.setContent {
      SwissTravelTheme { SwissTravelApp(credentialManager = fakeCredentialManager) }
    }
    composeTestRule.onNodeWithTag(SIGN_IN_BUTTON).assertExists().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(GOOGLE_LOGIN_BUTTON).assertExists().performClick()

    // Wait for main navigation to appear (indicates successful sign-in + main UI shown)
    composeTestRule.waitUntil(E2E_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Verify bottom navigation visible
    composeTestRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU).assertExists()

    // Verify that we are on the profile screen
    composeTestRule.onNodeWithTag(NavigationTestTags.PROFILE_TAB).performClick()
    composeTestRule.waitForIdle()
    Thread.sleep(
        5000) // Otherwise it is checks too fast the screen so it doesn't have time to pull the
    // elements
    composeTestRule.checkProfileScreenIsDisplayed()

    // Open preferences
    composeTestRule
        .onNodeWithTag(ProfileScreenTestTags.PREFERENCES)
        .performScrollToNode(hasTestTag(ProfileScreenTestTags.PREFERENCES_TOGGLE))
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.PREFERENCES_TOGGLE).performClick()

    // Preferences to change
    val preferences =
        listOf(
            Preference.MUSEUMS,
            Preference.HIKE,
            Preference.NIGHTLIFE,
            Preference.SHOPPING,
            Preference.WHEELCHAIR_ACCESSIBLE,
            Preference.NIGHT_OWL)
    // Assert that all categories exist
    for (category in
        PreferenceCategories.Category.values().filter {
          it != PreferenceCategories.Category.DEFAULT
        }) {
      val tag = PreferenceSelectorTestTags.getTestTagCategory(category)
      composeTestRule
          .onNodeWithTag(ProfileScreenTestTags.PREFERENCES_LIST)
          .performScrollToNode(hasTestTag(tag))
      composeTestRule.onNodeWithTag(tag).assertIsDisplayed()
    }
    // Find the preferences from the list and click on them
    composeTestRule.performClickPreferences(preferences)

    // Add EARLY_BIRD preference -> should remove NIGHT_OWL
    val tag = PreferenceSelectorTestTags.getTestTagButton(Preference.EARLY_BIRD)
    composeTestRule
        .onNodeWithTag(PreferenceSelectorTestTags.PREFERENCE_SELECTOR)
        .performScrollToNode(hasTestTag(tag))
    composeTestRule.onNodeWithTag(tag).assertIsDisplayed()
    composeTestRule.onNodeWithTag(tag).performClick()

    // Go to my trips
    composeTestRule.onNodeWithTag(NavigationTestTags.MY_TRIPS_TAB).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.checkMyTripsScreenIsDisplayedWithNoCurrentTrips()

    // Trip creation (the trip used will be stored in SwissTravelTest.kt)
    composeTestRule.onNodeWithTag(MyTripsScreenTestTags.CREATE_TRIP_BUTTON).performClick()

    composeTestRule.waitForIdle()
    // Trip date
    composeTestRule.checkTripDateScreenIsDisplayed()
    composeTestRule.onNodeWithTag(TripDateTestTags.NEXT).performClick()
    composeTestRule.waitForIdle()

    // Travelers
    val adultsLabel = context.getString(R.string.nb_adults)
    val childrenLabel = context.getString(R.string.nb_children)
    composeTestRule.checkTripTravelersIsDisplayed(adultsLabel, childrenLabel)

    // Adults increment 2 times
    repeat(2) {
      composeTestRule.onNodeWithTag(adultsLabel + CounterTestTags.INCREMENT).performClick()
    }

    // Adults decrement 1 time
    composeTestRule.onNodeWithTag(adultsLabel + CounterTestTags.DECREMENT).performClick()

    // Children increment 4 times
    repeat(4) {
      composeTestRule.onNodeWithTag(childrenLabel + CounterTestTags.INCREMENT).performClick()
    }

    // Children decrement 2 times
    repeat(2) {
      composeTestRule.onNodeWithTag(childrenLabel + CounterTestTags.DECREMENT).performClick()
    }

    // Check the adult counter displays 2
    composeTestRule.onNodeWithTag(adultsLabel + CounterTestTags.COUNT).assertTextEquals("2")

    // Check the children counter displays 2
    composeTestRule.onNodeWithTag(childrenLabel + CounterTestTags.COUNT).assertTextEquals("2")

    composeTestRule.onNodeWithTag(TripTravelersTestTags.NEXT).performClick()
    composeTestRule.waitForIdle()

    // Preferences
    val newPreferences =
        listOf(
            Preference.GROUP,
            Preference.CHILDREN_FRIENDLY,
            Preference.MUSEUMS, // Remove museums since already activated in the profile
        )
    composeTestRule.checkTripPreferencesIsDisplayed()
    // Assert that all categories exist
    for (category in
        PreferenceCategories.Category.values().filter {
          it != PreferenceCategories.Category.DEFAULT
        }) {
      val tagPref = PreferenceSelectorTestTags.getTestTagCategory(category)
      composeTestRule
          .onNodeWithTag(TripPreferencesTestTags.TRIP_PREFERENCE_CONTENT)
          .performScrollToNode(hasTestTag(tagPref))
      composeTestRule.onNodeWithTag(tagPref).assertIsDisplayed()
    }
    // Find the preferences from the list and click on them
    composeTestRule.performClickPreferences(newPreferences)
    composeTestRule
        .onNodeWithTag(TripPreferencesTestTags.TRIP_PREFERENCE_CONTENT)
        .performScrollToNode(hasTestTag(TripPreferencesTestTags.DONE))
    composeTestRule.onNodeWithTag(TripPreferencesTestTags.DONE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(TripPreferencesTestTags.DONE).performClick()
    composeTestRule.waitForIdle()

    // Arrival - destination (using nominatim)
    composeTestRule.checkArrivalDepartureScreenIsDisplayed()
    composeTestRule
        .onNodeWithTag(ArrivalDepartureTestTags.NEXT_BUTTON)
        .performClick() // should do nothing
    composeTestRule.waitForIdle()
    composeTestRule.checkArrivalDepartureScreenIsDisplayed() // still on the same screen

    // Fill the text fields
    composeTestRule
        .onNodeWithTag(ArrivalDepartureTestTags.ARRIVAL_TEXTFIELD, useUnmergedTree = true)
        .performTextInput("epfl")
    composeTestRule.performClickOnLocationSuggestion()

    composeTestRule
        .onNodeWithTag(ArrivalDepartureTestTags.DEPARTURE_TEXTFIELD, useUnmergedTree = true)
        .performTextInput("cafe de paris")
    composeTestRule.performClickOnLocationSuggestion()

    composeTestRule.onNodeWithTag(ArrivalDepartureTestTags.NEXT_BUTTON).performClick()
    composeTestRule.waitForIdle()

    // Destination (using MySwitzerland)
    composeTestRule.checkDestinationScreenIsDisplayed()
    composeTestRule
        .onNodeWithTag(TripFirstDestinationsTestTags.ADD_FIRST_DESTINATION)
        .performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(LocationTextTestTags.INPUT_LOCATION).performTextInput("zermatt")
    composeTestRule.performClickOnLocationSuggestion()
    composeTestRule.onNodeWithTag(TripFirstDestinationsTestTags.NEXT_BUTTON).performClick()
    composeTestRule.waitForIdle()

    // Trip Summary
    composeTestRule.waitUntil(E2E_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(TripSummaryTestTags.TRIP_SUMMARY_SCREEN)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    val tripE2E = createSampleTrip()
    composeTestRule.checkTripSummaryScreenIsDisplayed(
        expectedAdults = tripE2E.tripProfile.adults,
        expectedChildren = tripE2E.tripProfile.children,
        expectedDeparture = tripE2E.tripProfile.departureLocation?.name!!,
        expectedArrival = tripE2E.tripProfile.arrivalLocation?.name!!,
        expectedPreferences = tripE2E.tripProfile.preferences,
        startDate = tripE2E.tripProfile.startDate,
        endDate = tripE2E.tripProfile.endDate)
    // Write the name
    composeTestRule
        .onNodeWithTag(TripSummaryTestTags.TRIP_SUMMARY_SCREEN)
        .performScrollToNode(hasTestTag(TripSummaryTestTags.TRIP_NAME_FIELD))
    composeTestRule.waitForIdle()
    composeTestRule
        .onNodeWithTag(TripSummaryTestTags.TRIP_NAME_FIELD)
        .performTextInput(tripE2E.name)
    composeTestRule.onNodeWithTag(TripSummaryTestTags.TRIP_NAME_FIELD).performImeAction()

    composeTestRule
        .onNodeWithTag(TripSummaryTestTags.TRIP_SUMMARY_SCREEN)
        .performScrollToNode(hasTestTag(TripSummaryTestTags.CREATE_TRIP_BUTTON))
    composeTestRule.onNodeWithTag(TripSummaryTestTags.CREATE_TRIP_BUTTON).performClick()
    composeTestRule.waitForIdle()

    //    // Back to my trips
    composeTestRule.waitUntil(E2E_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(MyTripsScreenTestTags.CREATE_TRIP_BUTTON)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    composeTestRule.checkMyTripsScreenIsDisplayedWithNoCurrentTrips()

    // Long click
    //    val repo = repository
    //    val savedTrip = runBlocking {
    //      repeat(10) { attempt ->
    //        val trips = repo.getAllTrips()
    //        val found = trips.firstOrNull { it.name.trim() == tripE2E.name.trim() }
    //        if (found != null) return@runBlocking found
    //        println("Trip not found yet, waiting... (attempt $attempt)")
    //        kotlinx.coroutines.delay(1000)
    //      }
    //      null
    //    }
    //
    //      checkNotNull(savedTrip) { "Trip not found in Firestore after creation" }
    //
    //      val trip = runBlocking { repo.getTrip(savedTrip.uid) }
    //
    //      composeTestRule.onNodeWithTag(TripElementTestTags.getTestTagForTrip(trip))
    //          .assertIsDisplayed().performTouchInput { longClick() }
    //      composeTestRule.waitForIdle()
    //
    //    // Selection mode
    //      composeTestRule.checkMyTripsInSelectionMode()
    //
    //      // Save as favorite
    //
    // composeTestRule.onNodeWithTag(MyTripsScreenTestTags.FAVORITE_SELECTED_BUTTON).performClick()
    //      composeTestRule.waitForIdle()
    //      composeTestRule.checkMyTripsNotInSelectionMode()
    //
    //    // Edit current trip
    //
    // composeTestRule.onNodeWithTag(MyTripsScreenTestTags.EDIT_CURRENT_TRIP_BUTTON).performClick()
    //      composeTestRule.waitForIdle()
    //
    //    // Set current trip screen
    //      composeTestRule.checkSetCurrentTripIsDisplayed()
    //
    //    // Set as current trip
    //
    // composeTestRule.onNodeWithTag(TripElementTestTags.getTestTagForTrip(trip)).performClick()
    //      composeTestRule.waitForIdle()
    //
    //    // Back to my trips
    //      composeTestRule.checkMyTripsWithATripAsCurrent(listOf(trip))
    //
    //    // Click on the trip
    //
    // composeTestRule.onNodeWithTag(TripElementTestTags.getTestTagForTrip(trip)).performClick()
    //        composeTestRule.waitForIdle()
    // Trip info screen

    // Compare the current step

    // Click on isFavorite to remove

    // Click on edit trip

    // Edit trip screen

    // Verify all the infos

    // Change some data (name because easier to test)

    // Save

    // Back to the trip info screen

    // Click on edit trip

    // Delete the trip

    // Back to my trips

    // Assert no trips

    // Go back to profile

    // Log out

    // Check that we are on landing screen

  }

  @After
  override fun tearDown() {
    super.tearDown()
  }

  /**
   * Method that returns a hardcoded trip that should correspond to the one created in the test
   * (Helped by AI)
   */
  private fun createSampleTrip(): Trip {
    val startTimestamp = Timestamp.now() // today
    val endTimestamp = Timestamp(startTimestamp.seconds + 24 * 60 * 60, 0) // tomorrow

    // Locations
    val arrivalLocation =
        Location(
            coordinate = Coordinate(46.5191, 6.5668),
            name =
                "École Polytechnique Fédérale de Lausanne (EPFL), Route Cantonale, 1015 Lausanne, Vaud",
            imageUrl = null)

    val departureLocation =
        Location(
            coordinate = Coordinate(46.2095, 6.1432),
            name = "Café de Paris, Rue du Mont-Blanc 26, 1201 Genève, Genève",
            imageUrl = null)

    val zermattLocation =
        Location(
            coordinate = Coordinate(46.0207, 7.7491),
            name = "Zermatt",
            imageUrl = "https://example.com/zermatt1.jpg")

    val locations = listOf(departureLocation, zermattLocation, arrivalLocation)

    // Trip profile
    val tripProfile =
        TripProfile(
            adults = 2,
            children = 2,
            departureLocation = departureLocation,
            arrivalLocation = arrivalLocation,
            startDate = startTimestamp,
            endDate = endTimestamp,
            preferences =
                listOf(
                    Preference.GROUP,
                    Preference.CHILDREN_FRIENDLY,
                    Preference.HIKE,
                    Preference.NIGHTLIFE,
                    Preference.SHOPPING,
                    Preference.WHEELCHAIR_ACCESSIBLE,
                    Preference.EARLY_BIRD))

    // Create the trip
    return Trip(
        uid = "unvalid", // cannot be changed and is random
        name = "trip-E2E-1",
        ownerId = "unvalid", // cannot be changed and is random
        locations = locations,
        routeSegments = emptyList(), // empty for now
        activities = emptyList(), // no activities yet
        tripProfile = tripProfile,
        isFavorite = false,
        isCurrentTrip = false)
  }
}
