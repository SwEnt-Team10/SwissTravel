package com.github.swent.swisstravel.ui.friends

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.model.user.User

object FriendsScreenTestTags {
  const val FRIENDS_LIST = "friendsList"
  const val ADD_FRIEND_BUTTON = "addFriend"
  const val SEARCH_FRIENDS_BUTTON = "searchFriends"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsListScreen(
    friendsViewModel: FriendsListScreenViewModel = viewModel(),
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
            onSearchQueryChange = { searchQuery = it },
            onStartSearch = { isSearching = true },
            onCloseSearch = {
              isSearching = false
              searchQuery = ""
            })
      },
      floatingActionButton = {
        FloatingActionButton(
            onClick = onAddFriend,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.testTag(FriendsScreenTestTags.ADD_FRIEND_BUTTON)) {
              Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_friend))
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
              FriendsListSection(
                  friends = friendsViewModel.friendsToDisplay,
                  onSelectFriend = onSelectFriend,
              )
            }
      }
}

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
          TextField(
              value = searchQuery,
              onValueChange = onSearchQueryChange,
              placeholder = { Text(text = stringResource(R.string.search_friends)) },
              singleLine = true,
              textStyle = MaterialTheme.typography.titleLarge,
              modifier = Modifier.fillMaxSize(),
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
