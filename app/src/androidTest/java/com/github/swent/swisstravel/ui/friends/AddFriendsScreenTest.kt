package com.github.swent.swisstravel.ui.friends

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.swent.swisstravel.model.user.Friend
import com.github.swent.swisstravel.model.user.FriendStatus
import com.github.swent.swisstravel.model.user.Preference
import com.github.swent.swisstravel.model.user.User
import com.github.swent.swisstravel.model.user.UserRepository
import com.github.swent.swisstravel.model.user.UserStats
import com.github.swent.swisstravel.ui.theme.SwissTravelTheme
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AddFriendScreenTest {

  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  /** Simple fake repo for UI test – no Firebase. */
  private class FakeUserRepository : UserRepository {
    var currentUser =
        User(
            uid = "currentUser",
            name = "Current User",
            biography = "",
            email = "current@example.com",
            profilePicUrl = "",
            preferences = emptyList(),
            friends = emptyList(),
            stats = UserStats(),
            emptyList(),
            emptyList())

    var searchResults: List<User> = emptyList()
    // The pool of all users available to "search" or "get by uid"
    val allUsers = mutableMapOf<String, User>()

    // Explicit search results override for testing convenience
    var explicitSearchResults: List<User>? = null

    val sendFriendRequestCalls = mutableListOf<Pair<String, String>>()

    override suspend fun getCurrentUser(): User = currentUser

    override suspend fun getUserByUid(uid: String): User? =
        if (uid == currentUser.uid) currentUser else allUsers[uid]

    override suspend fun getUserByNameOrEmail(query: String): List<User> {
      return explicitSearchResults
          ?: allUsers.values.filter { it.name.contains(query, ignoreCase = true) }
    }

    override suspend fun sendFriendRequest(fromUid: String, toUid: String) {
      sendFriendRequestCalls += fromUid to toUid
    }

    // Unused methods
    override suspend fun updateUserPreferences(uid: String, preferences: List<Preference>) {}

    override suspend fun updateUserStats(uid: String, stats: UserStats) {}

    override suspend fun acceptFriendRequest(currentUid: String, fromUid: String) {}

    override suspend fun removeFriend(uid: String, friendUid: String) {}

    override suspend fun updateUser(
        uid: String,
        name: String?,
        biography: String?,
        profilePicUrl: String?,
        preferences: List<Preference>?,
        pinnedTripsUids: List<String>?,
        pinnedPicturesUids: List<String>?
    ) {}
  }

  private fun createUser(uid: String, name: String) =
      User(
          uid = uid,
          name = name,
          biography = "",
          email = "$name@test.com",
          profilePicUrl = "",
          preferences = emptyList(),
          friends = emptyList(),
          stats = UserStats(),
          emptyList(),
          emptyList())

  @Test
  fun typingSearch_showsResults_andClickSendsFriendRequestAndCallsBack() {
    val fakeRepo = FakeUserRepository()

    // User we expect to appear in search results
    val targetUser =
        User(
            uid = "friend123",
            name = "Alice Friend",
            biography = "",
            email = "alice@example.com",
            profilePicUrl = "",
            preferences = emptyList(),
            friends = emptyList(),
            stats = UserStats(),
            emptyList(),
            emptyList())

    fakeRepo.searchResults = listOf(targetUser)

    val friendsViewModel = FriendsViewModel(fakeRepo)

    var backCalled = false

    composeRule.setContent {
      SwissTravelTheme {
        AddFriendScreen(
            friendsViewModel = friendsViewModel,
            onBack = { backCalled = true },
        )
      }
    }

    // Type into the search field
    composeRule
        .onNodeWithTag(AddFriendsScreenTestTags.ADD_FRIEND_SEARCH_FIELD)
        .assertIsDisplayed()
        .performTextInput("alice")

    // Results list should appear
    composeRule.onNodeWithTag(AddFriendsScreenTestTags.ADD_FRIEND_RESULTS_LIST).assertIsDisplayed()

    // The specific result item for our user should be clickable
    composeRule
        .onNodeWithTag(AddFriendsScreenTestTags.addFriendResultItemTag(targetUser.uid))
        .assertIsDisplayed()
        .performClick()

    // Verify repository sendFriendRequest was called with correct UIDs
    assertEquals(
        listOf(fakeRepo.currentUser.uid to targetUser.uid), fakeRepo.sendFriendRequestCalls)

    // Verify onBack was called after clicking
    assertEquals(true, backCalled)
  }

  @Test
  fun clickingNewUser_sendsFriendRequest() {
    val fakeRepo = FakeUserRepository()
    val targetUser = createUser("newTarget", "Alice Target")
    fakeRepo.allUsers[targetUser.uid] = targetUser

    val viewModel = FriendsViewModel(fakeRepo)

    composeRule.setContent { SwissTravelTheme { AddFriendScreen(friendsViewModel = viewModel) } }

    // Search for Alice
    composeRule
        .onNodeWithTag(AddFriendsScreenTestTags.ADD_FRIEND_SEARCH_FIELD)
        .performTextInput("Alice")

    // Click Alice
    composeRule.waitUntil(timeoutMillis = 1000) {
      fakeRepo.sendFriendRequestCalls.isEmpty() // Just wait a bit for compose to settle if needed
      true
    }

    composeRule.onNodeWithText("Alice Target").performClick()

    // Assert request sent
    assertEquals(1, fakeRepo.sendFriendRequestCalls.size)
    assertEquals("newTarget", fakeRepo.sendFriendRequestCalls[0].second)
  }

  @Test
  fun clickingAlreadyFriend_showsToastAndDoesNotSendRequest() {
    val fakeRepo = FakeUserRepository()
    val friendUser = createUser("friend1", "Best Friend")

    // 1. Setup Repo: "friend1" is already ACCEPTED in currentUser's friend list
    fakeRepo.allUsers[friendUser.uid] = friendUser
    fakeRepo.currentUser =
        fakeRepo.currentUser.copy(
            friends = listOf(Friend(uid = friendUser.uid, status = FriendStatus.ACCEPTED)))

    val viewModel = FriendsViewModel(fakeRepo)
    // Force refresh to ensure ViewModel knows about the friend status
    viewModel.refreshFriends()

    // Wait for VM to update state (simple way in UI test)
    composeRule.waitForIdle()

    composeRule.setContent { SwissTravelTheme { AddFriendScreen(friendsViewModel = viewModel) } }

    // 2. Search for the friend
    composeRule
        .onNodeWithTag(AddFriendsScreenTestTags.ADD_FRIEND_SEARCH_FIELD)
        .performTextInput("Best")

    // 3. Click the friend
    composeRule.onNodeWithText("Best Friend").assertIsDisplayed().performClick()

    // 4. Assert NO request was sent (because we are already friends)
    assertTrue(
        fakeRepo.sendFriendRequestCalls.isEmpty(), "Should not send request to existing friend")
  }

  @Test
  fun clickingPendingRequest_showsToastAndDoesNotSendRequest() {
    val fakeRepo = FakeUserRepository()
    val pendingUser = createUser("pending1", "Pending Guy")

    // 1. Setup Repo: "pending1" is PENDING_OUTGOING in currentUser's friend list
    fakeRepo.allUsers[pendingUser.uid] = pendingUser
    fakeRepo.currentUser =
        fakeRepo.currentUser.copy(
            friends = listOf(Friend(uid = pendingUser.uid, status = FriendStatus.PENDING_OUTGOING)))

    val viewModel = FriendsViewModel(fakeRepo)
    viewModel.refreshFriends()
    composeRule.waitForIdle()

    composeRule.setContent { SwissTravelTheme { AddFriendScreen(friendsViewModel = viewModel) } }

    // 2. Search for the pending user
    composeRule
        .onNodeWithTag(AddFriendsScreenTestTags.ADD_FRIEND_SEARCH_FIELD)
        .performTextInput("Pending")

    // 3. Click the pending user
    composeRule.onNodeWithText("Pending Guy").assertIsDisplayed().performClick()

    // 4. Assert NO request was sent (because request is already pending)
    assertTrue(fakeRepo.sendFriendRequestCalls.isEmpty(), "Should not resend pending request")
  }
}
