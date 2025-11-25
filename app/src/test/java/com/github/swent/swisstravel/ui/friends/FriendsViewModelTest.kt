package com.github.swent.swisstravel.ui.friends

import com.github.swent.swisstravel.model.user.Friend
import com.github.swent.swisstravel.model.user.FriendStatus
import com.github.swent.swisstravel.model.user.Preference
import com.github.swent.swisstravel.model.user.User
import com.github.swent.swisstravel.model.user.UserRepository
import com.github.swent.swisstravel.model.user.UserStats
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(private val dispatcher: TestDispatcher = StandardTestDispatcher()) :
    TestWatcher() {
  override fun starting(description: Description) {
    Dispatchers.setMain(dispatcher)
  }

  override fun finished(description: Description) {
    Dispatchers.resetMain()
  }
}

/** A simple fake implementation of [UserRepository] for testing [FriendsViewModel]. */
private class FakeUserRepository : UserRepository {

  var currentUser: User =
      User(
          uid = "me",
          name = "Me",
          biography = "",
          email = "me@example.com",
          profilePicUrl = "",
          preferences = emptyList(),
          friends = emptyList(),
          stats = UserStats())

  val usersByUid: MutableMap<String, User> = mutableMapOf()

  var searchResults: List<User> = emptyList()

  var shouldThrowOnGetCurrentUser = false
  var shouldThrowOnSendFriendRequest = false
  var shouldThrowOnAcceptFriendRequest = false
  var shouldThrowOnRemoveFriend = false

  val sendFriendRequestCalls = mutableListOf<Pair<String, String>>()
  val acceptFriendRequestCalls = mutableListOf<Pair<String, String>>()
  val removeFriendCalls = mutableListOf<Pair<String, String>>()

  override suspend fun getCurrentUser(): User {
    if (shouldThrowOnGetCurrentUser) throw IllegalStateException("getCurrentUser failure")
    return currentUser
  }

  override suspend fun getUserByUid(uid: String): User? = usersByUid[uid]

  override suspend fun getUserByNameOrEmail(query: String): List<User> = searchResults

  override suspend fun updateUserPreferences(uid: String, preferences: List<Preference>) {
    // Not needed for these tests
  }

  override suspend fun updateUserStats(uid: String, stats: UserStats) {
    // Not needed for these tests
  }

  override suspend fun sendFriendRequest(fromUid: String, toUid: String) {
    if (shouldThrowOnSendFriendRequest) throw IllegalStateException("sendFriendRequest failure")
    sendFriendRequestCalls += fromUid to toUid

    // For simplicity, directly add an ACCEPTED friend so that refreshFriends() will show it
    val newFriend = Friend(uid = toUid, status = FriendStatus.ACCEPTED)
    currentUser = currentUser.copy(friends = currentUser.friends + newFriend)
  }

  override suspend fun acceptFriendRequest(currentUid: String, fromUid: String) {
    if (shouldThrowOnAcceptFriendRequest) throw IllegalStateException("acceptFriendRequest failure")
    acceptFriendRequestCalls += currentUid to fromUid

    // Mark existing PENDING_INCOMING as ACCEPTED on current user
    val updatedFriends =
        currentUser.friends.map {
          if (it.uid == fromUid) it.copy(status = FriendStatus.ACCEPTED) else it
        }
    currentUser = currentUser.copy(friends = updatedFriends)
  }

  override suspend fun removeFriend(uid: String, friendUid: String) {
    if (shouldThrowOnRemoveFriend) throw IllegalStateException("removeFriend failure")
    removeFriendCalls += uid to friendUid

    currentUser = currentUser.copy(friends = currentUser.friends.filterNot { it.uid == friendUid })
  }
}

@OptIn(ExperimentalCoroutinesApi::class)
class FriendsViewModelTest {

  @get:Rule val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

  private lateinit var fakeRepo: FakeUserRepository
  private lateinit var viewModel: FriendsViewModel

