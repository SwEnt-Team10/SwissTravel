package com.github.swent.swisstravel.ui.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.model.trip.Trip
import com.github.swent.swisstravel.ui.trips.TripSortType
import com.github.swent.swisstravel.ui.trips.TripsViewModel

/** Object containing test tags for the [SortedTripList] composable. */
object SortedTripListTestTags {
  const val TITLE_BUTTON_ROW = "SortedTripListTitleButtonRow"
  const val TRIP_LIST = TripListTestTags.TRIP_LIST
  const val EMPTY_MESSAGE = TripListTestTags.EMPTY_MESSAGE
  const val SORT_DROPDOWN_MENU = SortMenuTestTags.SORT_DROPDOWN_MENU
  const val TITLE = "SortedTripListTitle"
  const val SORTED_TRIP_LIST = "SortedTripList"

  fun getTestTagSortOption(type: TripSortType): String {
    return SortMenuTestTags.getTestTagSortOption(type)
  }
}

/**
 * Holds the state parameters for the [TripList].
 *
 * @param trips The list of trips to display.
 * @param emptyListString The string to display when the trip list is empty.
 * @param isSelectionMode Whether the selection mode is active.
 * @param noIconTripElement Whether to hide the icon in the trip element.
 * @param isSelected Function to determine if a trip is selected.
 * @param collaboratorsLookup Function to retrieve collaborators for a specific trip.
 */
data class TripListState(
    val trips: List<Trip> = emptyList(),
    val emptyListString: String = "",
    val isSelectionMode: Boolean = false,
    val noIconTripElement: Boolean = false,
    val isSelected: (Trip) -> Boolean = { false },
    val collaboratorsLookup: (String) -> List<TripsViewModel.CollaboratorUi> = { emptyList() }
)

/**
 * Holds the event callbacks for the [TripList].
 *
 * @param onClickTripElement Callback when a trip element is clicked.
 * @param onLongPress Callback when a trip element is long-pressed.
 */
data class TripListEvents(
    val onClickTripElement: (Trip?) -> Unit = {},
    val onLongPress: (Trip?) -> Unit = {}
)

/**
 * A composable that displays a sorted list of trips with a dropdown menu for sorting options.
 *
 * @param title The title to display above the trip list.
 * @param listState The state object for the underlying trip list.
 * @param listEvents The event handler for the underlying trip list.
 * @param onClickDropDownMenu Callback when a sorting option is selected from the dropdown menu.
 * @param selectedSortType The currently selected sorting option.
 */
@Composable
fun SortedTripList(
    title: String = "",
    listState: TripListState,
    listEvents: TripListEvents,
    onClickDropDownMenu: (TripSortType) -> Unit = {},
    selectedSortType: TripSortType,
) {
  Column(modifier = Modifier.fillMaxWidth().testTag(SortedTripListTestTags.SORTED_TRIP_LIST)) {
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .padding(
                    top = dimensionResource(R.dimen.sorted_trip_list_top_padding),
                    bottom = dimensionResource(R.dimen.sorted_trip_list_bottom_padding))
                .testTag(SortedTripListTestTags.TITLE_BUTTON_ROW),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
          Text(
              text = title,
              style = MaterialTheme.typography.headlineLarge,
              color = MaterialTheme.colorScheme.onBackground,
              modifier = Modifier.testTag(SortedTripListTestTags.TITLE))

          SortMenu(onClickDropDownMenu = onClickDropDownMenu, selectedSortType = selectedSortType)
        }

    TripList(
        trips = listState.trips,
        onClickTripElement = listEvents.onClickTripElement,
        onLongPress = listEvents.onLongPress,
        isSelected = listState.isSelected,
        isSelectionMode = listState.isSelectionMode,
        noIconTripElement = listState.noIconTripElement,
        emptyListString = listState.emptyListString,
        collaboratorsLookup = listState.collaboratorsLookup)
  }
}
