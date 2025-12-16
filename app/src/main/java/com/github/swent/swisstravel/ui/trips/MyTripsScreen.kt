package com.github.swent.swisstravel.ui.trips

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.model.trip.Trip
import com.github.swent.swisstravel.ui.composable.DeleteDialog
import com.github.swent.swisstravel.ui.composable.SortedTripListTestTags
import com.github.swent.swisstravel.ui.composable.TripListEvents
import com.github.swent.swisstravel.ui.composable.TripListState
import com.github.swent.swisstravel.ui.composable.TripListTestTags
import com.github.swent.swisstravel.ui.composable.sortedTripListItems

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
  const val FAVORITE_SELECTED_BUTTON = "favoriteSelected"
  const val DELETE_SELECTED_BUTTON = "deleteSelected"
  const val SELECT_ALL_BUTTON = "selectAll"
  const val CANCEL_SELECTION_BUTTON = "cancelSelection"
  const val MORE_OPTIONS_BUTTON = "moreOptions"
  const val EDIT_CURRENT_TRIP_BUTTON = "editCurrentTrip"

  /** Returns a unique test tag for the given [trip] element. */
  fun getTestTagForTrip(trip: Trip): String = "trip${trip.uid}"
}

private const val NO_UPCOMPING_TRIPS = "You don't have any upcoming trips. Time to create one !"
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
 * @param onCreateTrip Callback invoked when the "Create Trip" button is pressed.
 * @param onEditCurrentTrip Callback invoked when the "Edit Current Trip" button is pressed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyTripsScreen(
    myTripsViewModel: MyTripsViewModel = viewModel(),
    onSelectTrip: (String) -> Unit = {},
    onPastTrips: () -> Unit = {},
    onCreateTrip: () -> Unit = {},
    onEditCurrentTrip: () -> Unit = {}
) {
  val context = LocalContext.current
  val uiState by myTripsViewModel.uiState.collectAsState()
  val selectedTripCount = uiState.selectedTrips.size
  val upcomingTripsTitle = stringResource(R.string.upcoming_trips)

  // Handle back press while in selection mode
  BackHandler(enabled = uiState.isSelectionMode) { myTripsViewModel.toggleSelectionMode(false) }

  val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

  // This piece of code is to make sure that the trips recompose after creating a trip, had issues
  // before
  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, e ->
      if (e == Lifecycle.Event.ON_RESUME) myTripsViewModel.refreshUIState()
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }

  // Refresh trips when entering the screen
  LaunchedEffect(Unit) { myTripsViewModel.refreshUIState() }

  // Display any error messages as Toasts
  LaunchedEffect(uiState.errorMsg) {
    uiState.errorMsg?.let {
      Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
      myTripsViewModel.clearErrorMsg()
    }
  }

  var showDeleteConfirmation by remember { mutableStateOf(false) }

  if (showDeleteConfirmation) {
    DeleteDialog(
        onConfirm = {
          myTripsViewModel.deleteSelectedTrips()
          showDeleteConfirmation = false
        },
        onCancel = { showDeleteConfirmation = false },
        title =
            pluralStringResource(
                R.plurals.confirm_delete_title_trips, selectedTripCount, selectedTripCount))
  }

  Scaffold(
      topBar = {
        MyTripsTopAppBar(
            uiState = uiState,
            selectedTripCount = selectedTripCount,
            onCancelSelection = { myTripsViewModel.toggleSelectionMode(false) },
            onFavoriteSelected = { myTripsViewModel.toggleFavoriteForSelectedTrips() },
            onDeleteSelected = { showDeleteConfirmation = true },
            onSelectAll = { myTripsViewModel.selectAllTrips() },
            onPastTrips = onPastTrips)
      },
      floatingActionButton = {
        if (!uiState.isSelectionMode) {
          FloatingActionButton(
              onClick = onCreateTrip,
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary,
              modifier = Modifier.testTag(MyTripsScreenTestTags.CREATE_TRIP_BUTTON)) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add))
              }
        }
      },
      content = { padding ->
        PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = { myTripsViewModel.refreshUIState() },
            modifier = Modifier.padding(padding)) {
              LazyColumn(
                  modifier =
                      Modifier.fillMaxSize()
                          .testTag(TripListTestTags.TRIP_LIST)
                          .testTag(SortedTripListTestTags.SORTED_TRIP_LIST)
                          .padding(
                              start = dimensionResource(R.dimen.my_trip_padding_start_end),
                              end = dimensionResource(R.dimen.my_trip_padding_start_end),
                              bottom = dimensionResource(R.dimen.my_trip_padding_top_bottom))) {
                    item {
                      Spacer(modifier = Modifier.height(dimensionResource(R.dimen.mid_spacer)))
                      CurrentTripSection(
                          myTripsViewModel = myTripsViewModel,
                          onSelectTrip = onSelectTrip,
                          onToggleSelection = { myTripsViewModel.toggleTripSelection(it) },
                          onEditCurrentTrip = onEditCurrentTrip)
                    }

                    val listState =
                        TripListState(
                            trips = uiState.tripsList,
                            isSelectionMode = uiState.isSelectionMode,
                            emptyListString = NO_UPCOMPING_TRIPS,
                            isSelected = { trip -> trip in uiState.selectedTrips },
                            collaboratorsLookup = { uid ->
                              uiState.collaboratorsByTripId[uid] ?: emptyList()
                            })
                    val listEvent =
                        TripListEvents(
                            onClickTripElement = {
                              it?.let { trip ->
                                if (uiState.isSelectionMode)
                                    myTripsViewModel.toggleTripSelection(trip)
                                else onSelectTrip(trip.uid)
                              }
                            },
                            onLongPress = {
                              it?.let { trip ->
                                myTripsViewModel.toggleSelectionMode(true)
                                myTripsViewModel.toggleTripSelection(trip)
                              }
                            },
                        )
                    item {
                      Spacer(modifier = Modifier.height(dimensionResource(R.dimen.large_spacer)))
                    }
                    sortedTripListItems(
                        title = upcomingTripsTitle,
                        listState = listState,
                        listEvents = listEvent,
                        onClickDropDownMenu = { myTripsViewModel.updateSortType(it) },
                        selectedSortType = uiState.sortType,
                    )
                  }
            }
      })
}

