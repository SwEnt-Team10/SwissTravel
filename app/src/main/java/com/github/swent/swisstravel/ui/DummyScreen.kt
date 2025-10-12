package com.github.swent.swisstravel.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
fun DummyScreen(navigationActions: NavigationActions? = null) {

  Scaffold(
      bottomBar = {
        BottomNavigationMenu(
            selectedTab = Tab.MyTrips,
            onTabSelected = { tab -> navigationActions?.navigateTo(tab.destination) },
            modifier = Modifier.testTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU))
      },
      content = { pd ->
        Column(
            modifier = Modifier.fillMaxSize().padding(pd),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center) {
              Text(
                  modifier = Modifier.testTag(DummyScreenTestTags.TEMPORARY_TEST_TAG),
                  text = "Dummy Screen",
                  fontSize = 24.sp)

              Button(onClick = { navigationActions?.navigateTo(Screen.TripSettings1) }) {
                Text("Create trip")
              }
            }
      })
}
