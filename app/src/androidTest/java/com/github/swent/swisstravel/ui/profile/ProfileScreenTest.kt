package com.github.swent.swisstravel.ui.profile

import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.swent.swisstravel.model.trip.Trip
import com.github.swent.swisstravel.model.trip.TripProfile
import com.github.swent.swisstravel.model.trip.TripRepositoryLocal
import com.github.swent.swisstravel.model.trip.TripsRepository
import com.github.swent.swisstravel.model.user.Preference
import com.github.swent.swisstravel.model.user.User
import com.github.swent.swisstravel.model.user.UserRepository
import com.github.swent.swisstravel.model.user.UserStats
import com.github.swent.swisstravel.ui.theme.SwissTravelTheme
import com.github.swent.swisstravel.ui.trips.TripElementTestTags
import com.github.swent.swisstravel.utils.SwissTravelTest
import com.google.firebase.Timestamp
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Minimal user repository for tests */
private class TestUserRepository(private val user: User) : UserRepository {
  override suspend fun getCurrentUser() = user

  override suspend fun getUserByUid(uid: String) = user

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
      pinnedImagesUris: List<Uri>?
  ) {}

  override suspend fun updateUserStats(uid: String, stats: UserStats) {}
}

/**
 * Tests for the ProfileScreen composable.
 *
 * Made with the help of AI
 */
@RunWith(AndroidJUnit4::class)
class ProfileScreenTest : SwissTravelTest() {

  override fun createInitializedRepository(): TripsRepository {
    return TripRepositoryLocal()
  }

  private val tripOne =
      Trip(
          uid = "trip1",
          name = "Trip One",
          ownerId = "currentUser",
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
          uriLocation = emptyMap(),
          collaboratorsId = emptyList())
  private val tripTwo =
      Trip(
          uid = "trip2",
          name = "Trip Two",
          ownerId = "currentUser",
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
          uriLocation = emptyMap(),
          collaboratorsId = emptyList())

  private val fakeTripRepo = TripRepositoryLocal()

  init {
    runBlocking {
      fakeTripRepo.addTrip(tripOne)
      fakeTripRepo.addTrip(tripTwo)
    }
  }

  @get:Rule val composeTestRule = createComposeRule()

  private val sampleStats =
      UserStats(
          totalTrips = 5,
          totalTravelMinutes = 300,
          uniqueLocations = 3,
          mostUsedTransportMode = null,
          longestRouteSegmentMin = 120)

  /** Test own profile UI elements */
  @Test
  fun ownProfile_displaysAllKeyUIElements() {
    runBlocking {
      val testUser =
          User(
              uid = "getolover1",
              name = "Satoru Gojo",
              biography = "nah id win",
              email = "blbl@example.com",
              profilePicUrl = "",
              preferences = emptyList(),
              friends = emptyList(),
              stats = sampleStats,
              pinnedTripsUids = emptyList(),
              pinnedImagesUris = emptyList())

      val viewModel =
          ProfileViewModel(
              userRepository = TestUserRepository(testUser),
              tripsRepository = fakeTripRepo,
              requestedUid = "getolover1")

      composeTestRule.setContent {
        SwissTravelTheme { ProfileScreen(profileViewModel = viewModel) }
      }

      // Profile header
      composeTestRule.onNodeWithText("Satoru Gojo").assertIsDisplayed()
      composeTestRule.onNodeWithTag(ProfileScreenTestTags.PROFILE_PIC).assertIsDisplayed()

      // Biography
      composeTestRule.onNodeWithTag(ProfileScreenTestTags.BIOGRAPHY).assertIsDisplayed()

      // Achievements
      composeTestRule.onNodeWithTag(ProfileScreenTestTags.ACHIEVEMENTS).assertIsDisplayed()

      // Settings button (own profile)
      composeTestRule.onNodeWithTag(ProfileScreenTestTags.SETTINGS_BUTTON).assertIsDisplayed()

      // Pinned trips section
      composeTestRule.onNodeWithTag(ProfileScreenTestTags.PINNED_TRIPS_TITLE).assertIsDisplayed()
      composeTestRule.onNodeWithTag(ProfileScreenTestTags.PINNED_TRIPS_EDIT_BUTTON).assertExists()

      // Pinned images section
      composeTestRule.onNodeWithTag(ProfileScreenTestTags.PINNED_IMAGES_TITLE).assertIsDisplayed()
      composeTestRule.onNodeWithTag(ProfileScreenTestTags.PINNED_IMAGES_LIST).assertExists()
      composeTestRule.onNodeWithTag(ProfileScreenTestTags.PINNED_IMAGES_EDIT_BUTTON).assertExists()
    }
  }

