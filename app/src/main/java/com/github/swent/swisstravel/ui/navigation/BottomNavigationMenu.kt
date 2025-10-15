package com.github.swent.swisstravel.ui.navigation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuOpen
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import com.github.swent.swisstravel.ui.theme.navBarItemBackground

/** Heavily inspired from the B3 of the SwEnt course at EPFL */

/** This class stores the bottom navigation tabs */
sealed class Tab(
    val name: String,
    val destination: Screen,
    val iconSelected: ImageVector,
    val iconNotSelected: ImageVector
) {

  object MyTrips :
      Tab("My trips", Screen.MyTrips, Icons.AutoMirrored.Filled.MenuOpen, Icons.Filled.Menu)

  object CurrentTrip :
      Tab("Current trip", Screen.CurrentTrip, Icons.Filled.LocationOn, Icons.Outlined.LocationOn)

  object Profile : Tab("Profile", Screen.Profile, Icons.Filled.Person, Icons.Outlined.Person)
}

/* List of all the tabs in the bottom bar */
private val tabs = listOf(Tab.MyTrips, Tab.CurrentTrip, Tab.Profile)

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
      modifier = modifier.fillMaxWidth().testTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU),
      containerColor = MaterialTheme.colorScheme.primary,
  ) {
    tabs.forEach { tab ->
      NavigationBarItem(
          modifier = Modifier.testTag(NavigationTestTags.getTestTag(tab)),
          selected = (tab == selectedTab),
          onClick = { onTabSelected(tab) },
          colors =
              NavigationBarItemDefaults.colors(
                  indicatorColor = navBarItemBackground,
              ),
          icon = {
            if (tab == selectedTab) {
              Icon(
                  imageVector = tab.iconSelected, contentDescription = tab.name, tint = Color.White)
            } else {
              Icon(
                  imageVector = tab.iconNotSelected,
                  contentDescription = tab.name,
                  tint = Color.White)
            }
          },
          label = { Text(tab.name, color = Color.White) },
          alwaysShowLabel = true)
    }
  }
}
