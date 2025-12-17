package com.github.swent.swisstravel.ui.profile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.swent.swisstravel.model.trip.TripsRepository
import com.github.swent.swisstravel.model.user.User
import com.github.swent.swisstravel.model.user.UserStats
import com.github.swent.swisstravel.ui.composable.TripElementTestTags
import com.github.swent.swisstravel.utils.FakeTripsRepository
import com.github.swent.swisstravel.utils.FakeUserRepository
import com.github.swent.swisstravel.utils.SwissTravelTest
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests for the ProfileScreen composable.
 *
 * Made with the help of AI
 */
@RunWith(AndroidJUnit4::class)
class ProfileScreenTest : SwissTravelTest() {

  override fun createInitializedRepository(): TripsRepository {
    return FakeTripsRepository()
  }

  // Trips for testing pinned trips
  private val tripOne = createTestTrip(uid = "trip1", name = "Trip One", ownerId = "current")
  private val tripTwo = createTestTrip(uid = "trip2", name = "Trip Two", ownerId = "current")

  // Replaced TripRepositoryLocal with FakeTripsRepository
  private val fakeTripRepo = FakeTripsRepository()

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
      // FakeUserRepository initializes with a user holding UID "current".
      val testUser =
          createTestUser(
              uid = "current",
              name = "Satoru Gojo",
              bio = "nah id win",
              mail = "blbl@example.com",
              stats = sampleStats)

      val userRepo = FakeUserRepository()
      userRepo.addUser(testUser)

      val viewModel =
          ProfileViewModel(
              userRepository = userRepo, tripsRepository = fakeTripRepo, requestedUid = "current")

      composeTestRule.setContent { ProfileScreen(profileViewModel = viewModel) }

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
      composeTestRule.onNodeWithTag(ProfileScreenTestTags.PINNED_PICTURES_TITLE).assertIsDisplayed()
      composeTestRule.onNodeWithTag(ProfileScreenTestTags.EMPTY_PINNED_PICTURES).assertExists()
      composeTestRule
          .onNodeWithTag(ProfileScreenTestTags.PINNED_PICTURES_EDIT_BUTTON)
          .assertExists()
    }
  }

  /** Test other user's profile: unfriend button & dialog */
  @Test
  fun otherProfile_displaysUnfriendButton_andCanCancelDialog() {
    runBlocking {
      // Current user is automatically "current" in FakeUserRepository.
      val userRepo = FakeUserRepository()

      // Add the other user we are visiting
      val otherUser =
          createTestUser(uid = "gojolover999", name = "Other User", mail = "", stats = sampleStats)

      userRepo.addUser(otherUser)

      val viewModel =
          ProfileViewModel(
              userRepository = userRepo,
              tripsRepository = fakeTripRepo,
              requestedUid = "gojolover999")

      composeTestRule.setContent { ProfileScreen(profileViewModel = viewModel) }

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
      val userRepo = FakeUserRepository()

      // Add the friend user
      val friendUser =
          createTestUser(uid = "gojolover999", name = "Friend User", mail = "", stats = sampleStats)

      userRepo.addUser(friendUser)

      val viewModel =
          ProfileViewModel(
              userRepository = userRepo,
              tripsRepository = fakeTripRepo,
              requestedUid = "gojolover999")

      composeTestRule.setContent { ProfileScreen(profileViewModel = viewModel) }

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
      val userRepo = FakeUserRepository()

      // Add friend
      val friendUser =
          createTestUser(uid = "friend123", name = "Friend", mail = "", stats = sampleStats)

      userRepo.addUser(friendUser)

      val viewModel =
          ProfileViewModel(
              userRepository = userRepo, tripsRepository = fakeTripRepo, requestedUid = "friend123")

      composeTestRule.setContent { ProfileScreen(profileViewModel = viewModel) }

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
      val userRepo = FakeUserRepository()

      val otherUser =
          createTestUser(uid = "friend123", name = "Other User", mail = "", stats = sampleStats)

      userRepo.addUser(otherUser)

      val viewModel =
          ProfileViewModel(
              userRepository = userRepo, tripsRepository = fakeTripRepo, requestedUid = "friend123")

      composeTestRule.setContent { ProfileScreen(profileViewModel = viewModel) }

      // Edit buttons for pinned trips/images should not exist
      composeTestRule
          .onNodeWithTag(ProfileScreenTestTags.PINNED_TRIPS_EDIT_BUTTON)
          .assertDoesNotExist()
      composeTestRule
          .onNodeWithTag(ProfileScreenTestTags.PINNED_PICTURES_EDIT_BUTTON)
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
      val userRepo = FakeUserRepository()

      // Update the "current" user to have the specific bio needed for this test
      val currentUser =
          User(
              uid = "current",
              name = "Current User",
              biography = "This is my bio",
              email = "current@example.com",
              profilePicUrl = "",
              preferences = emptyList(),
              friends = emptyList(),
              stats = sampleStats,
              pinnedTripsUids = emptyList(),
              pinnedPicturesUids = emptyList(),
              favoriteTripsUids = emptyList())
      userRepo.addUser(currentUser)

      val viewModel =
          ProfileViewModel(
              userRepository = userRepo, tripsRepository = fakeTripRepo, requestedUid = "current")

      composeTestRule.setContent { ProfileScreen(profileViewModel = viewModel) }

      // Settings button should be displayed
      composeTestRule.onNodeWithTag(ProfileScreenTestTags.SETTINGS_BUTTON).assertIsDisplayed()

      // Edit buttons for pinned trips/images should be visible
      composeTestRule.onNodeWithTag(ProfileScreenTestTags.PINNED_TRIPS_EDIT_BUTTON).assertExists()
      composeTestRule
          .onNodeWithTag(ProfileScreenTestTags.PINNED_PICTURES_EDIT_BUTTON)
          .assertExists()

      // Remove friend button should NOT be displayed
      composeTestRule.onNodeWithTag(ProfileScreenTestTags.UNFRIEND_BUTTON).assertDoesNotExist()
    }
  }

  @Test
  fun profileScreen_displaysPinnedTrips() {
    runBlocking {
      val userRepo = FakeUserRepository()

      val currentUser =
          User(
              uid = "current",
              name = "Current User",
              biography = "",
              email = "",
              profilePicUrl = "",
              preferences = emptyList(),
              friends = emptyList(),
              stats = sampleStats,
              pinnedTripsUids = listOf("trip1", "trip2"), // trips to display
              pinnedPicturesUids = emptyList(),
              favoriteTripsUids = emptyList())
      userRepo.addUser(currentUser)

      val viewModel =
          ProfileViewModel(
              userRepository = userRepo, tripsRepository = fakeTripRepo, requestedUid = "current")

      composeTestRule.setContent { ProfileScreen(profileViewModel = viewModel) }

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
