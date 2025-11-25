package com.github.swent.swisstravel.ui.friends

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PersonAddAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.model.user.User

object FriendsScreenTestTags {
  const val FRIENDS_LIST = "friendsList"
  const val ADD_FRIEND_BUTTON = "addFriend"
  const val SEARCH_FRIENDS_BUTTON = "searchFriends"
  const val PENDING_SECTION_CARD = "pendingSectionCard"
  const val PENDING_SECTION_HEADER = "pendingSectionHeader"
  const val PENDING_SECTION_ARROW = "pendingSectionArrow"
}

/**
 * A screen for displaying a list of friends.
 *
 * @param friendsViewModel The view model for the friends screen.
 * @param onSelectFriend The function to call when a friend is selected.
 * @param onAddFriend The function to call when the add friend button is pressed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsListScreen(
    friendsViewModel: FriendsViewModel = viewModel(),
    onSelectFriend: (String) -> Unit = {},
    onAddFriend: () -> Unit = {},
) {
  val context = LocalContext.current
  val uiState by friendsViewModel.uiState.collectAsState()

  var isSearching by rememberSaveable { mutableStateOf(false) }
  var searchQuery by rememberSaveable { mutableStateOf("") }

  // Refresh when entering screen
  LaunchedEffect(Unit) { friendsViewModel.refreshFriends() }

  // Show errors as toasts (same behavior as MyTrips)
  LaunchedEffect(uiState.errorMsg) {
    uiState.errorMsg?.let {
      Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
      friendsViewModel.clearErrorMsg()
    }
  }

  Scaffold(
      topBar = {
        FriendsTopAppBar(
            isSearching = isSearching,
            searchQuery = searchQuery,
            onSearchQueryChange = { newQuery ->
              searchQuery = newQuery
              friendsViewModel.updateSearchQuery(newQuery)
            },
            onStartSearch = { isSearching = true },
            onCloseSearch = {
              isSearching = false
              searchQuery = ""
              friendsViewModel.updateSearchQuery("")
            })
      },
      floatingActionButton = {
        FloatingActionButton(
            onClick = onAddFriend,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.testTag(FriendsScreenTestTags.ADD_FRIEND_BUTTON)) {
              Icon(
                  Icons.Default.PersonAddAlt,
                  contentDescription = stringResource(R.string.add_friend))
            }
      }) { padding ->
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .padding(padding)
                    .padding(
                        start = dimensionResource(R.dimen.my_trip_padding_start_end),
                        end = dimensionResource(R.dimen.my_trip_padding_start_end),
                        bottom = dimensionResource(R.dimen.my_trip_padding_top_bottom))) {
              PendingFriendRequestsSection(
                  pendingFriends = uiState.pendingFriends,
                  onAccept = { friendUid -> friendsViewModel.acceptFriendRequest(friendUid) },
                  onDecline = { friendUid -> friendsViewModel.removeFriend(friendUid) })

              Spacer(modifier = Modifier.height(20.dp))

              FriendsListSection(
                  friends = friendsViewModel.friendsToDisplay,
                  onSelectFriend = onSelectFriend,
              )
            }
      }
}

/**
 * The top app bar for the friends screen.
 *
 * @param isSearching Whether the search bar is visible.
 * @param searchQuery The current search query.
 * @param onSearchQueryChange The function to call when the search query changes.
 * @param onStartSearch The function to call when the search button is pressed.
 * @param onCloseSearch The function to call when the close button is pressed
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FriendsTopAppBar(
    isSearching: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onStartSearch: () -> Unit,
    onCloseSearch: () -> Unit,
) {
  TopAppBar(
      title = {
        if (isSearching) {
          FriendsSearchBar(
              query = searchQuery,
              onQueryChange = onSearchQueryChange,
              placeholder = stringResource(R.string.search_friends),
              onClose = onCloseSearch)
        } else {
          Text(
              text = stringResource(R.string.friends_list),
              style = MaterialTheme.typography.titleLarge,
              color = MaterialTheme.colorScheme.onBackground,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis)
        }
      },
      actions = {
        if (isSearching) {
          IconButton(onClick = onCloseSearch) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.close_search))
          }
        } else {
          IconButton(
              onClick = onStartSearch,
              modifier = Modifier.testTag(FriendsScreenTestTags.SEARCH_FRIENDS_BUTTON)) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = stringResource(R.string.search_friends))
              }
        }
      },
      colors =
          TopAppBarDefaults.topAppBarColors(
              titleContentColor = MaterialTheme.colorScheme.onBackground,
              navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
              actionIconContentColor = MaterialTheme.colorScheme.onBackground))
}

@Composable
private fun FriendsListSection(
    friends: List<User>,
    onSelectFriend: (String) -> Unit,
) {
  if (friends.isEmpty()) {
    Text(
        text = stringResource(R.string.no_friends),
        style = MaterialTheme.typography.bodyMedium,
    )
  } else {
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag(FriendsScreenTestTags.FRIENDS_LIST),
        verticalArrangement =
            Arrangement.spacedBy(dimensionResource(R.dimen.trip_list_vertical_arrangement))) {
          items(items = friends, key = { it.uid }) { friend ->
            FriendElement(userToDisplay = friend, onClick = { onSelectFriend(friend.uid) })
          }
        }
  }
}

/**
 * The search bar for the friends screen.
 *
 * @param query The current search query.
 * @param onQueryChange The function to call when the search query changes.
 * @param placeholder The text to display in the search bar.
 * @param onClose The function to call when the close button is pressed.
 * @param trailingIcon Whether to show the close button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsSearchBar(
    modifier: Modifier = Modifier,
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    onClose: () -> Unit,
    trailingIcon: Boolean = false,
) {
  TextField(
      value = query,
      onValueChange = onQueryChange,
      placeholder = { Text(placeholder) },
      singleLine = true,
      textStyle = MaterialTheme.typography.titleLarge,
      modifier = modifier.fillMaxWidth(),
      trailingIcon = {
        if (trailingIcon) {
          IconButton(onClick = onClose) {
            Icon(Icons.Default.Close, contentDescription = "Close search")
          }
        }
      },
      colors =
          TextFieldDefaults.colors(
              focusedContainerColor = Color.Transparent,
              unfocusedContainerColor = Color.Transparent,
              disabledContainerColor = Color.Transparent,
              errorContainerColor = Color.Transparent,
              focusedIndicatorColor = Color.Transparent,
              unfocusedIndicatorColor = Color.Transparent,
              disabledIndicatorColor = Color.Transparent,
              errorIndicatorColor = Color.Transparent,
          ))
}

/**
 * Section for displaying pending friend requests.
 *
 * @param pendingFriends The list of pending friends.
 * @param onAccept The function to call when a friend request is accepted.
 * @param onDecline The function to call when a friend request is declined
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PendingFriendRequestsSection(
    pendingFriends: List<User>,
    onAccept: (String) -> Unit,
    onDecline: (String) -> Unit,
) {
  if (pendingFriends.isEmpty()) return

  val context = LocalContext.current
  var expanded by rememberSaveable { mutableStateOf(false) }

  Card(
      modifier = Modifier.fillMaxWidth().testTag(FriendsScreenTestTags.PENDING_SECTION_CARD),
      shape = RoundedCornerShape(dimensionResource(R.dimen.trip_element_radius)),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onPrimary)) {
        Column(modifier = Modifier.fillMaxWidth()) {

          // Header row (inside the card)
          Row(
              modifier =
                  Modifier.fillMaxWidth()
                      .padding(horizontal = dimensionResource(R.dimen.trip_element_padding))
                      .padding(vertical = dimensionResource(R.dimen.small_spacer))
                      .clickable { expanded = !expanded }
                      .testTag(FriendsScreenTestTags.PENDING_SECTION_HEADER),
              verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.pending_friend_requests, pendingFriends.size),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f))
                Icon(
                    imageVector =
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag(FriendsScreenTestTags.PENDING_SECTION_ARROW))
              }

          if (expanded) {
            Column(
                verticalArrangement =
                    Arrangement.spacedBy(
                        dimensionResource(R.dimen.trip_list_vertical_arrangement))) {
                  pendingFriends.forEach { friend ->
                    FriendElement(
                        userToDisplay = friend,
                        onClick = {},
                        isPendingRequest = true,
                        shouldAccept = true,
                        onAccept = {
                          Toast.makeText(context, "Friend request accepted!", Toast.LENGTH_SHORT)
                              .show()
                          onAccept(friend.uid)
                          expanded = false
                        },
                        onDecline = {
                          Toast.makeText(context, "Friend request declined!", Toast.LENGTH_SHORT)
                              .show()
                          onDecline(friend.uid)
                          expanded = false
                        })
                  }
                }
          }
        }
      }
}
