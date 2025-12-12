package com.github.swent.swisstravel.ui.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.ui.trips.TripSortType

/** Object containing test tags for the [SortedTripList] composable. */
object SortedTripListTestTags {
  const val TITLE_BUTTON_ROW = "SortedTripListTitleButtonRow"
  const val TRIP_LIST = TripListTestTags.TRIP_LIST
  const val EMPTY_MESSAGE = TripListTestTags.EMPTY_MESSAGE
  const val SORT_DROPDOWN_MENU = SortMenuTestTags.SORT_DROPDOWN_MENU
  const val TITLE = "SortedTripListTitle"
  const val SORTED_TRIP_LIST = "SortedTripList"

  /** Returns a unique test tag for the given [type] of sorting option. */
  fun getTestTagSortOption(type: TripSortType): String {
    return SortMenuTestTags.getTestTagSortOption(type)
  }
}

/**
 * An extension function for [LazyListScope] that adds a sorted list of trips with a header. This
 * was made with the help of AI.
 *
 * @param title The title to display above the trip list.
 * @param listState The state object for the underlying trip list.
 * @param listEvents The event handler for the underlying trip list.
 * @param onClickDropDownMenu Callback when a sorting option is selected from the dropdown menu.
 * @param selectedSortType The currently selected sorting option.
 */
fun LazyListScope.sortedTripListItems(
    title: String = "",
    listState: TripListState,
    listEvents: TripListEvents,
    onClickDropDownMenu: (TripSortType) -> Unit = {},
    selectedSortType: TripSortType,
) {
  item {
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
  }
  tripListItems(listState = listState, listEvents = listEvents)
}
