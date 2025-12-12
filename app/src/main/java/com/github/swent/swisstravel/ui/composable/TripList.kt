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
import com.github.swent.swisstravel.ui.trips.TripElement
import com.github.swent.swisstravel.ui.trips.TripElementState

/** Test tags for the [TripList] composable. */
object TripListTestTags {
  /** The tag for the lazy column containing the trips. */
  const val TRIP_LIST = "TripListLazyColumn"

  /** The tag for the empty message text. */
  const val EMPTY_MESSAGE = "TripListEmptyMessage"
}

/**
 * A composable that displays a list of trips.
 *
 * @param listState The state object for the underlying trip list.
 * @param listEvents The event handler for the underlying trip list.
 */
@Composable
fun TripList(listState: TripListState, listEvents: TripListEvents) {
  if (listState.trips.isNotEmpty()) {
    LazyColumn(
        verticalArrangement =
            Arrangement.spacedBy(dimensionResource(R.dimen.trip_list_vertical_arrangement)),
        modifier = Modifier.fillMaxWidth().testTag(TripListTestTags.TRIP_LIST)) {
          items(listState.trips.size) { index ->
            val trip = listState.trips[index]
            val collaboratorsForThisTrip = listState.collaboratorsLookup(trip.uid)

            val elementState =
                TripElementState(
                    trip = trip,
                    isSelected = listState.isSelected(trip),
                    isSelectionMode = listState.isSelectionMode,
                    noIcon = listState.noIconTripElement,
                    collaborators = collaboratorsForThisTrip,
                    isFavorite = trip.uid in listState.favoriteTripsUids)

            TripElement(
                tripElementState = elementState,
                onClick = { listEvents.onClickTripElement(trip) },
                onLongPress = { listEvents.onLongPress(trip) },
            )
          }
        }
  } else {
      Text(
          text = listState.emptyListString,
          modifier = Modifier.testTag(TripListTestTags.EMPTY_MESSAGE))
  }
}
/**
 * An extension function for [LazyListScope] that adds a list of trips. This was made with the help
 * of AI.
 *
 * @param listState The state object for the underlying trip list.
 * @param listEvents The event handler for the underlying trip list.
 */
fun LazyListScope.tripListItems(listState: TripListState, listEvents: TripListEvents) {
    if (listState.trips.isNotEmpty()) {
            items(listState.trips.size) { index ->
                val trip = listState.trips[index]
                val collaboratorsForThisTrip = listState.collaboratorsLookup(trip.uid)

                val elementState =
                    TripElementState(
                        trip = trip,
                        isSelected = listState.isSelected(trip),
                        isSelectionMode = listState.isSelectionMode,
                        noIcon = listState.noIconTripElement,
                        collaborators = collaboratorsForThisTrip,
                        isFavorite = trip.uid in listState.favoriteTripsUids)

                TripElement(
                    tripElementState = elementState,
                    onClick = { listEvents.onClickTripElement(trip) },
                    onLongPress = { listEvents.onLongPress(trip) },
                )
            }
    } else {
        item {
        Text(
            text = listState.emptyListString,
            modifier = Modifier.testTag(TripListTestTags.EMPTY_MESSAGE))
    }
  }
}
