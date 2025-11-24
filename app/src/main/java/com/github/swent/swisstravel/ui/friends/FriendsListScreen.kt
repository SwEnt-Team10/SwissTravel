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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
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
    onSearchFriends: () -> Unit = {},
) {
  val context = LocalContext.current
  val uiState by friendsViewModel.uiState.collectAsState()

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
            onSearchFriends = onSearchFriends,
        )
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
                  friends = uiState.friends,
                  onSelectFriend = onSelectFriend,
              )
            }
      }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FriendsTopAppBar(
    onSearchFriends: () -> Unit,
) {
  TopAppBar(
      title = {
        Text(
            text = stringResource(R.string.friends_list),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground)
      },
      actions = {
        IconButton(
            onClick = onSearchFriends,
            modifier = Modifier.testTag(FriendsScreenTestTags.SEARCH_FRIENDS_BUTTON)) {
              Icon(
                  Icons.Default.Search,
                  contentDescription = stringResource(R.string.search_friends))
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