  /** Test other user's profile: unfriend button & dialog */
  @Test
  fun otherProfile_displaysUnfriendButton_andCanCancelDialog() {
    runBlocking {
      val currentUser =
          User(
              uid = "getolover1",
              name = "Current User",
              biography = "",
              email = "",
              profilePicUrl = "",
              preferences = emptyList(),
              friends = emptyList(),
              stats = sampleStats,
              pinnedTripsUids = emptyList(),
              pinnedImagesUris = emptyList())

      val otherUser =
          User(
              uid = "gojolover999",
              name = "Suguru Geto",
              biography = "",
              email = "",
              profilePicUrl = "",
              preferences = emptyList(),
              friends = emptyList(),
              stats = sampleStats,
              pinnedTripsUids = emptyList(),
              pinnedImagesUris = emptyList())

      val viewModel =
          ProfileViewModel(
              userRepository = TestUserRepository(currentUser),
              tripsRepository = fakeTripRepo,
              requestedUid = "gojolover999")

      composeTestRule.setContent {
        SwissTravelTheme { ProfileScreen(profileViewModel = viewModel) }
      }

      composeTestRule
          .onNodeWithTag(ProfileScreenTestTags.UNFRIEND_BUTTON)
          .assertIsDisplayed()
          .performClick()

      composeTestRule
          .onNodeWithTag(ProfileScreenTestTags.CONFIRM_UNFRIEND_BUTTON)
          .assertIsDisplayed()
      composeTestRule.onNodeWithTag(ProfileScreenTestTags.CANCEL_UNFRIEND_BUTTON).performClick()
      composeTestRule
          .onNodeWithTag(ProfileScreenTestTags.CONFIRM_UNFRIEND_BUTTON)
          .assertDoesNotExist()
    }
  }

  @Test
  fun otherProfile_canRemoveFriend() {
    runBlocking {
      val currentUser =
          User(
              uid = "getolover1",
              name = "Satoru Gojo",
              biography = "",
              email = "",
              profilePicUrl = "",
              preferences = emptyList(),
              friends = emptyList(),
              stats = sampleStats,
              pinnedTripsUids = emptyList(),
              pinnedImagesUris = emptyList())

      val otherUser =
          User(
              uid = "gojolover999",
              name = "Suguru Geto",
              biography = "",
              email = "",
              profilePicUrl = "",
              preferences = emptyList(),
              friends = emptyList(),
              stats = sampleStats,
              pinnedTripsUids = emptyList(),
              pinnedImagesUris = emptyList())

      val viewModel =
          ProfileViewModel(
              userRepository = TestUserRepository(currentUser),
              tripsRepository = fakeTripRepo,
              requestedUid = "gojolover999")

      composeTestRule.setContent {
        SwissTravelTheme { ProfileScreen(profileViewModel = viewModel) }
      }

      // Open unfriend dialog
      composeTestRule
          .onNodeWithTag(ProfileScreenTestTags.UNFRIEND_BUTTON)
          .assertIsDisplayed()
          .performClick()

      // Confirm removal
      composeTestRule
          .onNodeWithTag(ProfileScreenTestTags.CONFIRM_UNFRIEND_BUTTON)
          .assertIsDisplayed()
          .performClick()

      // The confirm button disappears (dialog dismissed)
      composeTestRule
          .onNodeWithTag(ProfileScreenTestTags.CONFIRM_UNFRIEND_BUTTON)
          .assertDoesNotExist()

      // UI state should not have an error message after removal
      composeTestRule.runOnIdle { assert(viewModel.uiState.value.errorMsg.isNullOrEmpty()) }
    }
  }

  @Test
  fun otherProfile_openUnfriendDialog_thenCancel_doesNotRemoveFriend() {
    runBlocking {
      val currentUser =
          User(
              uid = "currentUser",
              name = "Current User",
              biography = "",
              email = "",
              profilePicUrl = "",
              preferences = emptyList(),
              friends = emptyList(),
              stats = sampleStats,
              pinnedTripsUids = emptyList(),
              pinnedImagesUris = emptyList())

      val otherUser =
          User(
              uid = "friend123",
              name = "Friend User",
              biography = "",
              email = "",
              profilePicUrl = "",
              preferences = emptyList(),
              friends = emptyList(),
              stats = sampleStats,
              pinnedTripsUids = emptyList(),
              pinnedImagesUris = emptyList())

      val viewModel =
          ProfileViewModel(
              userRepository = TestUserRepository(currentUser),
              tripsRepository = fakeTripRepo,
              requestedUid = "friend123")

      composeTestRule.setContent {
        SwissTravelTheme { ProfileScreen(profileViewModel = viewModel) }
      }

      // Open unfriend dialog
      composeTestRule
          .onNodeWithTag(ProfileScreenTestTags.UNFRIEND_BUTTON)
          .assertIsDisplayed()
          .performClick()

      // Cancel removal
      composeTestRule
          .onNodeWithTag(ProfileScreenTestTags.CANCEL_UNFRIEND_BUTTON)
          .assertIsDisplayed()
          .performClick()

      // Confirm button should no longer exist (dialog dismissed)
      composeTestRule
          .onNodeWithTag(ProfileScreenTestTags.CONFIRM_UNFRIEND_BUTTON)
          .assertDoesNotExist()

      // UI state should still be fine, no error messages
      composeTestRule.runOnIdle { assert(viewModel.uiState.value.errorMsg.isNullOrEmpty()) }
    }
  }

