package com.github.swent.swisstravel.ui.currenttrip

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.ui.navigation.NavigationActions
import com.github.swent.swisstravel.ui.navigation.Screen
import com.github.swent.swisstravel.ui.trip.tripinfos.TripInfoScreen
import com.github.swent.swisstravel.ui.trips.MyTripsViewModel

object CurrentTripScreenTestTags {
  const val CREATE_TRIP_BUTTON = "createTripButton"
  const val CREATE_TRIP_TEXT = "createTripText"
  const val LOG_IN_TEXT = "logInText"
}

@Composable
fun CurrentTripScreen(
    navigationActions: NavigationActions? = null,
    isLoggedIn: Boolean = false,
    myTripsViewModel: MyTripsViewModel = viewModel(),
) {

  val context = LocalContext.current
  val tripsState = myTripsViewModel.uiState.collectAsState()
  val currentTrip = tripsState.value.currentTrip

  LaunchedEffect(Unit) {
    if (!isLoggedIn) {
      Toast.makeText(context, R.string.log_in_to_display, Toast.LENGTH_LONG).show()
    }
  }
  if (currentTrip != null) {
    TripInfoScreen(
        currentTrip.uid,
        onFullscreenClick = { navigationActions?.navigateTo(Screen.TripInfoMap) },
        onEditTrip = { navigationActions?.navigateToEditTrip(currentTrip.uid) },
        isOnCurrentTripScreen = true)
  } else {
    Scaffold(
        content = { pd ->
          Column(
              modifier =
                  Modifier.fillMaxSize()
                      .padding(pd)
                      .padding(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 4.dp),
              verticalArrangement = Arrangement.Center,
              horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.create_trip),
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.testTag(CurrentTripScreenTestTags.CREATE_TRIP_TEXT))
                Spacer(modifier = Modifier.height(72.dp))
                // Create a new trip
                Button(
                    onClick = { navigationActions?.navigateTo(Screen.TripSettings1) },
                    enabled = isLoggedIn,
                    modifier = Modifier.testTag(CurrentTripScreenTestTags.CREATE_TRIP_BUTTON)) {
                      Text(
                          text = stringResource(R.string.where_starting),
                          style = MaterialTheme.typography.titleMedium,
                          color = MaterialTheme.colorScheme.onPrimary,
                      )
                    }
              }
        })
  }

  Scaffold(
      content = { pd ->
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .padding(pd)
                    .padding(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 4.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                  text = stringResource(R.string.create_trip),
                  style = MaterialTheme.typography.displayLarge,
                  color = MaterialTheme.colorScheme.onBackground,
                  modifier = Modifier.testTag(CurrentTripScreenTestTags.CREATE_TRIP_TEXT))
              Spacer(modifier = Modifier.height(72.dp))
              // Create a new trip
              Button(
                  onClick = { navigationActions?.navigateTo(Screen.TripSettingsDates) },
                  enabled = isLoggedIn,
                  modifier = Modifier.testTag(CurrentTripScreenTestTags.CREATE_TRIP_BUTTON)) {
                    Text(
                        text = stringResource(R.string.where_starting),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                  }
            }
      })
}