  @Before
  fun setUp() {
    fakeRepo = FakeUserRepository()

    // Seed some users in the fake repo
    val friendAccepted =
        User(
            uid = "friendAccepted",
            name = "Alice",
            biography = "",
            email = "alice@example.com",
            profilePicUrl = "",
            preferences = emptyList(),
            friends = emptyList(),
            stats = UserStats())

    val friendPending =
        User(
            uid = "friendPending",
            name = "Bob",
            biography = "",
            email = "bob@example.com",
            profilePicUrl = "",
            preferences = emptyList(),
            friends = emptyList(),
            stats = UserStats())

    fakeRepo.usersByUid[friendAccepted.uid] = friendAccepted
    fakeRepo.usersByUid[friendPending.uid] = friendPending

    // Current user has one ACCEPTED and one PENDING_INCOMING friend
    fakeRepo.currentUser =
        fakeRepo.currentUser.copy(
            friends =
                listOf(
                    Friend(uid = "friendAccepted", status = FriendStatus.ACCEPTED),
                    Friend(uid = "friendPending", status = FriendStatus.PENDING_INCOMING)))

    viewModel = FriendsViewModel(fakeRepo)
  }

  @Test
  fun refreshFriends_populatesAcceptedAndPendingAndCurrentUid() = runTest {
    // Act
    viewModel.refreshFriends()

    val state = viewModel.uiState.value

    // Assert
    assertEquals("me", state.currentUserUid)
    assertFalse(state.isLoading)
    assertNull(state.errorMsg)

    // Only ACCEPTED goes into friends
    assertEquals(1, state.friends.size)
    assertEquals("friendAccepted", state.friends.first().uid)

    // Only PENDING_INCOMING goes into pendingFriends
    assertEquals(1, state.pendingFriends.size)
    assertEquals("friendPending", state.pendingFriends.first().uid)
  }

  @Test
  fun refreshFriends_setsErrorOnException() = runTest {
    // Arrange
    fakeRepo.shouldThrowOnGetCurrentUser = true

    // Act
    viewModel.refreshFriends()

    val state = viewModel.uiState.value

    // Assert
    assertFalse(state.isLoading)
    assertTrue(state.errorMsg?.contains("Error fetching friends") == true)
  }

  @Test
  fun friendsToDisplay_filtersBySearchAndExcludesCurrentUser() = runTest {
    // First refresh to fill state.friends
    viewModel.refreshFriends()

    // Sanity: friends list contains only friendAccepted
    val allFriends = viewModel.friendsToDisplay
    assertEquals(1, allFriends.size)
    assertEquals("friendAccepted", allFriends.first().uid)

    // Act: apply a search query that matches "Alice"
    viewModel.updateSearchQuery("ali")
    val filtered = viewModel.friendsToDisplay

    // Assert: still contains the friend
    assertEquals(1, filtered.size)
    assertEquals("friendAccepted", filtered.first().uid)

    // Act: apply a search query that matches nothing
    viewModel.updateSearchQuery("zzz")
    val filteredNone = viewModel.friendsToDisplay
    assertTrue(filteredNone.isEmpty())
  }

  @Test
  fun clearErrorMsg_resetsErrorMessage() = runTest {
    // Arrange
    viewModel.acceptFriendRequest("someone") // will not error yet, but let's force error
    fakeRepo.shouldThrowOnAcceptFriendRequest = true
    viewModel.acceptFriendRequest("other")

    var state = viewModel.uiState.value
    assertTrue(state.errorMsg?.contains("Error accepting friend request") == true)

    // Act
    viewModel.clearErrorMsg()
    state = viewModel.uiState.value

    // Assert
    assertNull(state.errorMsg)
  }

  @Test
  fun searchUsersGlobal_blankQueryClearsResults() = runTest {
    // Arrange
    fakeRepo.searchResults =
        listOf(
            fakeRepo.usersByUid["friendAccepted"]!!,
            fakeRepo.usersByUid["friendPending"]!!,
        )

    // Act: first do a non-blank search
    viewModel.searchUsersGlobal("a")
    var state = viewModel.uiState.value
    assertEquals(2, state.searchResults.size)

    // Act: now blank query must clear results
    viewModel.searchUsersGlobal("")
    state = viewModel.uiState.value

    // Assert
    assertTrue(state.searchResults.isEmpty())
  }

