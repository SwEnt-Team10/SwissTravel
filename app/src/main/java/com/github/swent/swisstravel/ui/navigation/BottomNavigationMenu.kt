package com.github.swent.swisstravel.ui.navigation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag

/** This class stores the bottom navigation tabs */
sealed class Tab(val name: String, val icon: ImageVector, val destination: Screen) {
  object Profile : Tab("Profile", Icons.Outlined.Person, Screen.Profile)

  object MyTrips : Tab("My trips", Icons.Outlined.Menu, Screen.MyTrips)

  object CurrentTrip : Tab("Current trip", Icons.Outlined.LocationOn, Screen.CurrentTrip)
  // TODO Change this once there are new screens
  // TODO Add test tags each time a new tab is added
}

/* List of all the tabs in the bottom bar */
private val tabs = listOf(Tab.Profile, Tab.MyTrips, Tab.CurrentTrip)

/**
 * Composable setting up the bottom navigation bar
 *
 * @param selectedTab the tab that is currently selected
 * @param onTabSelected the callback that is called when a tab is selected
 * @param modifier the modifier to be applied to the bottom navigation bar
 */
@Composable
fun BottomNavigationMenu(
    selectedTab: Tab,
    onTabSelected: (Tab) -> Unit,
    modifier: Modifier = Modifier,
) {
  NavigationBar(
      modifier = modifier.fillMaxWidth().testTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU)) {
        tabs.forEach { tab ->
          NavigationBarItem(
              modifier = Modifier.testTag(NavigationTestTags.getTestTag(tab)),
              selected = (tab == selectedTab),
              onClick = { onTabSelected(tab) },
              icon = { Icon(imageVector = tab.icon, contentDescription = tab.name) },
              label = { Text(tab.name) },
              alwaysShowLabel = false)
        }
      }
}
