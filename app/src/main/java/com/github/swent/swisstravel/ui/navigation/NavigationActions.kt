package com.github.swent.swisstravel.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.github.swent.swisstravel.ui.trip.tripinfos.TripInfoViewModel
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

  object ProfileSettings : Screen(route = "profile_settings", name = "Profile Settings")

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

  data class DailyView(val uid: String) : Screen(route = "daily_view/${uid}", name = "Daily View") {
    companion object {
      const val route = "daily_view/{uid}"
      const val name = "Daily View"
    }
  }

  object ActivityInfo : Screen("activityInfo/{uid}", name = "Activity Infos") {
    fun route(uid: String) = "activityInfo/$uid"
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

  object FriendsList : Screen(route = "friends_list", name = "Friends")

  object AddFriend : Screen(route = "add_friend", name = "Add Friend")

  object SwipeActivities : Screen(route = "swipe_activities", name = "Swipe Activities")

  object LikedActivities : Screen(route = "liked_activities", name = "Liked Activities")

  data class FriendProfile(val uid: String) :
      Screen(route = "profile/${uid}", name = "Friend Profile") {
    companion object {
      const val route = "profile/{uid}"
      const val name = "Friend Profile"
    }
  }

  data class AddPhotos(val tripId: String) :
      Screen(route = "add_photos/${tripId}", name = "Add Photos") {
    companion object {
      const val route = "add_photos/{tripId}"
    }
  }
  data class EditPhotos(val tripId: String) :
  Screen(route = "edit_photos/${tripId}", name = "Edit Photos") {
    companion object {
      const val route = "edit_photos/{tripId}"
    }
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

  fun navigateToActivityInfo(tripId: String) {
    navController.navigate(Screen.ActivityInfo.route(tripId))
  }

  fun goBackToTripInfo(tripId: String): Boolean { // TODO this is unused ?
    // trip_info/<real-uid>, not the pattern
    val route = Screen.DailyView(tripId).route // "daily_view/$tripId"
    return navController.popBackStack(route = route, inclusive = false)
  }

  @Composable
  fun tripInfoViewModel(navController: NavHostController): TripInfoViewModel {
    val currentEntry by navController.currentBackStackEntryAsState()

    // Scope the VM to the TripInfo navigation graph
    val parentEntry =
        remember(currentEntry) {
          runCatching { navController.getBackStackEntry(Screen.TripInfo.name) }.getOrNull()
        }

    return if (parentEntry != null) {
      viewModel(parentEntry) // shared between all composables in TripInfo graph
    } else {
      viewModel() // fallback, should rarely happen
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

  /**
   * Resets the navigation graph by clearing the entire back stack.
   *
   * @param destination the destination to reset the navigation graph to.
   */
  fun resetTo(destination: Screen) {
    navController.popBackStack(destination.route, inclusive = true)
  }
}
