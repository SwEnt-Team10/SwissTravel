package com.github.swent.swisstravel.ui.currenttrip

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.ui.navigation.BottomNavigationMenu
import com.github.swent.swisstravel.ui.navigation.NavigationActions
import com.github.swent.swisstravel.ui.navigation.NavigationTestTags
import com.github.swent.swisstravel.ui.navigation.Screen
import com.github.swent.swisstravel.ui.navigation.Tab

object CurrentTripScreenTestTags {
  const val CREATE_TRIP_BUTTON = "createTripButton"
  const val CREATE_TRIP_TEXT = "createTripText"
}

@Composable
fun CurrentTripScreen(navigationActions: NavigationActions? = null) {

  Scaffold(
      bottomBar = {
        BottomNavigationMenu(
            selectedTab = Tab.CurrentTrip,
            onTabSelected = { tab -> navigationActions?.navigateTo(tab.destination) },
            modifier = Modifier.testTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU))
      },
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
                  modifier =
                      Modifier.testTag(CurrentTripScreenTestTags.CREATE_TRIP_BUTTON)
                          .height(56.dp)
                          .width(240.dp)) {
                    Text(
                        text = stringResource(R.string.where_starting),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                  }
            }
      })
}