/**
 * Top bar for the My Trips screen.
 * - Displays either title or selection mode info.
 * - Provides actions for favorite, delete, select all, or navigate to past trips.
 *
 * @param uiState Current UI state for trip data.
 * @param selectedTripCount Number of selected trips.
 * @param onCancelSelection Callback to exit selection mode.
 * @param onFavoriteSelected Callback to toggle favorite status of selected trips.
 * @param onDeleteSelected Callback to trigger delete confirmation.
 * @param onSelectAll Callback to select all trips.
 * @param onPastTrips Callback to navigate to past trips.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MyTripsTopAppBar(
    uiState: TripsViewModel.TripsUIState,
    selectedTripCount: Int,
    onCancelSelection: () -> Unit,
    onFavoriteSelected: () -> Unit,
    onDeleteSelected: () -> Unit,
    onSelectAll: () -> Unit,
    onPastTrips: () -> Unit,
) {
  val allSelectedFavorites =
      uiState.selectedTrips.isNotEmpty() &&
          uiState.selectedTrips.all { it.uid in uiState.favoriteTripsUids }
  TopAppBar(
      title = {
        val title =
            if (uiState.isSelectionMode)
                pluralStringResource(R.plurals.n_selected, selectedTripCount, selectedTripCount)
            else stringResource(R.string.my_trips)
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground)
      },
      navigationIcon = {
        if (uiState.isSelectionMode) {
          IconButton(
              onClick = onCancelSelection,
              modifier = Modifier.testTag(MyTripsScreenTestTags.CANCEL_SELECTION_BUTTON)) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(R.string.cancel_selection))
              }
        }
      },
      actions = {
        if (uiState.isSelectionMode) {
          var expanded by remember { mutableStateOf(false) }
          IconButton(
              onClick = onFavoriteSelected,
              modifier = Modifier.testTag(MyTripsScreenTestTags.FAVORITE_SELECTED_BUTTON)) {
                Icon(
                    imageVector =
                        if (allSelectedFavorites) Icons.Default.Star else Icons.Default.StarOutline,
                    contentDescription = stringResource(R.string.favorite_selected))
              }
          IconButton(
              onClick = onDeleteSelected,
              modifier = Modifier.testTag(MyTripsScreenTestTags.DELETE_SELECTED_BUTTON)) {
                Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = stringResource(R.string.delete_selected))
              }
          IconButton(
              onClick = { expanded = true },
              modifier = Modifier.testTag(MyTripsScreenTestTags.MORE_OPTIONS_BUTTON)) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = stringResource(R.string.more_options))
              }
          DropdownMenu(
              expanded = expanded,
              onDismissRequest = { expanded = false },
              modifier = Modifier.background(MaterialTheme.colorScheme.onPrimary)) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.select_all)) },
                    onClick = {
                      expanded = false
                      onSelectAll()
                    },
                    modifier = Modifier.testTag(MyTripsScreenTestTags.SELECT_ALL_BUTTON))
              }
        } else {
          IconButton(
              onClick = onPastTrips,
              modifier = Modifier.testTag(MyTripsScreenTestTags.PAST_TRIPS_BUTTON)) {
                Icon(
                    Icons.Default.History,
                    contentDescription = stringResource(R.string.go_past_trips))
              }
        }
      },
      colors =
          TopAppBarDefaults.topAppBarColors(
              titleContentColor = MaterialTheme.colorScheme.onBackground,
              navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
              actionIconContentColor = MaterialTheme.colorScheme.onBackground))
}

/**
 * Displays the "Current Trip" section of the screen.
 * - Shows the user's active trip, or a placeholder if none exists.
 * - Allows entering selection mode by long-pressing.
 * - Provides button to enter the navigation map.
 *
 * @param myTripsViewModel The [MyTripsViewModel] providing state and business logic.
 * @param onSelectTrip Callback when a trip is clicked.
 * @param onToggleSelection Toggles trip selection.
 * @param onEditCurrentTrip Callback when the edit button is clicked.
 */