  @Test
  fun searchUsersGlobal_filtersOutCurrentUser() = runTest {
    // Arrange: include current user + another in search results
    val otherUser =
        User(
            uid = "other",
            name = "Other User",
            biography = "",
            email = "other@example.com",
            profilePicUrl = "",
            preferences = emptyList(),
            friends = emptyList(),
            stats = UserStats())

    fakeRepo.usersByUid[otherUser.uid] = otherUser
    fakeRepo.searchResults = listOf(fakeRepo.currentUser, otherUser)

    // Make sure currentUserUid is set by refresh
    viewModel.refreshFriends()

    // Act
    viewModel.searchUsersGlobal("user")

    val state = viewModel.uiState.value

    // Assert: only "other" remains
    assertEquals(1, state.searchResults.size)
    assertEquals("other", state.searchResults.first().uid)
  }

  @Test
  fun sendFriendRequest_callsRepositoryAndRefreshesFriends() = runTest {
    // Arrange: ensure friend target exists
    val target =
        User(
            uid = "newFriend",
            name = "New Friend",
            biography = "",
            email = "new@example.com",
            profilePicUrl = "",
            preferences = emptyList(),
            friends = emptyList(),
            stats = UserStats())
    fakeRepo.usersByUid[target.uid] = target

    // Act
    viewModel.sendFriendRequest("newFriend")
    // refreshFriends is called inside sendFriendRequest on success
    val state = viewModel.uiState.value

    // Assert repository call
    assertEquals(listOf("me" to "newFriend"), fakeRepo.sendFriendRequestCalls)

    // Assert friend list updated (we made Fake repo add ACCEPTED entry)
    assertTrue(state.friends.any { it.uid == "newFriend" })
  }

  @Test
  fun sendFriendRequest_setsErrorOnFailure() = runTest {
    fakeRepo.shouldThrowOnSendFriendRequest = true

    viewModel.sendFriendRequest("someone")
    val state = viewModel.uiState.value

    assertTrue(state.errorMsg?.contains("Error sending friend request") == true)
  }

  @Test
  fun acceptFriendRequest_callsRepositoryAndRefreshesFriends() = runTest {
    // Arrange: ensure there is a pending incoming friend
    val pendingUid = "friendPending"
    fakeRepo.currentUser =
        fakeRepo.currentUser.copy(
            friends = listOf(Friend(uid = pendingUid, status = FriendStatus.PENDING_INCOMING)))

    // Need userByUid entry as well
    val pendingUser =
        User(
            uid = pendingUid,
            name = "Pending Friend",
            biography = "",
            email = "pending@example.com",
            profilePicUrl = "",
            preferences = emptyList(),
            friends = emptyList(),
            stats = UserStats())
    fakeRepo.usersByUid[pendingUid] = pendingUser

    viewModel.refreshFriends()

    // Act
    viewModel.acceptFriendRequest(pendingUid)
    val state = viewModel.uiState.value

    // Assert repository call
    assertEquals(listOf("me" to pendingUid), fakeRepo.acceptFriendRequestCalls)

    // After accept + refresh, the friend should be in "friends" list
    assertTrue(state.friends.any { it.uid == pendingUid })
  }

  @Test
  fun acceptFriendRequest_setsErrorOnFailure() = runTest {
    fakeRepo.shouldThrowOnAcceptFriendRequest = true

    viewModel.acceptFriendRequest("someone")
    val state = viewModel.uiState.value

    assertTrue(state.errorMsg?.contains("Error accepting friend request") == true)
  }

  @Test
  fun removeFriend_callsRepository() = runTest {
    // Arrange: ensure current user has this friend
    val friendToRemove = "friendAccepted"
    fakeRepo.currentUser =
        fakeRepo.currentUser.copy(
            friends = listOf(Friend(uid = friendToRemove, status = FriendStatus.ACCEPTED)))

    viewModel.refreshFriends()

    // Act
    viewModel.removeFriend(friendToRemove)

    // Assert call was made to repository
    assertEquals(listOf("me" to friendToRemove), fakeRepo.removeFriendCalls)
  }

  @Test
  fun removeFriend_setsErrorOnFailure() = runTest {
    fakeRepo.shouldThrowOnRemoveFriend = true

    viewModel.removeFriend("someone")
    val state = viewModel.uiState.value

    assertTrue(state.errorMsg?.contains("Error removing friend") == true)
  }

  @After
  fun tearDown() {
    // Nothing special; Main dispatcher is reset by the rule
  }
}
