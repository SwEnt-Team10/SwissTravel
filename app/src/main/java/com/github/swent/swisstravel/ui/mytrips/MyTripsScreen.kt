package com.github.swent.swisstravel.ui.mytrips

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.model.trip.Trip
import com.github.swent.swisstravel.ui.map.NavigationMapScreenTestTags
import com.github.swent.swisstravel.ui.navigation.BottomNavigationMenu
import com.github.swent.swisstravel.ui.navigation.NavigationActions
import com.github.swent.swisstravel.ui.navigation.NavigationTestTags
import com.github.swent.swisstravel.ui.navigation.Screen
import com.github.swent.swisstravel.ui.navigation.Tab

object MyTripsScreenTestTags {
  const val PAST_TRIPS_BUTTON = "pastTrips"
  const val CURRENT_TRIP_TITLE = "currentTripTitle"
  const val EMPTY_CURRENT_TRIP_MSG = "emptyCurrentTrip"
  const val UPCOMING_TRIPS_TITLE = "upcomingTripsTitle"
  const val UPCOMING_TRIPS = "upcomingTrips"
  const val EMPTY_UPCOMING_TRIPS_MSG = "emptyUpcomingTrips"
  const val CONFIRM_DELETE_BUTTON = "confirmDelete"
  const val CANCEL_DELETE_BUTTON = "cancelDelete"
  const val DELETE_SELECTED_BUTTON = "deleteSelected"
  const val SELECT_ALL_BUTTON = "selectAll"
  const val CANCEL_SELECTION_BUTTON = "cancelSelection"
  const val MORE_OPTIONS_BUTTON = "moreOptions"

