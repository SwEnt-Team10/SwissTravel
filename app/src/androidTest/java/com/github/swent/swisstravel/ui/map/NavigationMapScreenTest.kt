package com.github.swent.swisstravel.ui.map

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ApplicationProvider
import com.github.swent.swisstravel.model.trip.Coordinate
import com.github.swent.swisstravel.model.trip.Location
import junit.framework.TestCase.assertTrue
import org.junit.Rule
import org.junit.Test

class NavigationMapScreenTest {

  @get:Rule val composeRule = createComposeRule()

  /** Check the map is displayed */
  @Test
  fun mapIsDisplayed() {
    composeRule.setContent {
      NavigationMapScreen(
          locations =
              listOf(
                  Location(Coordinate(46.0, 6.6), "A"),
                  Location(Coordinate(46.51, 6.61), "B"),
              ))
    }

    composeRule.onNodeWithTag(NavigationMapScreenTestTags.MAP).assertIsDisplayed()
  }

  /** Check the VM updates the state correctly */
  @Test
  fun setRouteRenderedUpdatesState() {
    val vm = NavigationMapViewModel()
    assertTrue(!vm.uiState.value.isRouteRendered)

    vm.setRouteRendered(true)
    assertTrue(vm.uiState.value.isRouteRendered)

    vm.setRouteRendered(false)
    assertTrue(!vm.uiState.value.isRouteRendered)
  }

  /** Check 'updateLocations' modifies the list in the uiState */
  @Test
  fun updateLocationsChangesUiState() {
    val vm = NavigationMapViewModel()
    val points =
        listOf(
            com.mapbox.geojson.Point.fromLngLat(6.6, 46.5),
            com.mapbox.geojson.Point.fromLngLat(6.7, 46.6))

    vm.updateLocations(points)
    assertTrue(vm.uiState.value.locationsList == points)
  }

  /** Check 'attachMapObjects' configures the MapboxNavigation and the RouteLineApi correctly */
  @Test
  fun attachMapObjectsUpdatesUiState() {
    val vm = NavigationMapViewModel()
    val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    val nav =
        com.mapbox.navigation.core.MapboxNavigationProvider.create(
            com.mapbox.navigation.base.options.NavigationOptions.Builder(context).build())
    val api =
        com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi(
            com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineApiOptions.Builder()
                .build())

    vm.attachMapObjects(nav, api)

    assertTrue(vm.uiState.value.mapboxNavigation === nav)
    assertTrue(vm.uiState.value.routeLineApi === api)
  }
}
