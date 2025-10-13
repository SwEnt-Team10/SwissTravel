package com.github.swent.swisstravel.ui.map

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.github.swent.swisstravel.ui.navigation.NavigationActions
import com.github.swent.swisstravel.ui.navigation.Screen
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.directions.session.RoutesUpdatedResult
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineApiOptions
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineViewOptions
import org.w3c.dom.Text

object NavigationMapScreenTestTags {
  const val PERMISSION_BUTTON = "permissionButton"
  const val BOTTOM_SHEET = "bottomSheet"
  const val ENTER_BUTTON = "enterButton"
  const val EXIT_BUTTON = "exitButton"
  const val MAP = "map"
}

@Composable
fun EnterMapButton(navigationActions: NavigationActions?) {
  Box(contentAlignment = Alignment.TopCenter) {
    Button(
        onClick = { navigationActions?.navigateTo(Screen.SelectedTripMap) },
        modifier = Modifier.testTag(NavigationMapScreenTestTags.ENTER_BUTTON)) {
          // TODO : modify this to an "extend" icon when the map preview is implemented
          Text("Enter Map")
        }
  }
}

@Composable
fun NavigationMapScreen(navigationActions: NavigationActions) {
  Button(
      onClick = { navigationActions.navigateTo(Screen.CurrentTrip) },
      modifier = Modifier.testTag(NavigationMapScreenTestTags.EXIT_BUTTON)) {
        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Exit Map Icon")
      }
  NavigationMap()
}

/* TODO : delete if we choose to implement view A (see Figma)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheet(navigationActions: NavigationActions) {
  val sheetState = rememberBottomSheetScaffoldState()

  BottomSheetScaffold(
      scaffoldState = sheetState,
      sheetContent = {
        LazyColumn(
            modifier =
                Modifier.fillMaxWidth()
                    .height(300.dp)
                    .testTag(
                        NavigationMapScreenTestTags
                            .BOTTOM_SHEET) // maximum height of the bottom sheet
            ) { // sample items list
              item {
                OutlinedTextField(
                    value = "TODO: select location",
                    onValueChange = {},
                    modifier = Modifier.padding(16.dp))
              }
            }
      },
      // height of the bottom sheet when collapsed
      sheetPeekHeight = 64.dp) {
        // main content of the screen
        Box(modifier = Modifier.fillMaxSize()) {
          NavigationMap(navigationActions = navigationActions)
        }
      }
} */

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
@Composable
fun NavigationMap() {
  val context = LocalContext.current

  // get a route line object (to access methods for data)
  val routeLineApiOptions = MapboxRouteLineApiOptions.Builder().build()
  val routeLineApi = MapboxRouteLineApi(routeLineApiOptions)

  // get a route line view object (to display the route)
  val routeLineViewOptions = MapboxRouteLineViewOptions.Builder(context).build()
  val routeLineView = MapboxRouteLineView(routeLineViewOptions)

  // create the main map component (a "mapboxNavigation" instance)
  val mapboxNavigation = remember {
    MapboxNavigationProvider.create(NavigationOptions.Builder(context).build())
  }

  // TODO : change these hardcoded points to variables

  // Hardcoded points for testing
  val EPFL_IC = com.mapbox.geojson.Point.fromLngLat(6.563349085567107, 46.51823826885176)
  val ZERMATT = com.mapbox.geojson.Point.fromLngLat(7.747, 46.019)
  val OLYMPIC_MUSEUM = com.mapbox.geojson.Point.fromLngLat(6.6339, 46.5086)
  val CHUV = com.mapbox.geojson.Point.fromLngLat(6.6209, 46.5197)
  // ================================

  // Hardcoded trips for testing
  val EPFL_ZERMATT = listOf(EPFL_IC, ZERMATT)
  val EPFL_OLYMPIC = listOf(EPFL_IC, OLYMPIC_MUSEUM)
  val EPFL_CHUV_OLYMPIC = listOf(EPFL_IC, CHUV, OLYMPIC_MUSEUM)
  // ================================

  // get the possible routes from origin to destination
  val routeOptions =
      RouteOptions.builder()
          .applyDefaultNavigationOptions()
          .coordinatesList(EPFL_OLYMPIC) // can add intermediary points here
          .build()

  val callback =
      object : NavigationRouterCallback {
        override fun onCanceled(routeOptions: RouteOptions, routerOrigin: String) {}

        override fun onFailure(reasons: List<RouterFailure>, routeOptions: RouteOptions) {}

        override fun onRoutesReady(routes: List<NavigationRoute>, routerOrigin: String) {
          mapboxNavigation.setNavigationRoutes(routes)
        }
      }

  // create a map
  val mapViewportState = rememberMapViewportState()
  MapboxMap(modifier = Modifier.fillMaxSize(), mapViewportState = mapViewportState) {
    MapEffect(Unit) { mapView ->

      // set the initial location of the map
      val initialLocation = CameraOptions.Builder().center(EPFL_IC).zoom(14.0).build()
      mapView.mapboxMap.setCamera(initialLocation)

      // observer to update the route on the map when routes change
      val routesObserver =
          object : RoutesObserver {
            override fun onRoutesChanged(result: RoutesUpdatedResult) {
              val alternativesMetadata =
                  mapboxNavigation.getAlternativeMetadataFor(result.navigationRoutes)
              routeLineApi.setNavigationRoutes(result.navigationRoutes, alternativesMetadata) {
                  routeDrawData ->
                mapView.getMapboxMap().getStyle()?.let { style ->
                  routeLineView.renderRouteDrawData(style, routeDrawData)
                }
              }
            }
          }

      // add the route observer to the map component
      mapboxNavigation.registerRoutesObserver(routesObserver)
    }
  }

  mapboxNavigation.requestRoutes(routeOptions, callback)

  // create a manageable lifecycle
  DisposableEffect(Unit) { onDispose { MapboxNavigationProvider.destroy() } }
}
