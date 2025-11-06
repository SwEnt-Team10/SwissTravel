package com.github.swent.swisstravel.ui.mytrips.pasttrips

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.model.trip.Trip
import com.github.swent.swisstravel.ui.composable.DeleteTripsDialog
import com.github.swent.swisstravel.ui.composable.SortMenu
import com.github.swent.swisstravel.ui.composable.TripList
import com.github.swent.swisstravel.ui.mytrips.TripSortType
import com.github.swent.swisstravel.ui.mytrips.TripsViewModel
import com.github.swent.swisstravel.ui.navigation.BottomNavigationMenu
import com.github.swent.swisstravel.ui.navigation.NavigationActions
import com.github.swent.swisstravel.ui.navigation.NavigationTestTags
import com.github.swent.swisstravel.ui.navigation.Tab

/**
 * Contains constants for test tags used within [PastTripsScreen].
 *
 * These tags enable UI tests to locate and assert specific UI elements, such as buttons, dialogs,
 * and lists.
 */
object PastTripsScreenTestTags {
  const val FAVORITE_SELECTED_BUTTON = "favoriteSelected"
  const val DELETE_SELECTED_BUTTON = "deleteSelected"
  const val SELECT_ALL_BUTTON = "selectAll"
  const val CANCEL_SELECTION_BUTTON = "cancelSelection"
  const val MORE_OPTIONS_BUTTON = "moreOptions"

  /** Returns a unique test tag for the given [trip] element. */
  fun getTestTagForTrip(trip: Trip): String = "trip${trip.uid}"
}

