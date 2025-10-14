package com.github.swent.swisstravel.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.sp
import com.github.swent.swisstravel.ui.navigation.BottomNavigationMenu
import com.github.swent.swisstravel.ui.navigation.NavigationActions
import com.github.swent.swisstravel.ui.navigation.NavigationTestTags
import com.github.swent.swisstravel.ui.navigation.Screen
import com.github.swent.swisstravel.ui.navigation.Tab

object DummyScreenTestTags {
  const val TEMPORARY_TEST_TAG = "temporaryTestTagDummy"
}

@Composable
fun DummyScreen(
    navigationActions: NavigationActions? = null,
    viewModel: DummyScreenViewModel = DummyScreenViewModel()
) {
  val trip by viewModel.trip.collectAsState()

  Scaffold(
      bottomBar = {
        BottomNavigationMenu(
            selectedTab = Tab.MyTrips,
            onTabSelected = { tab -> navigationActions?.navigateTo(tab.destination) },
            modifier = Modifier.testTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU))
      },
      content = { pd ->
        Box(modifier = Modifier.fillMaxSize().padding(pd), contentAlignment = Alignment.Center) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                modifier = Modifier.testTag(DummyScreenTestTags.TEMPORARY_TEST_TAG),
                text = "Dummy Screen",
                fontSize = 24.sp)
            Button(
                onClick = { viewModel.addTripModel() },
            ) {
              Text("Testing adding a trip")
            }
            Button(
                onClick = { viewModel.getTripModel("testID") },
            ) {
              Text("Testing getting a trip")
            }
            trip?.let {
              Text("Trip UID: ${it.uid}")
              Text("Trip Name: ${it.name}")
              Text("Trip Owner ID: ${it.ownerId}")
            }
            Button(onClick = { navigationActions?.navigateTo(Screen.TripSettings1) }) {
              Text("Create trip")
            }
          }
        }
      })
}
