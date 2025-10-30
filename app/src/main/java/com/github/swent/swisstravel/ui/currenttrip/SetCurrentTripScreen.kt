package com.github.swent.swisstravel.ui.currenttrip

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.ui.composable.SortedTripList
import com.github.swent.swisstravel.ui.mytrips.MyTripsViewModel
import com.github.swent.swisstravel.ui.navigation.NavigationActions
import com.github.swent.swisstravel.ui.navigation.Screen
import com.github.swent.swisstravel.ui.navigation.TopBar

object SetCurrentTripTestTags {
  const val SET_CURRENT_TRIP_SCREEN = "setCurrentTripScreen"
  const val SET_CURRENT_TRIP_TITLE = "setCurrentTripTitle"
  const val SET_CURRENT_TRIP_LAZY_COLUMN = "setCurrentTripLazyColumn"
  const val SET_CURRENT_TRIP_EMPTY_MESSAGE = "setCurrentTripEmptyMessage"
}

@Composable
fun SetCurrentTripScreen(
    viewModel: MyTripsViewModel = viewModel(),
    onPrevious: () -> Unit = {},
    navigationActions: NavigationActions? = null,
) {
  Scaffold(
      topBar = { TopBar(onClick = onPrevious, title = stringResource(R.string.select_a_trip)) },
      modifier = Modifier.testTag(SetCurrentTripTestTags.SET_CURRENT_TRIP_SCREEN)) { pd ->
        val uiState by viewModel.uiState.collectAsState()

        Surface(modifier = Modifier.padding(pd)) {
          SortedTripList(
              title = stringResource(R.string.your_trips),
              trips = uiState.upcomingTrips,
              onClickTripElement = { trip ->
                viewModel.changeCurrentTrip(trip!!)
                navigationActions?.navigateTo(Screen.MyTrips)
              },
              onClickDropDownMenu = { trip -> viewModel.updateSortType(trip) },
              onLongPress = { trip ->
                viewModel.changeCurrentTrip(trip!!)
                navigationActions?.navigateTo(Screen.MyTrips)
              },
              isCurrentTrip = { trip -> uiState.currentTrip?.uid == trip.uid },
              titleTestTag = SetCurrentTripTestTags.SET_CURRENT_TRIP_TITLE,
              lazyColumnTestTag = SetCurrentTripTestTags.SET_CURRENT_TRIP_LAZY_COLUMN,
              emptyMessageTestTag = SetCurrentTripTestTags.SET_CURRENT_TRIP_EMPTY_MESSAGE)
        }
      }
}