/**
 * Displays the "Past Trips" screen, which shows the user's past trips.
 *
 * The screen allows users to:
 * - View their past trips.
 * - Enter selection mode (via long press) to select and delete multiple trips.
 * - Navigate to trip details.
 * - Confirm deletion through a modal dialog.
 *
 * It reacts to state changes from [PastTripsViewModel], handling UI logic such as refreshing data,
 * error messages, and trip selection.
 *
 * @param pastTripsViewModel The [PastTripsViewModel] providing state and business logic.
 * @param onSelectTrip Callback invoked when a trip is selected (normal mode).
 * @param navigationActions Optional [NavigationActions] for handling bottom navigation and screen
 *   transitions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PastTripsScreen(
    pastTripsViewModel: PastTripsViewModel = viewModel(),
    onBack: () -> Unit = {},
    onSelectTrip: (String) -> Unit = {},
    navigationActions: NavigationActions? = null,
) {
  val context = LocalContext.current
  val uiState by pastTripsViewModel.uiState.collectAsState()
  val selectedTripCount = uiState.selectedTrips.size

  // Handle back press while in selection mode
  BackHandler(enabled = uiState.isSelectionMode) { pastTripsViewModel.toggleSelectionMode(false) }

  // Refresh trips when entering the screen
  LaunchedEffect(Unit) { pastTripsViewModel.refreshUIState() }

  // Display any error messages as Toasts
  LaunchedEffect(uiState.errorMsg) {
    uiState.errorMsg?.let {
      Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
      pastTripsViewModel.clearErrorMsg()
    }
  }

  var showDeleteConfirmation by remember { mutableStateOf(false) }

  if (showDeleteConfirmation) {
    DeleteTripsDialog(
        count = selectedTripCount,
        onConfirm = {
          pastTripsViewModel.deleteSelectedTrips()
          showDeleteConfirmation = false
        },
        onCancel = { showDeleteConfirmation = false })
  }

  Scaffold(
      topBar = {
        PastTripsTopAppBar(
            uiState = uiState,
            onBack = onBack,
            selectedTripCount = selectedTripCount,
            onClickDropDownMenu = { pastTripsViewModel.updateSortType(it) },
            onCancelSelection = { pastTripsViewModel.toggleSelectionMode(false) },
            onFavoriteSelected = { pastTripsViewModel.toggleFavoriteForSelectedTrips() },
            onDeleteSelected = { showDeleteConfirmation = true },
            onSelectAll = { pastTripsViewModel.selectAllTrips() })
      },
      bottomBar = {
        BottomNavigationMenu(
            selectedTab = Tab.MyTrips,
            onTabSelected = { tab -> navigationActions?.navigateTo(tab.destination) },
            modifier = Modifier.testTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU))
      },
      content = { padding ->
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .padding(padding)
                    .padding(start = 16.dp, end = 16.dp, bottom = 4.dp)) {
              TripList(
                  trips = uiState.tripsList,
                  onClickTripElement = {
                    it?.let { trip ->
                      if (uiState.isSelectionMode) {
                        pastTripsViewModel.toggleTripSelection(it)
                      } else {
                        onSelectTrip(trip.uid)
                      }
                    }
                  },
                  onLongPress = {
                    it?.let { _ ->
                      pastTripsViewModel.toggleSelectionMode(true)
                      pastTripsViewModel.toggleTripSelection(it)
                    }
                  },
                  isSelected = { trip -> trip in uiState.selectedTrips },
                  isSelectionMode = uiState.isSelectionMode)
            }
      })
}

/**
 * Top bar for the Past Trips screen.
 * - Displays either title or selection mode info.
 * - Provides actions for navigate back, delete, favorite, or select all.
 *
 * @param uiState Current UI state for trip data.
 * @param selectedTripCount Number of selected trips.
 * @param onClickDropDownMenu Callback to open the sort menu.
 * @param onCancelSelection Callback to exit selection mode.
 * @param onFavoriteSelected Callback to toggle favorite status of selected trips.
 * @param onDeleteSelected Callback to trigger delete confirmation.
 * @param onSelectAll Callback to select all trips.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PastTripsTopAppBar(
    uiState: TripsViewModel.TripsUIState,
    onBack: () -> Unit,
    selectedTripCount: Int,
    onClickDropDownMenu: (TripSortType) -> Unit = {},
    onCancelSelection: () -> Unit,
    onFavoriteSelected: () -> Unit,
    onDeleteSelected: () -> Unit,
    onSelectAll: () -> Unit,
) {
  TopAppBar(
      title = {
        val title =
            if (uiState.isSelectionMode)
                pluralStringResource(R.plurals.n_selected, selectedTripCount, selectedTripCount)
            else stringResource(R.string.past_trips)
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground)
      },
      navigationIcon = {
        if (uiState.isSelectionMode) {
          IconButton(
              onClick = onCancelSelection,
              modifier = Modifier.testTag(PastTripsScreenTestTags.CANCEL_SELECTION_BUTTON)) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(R.string.cancel_selection))
              }
        } else {
          IconButton(
              onClick = onBack, modifier = Modifier.testTag(NavigationTestTags.TOP_BAR_BUTTON)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = MaterialTheme.colorScheme.onBackground)
              }
        }
      },
      actions = {
        if (uiState.isSelectionMode) {
          var expanded by remember { mutableStateOf(false) }
          IconButton(
              onClick = onFavoriteSelected,
              modifier = Modifier.testTag(PastTripsScreenTestTags.FAVORITE_SELECTED_BUTTON)) {
                Icon(
                    Icons.Default.StarOutline,
                    contentDescription = stringResource(R.string.favorite_selected))
              }
          IconButton(
              onClick = onDeleteSelected,
              modifier = Modifier.testTag(PastTripsScreenTestTags.DELETE_SELECTED_BUTTON)) {
                Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = stringResource(R.string.delete_selected))
              }
          IconButton(
              onClick = { expanded = true },
              modifier = Modifier.testTag(PastTripsScreenTestTags.MORE_OPTIONS_BUTTON)) {
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
                    modifier = Modifier.testTag(PastTripsScreenTestTags.SELECT_ALL_BUTTON))
              }
        } else {
          SortMenu(onClickDropDownMenu = onClickDropDownMenu)
        }
      },
      colors =
          TopAppBarDefaults.topAppBarColors(
              titleContentColor = MaterialTheme.colorScheme.onBackground,
              navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
              actionIconContentColor = MaterialTheme.colorScheme.onBackground))
}
