package com.github.swent.swisstravel.ui.map

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.github.swent.swisstravel.model.trip.Location
import com.mapbox.geojson.Point
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineViewOptions

object NavigationMapScreenTestTags {
  const val ENTER_MAP_BUTTON = "enterMapButton"
  const val EXIT_BUTTON = "exitButton"
  const val MAP = "map"
}

object LocationsHardCoded {
  val EPFL_IC = Point.fromLngLat(6.563349085567107, 46.51823826885176)
  val ZERMATT = Point.fromLngLat(7.747, 46.019)
  val OLYMPIC_MUSEUM = Point.fromLngLat(6.6339, 46.5086)
  val CHUV = Point.fromLngLat(6.6209, 46.5197)
}

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
@Composable
fun NavigationMap(locations: List<Location>) {

  val context = LocalContext.current
  val viewModel =
      NavigationMapViewModel(
          application = context.applicationContext as android.app.Application, locations)

  // get a route line view object (to display the route), and the data to draw the route
  val routeLineViewOptions = MapboxRouteLineViewOptions.Builder(context).build()
  val routeLineView = MapboxRouteLineView(routeLineViewOptions)
  val routeDrawData = viewModel.uiState.collectAsState().value.routeLineDrawData

  // create a map and set the initial camera position to see the start of the route
  val mapViewportState = rememberMapViewportState()
  LaunchedEffect(Unit) {
    mapViewportState.setCameraOptions {
      center(locationsAsPoints(locations).first())
      zoom(14.0)
    }
  }

  MapboxMap(
      modifier = Modifier.fillMaxSize().testTag(NavigationMapScreenTestTags.MAP),
      mapViewportState = mapViewportState) {
        MapEffect(routeDrawData) { mapView ->
          // render route draw data provided by the ViewModel whenever available
          val drawData = routeDrawData ?: return@MapEffect
          mapView.mapboxMap.getStyle { style ->
            routeLineView.renderRouteDrawData(style, drawData)
            viewModel.setRouteRendered(true)
          }
        }
      }

  DisposableEffect(Unit) {
    onDispose {
      routeLineView.cancel()
      viewModel.setRouteRendered(false)
    }
  }
}
