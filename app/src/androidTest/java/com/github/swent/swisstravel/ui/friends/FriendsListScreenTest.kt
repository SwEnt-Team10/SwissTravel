package com.github.swent.swisstravel.ui.friends

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.swent.swisstravel.model.user.Friend
import com.github.swent.swisstravel.model.user.FriendStatus
import com.github.swent.swisstravel.model.user.Preference
import com.github.swent.swisstravel.model.user.User
import com.github.swent.swisstravel.model.user.UserRepository
import com.github.swent.swisstravel.model.user.UserStats
import com.github.swent.swisstravel.ui.theme.SwissTravelTheme
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FriendsListScreenTest {

  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  private fun fakeUser(uid: String, name: String) =
      User(
          uid = uid,
          name = name,
          biography = "",
          email = "$name@example.com",
          profilePicUrl = "",
          preferences = emptyList(),
          friends = emptyList(),
          stats = UserStats())

  private fun fakeViewModel(
      friends: List<User>,
      pending: List<User>,
      currentUid: String = "me"
  ): FriendsViewModel {
    val vm = FriendsViewModel(FakeUserRepoForUI(friends, pending))
    vm.apply {
      // inject state directly
      val state =
          FriendsListScreenUIState(
              friends = friends,
              pendingFriends = pending,
              isLoading = false,
              currentUserUid = currentUid)
      @Suppress("UNCHECKED_CAST")
      val field = FriendsViewModel::class.java.getDeclaredField("_uiState")
      field.isAccessible = true
      (field.get(this) as MutableStateFlow<FriendsListScreenUIState>).value = state
    }
    return vm
  }

  @Test
  fun friendsList_isDisplayed() {
    val alice = fakeUser("1", "Alice")
    val bob = fakeUser("2", "Bob")

    composeRule.setContent {
      SwissTravelTheme {
        FriendsListScreen(
            friendsViewModel = fakeViewModel(friends = listOf(alice, bob), pending = emptyList()))
      }
    }

    composeRule.onNodeWithTag(FriendsScreenTestTags.FRIENDS_LIST).assertIsDisplayed()
    composeRule.onNodeWithTag(FriendElementTestTags.getTestTagForFriend(alice)).assertIsDisplayed()
    composeRule.onNodeWithTag(FriendElementTestTags.getTestTagForFriend(bob)).assertIsDisplayed()
  }

  @Test
  fun pendingSection_expandsAndShowsRequests() {
    val pendingUser = fakeUser("9", "PendingUser")

    // Use fake repo that exposes one pending friend
    val repo =
        FakeUserRepoForUI(acceptedFriends = emptyList(), pendingFriends = listOf(pendingUser))
    val viewModel = FriendsViewModel(userRepository = repo)

    composeRule.setContent {
      SwissTravelTheme {
        FriendsListScreen(friendsViewModel = viewModel, onSelectFriend = {}, onAddFriend = {})
      }
    }

    // Section is visible
    composeRule.onNodeWithTag(FriendsScreenTestTags.PENDING_SECTION_CARD).assertIsDisplayed()

    // Expand header
    composeRule.onNodeWithTag(FriendsScreenTestTags.PENDING_SECTION_HEADER).performClick()

    // Pending friend element becomes visible
    composeRule
        .onNodeWithTag(FriendElementTestTags.getTestTagForFriend(pendingUser))
        .assertIsDisplayed()
  }

  @Test
  fun floatingButton_isVisible() {
    composeRule.setContent {
      SwissTravelTheme {
        FriendsListScreen(
            friendsViewModel = fakeViewModel(friends = emptyList(), pending = emptyList()))
      }
    }

    composeRule.onNodeWithTag(FriendsScreenTestTags.ADD_FRIEND_BUTTON).assertIsDisplayed()
  }

  @Test
  fun searchButton_opensSearchBar() {
    composeRule.setContent {
      SwissTravelTheme {
        FriendsListScreen(
            friendsViewModel = fakeViewModel(friends = emptyList(), pending = emptyList()))
      }
    }

    // Open search
    composeRule.onNodeWithTag(FriendsScreenTestTags.SEARCH_FRIENDS_BUTTON).performClick()

    // Search bar is visible
    composeRule.onNodeWithText("Search").assertExists()
  }
}

/**
 * Fake repo used only for FriendsListScreen UI tests.
 *
 * It creates a "current" user with ACCEPTED and PENDING_INCOMING friend relations and exposes the
 * corresponding User objects via getUserByUid.
 */
class FakeUserRepoForUI(
    acceptedFriends: List<User>,
    pendingFriends: List<User>,
    private val currentUid: String = "me"
) : UserRepository {

  private val usersByUid = mutableMapOf<String, User>()
  private val currentUser: User

  init {
    acceptedFriends.forEach { usersByUid[it.uid] = it }
    pendingFriends.forEach { usersByUid[it.uid] = it }

    val friendLinks =
        acceptedFriends.map { Friend(uid = it.uid, status = FriendStatus.ACCEPTED) } +
            pendingFriends.map { Friend(uid = it.uid, status = FriendStatus.PENDING_INCOMING) }

    currentUser =
        User(
            uid = currentUid,
            name = "Current User",
            biography = "",
            email = "current@example.com",
            profilePicUrl = "",
            preferences = emptyList(),
            friends = friendLinks,
            stats = UserStats())
  }

  override suspend fun getCurrentUser(): User = currentUser

  override suspend fun getUserByUid(uid: String): User? = usersByUid[uid]

  override suspend fun getUserByNameOrEmail(query: String): List<User> =
      usersByUid.values.filter {
        it.name.contains(query, ignoreCase = true) || it.email.contains(query, ignoreCase = true)
      }

  override suspend fun updateUserPreferences(uid: String, preferences: List<Preference>) {}

  override suspend fun updateUserStats(uid: String, stats: UserStats) {}

  override suspend fun sendFriendRequest(fromUid: String, toUid: String) {}

  override suspend fun acceptFriendRequest(currentUid: String, fromUid: String) {}

  override suspend fun removeFriend(uid: String, friendUid: String) {}
}
