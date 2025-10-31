package com.github.swent.swisstravel.ui.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.model.trip.Trip
import com.github.swent.swisstravel.ui.mytrips.MyTripsScreenTestTags
import com.github.swent.swisstravel.ui.mytrips.TripElement
import com.github.swent.swisstravel.ui.mytrips.TripSortType

/**
 * A composable that displays a sorted list of trips with a dropdown menu for sorting options.
 *
 * @param title The title of the trip list.
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
    isSelectionMode: Boolean = false,
    titleTestTag: String,
    lazyColumnTestTag: String,
    emptyMessageTestTag: String
) {
  Row(
      modifier = Modifier.fillMaxWidth().padding(top = 26.dp, bottom = 10.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.testTag(titleTestTag))

        var expanded by remember { mutableStateOf(false) }

        Box {
          IconButton(
              onClick = { expanded = !expanded },
              modifier = Modifier.testTag(MyTripsScreenTestTags.SORT_DROPDOWN_MENU)) {
                Icon(
                    Icons.AutoMirrored.Filled.Sort,
                    contentDescription = stringResource(R.string.sort))
              }

          DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            val sortOptions =
                listOf(
                    TripSortType.START_DATE_ASC to R.string.start_date_asc,
                    TripSortType.START_DATE_DESC to R.string.start_date_desc,
                    TripSortType.END_DATE_ASC to R.string.end_date_asc,
                    TripSortType.END_DATE_DESC to R.string.end_date_desc,
                    TripSortType.NAME_ASC to R.string.name_asc,
                    TripSortType.NAME_DESC to R.string.name_desc)
            sortOptions.forEach { (type, resId) ->
              DropdownMenuItem(
                  text = { Text(stringResource(resId)) },
                  onClick = {
                    onClickDropDownMenu(type)
                    expanded = false
                  })
            }
          }
        }
      }

  if (trips.isNotEmpty()) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth().testTag(lazyColumnTestTag)) {
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
    Text(
        text = stringResource(R.string.no_upcoming_trip),
        modifier = Modifier.testTag(emptyMessageTestTag))
  }
}
