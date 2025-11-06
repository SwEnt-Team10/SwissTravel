package com.github.swent.swisstravel.ui.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.github.swent.swisstravel.model.trip.Trip
import com.github.swent.swisstravel.ui.mytrips.TripSortType

/** Object containing test tags for the [SortedTripList] composable. */
object SortedTripListTestTags {
  const val TITLE_BUTTON_ROW = "SortedTripListTitleButtonRow"
  const val TRIP_LIST = TripListTestTags.TRIP_LIST
  const val EMPTY_MESSAGE = TripListTestTags.EMPTY_MESSAGE
  const val SORT_DROPDOWN_MENU = SortMenuTestTags.SORT_DROPDOWN_MENU
  const val TITLE = "SortedTripListTitle"

  fun getTestTagSortOption(type: TripSortType): String {
    return SortMenuTestTags.getTestTagSortOption(type)
  }
}

/**
 * A composable that displays a sorted list of trips with a dropdown menu for sorting options.
 *
 * @param title The title to display above the trip list.
 * @param trips The list of trips to display.
 * @param onClickTripElement Callback when a trip element is clicked.
 * @param onClickDropDownMenu Callback when a sorting option is selected from the dropdown menu.
 * @param onLongPress Callback when a trip element is long-pressed.
 * @param isSelected Function to determine if a trip is selected.
 * @param isSelectionMode Whether the selection mode is active.
 */
@Composable
fun SortedTripList(
    title: String = "",
    trips: List<Trip> = emptyList(),
    onClickTripElement: (Trip?) -> Unit = {},
    onClickDropDownMenu: (TripSortType) -> Unit = {},
    onLongPress: (Trip?) -> Unit = {},
    isSelected: (Trip) -> Boolean = { false },
    isSelectionMode: Boolean = false
) {
  Row(
      modifier =
          Modifier.fillMaxWidth()
              .padding(top = 26.dp, bottom = 10.dp)
              .testTag(SortedTripListTestTags.TITLE_BUTTON_ROW),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.testTag(SortedTripListTestTags.TITLE))

        SortMenu(onClickDropDownMenu = onClickDropDownMenu)
      }

  TripList(
      trips = trips,
      onClickTripElement = onClickTripElement,
      onLongPress = onLongPress,
      isSelected = isSelected,
      isSelectionMode = isSelectionMode)
}
