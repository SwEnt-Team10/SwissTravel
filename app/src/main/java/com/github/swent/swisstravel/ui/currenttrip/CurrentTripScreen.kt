package com.github.swent.swisstravel.ui.currenttrip

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.ui.navigation.NavigationActions
import com.github.swent.swisstravel.ui.navigation.Screen
import com.github.swent.swisstravel.ui.trip.tripinfos.TripInfoViewModel
import com.github.swent.swisstravel.ui.trip.tripinfos.DailyViewScreen
import com.github.swent.swisstravel.ui.trip.tripinfos.DailyViewScreenCallbacks
import com.github.swent.swisstravel.ui.trips.MyTripsViewModel

/** Object for test tags */
object CurrentTripScreenTestTags {
  const val CREATE_TRIP_BUTTON = "createTripButton"
  const val CREATE_TRIP_TEXT = "createTripText"
}

/**
 * Screen to display the current trip. If not set, it will display a button to create a new trip.
 *
 * @param navigationActions Navigation actions to navigate to other screens.
 * @param isLoggedIn Whether the user is logged in.
 * @param myTripsViewModel View model for my trips.
 */
@OptIn(ExperimentalMaterial3Api::class)
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
  val tripInfoViewModel: TripInfoViewModel = viewModel()
  val tripInfoState = tripInfoViewModel.uiState.collectAsState()

  if (currentTrip != null) {
      PullToRefreshBox(
          isRefreshing = tripInfoState.value.isLoading,
          onRefresh = { tripInfoViewModel.loadTripInfo(currentTrip.uid) }) {
            DailyViewScreen(
                currentTrip.uid,
                tripInfoViewModel = tripInfoViewModel,
                isOnCurrentTripScreen = true,
                callbacks =
                    DailyViewScreenCallbacks(
                        onEditTrip = { navigationActions?.navigateToEditTrip(currentTrip.uid) },
                        onSwipeActivities = { navigationActions?.navigateTo(Screen.SwipeActivities) },
                        onLikedActivities = {
                          navigationActions?.navigateTo(Screen.LikedActivities)
                        }))
          }
  } else {
    PullToRefreshBox(
        isRefreshing = tripsState.value.isLoading,
        onRefresh = { myTripsViewModel.refreshUIState() }) {
          Scaffold(
              content = { pd ->
                Column(
                    modifier =
                        Modifier.fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(pd)
                            .padding(dimensionResource(R.dimen.current_trip_column_padding)),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally) {
                      Icon(
                          painter = painterResource(R.drawable.no_trip_sign),
                          contentDescription = null,
                          modifier =
                              Modifier.padding(
                                      bottom =
                                          dimensionResource(
                                              R.dimen.current_trip_icon_bottom_padding))
                                  .fillMaxWidth(),
                          tint = MaterialTheme.colorScheme.primary)
                      Text(
                          text = stringResource(R.string.create_trip),
                          style = MaterialTheme.typography.headlineMedium,
                          color = MaterialTheme.colorScheme.onBackground,
                          modifier = Modifier.testTag(CurrentTripScreenTestTags.CREATE_TRIP_TEXT))
                      Spacer(modifier = Modifier.height(dimensionResource(R.dimen.small_spacer)))
                      Text(
                          text = "Start planning your next adventure!",
                          style = MaterialTheme.typography.bodyLarge,
                          color = MaterialTheme.colorScheme.onSurfaceVariant)
                      Spacer(
                          modifier =
                              Modifier.height(dimensionResource(R.dimen.medium_large_spacer)))
                      // Create a new trip
                      Button(
                          onClick = { navigationActions?.navigateTo(Screen.TripSettingsDates) },
                          enabled = isLoggedIn,
                          modifier =
                              Modifier.height(
                                      dimensionResource(R.dimen.current_trip_button_height))
                                  .testTag(CurrentTripScreenTestTags.CREATE_TRIP_BUTTON)) {
                            Text(
                                text = stringResource(R.string.when_travelling),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                          }
                    }
              })
        }
  }
}
