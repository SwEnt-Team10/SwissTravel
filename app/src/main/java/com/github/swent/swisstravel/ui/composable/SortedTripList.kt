package com.github.swent.swisstravel.ui.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.model.trip.Trip
import com.github.swent.swisstravel.ui.mytrips.TripElement
import com.github.swent.swisstravel.ui.mytrips.TripSortType

/** Object containing test tags for the [SortedTripList] composable. */
object SortedTripListTestTags {
  const val TOP_BAR = "SortedTripListTopBar"
  const val TITLE_BUTTON_ROW = "SortedTripListTitleButtonRow"
  const val TRIP_LIST = "SortedTripListLazyColumn"
  const val EMPTY_MESSAGE = "SortedTripListEmptyMessage"
  const val TOP_BAR_BACK_BUTTON = "SortedTripListTopBarBackButton"
  const val SORT_DROPDOWN_MENU = "SortedTripListSortDropdownMenu"
  const val TITLE = "SortedTripListTitle"
  const val TOP_BAR_TITLE = "SortedTripListTopBarTitle"

  fun getTestTagSortOption(type: TripSortType): String {
    return "SortedTripListSortOption${type.name}"
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
 * @param topBar Whether to display the top bar with title and filter button.
 * @param onBackClick Callback when the back button in the top bar is clicked. !! Only used if
 *   topBar is true !!
 * @param topBarBackIcon The type of back icon to display in the top bar. !! Only used if topBar is
 *   true !!
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
    topBar: Boolean = false,
    onBackClick: () -> Unit = {},
    topBarBackIcon: IconType = IconType.BACK_ARROW
) {
  if (topBar) { // Trip list with a top bar including the title and the filter button
    Scaffold(
        topBar = {
          TopBarSortedList(
              title = title,
              backIcon = topBarBackIcon,
              onBackClick = onBackClick,
              onClickDropDownMenu = onClickDropDownMenu,
          )
        },
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) { pd ->
          Box(modifier = Modifier.padding(pd)) {
            TripList(
                trips = trips,
                onClickTripElement = onClickTripElement,
                onLongPress = onLongPress,
                isSelected = isSelected,
                isSelectionMode = isSelectionMode)
          }
        }
  } else { // No top bar but a title and the filter button on top of the list
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

          Filter(onClickDropDownMenu = onClickDropDownMenu)
        }

    TripList(
        trips = trips,
        onClickTripElement = onClickTripElement,
        onLongPress = onLongPress,
        isSelected = isSelected,
        isSelectionMode = isSelectionMode)
  }
}

/**
 * A composable that displays a list of trips.
 *
 * @param trips The list of trips to display.
 * @param onClickTripElement Callback when a trip element is clicked.
 * @param onLongPress Callback when a trip element is long-pressed.
 * @param isSelected Function to determine if a trip is selected.
 * @param isSelectionMode Whether the selection mode is active.
 */
@Composable
fun TripList(
    trips: List<Trip> = emptyList(),
    onClickTripElement: (Trip?) -> Unit = {},
    onLongPress: (Trip?) -> Unit = {},
    isSelected: (Trip) -> Boolean = { false },
    isSelectionMode: Boolean = false,
) {
  if (trips.isNotEmpty()) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth().testTag(SortedTripListTestTags.TRIP_LIST)) {
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
        modifier = Modifier.testTag(SortedTripListTestTags.EMPTY_MESSAGE))
  }
}

/**
 * A composable that displays a filter icon button with a dropdown menu for sorting options.
 *
 * @param onClickDropDownMenu Callback when a sorting option is selected from the dropdown menu.
 */
@Composable
fun Filter(onClickDropDownMenu: (TripSortType) -> Unit = {}) {
  var expanded by remember { mutableStateOf(false) }
  Box {
    IconButton(
        onClick = { expanded = !expanded },
        modifier = Modifier.testTag(SortedTripListTestTags.SORT_DROPDOWN_MENU)) {
          Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = stringResource(R.string.sort))
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
            modifier =
                Modifier.testTag(SortedTripListTestTags.getTestTagSortOption(type)).semantics(
                    mergeDescendants = true) {},
            text = { Text(stringResource(resId)) },
            onClick = {
              onClickDropDownMenu(type)
              expanded = false
            })
      }
    }
  }
}

/** Enum representing the type of icon to display in the top bar. */
enum class IconType {
  BACK_ARROW,
  CROSS
}

/**
 * Composable representing the top bar of the sorted trip list screen.
 *
 * @param title the title of the top bar
 * @param backIcon the type of back icon to display (back arrow or cross)
 * @param onBackClick the action to perform when the back icon is clicked
 * @param onClickDropDownMenu the action to perform when a sort option is selected from the dropdown
 *   menu
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBarSortedList(
    title: String = "",
    backIcon: IconType = IconType.BACK_ARROW,
    onBackClick: () -> Unit = {},
    onClickDropDownMenu: (TripSortType) -> Unit
) {
  TopAppBar(
      modifier = Modifier.testTag(SortedTripListTestTags.TOP_BAR),
      title = {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.testTag(SortedTripListTestTags.TOP_BAR_TITLE))
      },
      navigationIcon = {
        IconButton(
            onClick = onBackClick,
            modifier = Modifier.testTag(SortedTripListTestTags.TOP_BAR_BACK_BUTTON)) {
              when (backIcon) {
                IconType.BACK_ARROW -> {
                  Icon(
                      imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                      contentDescription = "Back Arrow",
                      tint = MaterialTheme.colorScheme.onBackground)
                }
                IconType.CROSS -> {
                  Icon(
                      imageVector = Icons.Filled.Close,
                      contentDescription = "Close Icon",
                      tint = MaterialTheme.colorScheme.onBackground)
                }
              }
            }
      },
      actions = { Filter(onClickDropDownMenu = onClickDropDownMenu) })
}
