package com.github.swent.swisstravel.ui.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.github.swent.swisstravel.model.trip.Trip
import com.github.swent.swisstravel.ui.mytrips.TripElement

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
    emptyListString: String = "",
) {
  if (trips.isNotEmpty()) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth().testTag(TripListTestTags.TRIP_LIST)) {
          items(trips.size) { index ->
            val trip = trips[index]
            TripElement(
                trip = trip,
                onClick = { onClickTripElement(trip) },
                onLongPress = { onLongPress(trip) },
                isSelected = isSelected(trip),
                isSelectionMode = isSelectionMode)
          }
        }
  } else {
    Text(text = emptyListString, modifier = Modifier.testTag(TripListTestTags.EMPTY_MESSAGE))
  }
}
