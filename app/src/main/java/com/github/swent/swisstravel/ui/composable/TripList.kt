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
import com.github.swent.swisstravel.ui.trips.TripsViewModel

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
 * @param trips The list of trips to display.
 * @param interaction The interaction and selection parameters.
 * @param noIconTripElement Whether to hide the icon in the trip element.
 * @param emptyListString The string to display when the list is empty.
 * @param collaboratorsLookup Function to lookup collaborators for a trip.
 */
@Composable
fun TripList(
    trips: List<Trip> = emptyList(),
    interaction: TripInteraction = TripInteraction(),
    noIconTripElement: Boolean = false,
    emptyListString: String = "",
    collaboratorsLookup: (String) -> List<TripsViewModel.CollaboratorUi> = { emptyList() }
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
                onClick = { interaction.onClick(trip) },
                onLongPress = { interaction.onLongPress(trip) },
                isSelected = interaction.isSelected(trip),
                isSelectionMode = interaction.isSelectionMode,
                noIcon = noIconTripElement,
                collaborators = collaboratorsLookup(trip.uid))
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
 * @param interaction The interaction and selection parameters.
 * @param noIconTripElement Whether to hide the icon in the trip element.
 * @param emptyListString The string to display when the list is empty.
 * @param collaboratorsLookup Function to lookup collaborators for a trip.
 */
fun LazyListScope.tripListItems(
    trips: List<Trip> = emptyList(),
    interaction: TripInteraction = TripInteraction(),
    noIconTripElement: Boolean = false,
    emptyListString: String = "",
    collaboratorsLookup: (String) -> List<TripsViewModel.CollaboratorUi> = { emptyList() }
) {
  if (trips.isNotEmpty()) {
    items(trips.size) { index ->
      val trip = trips[index]
      TripElement(
          trip = trip,
          onClick = { interaction.onClick(trip) },
          onLongPress = { interaction.onLongPress(trip) },
          isSelected = interaction.isSelected(trip),
          isSelectionMode = interaction.isSelectionMode,
          noIcon = noIconTripElement,
          collaborators = collaboratorsLookup(trip.uid))
    }
  } else {
    item {
      Text(text = emptyListString, modifier = Modifier.testTag(TripListTestTags.EMPTY_MESSAGE))
    }
  }
}
