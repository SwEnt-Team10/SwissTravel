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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.ui.theme.navBarItemBackground

/** Heavily inspired from the B3 of the SwEnt course at EPFL */

/** This class stores the bottom navigation tabs */
sealed class Tab(
    val name: Int,
    val destination: Screen,
    val iconSelected: ImageVector,
    val iconNotSelected: ImageVector
) {

  object MyTrips :
      Tab(
          R.string.my_trips_tab,
          Screen.MyTrips,
          Icons.AutoMirrored.Filled.MenuOpen,
          Icons.Filled.Menu)

  object CurrentTrip :
      Tab(
          R.string.current_trip_tab,
          Screen.CurrentTrip,
          Icons.Filled.LocationOn,
          Icons.Outlined.LocationOn)

  object Profile :
      Tab(R.string.profile_tab, Screen.Profile, Icons.Filled.Person, Icons.Outlined.Person)
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
      val tabName = stringResource(tab.name)
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
                  imageVector = tab.iconSelected,
                  contentDescription = tabName,
                  tint = MaterialTheme.colorScheme.onPrimary)
            } else {
              Icon(
                  imageVector = tab.iconNotSelected,
                  contentDescription = tabName,
                  tint = MaterialTheme.colorScheme.onPrimary)
            }
          },
          label = { Text(tabName, color = MaterialTheme.colorScheme.onPrimary) },
          alwaysShowLabel = true)
    }
  }
}
