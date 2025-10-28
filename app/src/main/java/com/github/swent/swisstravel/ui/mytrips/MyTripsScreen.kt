package com.github.swent.swisstravel.ui.mytrips

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.github.swent.swisstravel.ui.navigation.*

/**
 * Contains constants for test tags used within [MyTripsScreen].
 *
 * These tags enable UI tests to locate and assert specific UI elements, such as buttons, dialogs,
 * and lists.
 */
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

  /** Returns a unique test tag for the given [trip] element. */
  fun getTestTagForTrip(trip: Trip): String = "trip${trip.uid}"
}

/**
 * Displays the "My Trips" screen, which shows the user's current and upcoming trips.
 *
 * The screen allows users to:
 * - View their current and upcoming trips.
 * - Enter selection mode (via long press) to select and delete multiple trips.
 * - Navigate to past trips or trip details.
 * - Confirm deletion through a modal dialog.
 *
 * It reacts to state changes from [MyTripsViewModel], handling UI logic such as refreshing data,
 * error messages, and trip selection.
 *
 * @param myTripsViewModel The [MyTripsViewModel] providing state and business logic.
 * @param onSelectTrip Callback invoked when a trip is selected (normal mode).
 * @param onPastTrips Callback invoked when the "Past Trips" button is pressed.
 * @param navigationActions Optional [NavigationActions] for handling bottom navigation and screen
 *   transitions.
 */
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

  // Refresh trips whenever the screen is displayed
  LaunchedEffect(Unit) { myTripsViewModel.refreshUIState() }

  // Display any error messages as Toast notifications
  LaunchedEffect(uiState.errorMsg) {
    uiState.errorMsg?.let { message ->
      Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
      myTripsViewModel.clearErrorMsg()
    }
  }

  // State for delete confirmation dialog visibility
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
                    color = MaterialTheme.colorScheme.onBackground)
              } else {
                Text(
                    text = stringResource(R.string.my_trips),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground)
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
                // Past Trips button
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
                    .padding(start = 16.dp, end = 16.dp, bottom = 4.dp),
        ) {
          // Current Trip section
          Text(
              text = stringResource(R.string.current_trip),
              style = MaterialTheme.typography.headlineLarge,
              color = MaterialTheme.colorScheme.onBackground,
              modifier =
                  Modifier.testTag(MyTripsScreenTestTags.CURRENT_TRIP_TITLE)
                      .padding(bottom = 10.dp))

          Spacer(modifier = Modifier.height(4.dp))
          Box(contentAlignment = Alignment.TopCenter) {
            Button(
                onClick = { navigationActions?.navigateTo(Screen.SelectedTripMap) },
                modifier = Modifier.testTag(NavigationMapScreenTestTags.ENTER_MAP_BUTTON)) {
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

          // Upcoming Trips section
          Text(
              text = stringResource(R.string.upcoming_trip),
              style = MaterialTheme.typography.headlineLarge,
              color = MaterialTheme.colorScheme.onBackground,
              modifier =
                  Modifier.testTag(MyTripsScreenTestTags.UPCOMING_TRIPS_TITLE)
                      .padding(top = 26.dp, bottom = 10.dp))

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
