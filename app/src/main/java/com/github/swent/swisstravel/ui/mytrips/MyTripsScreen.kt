package com.github.swent.swisstravel.ui.mytrips

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.model.trip.Trip
import com.github.swent.swisstravel.ui.map.NavigationMapScreenTestTags
import com.github.swent.swisstravel.ui.navigation.BottomNavigationMenu
import com.github.swent.swisstravel.ui.navigation.NavigationActions
import com.github.swent.swisstravel.ui.navigation.NavigationTestTags
import com.github.swent.swisstravel.ui.navigation.Screen
import com.github.swent.swisstravel.ui.navigation.Tab

object MyTripsScreenTestTags {
  const val PAST_TRIPS_BUTTON = "pastTrips"
  const val CURRENT_TRIP_TITLE = "currentTripTitle"
  const val CREATE_TRIP_BUTTON = "createTrip"
  const val EMPTY_CURRENT_TRIP_MSG = "emptyCurrentTrip"
  const val UPCOMING_TRIPS_TITLE = "upcomingTripsTitle"
  const val UPCOMING_TRIPS = "upcomingTrips"
  const val EMPTY_UPCOMING_TRIPS_MSG = "emptyUpcomingTrips"

  fun getTestTagForTrip(trip: Trip): String = "trip${trip.uid}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyTripsScreen(
    myTripsViewModel: MyTripsViewModel = viewModel(),
    onSelectTrip: (Trip) -> Unit = {},
    onPastTrips: () -> Unit = {},
    onCreateTrip: () -> Unit = {},
    navigationActions: NavigationActions? = null,
) {

  val context = LocalContext.current
  val uiState by myTripsViewModel.uiState.collectAsState()
  val currentTrip = uiState.currentTrip
  val upcomingTrips = uiState.upcomingTrips

  // Fetch trips when the screen is recomposed
  LaunchedEffect(Unit) { myTripsViewModel.refreshUIState() }

  // Show error message if fetching Trips fails
  LaunchedEffect(uiState.errorMsg) {
    uiState.errorMsg?.let { message ->
      Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
      myTripsViewModel.clearErrorMsg()
    }
  }

  Scaffold(
      topBar = {
        TopAppBar(
            title = {
              Text(
                  text = stringResource(R.string.my_trips),
                  style = MaterialTheme.typography.titleLarge,
                  color = MaterialTheme.colorScheme.onBackground
                  /*,modifier = Modifier.testTag(NavigationTestTags.TOP_BAR_TITLE)*/ )
            },
            actions = {
              // Past Trips Icon Button
              IconButton(
                  onClick = { onPastTrips() },
                  modifier = Modifier.testTag(MyTripsScreenTestTags.PAST_TRIPS_BUTTON)) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = stringResource(R.string.go_past_trips))
                  }
            })
      },
      floatingActionButton = {
        FloatingActionButton(
            onClick = { onCreateTrip() },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.testTag(MyTripsScreenTestTags.CREATE_TRIP_BUTTON)) {
              Icon(imageVector = Icons.Default.Add, contentDescription = "Add")
            }
      },
      bottomBar = {
        BottomNavigationMenu(
            selectedTab = Tab.MyTrips,
            onTabSelected = { tab -> navigationActions?.navigateTo(tab.destination) },
            modifier = Modifier.testTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU))
      },
      content = { pd ->
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .padding(pd)
                    .padding(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 4.dp),
        ) {
          // Current Trip section
          Text(
              text = stringResource(R.string.current_trip),
              style = MaterialTheme.typography.headlineLarge,
              color = MaterialTheme.colorScheme.onBackground,
              modifier =
                  Modifier.testTag(MyTripsScreenTestTags.CURRENT_TRIP_TITLE)
                      .padding(top = 0.dp, bottom = 10.dp))

          Spacer(modifier = Modifier.height(4.dp))
          Box(contentAlignment = Alignment.TopCenter) {
            Button(
                onClick = { navigationActions?.navigateTo(Screen.SelectedTripMap) },
                modifier = Modifier.testTag(NavigationMapScreenTestTags.ENTER_MAP_BUTTON)) {
                  // TODO : modify this to an "extend" icon when the map preview is implemented
                  Text(stringResource(R.string.enter_map))
                }
          }
          Spacer(modifier = Modifier.height(4.dp))

          if (currentTrip != null) {
            TripElement(trip = currentTrip, onClick = { onSelectTrip(currentTrip) })
            Text(
                text = stringResource(R.string.warning_multiple_current_trip),
                style = MaterialTheme.typography.labelSmall)
          } else {
            Text(
                text = stringResource(R.string.no_current_trip),
                modifier = Modifier.testTag(MyTripsScreenTestTags.EMPTY_CURRENT_TRIP_MSG))
          }

          // Upcoming Trip section
          Text(
              text = stringResource(R.string.upcoming_trip),
              style = MaterialTheme.typography.headlineLarge,
              color = MaterialTheme.colorScheme.onBackground,
              modifier =
                  Modifier.testTag(MyTripsScreenTestTags.UPCOMING_TRIPS_TITLE)
                      .padding(top = 26.dp, bottom = 10.dp))

          Spacer(modifier = Modifier.height(4.dp))

          if (upcomingTrips.isNotEmpty()) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth().testTag(MyTripsScreenTestTags.UPCOMING_TRIPS)) {
                  items(upcomingTrips.size) { index ->
                    TripElement(
                        trip = upcomingTrips[index],
                        onClick = { onSelectTrip(upcomingTrips[index]) })
                  }
                }
          } else {
            Text(
                text = stringResource(R.string.no_upcoming_trip),
                modifier = Modifier.testTag(MyTripsScreenTestTags.EMPTY_UPCOMING_TRIPS_MSG))
          }
        }
      })
}
