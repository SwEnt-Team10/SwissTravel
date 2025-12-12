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

class MapScreenTest {

  @get:Rule val composeRule = createComposeRule()

  // Grant location permissions for the tests
  @get:Rule
  val grantPermissionRule: GrantPermissionRule =
      GrantPermissionRule.grant(
          Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

  /** Check the map is displayed with two locations */
  @Test
  fun mapIsDisplayed() {
    composeRule.setContent {
      MapScreen(
          locations =
              listOf(
                  Location(Coordinate(46.0, 6.6), "A"),
                  Location(Coordinate(46.51, 6.61), "B"),
              ),
          drawRoute = true)
    }
    composeRule.onNodeWithTag(MapScreenTestTags.MAP).assertIsDisplayed()
  }

  /** Map renders with a single location (camera path branches) */
  @Test
  fun mapIsDisplayed_withSingleLocation() {
    composeRule.setContent {
      MapScreen(locations = listOf(Location(Coordinate(46.2, 6.7), "OnlyOne")), drawRoute = false)
    }
    composeRule.onNodeWithTag(MapScreenTestTags.MAP).assertIsDisplayed()
  }

  /** Check that photo pins are displayed on the map Note: AI did the test */
  @Test
  fun photoPinsAreDisplayed() {
    // Use a valid empty URI for the test
    val photoUri = android.net.Uri.EMPTY
    val pinName = "Test Photo Pin"
    val photoLocation = Location(Coordinate(46.0, 6.6), pinName)

    composeRule.setContent {
      MapScreen(
          // FIX: Pass the location here so the camera centers on it (via FitCamera logic)
          locations = listOf(photoLocation),
          drawRoute = false,
          photoEntries = listOf(photoUri to photoLocation))
    }

    // Wait for the map and camera to stabilize
    composeRule.waitForIdle()

    // The component should now be visible on screen
    composeRule.onNode(hasContentDescription(pinName)).assertIsDisplayed()
  }

  /** ViewModel flag toggles as expected */
  @Test
  fun setRouteRenderedUpdatesState() {
    val vm = MapScreenViewModel()
    assertTrue(!vm.uiState.value.isRouteRendered)

    vm.setRouteRendered(true)
    assertTrue(vm.uiState.value.isRouteRendered)

    vm.setRouteRendered(false)
    assertTrue(!vm.uiState.value.isRouteRendered)
  }

  /** updateLocations modifies the list in uiState */
  @Test
  fun updateLocationsChangesUiState() {
    val vm = MapScreenViewModel()
    val points =
        listOf(
            com.mapbox.geojson.Point.fromLngLat(6.6, 46.5),
            com.mapbox.geojson.Point.fromLngLat(6.7, 46.6))
    vm.updateLocations(points)
    assertTrue(vm.uiState.value.locationsList == points)
  }

  /** attachMapObjects wires nav + route API */
  @Test
  fun attachMapObjectsUpdatesUiState() {
    val vm = MapScreenViewModel()
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

  /** When permission is granted, the follow-puck button shows */
  @Test
  fun followPuckButton_visible_whenPermissionGranted() {
    val vm = MapScreenViewModel().apply { setPermissionGranted(true) }
    composeRule.setContent {
      MapScreen(
          locations =
              listOf(
                  Location(Coordinate(46.0, 6.6), "A"),
                  Location(Coordinate(46.51, 6.61), "B"),
              ),
          drawRoute = true,
          viewModel = vm)
    }
    val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    val cd = context.getString(R.string.allow_location)
    composeRule.onNode(hasContentDescription(cd)).assertIsDisplayed()
  }

  /** Follow-puck button is clickable (smoke: no crash) */
  @Test
  fun followPuckButton_isClickable() {
    val vm = MapScreenViewModel().apply { setPermissionGranted(true) }
    composeRule.setContent {
      MapScreen(
          locations =
              listOf(
                  Location(Coordinate(46.0, 6.6), "A"),
                  Location(Coordinate(46.51, 6.61), "B"),
              ),
          drawRoute = true,
          viewModel = vm)
    }
    val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    val cd = context.getString(R.string.allow_location)
    composeRule.onNode(hasContentDescription(cd)).assertIsDisplayed().performClick()
  }

  /** Toggling drawRoute from true -> false clears the route (smoke: no crash / renders) */
  @Test
  fun toggling_drawRoute_doesNotCrash_andRenders() {
    val vm = MapScreenViewModel().apply { setPermissionGranted(true) }
    var drawRoute by mutableStateOf(true)
    composeRule.setContent {
      MapScreen(
          locations =
              listOf(
                  Location(Coordinate(46.0, 6.6), "A"),
                  Location(Coordinate(46.51, 6.61), "B"),
              ),
          drawRoute = drawRoute,
          viewModel = vm)
    }

    // Map is visible initially
    composeRule.onNodeWithTag(MapScreenTestTags.MAP).assertIsDisplayed()

    // Toggle to pin-only mode; ensures our clear logic path is exercised
    composeRule.runOnUiThread { drawRoute = false }
    composeRule.waitForIdle()

    // Still rendering fine
    composeRule.onNodeWithTag(MapScreenTestTags.MAP).assertIsDisplayed()
  }

  /** Rapid recomposition: change locations & drawRoute to ensure layer init is idempotent */
  @Test
  fun rapidRecomposition_layerInitIsStable() {
    val vm = MapScreenViewModel().apply { setPermissionGranted(true) }

    var drawRoute by mutableStateOf(true)
    var locs by
        mutableStateOf(
            listOf(
                Location(Coordinate(46.0, 6.6), "A"),
                Location(Coordinate(46.51, 6.61), "B"),
            ))

    composeRule.setContent { MapScreen(locations = locs, drawRoute = drawRoute, viewModel = vm) }

    // Flip a few times to simulate user stepping through trip steps
    repeat(3) {
      composeRule.runOnUiThread {
        drawRoute = !drawRoute
        locs =
            if (drawRoute) {
              listOf(
                  Location(Coordinate(46.0, 6.6), "A"),
                  Location(Coordinate(46.51, 6.61), "B"),
                  Location(Coordinate(46.8, 6.7), "C"),
              )
            } else {
              listOf(Location(Coordinate(46.2, 6.7), "OnlyOne"))
            }
      }
      composeRule.waitForIdle()
      composeRule.onNodeWithTag(MapScreenTestTags.MAP).assertIsDisplayed()
    }
  }
}
