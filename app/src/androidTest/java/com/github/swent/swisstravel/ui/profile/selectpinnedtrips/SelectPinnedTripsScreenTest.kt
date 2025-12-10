package com.github.swent.swisstravel.ui.profile.selectpinnedtrips

import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.github.swent.swisstravel.model.trip.Trip
import com.github.swent.swisstravel.model.trip.TripProfile
import com.github.swent.swisstravel.model.trip.TripRepositoryLocal
import com.github.swent.swisstravel.model.trip.TripsRepository
import com.github.swent.swisstravel.model.user.Preference
import com.github.swent.swisstravel.model.user.User
import com.github.swent.swisstravel.model.user.UserRepository
import com.github.swent.swisstravel.model.user.UserStats
import com.github.swent.swisstravel.ui.composable.TripListTestTags
import com.github.swent.swisstravel.ui.theme.SwissTravelTheme
import com.github.swent.swisstravel.ui.trips.TripElementTestTags
import com.google.firebase.Timestamp
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Contains emulator tests tags for [SelectPinnedTripsScreen]. Tests made with the help of AI. */
class SelectPinnedTripsScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  /** Fake repository to avoid Firebase. */
  private val trip1 =
      Trip(
          uid = "trip1",
          name = "Trip One",
          ownerId = "user1",
          locations = emptyList(),
          routeSegments = emptyList(),
          activities = emptyList(),
          tripProfile =
              TripProfile(
                  startDate = Timestamp(0, 0),
                  endDate = Timestamp(0, 0),
                  preferredLocations = emptyList(),
                  preferences = emptyList()),
          isFavorite = false,
          isCurrentTrip = false,
          listUri = emptyList(),
          collaboratorsId = emptyList())
  private val trip2 =
      Trip(
          uid = "trip2",
          name = "Trip Two",
          ownerId = "user1",
          locations = emptyList(),
          routeSegments = emptyList(),
          activities = emptyList(),
          tripProfile =
              TripProfile(
                  startDate = Timestamp(0, 0),
                  endDate = Timestamp(0, 0),
                  preferredLocations = emptyList(),
                  preferences = emptyList()),
          isFavorite = false,
          isCurrentTrip = false,
          listUri = emptyList(),
          collaboratorsId = emptyList())
  private val trip3 =
      Trip(
          uid = "trip3",
          name = "Trip Three",
          ownerId = "user1",
          locations = emptyList(),
          routeSegments = emptyList(),
          activities = emptyList(),
          tripProfile =
              TripProfile(
                  startDate = Timestamp(0, 0),
                  endDate = Timestamp(0, 0),
                  preferredLocations = emptyList(),
                  preferences = emptyList()),
          isFavorite = false,
          isCurrentTrip = false,
          listUri = emptyList(),
          collaboratorsId = emptyList())
  private val trip4 =
      Trip(
          uid = "trip4",
          name = "Trip Four",
          ownerId = "user1",
          locations = emptyList(),
          routeSegments = emptyList(),
          activities = emptyList(),
          tripProfile =
              TripProfile(
                  startDate = Timestamp(0, 0),
                  endDate = Timestamp(0, 0),
                  preferredLocations = emptyList(),
                  preferences = emptyList()),
          isFavorite = false,
          isCurrentTrip = false,
          listUri = emptyList(),
          collaboratorsId = emptyList())
  private val fakeTripsRepo = TripRepositoryLocal()

  init {
    runBlocking {
      fakeTripsRepo.addTrip(trip1)
      fakeTripsRepo.addTrip(trip2)
      fakeTripsRepo.addTrip(trip3)
      fakeTripsRepo.addTrip(trip4)
    }
  }

  private val fakeUserRepo =
      object : UserRepository {
        var user =
            User(
                uid = "user1",
                name = "Test User",
                biography = "Bio",
                email = "test@example.com",
                profilePicUrl = "",
                preferences = emptyList(),
                friends = emptyList(),
                stats = UserStats(),
                pinnedTripsUids = listOf("trip1"),
                pinnedPicturesUris = emptyList())

        override suspend fun getCurrentUser() = user

        override suspend fun getUserByUid(uid: String) = null

        override suspend fun getUserByNameOrEmail(query: String) = emptyList<User>()

        override suspend fun updateUserPreferences(uid: String, preferences: List<Preference>) {}

        override suspend fun sendFriendRequest(fromUid: String, toUid: String) {}

        override suspend fun acceptFriendRequest(currentUid: String, fromUid: String) {}

        override suspend fun removeFriend(uid: String, friendUid: String) {}

        override suspend fun updateUser(
            uid: String,
            name: String?,
            biography: String?,
            profilePicUrl: String?,
            preferences: List<Preference>?,
            pinnedTripsUids: List<String>?,
            pinnedPicturesUrls: List<Uri>?
        ) {
          user =
              user.copy(
                  pinnedTripsUids = pinnedTripsUids ?: user.pinnedTripsUids,
              )
        }

        override suspend fun updateUserStats(uid: String, stats: UserStats) {}
      }

  private fun setContentHelper(
      userRepo: UserRepository = fakeUserRepo,
      tripRepo: TripsRepository = fakeTripsRepo
  ) {
    composeTestRule.setContent {
      SwissTravelTheme { SelectPinnedTripsScreen(SelectPinnedTripsViewModel(tripRepo, userRepo)) }
    }
  }

  @Test
  fun allKeyUIElementsAreDisplayed() {
    setContentHelper()

    composeTestRule.onNodeWithTag(SelectPinnedTripsScreenTestTags.TOP_APP_BAR).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(SelectPinnedTripsScreenTestTags.SAVE_SELECTED_TRIPS_FAB)
        .assertIsDisplayed()
    composeTestRule.onNodeWithTag(TripListTestTags.TRIP_LIST).assertIsDisplayed()

    // Ensure pinned trip is displayed
    composeTestRule.onNodeWithText("Trip One").assertIsDisplayed()
    // Non-pinned trips may appear in available list
    composeTestRule.onNodeWithText("Trip Two").assertIsDisplayed()
  }

  @Test
  fun emptyPinnedTrips_showsFallback() {
    val emptyTripsRepo = TripRepositoryLocal() // contains no trips

    setContentHelper(tripRepo = emptyTripsRepo)

    composeTestRule.onNodeWithTag(TripListTestTags.EMPTY_MESSAGE).assertIsDisplayed()
  }

  @Test
  fun selectingATrip_addsItToPinnedTrips() {
    setContentHelper()
    runBlocking {
      // Trip2 is NOT pinned initially
      assertFalse(fakeUserRepo.getCurrentUser().pinnedTripsUids.contains("trip2"))

      // Click Trip2 entry
      composeTestRule
          .onNodeWithTag(TripElementTestTags.getTestTagForTrip(fakeTripsRepo.getTrip("trip2")))
          .performClick()

      composeTestRule
          .onNodeWithTag(SelectPinnedTripsScreenTestTags.SAVE_SELECTED_TRIPS_FAB)
          .performClick()

      // Now trip2 should be pinned in the user repository
      assertTrue(fakeUserRepo.getCurrentUser().pinnedTripsUids.contains("trip2"))
    }
  }

  @Test
  fun selectingMoreThanThreeTrips_isBlocked() {
    setContentHelper()
    runBlocking {
      // Select trip2
      composeTestRule
          .onNodeWithTag(TripElementTestTags.getTestTagForTrip(fakeTripsRepo.getTrip("trip2")))
          .performClick()

      // Select trip3
      composeTestRule
          .onNodeWithTag(TripElementTestTags.getTestTagForTrip(fakeTripsRepo.getTrip("trip3")))
          .performClick()

      // Save selection
      composeTestRule
          .onNodeWithTag(SelectPinnedTripsScreenTestTags.SAVE_SELECTED_TRIPS_FAB)
          .performClick()

      // Attempt to select 4th trip (should be blocked)
      composeTestRule
          .onNodeWithTag(TripElementTestTags.getTestTagForTrip(fakeTripsRepo.getTrip("trip4")))
          .performClick()

      // Save selection
      composeTestRule
          .onNodeWithTag(SelectPinnedTripsScreenTestTags.SAVE_SELECTED_TRIPS_FAB)
          .performClick()

      val pinned = fakeUserRepo.getCurrentUser().pinnedTripsUids

      // ASSERTIONS
      assertFalse(pinned.contains("trip4")) // 4th trip is *not* added
      assertEquals(3, pinned.size) // Still max 3 pinned
    }
  }

  @Test
  fun removingATrip_unpinsIt() {
    setContentHelper()
    runBlocking {
      // Sanity check: trip1 starts pinned
      assertTrue(fakeUserRepo.getCurrentUser().pinnedTripsUids.contains("trip1"))

      // Click trip1 to unpin it
      composeTestRule
          .onNodeWithTag(TripElementTestTags.getTestTagForTrip(fakeTripsRepo.getTrip("trip1")))
          .performClick()

      // Save selection
      composeTestRule
          .onNodeWithTag(SelectPinnedTripsScreenTestTags.SAVE_SELECTED_TRIPS_FAB)
          .performClick()

      // Now it should be removed
      assertFalse(fakeUserRepo.getCurrentUser().pinnedTripsUids.contains("trip1"))
    }
  }
}
