package com.github.swent.swisstravel.ui.navigation

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
  // TODO change isTopLevelDestination
}

class NavigationActions {}