  @Test
  fun otherUserProfile_showsRemoveFriendAndNoEditOrSettingsButton() {
    runBlocking {
      val currentUser =
          User(
              uid = "currentUser",
              name = "Current User",
              biography = "",
              email = "",
              profilePicUrl = "",
              preferences = emptyList(),
              friends = emptyList(),
              stats = sampleStats,
              pinnedTripsUids = emptyList(),
              pinnedImagesUris = emptyList())

      val otherUser =
          User(
              uid = "friend123",
              name = "Friend User",
              biography = "",
              email = "",
              profilePicUrl = "",
              preferences = emptyList(),
              friends = emptyList(),
              stats = sampleStats,
              pinnedTripsUids = emptyList(),
              pinnedImagesUris = emptyList())

      val viewModel =
          ProfileViewModel(
              userRepository = TestUserRepository(currentUser),
              tripsRepository = fakeTripRepo,
              requestedUid = "friend123")

      composeTestRule.setContent {
        SwissTravelTheme { ProfileScreen(profileViewModel = viewModel) }
      }

      // Edit buttons for pinned trips/images should not exist
      composeTestRule
          .onNodeWithTag(ProfileScreenTestTags.PINNED_TRIPS_EDIT_BUTTON)
          .assertDoesNotExist()
      composeTestRule
          .onNodeWithTag(ProfileScreenTestTags.PINNED_IMAGES_EDIT_BUTTON)
          .assertDoesNotExist()

      // Settings button should not exist
      composeTestRule.onNodeWithTag(ProfileScreenTestTags.SETTINGS_BUTTON).assertDoesNotExist()

      // Remove friend button should exist
      composeTestRule.onNodeWithTag(ProfileScreenTestTags.UNFRIEND_BUTTON).assertIsDisplayed()
    }
  }

  @Test
  fun ownProfile_showsEditAndSettings_buttonsAndNoRemoveFriend() {
    runBlocking {
      val currentUser =
          User(
              uid = "currentUser",
              name = "Current User",
              biography = "This is my bio",
              email = "current@example.com",
              profilePicUrl = "",
              preferences = emptyList(),
              friends = emptyList(),
              stats = sampleStats,
              pinnedTripsUids = emptyList(),
              pinnedImagesUris = emptyList())

      val viewModel =
          ProfileViewModel(
              userRepository = TestUserRepository(currentUser),
              tripsRepository = fakeTripRepo,
              requestedUid = "currentUser")

      composeTestRule.setContent { ProfileScreen(profileViewModel = viewModel) }

      // Settings button should be displayed
      composeTestRule.onNodeWithTag(ProfileScreenTestTags.SETTINGS_BUTTON).assertIsDisplayed()

      // Edit buttons for pinned trips/images should be visible (even if features not implemented
      // yet)
      composeTestRule.onNodeWithTag(ProfileScreenTestTags.PINNED_TRIPS_EDIT_BUTTON).assertExists()
      composeTestRule.onNodeWithTag(ProfileScreenTestTags.PINNED_IMAGES_EDIT_BUTTON).assertExists()

      // Remove friend button should NOT be displayed
      composeTestRule.onNodeWithTag(ProfileScreenTestTags.UNFRIEND_BUTTON).assertDoesNotExist()
    }
  }

  @Test
  fun profileScreen_displaysPinnedTrips() {
    runBlocking {
      val currentUser =
          User(
              uid = "currentUser",
              name = "Current User",
              biography = "",
              email = "",
              profilePicUrl = "",
              preferences = emptyList(),
              friends = emptyList(),
              stats = sampleStats,
              pinnedTripsUids = listOf("trip1", "trip2"), // trips to display
              pinnedImagesUris = emptyList())

      val viewModel =
          ProfileViewModel(
              userRepository = TestUserRepository(currentUser),
              tripsRepository = fakeTripRepo,
              requestedUid = "currentUser")

      composeTestRule.setContent {
        SwissTravelTheme { ProfileScreen(profileViewModel = viewModel) }
      }

      // Ensure the section title exists
      composeTestRule.onNodeWithTag(ProfileScreenTestTags.PINNED_TRIPS_TITLE).assertIsDisplayed()

      // Each pinned trip must appear
      composeTestRule
          .onNodeWithTag(TripElementTestTags.getTestTagForTrip(tripOne))
          .assertIsDisplayed()

      composeTestRule
          .onNodeWithTag(TripElementTestTags.getTestTagForTrip(tripTwo))
          .assertIsDisplayed()
    }
  }
}
