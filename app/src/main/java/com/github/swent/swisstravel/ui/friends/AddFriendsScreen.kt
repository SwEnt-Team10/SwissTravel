package com.github.swent.swisstravel.ui.friends

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.model.user.User

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFriendScreen(
    friendsViewModel: FriendsViewModel = viewModel(),
    onBack: () -> Unit = {},
) {
  val uiState by friendsViewModel.uiState.collectAsState()

  var searchQuery by rememberSaveable { mutableStateOf("") }

  // Trigger global search every time the query changes
  LaunchedEffect(searchQuery) { friendsViewModel.searchUsersGlobal(searchQuery) }

  Scaffold(
      topBar = {
        AddFriendTopAppBar(
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            onBack = onBack,
        )
      }) { padding ->
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .padding(padding)
                    .padding(
                        start = dimensionResource(R.dimen.my_trip_padding_start_end),
                        end = dimensionResource(R.dimen.my_trip_padding_start_end),
                        bottom = dimensionResource(R.dimen.my_trip_padding_top_bottom))) {
              AddFriendResultsSection(
                  users = uiState.searchResults,
                  onClickUser = { user ->
                    friendsViewModel.sendFriendRequest(toUid = user.uid)
                    onBack()
                  })
            }
      }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddFriendTopAppBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onBack: () -> Unit,
) {
  TopAppBar(
      title = {
        FriendsSearchBar(
            query = searchQuery,
            onQueryChange = onSearchQueryChange,
            placeholder = stringResource(R.string.search_users),
            onClose = onBack,
            trailingIcon = true)
      },
      navigationIcon = {
        IconButton(onClick = onBack) {
          Icon(
              Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = stringResource(R.string.go_back))
        }
      })
}

@Composable
private fun AddFriendResultsSection(
    users: List<User>,
    onClickUser: (User) -> Unit,
) {
  val context = LocalContext.current
  if (users.isEmpty()) {
    Text(
        text = stringResource(R.string.no_users_found), style = MaterialTheme.typography.bodyMedium)
  } else {
    LazyColumn(
        verticalArrangement =
            Arrangement.spacedBy(dimensionResource(R.dimen.trip_list_vertical_arrangement)),
        modifier = Modifier.fillMaxWidth()) {
          items(users, key = { it.uid }) { user ->
            FriendElement(
                userToDisplay = user,
                onClick = {
                  onClickUser(user)
                  Toast.makeText(context, "Friend request sent to ${user.name}", Toast.LENGTH_SHORT)
                      .show()
                })
          }
        }
  }
}