  fun getTestTagForTrip(trip: Trip): String = "trip${trip.uid}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyTripsScreen(
    myTripsViewModel: MyTripsViewModel = viewModel(),
    onSelectTrip: (Trip) -> Unit = {},
    onPastTrips: () -> Unit = {},
    navigationActions: NavigationActions? = null,
) {

  val context = LocalContext.current
  val uiState by myTripsViewModel.uiState.collectAsState()
  val currentTrip = uiState.currentTrip
  val upcomingTrips = uiState.upcomingTrips

  // Fetch trips when the screen is recomposed
  LaunchedEffect(Unit) { myTripsViewModel.refreshUIState() }

  // Show error message if fetching Trips fails
  LaunchedEffect(uiState.errorMsg) {
    uiState.errorMsg?.let { message ->
      Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
      myTripsViewModel.clearErrorMsg()
    }
  }

  var showDeleteConfirmation by remember { mutableStateOf(false) }
  if (showDeleteConfirmation) {
    val count = uiState.selectedTrips.size
    AlertDialog(
        onDismissRequest = { showDeleteConfirmation = false },
        title = { Text(pluralStringResource(R.plurals.confirm_delete_title, count, count)) },
        text = { Text(stringResource(R.string.confirm_delete_message)) },
        confirmButton = {
          TextButton(
              onClick = {
                myTripsViewModel.deleteSelectedTrips()
                showDeleteConfirmation = false
              },
              modifier = Modifier.testTag(MyTripsScreenTestTags.CONFIRM_DELETE_BUTTON)) {
                Text(stringResource(R.string.delete))
              }
        },
        dismissButton = {
          TextButton(
              onClick = { showDeleteConfirmation = false },
              colors =
                  ButtonDefaults.textButtonColors(
                      contentColor = MaterialTheme.colorScheme.onBackground),
              modifier = Modifier.testTag(MyTripsScreenTestTags.CANCEL_DELETE_BUTTON)) {
                Text(stringResource(R.string.cancel))
              }
        },
        containerColor = MaterialTheme.colorScheme.onPrimary,
    )
  }

  Scaffold(
      topBar = {
        TopAppBar(
            title = {
              if (uiState.isSelectionMode) {
                val count = uiState.selectedTrips.size
                Text(
                    text = stringResource(R.string.n_selected, count),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                    /*,modifier = Modifier.testTag(NavigationTestTags.TOP_BAR_TITLE)*/
                    )
              } else {
                Text(
                    text = stringResource(R.string.my_trips),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                    /*,modifier = Modifier.testTag(NavigationTestTags.TOP_BAR_TITLE)*/
                    )
              }
            },
            navigationIcon = {
              if (uiState.isSelectionMode) {
                IconButton(
                    onClick = { myTripsViewModel.toggleSelectionMode(false) },
                    modifier = Modifier.testTag(MyTripsScreenTestTags.CANCEL_SELECTION_BUTTON)) {
                      Icon(
                          Icons.Default.Close,
                          contentDescription = stringResource(R.string.cancel_selection))
                    }
              }
            },
            actions = {
              if (uiState.isSelectionMode) {
                var selectExpanded by remember { mutableStateOf(false) }
                IconButton(
                    onClick = { showDeleteConfirmation = true },
                    modifier = Modifier.testTag(MyTripsScreenTestTags.DELETE_SELECTED_BUTTON)) {
                      Icon(
                          Icons.Default.DeleteOutline,
                          contentDescription = stringResource(R.string.delete_selected))
                    }
                IconButton(
                    onClick = { selectExpanded = true },
                    modifier = Modifier.testTag(MyTripsScreenTestTags.MORE_OPTIONS_BUTTON)) {
                      Icon(
                          Icons.Default.MoreVert,
                          contentDescription = stringResource(R.string.more_options))
                    }
                DropdownMenu(
                    expanded = selectExpanded,
                    onDismissRequest = { selectExpanded = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.onPrimary)) {
                      DropdownMenuItem(
                          text = { Text(stringResource(R.string.select_all)) },
                          onClick = {
                            selectExpanded = false
                            myTripsViewModel.selectAllTrips()
                          },
                          modifier = Modifier.testTag(MyTripsScreenTestTags.SELECT_ALL_BUTTON))
                    }
              } else {
                // Past Trips Icon Button
                IconButton(
                    onClick = { onPastTrips() },
                    modifier = Modifier.testTag(MyTripsScreenTestTags.PAST_TRIPS_BUTTON)) {
                      Icon(
                          imageVector = Icons.Outlined.Archive,
                          contentDescription = stringResource(R.string.go_past_trips))
                    }
              }
            })
      },
      bottomBar = {
        BottomNavigationMenu(
            selectedTab = Tab.MyTrips,
            onTabSelected = { tab -> navigationActions?.navigateTo(tab.destination) },
            modifier = Modifier.testTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU))
      },
      content = { pd ->
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .padding(pd)
                    .padding(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 4.dp),
        ) {
          // Current Trip section
          Text(
              text = stringResource(R.string.current_trip),
              style = MaterialTheme.typography.headlineLarge,
              color = MaterialTheme.colorScheme.onBackground,
              modifier =
                  Modifier.testTag(MyTripsScreenTestTags.CURRENT_TRIP_TITLE)
                      .padding(top = 0.dp, bottom = 10.dp))

          Spacer(modifier = Modifier.height(4.dp))
          Box(contentAlignment = Alignment.TopCenter) {
            Button(
                onClick = { navigationActions?.navigateTo(Screen.SelectedTripMap) },
                modifier = Modifier.testTag(NavigationMapScreenTestTags.ENTER_MAP_BUTTON)) {
                  // TODO : modify this to an "extend" icon when the map preview is implemented
                  Text(stringResource(R.string.enter_map))
                }
          }
          Spacer(modifier = Modifier.height(4.dp))

          if (currentTrip != null) {
            TripElement(
                trip = currentTrip,
                onClick = {
                  if (uiState.isSelectionMode) {
                    myTripsViewModel.toggleTripSelection(currentTrip)
                  } else {
                    onSelectTrip(currentTrip)
                  }
                },
                onLongPress = {
                  myTripsViewModel.toggleSelectionMode(true)
                  myTripsViewModel.toggleTripSelection(currentTrip)
                },
                isSelected = currentTrip in uiState.selectedTrips,
                isSelectionMode = uiState.isSelectionMode)
            Text(
                text = stringResource(R.string.warning_multiple_current_trip),
                style = MaterialTheme.typography.labelSmall)
          } else {
            Text(
                text = stringResource(R.string.no_current_trip),
                modifier = Modifier.testTag(MyTripsScreenTestTags.EMPTY_CURRENT_TRIP_MSG))
          }

          // Upcoming Trip section
          Text(
              text = stringResource(R.string.upcoming_trip),
              style = MaterialTheme.typography.headlineLarge,
              color = MaterialTheme.colorScheme.onBackground,
              modifier =
                  Modifier.testTag(MyTripsScreenTestTags.UPCOMING_TRIPS_TITLE)
                      .padding(top = 26.dp, bottom = 10.dp))

          Spacer(modifier = Modifier.height(4.dp))

          if (upcomingTrips.isNotEmpty()) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth().testTag(MyTripsScreenTestTags.UPCOMING_TRIPS)) {
                  items(upcomingTrips.size) { index ->
                    val trip = upcomingTrips[index]
                    TripElement(
                        trip = trip,
                        onClick = {
                          if (uiState.isSelectionMode) {
                            myTripsViewModel.toggleTripSelection(trip)
                          } else {
                            onSelectTrip(trip)
                          }
                        },
                        onLongPress = {
                          myTripsViewModel.toggleSelectionMode(true)
                          myTripsViewModel.toggleTripSelection(trip)
                        },
                        isSelected = trip in uiState.selectedTrips,
                        isSelectionMode = uiState.isSelectionMode)
                  }
                }
          } else {
            Text(
                text = stringResource(R.string.no_upcoming_trip),
                modifier = Modifier.testTag(MyTripsScreenTestTags.EMPTY_UPCOMING_TRIPS_MSG))
          }
        }
      })
}
