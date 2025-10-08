package com.github.swent.swisstravel.ui.currenttrip

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import com.github.swent.swisstravel.ui.navigation.Tab

object CurrentTripScreenTestTags {
  // TODO remove when done implementing this screen and change the tests in NavigationTest
  // accordingly
  const val TEMPORARY_TEST_TAG = "temporaryTestTag"

  /* Real test tags */

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
        Box(modifier = Modifier.fillMaxSize().padding(pd), contentAlignment = Alignment.Center) {
          Text(
              modifier = Modifier.testTag(CurrentTripScreenTestTags.TEMPORARY_TEST_TAG),
              text = "Current Trip",
              fontSize = 24.sp)
        }
      })
}
