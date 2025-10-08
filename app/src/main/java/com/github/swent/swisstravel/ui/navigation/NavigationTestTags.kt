package com.github.swent.swisstravel.ui.navigation

/** Heavily inspired from the B3 of the SwEnt course at EPFL */
object NavigationTestTags {
  const val BOTTOM_NAVIGATION_MENU = "BottomNavigationMenu"
  const val PROFILE_TAB = "ProfileTab"
  const val MY_TRIPS_TAB = "MyTripsTab"
  const val CURRENT_TRIP_TAB = "CurrentTripTab"

  fun getTestTag(tab: Tab): String =
      when (tab) {
        Tab.Profile -> PROFILE_TAB
        Tab.MyTrips -> MY_TRIPS_TAB
        Tab.CurrentTrip -> CURRENT_TRIP_TAB
      }
}
