package com.github.swent.swisstravel.ui.trips

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.model.trip.Trip
import com.github.swent.swisstravel.ui.composable.SortMenu
import com.github.swent.swisstravel.ui.composable.TripInteraction
import com.github.swent.swisstravel.ui.composable.TripList
import com.github.swent.swisstravel.ui.navigation.NavigationActions
import com.github.swent.swisstravel.ui.navigation.Screen

/** Object containing test tags for the [SetCurrentTripScreen] composable. */
object SetCurrentTripScreenTestTags {
  const val TOP_BAR = "SetCurrentTripScreenTopBar"
  const val TOP_BAR_TITLE = "SetCurrentTripScreenTopBarTitle"
  const val TOP_BAR_CLOSE_BUTTON = "SetCurrentTripScreenTopBarCloseButton"
}

/**
 * A composable that displays the screen used to set the current trip.
 *
 * @param viewModel The view model for the screen.
 * @param title The title to display in the top bar.
 * @param isSelected Function to determine if a trip is selected.
 * @param onClose Callback when the close button is clicked.
 * @param navigationActions Navigation actions for screen transitions.
 */
@Composable
fun SetCurrentTripScreen(
    viewModel: MyTripsViewModel = viewModel(),
    title: String = "",
    isSelected: (Trip) -> Boolean = { false },
    onClose: () -> Unit = {},
    navigationActions: NavigationActions? = null
) {
  val uiState = viewModel.uiState.collectAsState().value
  // The current trip will always be at the top of the list even when sorting is applied
  val trips = buildList {
    uiState.currentTrip?.let { add(it) }
    addAll(uiState.tripsList)
  }

  val context = LocalContext.current

  // Refresh trips when entering the screen
  LaunchedEffect(Unit) { viewModel.refreshUIState() }

  Scaffold(
      topBar = {
        TopBarSetCurrentTrip(
            title = title,
            onClose = onClose,
            onClickDropDownMenu = { sortType -> viewModel.updateSortType(sortType) },
            selectedSortType = uiState.sortType)
      },
      modifier =
          Modifier.fillMaxWidth().padding(horizontal = dimensionResource(R.dimen.small_padding))) {
          pd ->
        Box(modifier = Modifier.padding(pd).fillMaxSize()) {
          TripList(
              trips = trips,
              interaction =
                  TripInteraction(
                      onClick = { trip ->
                        viewModel.changeCurrentTrip(trip!!)
                        navigationActions?.navigateTo(Screen.MyTrips)
                        Toast.makeText(context, R.string.current_trip_saved, Toast.LENGTH_SHORT)
                            .show()
                      },
                      onLongPress = { trip ->
                        viewModel.changeCurrentTrip(trip!!)
                        navigationActions?.navigateTo(Screen.MyTrips)
                        Toast.makeText(context, R.string.current_trip_saved, Toast.LENGTH_SHORT)
                            .show()
                      },
                      isSelected = isSelected,
                      isSelectionMode = false),
              noIconTripElement = true,
              emptyListString = stringResource(R.string.no_upcoming_trips))
        }
      }
}

/**
 * A composable that displays the top bar for the Set Current Trip screen.
 *
 * @param title The title to display in the top bar.
 * @param onClose Callback when the close button is clicked.
 * @param onClickDropDownMenu Callback when a sorting option is selected from the dropdown menu
 * @param selectedSortType The currently selected sorting option
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBarSetCurrentTrip(
    title: String = "",
    onClose: () -> Unit = {},
    onClickDropDownMenu: (TripSortType) -> Unit,
    selectedSortType: TripSortType
) {
  TopAppBar(
      modifier = Modifier.testTag(SetCurrentTripScreenTestTags.TOP_BAR),
      title = {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.testTag(SetCurrentTripScreenTestTags.TOP_BAR_TITLE))
      },
      navigationIcon = {
        IconButton(
            onClick = onClose,
            modifier = Modifier.testTag(SetCurrentTripScreenTestTags.TOP_BAR_CLOSE_BUTTON)) {
              Icon(
                  imageVector = Icons.Filled.Close,
                  contentDescription = "Close",
                  tint = MaterialTheme.colorScheme.onBackground)
            }
      },
      actions = {
        SortMenu(onClickDropDownMenu = onClickDropDownMenu, selectedSortType = selectedSortType)
      })
}
