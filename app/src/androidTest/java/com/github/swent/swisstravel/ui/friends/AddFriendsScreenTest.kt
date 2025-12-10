package com.github.swent.swisstravel.ui.friends

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.swent.swisstravel.model.user.Preference
import com.github.swent.swisstravel.model.user.User
import com.github.swent.swisstravel.model.user.UserRepository
import com.github.swent.swisstravel.model.user.UserStats
import com.github.swent.swisstravel.ui.theme.SwissTravelTheme
import kotlin.test.assertEquals
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

    val sendFriendRequestCalls = mutableListOf<Pair<String, String>>()

    override suspend fun getCurrentUser(): User = currentUser

    override suspend fun getUserByUid(uid: String): User? =
        (searchResults + currentUser).find { it.uid == uid }

    override suspend fun getUserByNameOrEmail(query: String): List<User> = searchResults

    override suspend fun updateUserPreferences(uid: String, preferences: List<Preference>) {}

    override suspend fun updateUserStats(uid: String, stats: UserStats) {}

    override suspend fun sendFriendRequest(fromUid: String, toUid: String) {
      sendFriendRequestCalls += fromUid to toUid
    }

    override suspend fun acceptFriendRequest(currentUid: String, fromUid: String) {
      // not used here
    }

    override suspend fun removeFriend(uid: String, friendUid: String) {
      // not used here
    }

    override suspend fun updateUser(
        uid: String,
        name: String?,
        biography: String?,
        profilePicUrl: String?,
        preferences: List<Preference>?,
        pinnedTripsUids: List<String>?,
        pinnedPicturesUids: List<String>?
    ) {
      // no op in test
    }
  }

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
}