@Composable
private fun CurrentTripSection(
    myTripsViewModel: MyTripsViewModel,
    onSelectTrip: (String) -> Unit,
    onToggleSelection: (Trip) -> Unit,
    onEditCurrentTrip: () -> Unit = {}
) {
  val uiState by myTripsViewModel.uiState.collectAsState()
  val currentTrip = uiState.currentTrip
  val isSelectionMode = uiState.isSelectionMode
  val selectedTrips = uiState.selectedTrips
  val editButtonShown = uiState.currentTrip != null || uiState.tripsList.isNotEmpty()

  CurrentTripTitle(editButtonShown = editButtonShown, onEditCurrentTrip = onEditCurrentTrip)
  HorizontalDivider(
      modifier =
          Modifier.padding(horizontal = dimensionResource(R.dimen.profile_padding_start_end)),
      color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

  Spacer(modifier = Modifier.height(dimensionResource(R.dimen.small_spacer)))

  currentTrip?.let {
    val tripElementState =
        TripElementState(
            trip = it,
            isSelected = it in selectedTrips,
            isSelectionMode = isSelectionMode,
            isFavorite = it.uid in uiState.favoriteTripsUids,
            collaborators = uiState.collaboratorsByTripId[it.uid] ?: emptyList())
    TripElement(
        tripElementState = tripElementState,
        onClick = { if (isSelectionMode) onToggleSelection(it) else onSelectTrip(it.uid) },
        onLongPress = {
          // Enter selection mode
          myTripsViewModel.toggleSelectionMode(true)
          onToggleSelection(it)
        })
  }
      ?: Text(
          text = stringResource(R.string.no_current_trip),
          modifier = Modifier.testTag(MyTripsScreenTestTags.EMPTY_CURRENT_TRIP_MSG))
}

/**
 * Displays the title for the "Current Trip" section.
 * - Optionally shows an edit button to modify the current trip.
 *
 * @param editButtonShown Whether to display the edit button.
 * @param onEditCurrentTrip Callback when the edit button is clicked.
 */
@Composable
private fun CurrentTripTitle(editButtonShown: Boolean = false, onEditCurrentTrip: () -> Unit = {}) {
  Row(
      modifier =
          Modifier.fillMaxWidth()
              .padding(
                  top = dimensionResource(R.dimen.my_trip_current_top_padding),
                  bottom = dimensionResource(R.dimen.my_trip_current_bottom_padding)),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(R.string.current_trip),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier =
                Modifier.testTag(MyTripsScreenTestTags.CURRENT_TRIP_TITLE)
                    .padding(bottom = dimensionResource(R.dimen.my_trip_current_bottom_padding)))
        if (editButtonShown) {
          IconButton(
              onClick = onEditCurrentTrip,
              modifier = Modifier.testTag(MyTripsScreenTestTags.EDIT_CURRENT_TRIP_BUTTON)) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = stringResource(R.string.select_current_trip))
              }
        }
      }
}
