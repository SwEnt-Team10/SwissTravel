package com.github.swent.swisstravel.ui.composable

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.style.TextAlign
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.model.trip.Trip
import com.github.swent.swisstravel.ui.trips.TripsViewModel

/** Test tags for the [tripListItems]. */
object TripListTestTags {
  const val TRIP_LIST = "TripListLazyColumn"
  const val EMPTY_MESSAGE = "TripListEmptyMessage"
}

/**
 * Holds the state parameters for the [tripListItems].
 *
 * @param trips The list of trips to display.
 * @param emptyListString The string to display when the trip list is empty.
 * @param isSelectionMode Whether the selection mode is active.
 * @param noIconTripElement Whether to hide the icon in the trip element.
 * @param isSelected Function to determine if a trip is selected.
 * @param collaboratorsLookup Function to retrieve collaborators for a specific trip.
 * @param favoriteTripsUids The set of favorite trip UIDs.
 */
data class TripListState(
    val trips: List<Trip> = emptyList(),
    val emptyListString: String = "",
    val isSelectionMode: Boolean = false,
    val noIconTripElement: Boolean = false,
    val isSelected: (Trip) -> Boolean = { false },
    val collaboratorsLookup: (String) -> List<TripsViewModel.CollaboratorUi> = { emptyList() },
    val favoriteTripsUids: Set<String> = emptySet(),
)

/**
 * Holds the event callbacks for the [tripListItems].
 *
 * @param onClickTripElement Callback when a trip element is clicked.
 * @param onLongPress Callback when a trip element is long-pressed.
 */
data class TripListEvents(
    val onClickTripElement: (Trip?) -> Unit = {},
    val onLongPress: (Trip?) -> Unit = {}
)

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

      Spacer(modifier = Modifier.height(dimensionResource(R.dimen.tiny_spacer)))
    }
  } else {
    item {
      Text(
          text = listState.emptyListString,
          textAlign = TextAlign.Center,
          modifier = Modifier.testTag(TripListTestTags.EMPTY_MESSAGE))
    }
  }
}
