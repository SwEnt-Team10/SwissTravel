package com.github.swent.swisstravel.ui.currenttrip

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.github.swent.swisstravel.model.trip.Trip
import com.github.swent.swisstravel.ui.composable.SortMenu
import com.github.swent.swisstravel.ui.composable.TripList
import com.github.swent.swisstravel.ui.mytrips.TripSortType

/** Object containing test tags for the [SetCurrentTripScreen] composable. */
object SetCurrentTripScreenTestTags {
  const val TOP_BAR = "SetCurrentTripScreenTopBar"
  const val TOP_BAR_TITLE = "SetCurrentTripScreenTopBarTitle"
  const val TOP_BAR_CLOSE_BUTTON = "SetCurrentTripScreenTopBarCloseButton"
}

// TODO will selectionMode really be necessary? Probably not, as only one trip can be selected at a
// time.
// TODO I will see in my PR where I put connect with the program
/**
 * A composable that displays the screen used to set the current trip.
 *
 * @param title The title to display in the top bar.
 * @param trips The list of trips to display.
 * @param onClickTripElement Callback when a trip element is clicked.
 * @param onClickDropDownMenu Callback when a sorting option is selected from the dropdown menu.
 * @param onLongPress Callback when a trip element is long-pressed.
 * @param isSelected Function to determine if a trip is selected.
 * @param isSelectionMode Whether the selection mode is active.
 * @param onClose Callback when the close button is clicked.
 */
@Composable
fun SetCurrentTripScreen(
    title: String = "",
    trips: List<Trip> = emptyList(),
    onClickTripElement: (Trip?) -> Unit = {},
    onClickDropDownMenu: (TripSortType) -> Unit = {},
    onLongPress: (Trip?) -> Unit = {},
    isSelected: (Trip) -> Boolean = { false },
    isSelectionMode: Boolean = false,
    onClose: () -> Unit = {},
) {
  Scaffold(
      topBar = {
        TopBarSetCurrentTrip(
            title = title,
            onClose = onClose,
            onClickDropDownMenu = onClickDropDownMenu,
        )
      },
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) { pd ->
        Box(modifier = Modifier.padding(pd).fillMaxSize()) {
          TripList(
              trips = trips,
              onClickTripElement = onClickTripElement,
              onLongPress = onLongPress,
              isSelected = isSelected,
              isSelectionMode = isSelectionMode)
        }
      }
}

/**
 * A composable that displays the top bar for the Set Current Trip screen.
 *
 * @param title The title to display in the top bar.
 * @param onClose Callback when the close button is clicked.
 * @param onClickDropDownMenu Callback when a sorting option is selected from the dropdown menu
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBarSetCurrentTrip(
    title: String = "",
    onClose: () -> Unit = {},
    onClickDropDownMenu: (TripSortType) -> Unit
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
      actions = { SortMenu(onClickDropDownMenu = onClickDropDownMenu) })
}
