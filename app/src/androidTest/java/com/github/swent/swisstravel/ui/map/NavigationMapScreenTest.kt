package com.github.swent.swisstravel.ui.map

import android.Manifest
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.rule.GrantPermissionRule
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.model.trip.Coordinate
import com.github.swent.swisstravel.model.trip.Location
import junit.framework.TestCase.assertTrue
import org.junit.Rule
import org.junit.Test

class NavigationMapScreenTest {

  @get:Rule val composeRule = createComposeRule()
  @get:Rule
  val grantPermissionRule: GrantPermissionRule =
      GrantPermissionRule.grant(
          Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

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

  @Test
  fun mapIsVisible_andFollowPuckButtonIsShown_whenPermissionGranted() {
    composeRule.setContent {
      NavigationMapScreen(
          locations =
              listOf(
                  Location(Coordinate(46.0, 6.6), "A"),
                  Location(Coordinate(46.51, 6.61), "B"),
              ))
    }

    // Map surface
    composeRule.onNodeWithTag(NavigationMapScreenTestTags.MAP).assertIsDisplayed()

    val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    val cd = context.getString(R.string.allow_location)
    composeRule.onNode(hasContentDescription(cd)).assertIsDisplayed()
  }

  @Test
  fun followPuckButton_isClickable() {
    composeRule.setContent {
      NavigationMapScreen(
          locations =
              listOf(
                  Location(Coordinate(46.0, 6.6), "A"),
                  Location(Coordinate(46.51, 6.61), "B"),
              ))
    }

    val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    val cd = context.getString(R.string.allow_location)
    composeRule.onNode(hasContentDescription(cd)).assertIsDisplayed().performClick()
    // No crash is success; we can't directly assert viewport state here.
  }

  @Test
  fun dispose_resetsRouteRenderedFlag() {
    val vm = NavigationMapViewModel()

    // Pre-set the flag to true so we can observe the reset on dispose
    vm.setRouteRendered(true)

    var show by mutableStateOf(true)
    composeRule.setContent {
      if (show) {
        NavigationMapScreen(
            locations = listOf(Location(Coordinate(46.2, 6.7), "OnlyOne")), viewModel = vm)
      }
    }

    // Now remove the composable; DisposableEffect should call setRouteRendered(false)
    composeRule.runOnUiThread { show = false }
    composeRule.waitForIdle()

    assert(!vm.uiState.value.isRouteRendered)
  }
}
