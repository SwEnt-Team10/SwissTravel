package com.github.swent.swisstravel.ui.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.model.trip.Trip
import com.github.swent.swisstravel.ui.trips.MyTripsScreenTestTags
import com.github.swent.swisstravel.ui.trips.TripElement

object TripListTestTags {
  const val TRIP_LIST = "TripListLazyColumn"
  const val EMPTY_MESSAGE = "TripListEmptyMessage"
}

/**
 * A composable that displays a list of trips.
 *
 * @param trips The list of trips to display.
 * @param onClickTripElement Callback when a trip element is clicked.
 * @param onLongPress Callback when a trip element is long-pressed.
 * @param isSelected Function to determine if a trip is selected.
 * @param isSelectionMode Whether the selection mode is active.
 * @param emptyListString The string to display when the list is empty.
 */
@Composable
fun TripList(
    trips: List<Trip> = emptyList(),
    onClickTripElement: (Trip?) -> Unit = {},
    onLongPress: (Trip?) -> Unit = {},
    isSelected: (Trip) -> Boolean = { false },
    isSelectionMode: Boolean = false,
    noIconTripElement: Boolean = false,
    emptyListString: String = "",
) {
  if (trips.isNotEmpty()) {
    LazyColumn(
        verticalArrangement =
            Arrangement.spacedBy(dimensionResource(R.dimen.trip_list_vertical_arrangement)),
        modifier = Modifier.fillMaxWidth().testTag(TripListTestTags.TRIP_LIST)) {
          items(trips.size) { index ->
            val trip = trips[index]
            TripElement(
                trip = trip,
                onClick = { onClickTripElement(trip) },
                onLongPress = { onLongPress(trip) },
                isSelected = isSelected(trip),
                isSelectionMode = isSelectionMode,
                noIcon = noIconTripElement)
          }
        }
  } else {
    Text(
        text = emptyListString,
        modifier =
            Modifier.testTag(TripListTestTags.EMPTY_MESSAGE)
                .testTag(MyTripsScreenTestTags.EMPTY_CURRENT_TRIP_MSG))
  }
}
/**
 * An extension function for [LazyListScope] that adds a list of trips. This was made with the help
 * of AI.
 *
 * @param trips The list of trips to display.
 * @param onClickTripElement Callback when a trip element is clicked.
 * @param onLongPress Callback when a trip element is long-pressed.
 * @param isSelected Function to determine if a trip is selected.
 * @param isSelectionMode Whether the selection mode is active.
 * @param noIconTripElement Whether to hide the icon in the trip element.
 * @param emptyListString The string to display when the list is empty.
 */
fun LazyListScope.tripListItems(
    trips: List<Trip> = emptyList(),
    onClickTripElement: (Trip?) -> Unit = {},
    onLongPress: (Trip?) -> Unit = {},
    isSelected: (Trip) -> Boolean = { false },
    isSelectionMode: Boolean = false,
    noIconTripElement: Boolean = false,
    emptyListString: String = "",
) {
  if (trips.isNotEmpty()) {
    items(trips.size) { index ->
      val trip = trips[index]
      TripElement(
          trip = trip,
          onClick = { onClickTripElement(trip) },
          onLongPress = { onLongPress(trip) },
          isSelected = isSelected(trip),
          isSelectionMode = isSelectionMode,
          noIcon = noIconTripElement)
    }
  } else {
      item{
          Text(
              text = emptyListString,
              modifier = Modifier.testTag(TripListTestTags.EMPTY_MESSAGE))

      }
   }
}
