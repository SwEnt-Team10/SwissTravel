package com.github.swent.swisstravel.ui.mytrips

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.text.get

object MyTripsScreenTestTags {
  const val PAST_TRIPS_BUTTON = "pastTrips"
  const val CURRENT_TRIP_TITLE = "currentTripTitle"
  const val CURRENT_TRIP = "currentTrip" // TODO
  const val EMPTY_CURRENT_TRIP_MSG = "emptyCurrentTrip"
  const val UPCOMING_TRIPS_TITLE = "upcomingTripsTitle"
  const val UPCOMING_TRIPS = "upcomingTrips"
  const val EMPTY_UPCOMING_TRIPS_MSG = "emptyUpcomingTrips"

  fun getTestTagForTrip(trip: Trip): String = "trip${trip.uid}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyTripsScreen(
    myTripsScreenViewModel: MyTripsViewModel = viewModel(),
    onSelectTrip: (Trip) -> Unit = {},
    navigationActions: NavigationActions? = null,
) {

  val context = LocalContext.current
  val uiState by myTripsScreenViewModel.uiState.collectAsState()
  val currentTrip = uiState.currentTrip
  val upcomingTrips = uiState.upcomingTrips

  // Fetch trips when the screen is recomposed
  LaunchedEffect(Unit) { myTripsScreenViewModel.refreshUIState() }

  // Show error message if fetching todos fails
  LaunchedEffect(uiState.errorMsg) {
    uiState.errorMsg?.let { message ->
      Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
      myTripsScreenViewModel.clearErrorMsg()
    }
  }

  Scaffold(
      topBar = {
        TopAppBar(
            title = {
              Text(
                  text = "My Trips",
                  style = MaterialTheme.typography.titleLarge,
                  color = MaterialTheme.colorScheme.onBackground,
                  modifier = Modifier.testTag(NavigationTestTags.TOP_BAR_TITLE))
            },
            actions = {
              // Past Trips Icon Button
              IconButton(
                  onClick = { /*TODO navigate to past trips*/},
                  modifier = Modifier.testTag(MyTripsScreenTestTags.PAST_TRIPS_BUTTON)) {
                    Icon(imageVector = Icons.Default.Archive, contentDescription = "Past Trips")
                  }
            })
      },
      bottomBar = {
        BottomNavigationMenu(
            selectedTab = Tab.MyTrips,
            onTabSelected = { tab -> navigationActions?.navigateTo(tab.destination) },
            modifier = Modifier.testTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU))
      },
      content = { pd ->
        Column(modifier = Modifier.padding(pd)) {
          Text(
              text = "Current Trip",
              style = MaterialTheme.typography.headlineLarge,
              color = MaterialTheme.colorScheme.onBackground,
              modifier = Modifier.testTag(MyTripsScreenTestTags.CURRENT_TRIP_TITLE))
          if (currentTrip != null) {
            TripElement(trip = currentTrip, onClick = { onSelectTrip(currentTrip) })
          } else {
            Text(
                modifier =
                    Modifier.padding(pd).testTag(MyTripsScreenTestTags.EMPTY_CURRENT_TRIP_MSG),
                text = "You have no current trip. Get planning!")
          }
          Text(
              text = "Upcoming Trip",
              style = MaterialTheme.typography.headlineLarge,
              color = MaterialTheme.colorScheme.onBackground,
              modifier = Modifier.testTag(MyTripsScreenTestTags.UPCOMING_TRIPS_TITLE))
          if (upcomingTrips.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier.padding(pd).testTag(MyTripsScreenTestTags.UPCOMING_TRIPS)) {
                  items(upcomingTrips.size) { index ->
                    TripElement(
                        trip = upcomingTrips[index],
                        onClick = { onSelectTrip(upcomingTrips[index]) })
                  }
                }
          } else {
            Text(
                modifier =
                    Modifier.padding(pd).testTag(MyTripsScreenTestTags.EMPTY_UPCOMING_TRIPS_MSG),
                text = "You have no upcoming trips. Get planning!")
          }
        }
      })
}
