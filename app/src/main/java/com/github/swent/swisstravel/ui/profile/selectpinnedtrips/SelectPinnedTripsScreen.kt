package com.github.swent.swisstravel.ui.profile.selectpinnedtrips

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.ui.composable.SortMenu
import com.github.swent.swisstravel.ui.composable.TripList
import com.github.swent.swisstravel.ui.composable.TripListEvents
import com.github.swent.swisstravel.ui.composable.TripListState
import com.github.swent.swisstravel.ui.navigation.NavigationTestTags
import com.github.swent.swisstravel.ui.trips.TripSortType
import com.github.swent.swisstravel.ui.trips.TripsViewModel

/**
 * Contains constants for test tags used within [SelectPinnedTripsScreen].
 *
 * These tags enable UI tests to locate and assert specific UI elements, such as buttons, dialogs,
 * and lists.
 */
object SelectPinnedTripsScreenTestTags {
  const val TOP_APP_BAR = "topAppBar"
  const val SAVE_SELECTED_TRIPS_FAB = "saveSelectedTripsFab"
}

/**
 * Displays the "Select Pinned Trips" screen, which allows the user to select the trips they want to
 * pin to their profile.
 *
 * The screen allows users to:
 * - View all their trips.
 * - Select up to 3 trips to pin to their profile.
 *
 * It reacts to state changes from [SelectPinnedTripsViewModel], handling UI logic such as
 * refreshing data, error messages, and trip selection.
 *
 * @param selectPinnedTripsViewModel The [SelectPinnedTripsViewModel] providing state and business
 *   logic.
 * @param onBack Callback invoked when the close button is pressed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectPinnedTripsScreen(
    selectPinnedTripsViewModel: SelectPinnedTripsViewModel = viewModel(),
    onBack: () -> Unit = {},
) {
  val context = LocalContext.current
  val uiState by selectPinnedTripsViewModel.uiState.collectAsState()

  // Refresh trips when entering the screen
  LaunchedEffect(Unit) { selectPinnedTripsViewModel.refreshUIState() }

  // Display any error messages as Toasts
  LaunchedEffect(uiState.errorMsg) {
    uiState.errorMsg?.let {
      Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
      selectPinnedTripsViewModel.clearErrorMsg()
    }
  }

  // If save is successful, navigate back to the previous screen
  val saveSuccess by selectPinnedTripsViewModel.saveSuccess
  LaunchedEffect(saveSuccess) {
    if (saveSuccess == true) {
      onBack()
      selectPinnedTripsViewModel.resetSaveSuccess()
    }
  }

  Scaffold(
      topBar = {
        SelectedPinnedTripsTopAppBar(
            uiState = uiState,
            onBack = onBack,
            onClickDropDownMenu = { selectPinnedTripsViewModel.updateSortType(it) })
      },
      floatingActionButton = {
        FloatingActionButton(
            onClick = {
              selectPinnedTripsViewModel.onSaveSelectedTrips()
              onBack()
            },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.testTag(SelectPinnedTripsScreenTestTags.SAVE_SELECTED_TRIPS_FAB)) {
              Icon(
                  Icons.Default.Check,
                  contentDescription = stringResource(R.string.save_selected_trips))
            }
      },
      content = { padding ->
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .padding(padding)
                    .padding(
                        start = dimensionResource(R.dimen.past_trips_padding_start_end),
                        end = dimensionResource(R.dimen.past_trips_padding_start_end),
                        bottom = dimensionResource(R.dimen.past_trips_padding_top_bottom))) {
              TripList(
                  trips = uiState.tripsList,
                  onClickTripElement = {
                    it?.let { selectPinnedTripsViewModel.toggleTripSelection(it) }
                  },
                  isSelected = { trip -> trip in uiState.selectedTrips },
                  isSelectionMode = uiState.isSelectionMode,
                  emptyListString = stringResource(R.string.no_trips))
            }
      })
}

/**
 * Top bar for the Select Pinned Trips screen.
 * - Displays title info.
 * - Provides actions for navigate back or sort.
 *
 * @param uiState Current UI state for trip data.
 * @param onBack Callback invoked when the back button is pressed.
 * @param onClickDropDownMenu Callback to open the sort menu.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectedPinnedTripsTopAppBar(
    uiState: TripsViewModel.TripsUIState,
    onBack: () -> Unit,
    onClickDropDownMenu: (TripSortType) -> Unit = {},
) {
  TopAppBar(
      title = {
        Text(
            stringResource(R.string.select_pinned_trips),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground)
      },
      navigationIcon = {
        IconButton(
            onClick = onBack, modifier = Modifier.testTag(NavigationTestTags.TOP_BAR_BUTTON)) {
              Icon(
                  Icons.Default.Close,
                  contentDescription = stringResource(R.string.cancel_selection))
            }
      },
      actions = {
        SortMenu(onClickDropDownMenu = onClickDropDownMenu, selectedSortType = uiState.sortType)
      },
      colors =
          TopAppBarDefaults.topAppBarColors(
              titleContentColor = MaterialTheme.colorScheme.onBackground,
              navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
              actionIconContentColor = MaterialTheme.colorScheme.onBackground),
      modifier = Modifier.testTag(SelectPinnedTripsScreenTestTags.TOP_APP_BAR))
}
