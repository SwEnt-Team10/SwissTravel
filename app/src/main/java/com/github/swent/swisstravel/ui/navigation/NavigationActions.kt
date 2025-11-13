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
  object Landing : Screen(route = "landing", name = "Landing")

  object SignUp : Screen(route = "signup", name = "Sign up")

  object Auth : Screen(route = "auth", name = "Authentication")

  object Profile : Screen(route = "profile", name = "Profile", isTopLevelDestination = true)

  object MyTrips : Screen(route = "my_trips", name = "My trips", isTopLevelDestination = true)

  object SetCurrentTrip : Screen(route = "set_current_trip", name = "Set current trip")

  object PastTrips : Screen(route = "past_trips", name = "Past trips")

  object CurrentTrip :
      Screen(route = "current_trip", name = "Current trip", isTopLevelDestination = true)

  object TripSettingsDates : Screen(route = "trip_settings_dates", name = "Trip Settings Dates")

  object TripSettingsTravelers :
      Screen(route = "trip_settings_travelers", name = "Trip Settings Travelers")

  object TripSettingsPreferences :
      Screen(route = "trip_settings_preferences", name = "Trip Settings Preferences")

  data class TripInfo(val uid: String) : Screen(route = "trip_info/${uid}", name = "Trip Info") {
    companion object {
      const val route = "trip_info/{uid}"
      const val name = "Trip Info"
    }
  }

  object TripSettingsArrivalDeparture :
      Screen(route = "arrival_departure", name = "Arrival_Departure")

  object TripSettingsFirstDestination :
      Screen(route = "first_destination", name = "First destination")

  object EditTrip : Screen(route = "edit_trip/{tripId}", name = "Edit trip") {
    fun createRoute(tripId: String) = "edit_trip/${URLEncoder.encode(tripId, "UTF-8")}"
  }

  object TripSummary : Screen(route = "trip_summary", name = "Trip Summary")

  object Loading : Screen(route = "loading", name = "Loading")

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
   * @param clearBackStack whether to clear the back stack
   */
  fun navigateTo(destination: Screen, clearBackStack: Boolean = false) {
    /* if the destination is the same as the current route, do nothing */
    if (destination.isTopLevelDestination && currentRoute() == destination.route) {
      return
    }

    navController.navigate(destination.route) {
      if (clearBackStack) {
        // Pop the entire back stack up to the very start of the graph.
        popUpTo(navController.graph.findStartDestination().id) {
          inclusive = true // This removes the start destination as well, clearing the stack.
        }
      } else {
        if (destination.isTopLevelDestination) {
          // Pop up to start of the graph to avoid large stacks
          popUpTo(destination.route) { inclusive = true }
        }
        restoreState = true
      }
      launchSingleTop = true
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
