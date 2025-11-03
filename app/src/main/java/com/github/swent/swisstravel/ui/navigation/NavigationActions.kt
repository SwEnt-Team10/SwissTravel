package com.github.swent.swisstravel.ui.navigation

import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import java.net.URLEncoder

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

  object Map : Screen(route = "map", name = "Map", isTopLevelDestination = true)

  object SelectedTripMap : Screen(route = "selected_trip_map", name = "Selected trip map")

  object CurrentTrip :
      Screen(route = "current_trip", name = "Current trip", isTopLevelDestination = true)

  object TripSettings1 : Screen(route = "trip_settings_1", name = "Trip Settings 1")

  object TripSettings2 : Screen(route = "trip_settings_2", name = "Trip Settings 2")

  object TripSettings3 : Screen(route = "trip_settings_3", name = "Trip Settings 3")

  data class TripInfo(val uid: String) : Screen(route = "trip_info/${uid}", name = "Trip Info") {
    companion object {
      const val route = "trip_info/{uid}"
      const val name = "Trip Info"
    }
  }

  object TripInfoMap : Screen(route = "trip_info_map", name = "Trip Info Map")

  object TripSettingsArrivalDeparture :
      Screen(route = "arrival_departure", name = "Arrival_Departure")

  object TripSettingsFirstDestination :
      Screen(route = "first_destination", name = "First destination")

  object TripSettingsName : Screen(route = "trip_name", name = "Name your trip")

  object EditTrip : Screen(route = "edit_trip/{tripId}", name = "Edit trip") {
    fun createRoute(tripId: String) = "edit_trip/${URLEncoder.encode(tripId, "UTF-8")}"
  }

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
        // Pop up to start of the graph to avoid large stacks
        popUpTo(destination.route) { inclusive = true }
      }
      if (destination is Screen.Auth) {
        // Pop the entire back stack up to the very start of the graph.
        popUpTo(navController.graph.findStartDestination().id) {
          inclusive = true // This removes the start destination as well, clearing the stack.
        }
      } else {
        restoreState = true
      }
    }
  }

  /**
   * Navigate to the selected trip edit screen
   *
   * @param tripId the id of the trip
   */
  fun navigateToEditTrip(tripId: String) {
    navController.navigate(Screen.EditTrip.createRoute(tripId)) {
      launchSingleTop = true
      restoreState = true
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
