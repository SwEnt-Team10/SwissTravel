package com.github.swent.swisstravel.ui.mytrips

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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

/**
 * Contains constants for test tags used within [MyTripsScreen].
 *
 * These tags enable UI tests to locate and assert specific UI elements, such as buttons, dialogs,
 * and lists.
 */
object MyTripsScreenTestTags {
  const val PAST_TRIPS_BUTTON = "pastTrips"
  const val CURRENT_TRIP_TITLE = "currentTripTitle"
  const val CREATE_TRIP_BUTTON = "createTrip"
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
  const val SORT_DROPDOWN_MENU = "sortDropdownMenu"

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
    onCreateTrip: () -> Unit = {},
    navigationActions: NavigationActions? = null,
) {
  val context = LocalContext.current
  val uiState by myTripsViewModel.uiState.collectAsState()
  val currentTrip = uiState.currentTrip
  val upcomingTrips = uiState.upcomingTrips
  val selectedTripCount = uiState.selectedTrips.size

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
    AlertDialog(
        onDismissRequest = { showDeleteConfirmation = false },
        title = {
          Text(
              pluralStringResource(
                  R.plurals.confirm_delete_title, selectedTripCount, selectedTripCount))
        },
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
                Text(
                    text = stringResource(R.string.n_selected, selectedTripCount),
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
                // Past Trips Icon Button
                IconButton(
                    onClick = { onPastTrips() },
                    modifier = Modifier.testTag(MyTripsScreenTestTags.PAST_TRIPS_BUTTON)) {
                      Icon(
                          imageVector = Icons.Default.History,
                          contentDescription = stringResource(R.string.go_past_trips))
                    }
              }
            })
      },
      floatingActionButton = {
        FloatingActionButton(
            onClick = { onCreateTrip() },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.testTag(MyTripsScreenTestTags.CREATE_TRIP_BUTTON)) {
              Icon(imageVector = Icons.Default.Add, contentDescription = "Add")
            }
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

          // Button to enter a map displaying the next route
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

          // Upcoming Trip section
          Row(
              modifier = Modifier.fillMaxWidth().padding(top = 26.dp, bottom = 10.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.upcoming_trip),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.testTag(MyTripsScreenTestTags.UPCOMING_TRIPS_TITLE))

                var expanded by remember { mutableStateOf(false) }

                Box {
                  IconButton(
                      onClick = { expanded = !expanded },
                      modifier = Modifier.testTag(MyTripsScreenTestTags.SORT_DROPDOWN_MENU)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Sort,
                            contentDescription = stringResource(R.string.sort))
                      }

                  DropdownMenu(
                      expanded = expanded,
                      onDismissRequest = { expanded = false },
                      modifier = Modifier.background(MaterialTheme.colorScheme.onPrimary)) {
                        val sortOptions =
                            listOf(
                                TripSortType.START_DATE_ASC to R.string.start_date_asc,
                                TripSortType.START_DATE_DESC to R.string.start_date_desc,
                                TripSortType.END_DATE_ASC to R.string.end_date_asc,
                                TripSortType.END_DATE_DESC to R.string.end_date_desc,
                                TripSortType.NAME_ASC to R.string.name_asc,
                                TripSortType.NAME_DESC to R.string.name_desc)

                        sortOptions.forEach { (type, resId) ->
                          DropdownMenuItem(
                              text = { Text(stringResource(resId)) },
                              onClick = {
                                myTripsViewModel.updateSortType(type)
                                expanded = false
                              })
                        }
                      }
                }
              }
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
