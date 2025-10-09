package com.github.swent.swisstravel.ui.navigation

import androidx.navigation.NavHostController

/** Heavily inspired from the B3 of the SwEnt course at EPFL */

/**
 * This class stores all the screens in the app
 *
 * @param route the route of the screen
 * @param name the name of the screen
 * @param isTopLevelDestination whether the screen is a top level destination or not
 */
sealed class Screen(
    val route: String,
    val name: String,
    val isTopLevelDestination: Boolean = false
) {
  object Auth : Screen(route = "auth", name = "Authentication")

  object Profile : Screen(route = "profile", name = "Profile", isTopLevelDestination = true)

  object MyTrips : Screen(route = "my_trips", name = "My trips", isTopLevelDestination = true)

  object CurrentTrip :
      Screen(route = "current_trip", name = "Current trip", isTopLevelDestination = true)
  // TODO change this when there is a new screen
}

/**
 * Class that manages the different navigation actions in the app
 *
 * @param navController the navigation controller
 */
class NavigationActions(
    private val navController: NavHostController,
) {
  /**
   * Navigate to the given destination
   *
   * @param destination the destination to navigate to
   */
  fun navigateTo(destination: Screen) {
    /* if the destination is the same as the current route, do nothing */
    if (destination.isTopLevelDestination && currentRoute() == destination.route) {
      return
    }

    navController.navigate(destination.route) {
      if (destination.isTopLevelDestination) {
        /* Pop up to the start of the graph to avoid large stacks */
        popUpTo(destination.route) {
          inclusive = true // Save state of popped destinations
        }
      }
      if (destination !is Screen.Auth) {
        restoreState = true
      }
    }
  }

  /** Navigate to the previous screen */
  fun goBack() {
    navController.popBackStack()
  }

  /** Get the current route */
  fun currentRoute(): String {
    return navController.currentDestination?.route ?: ""
  }
}
