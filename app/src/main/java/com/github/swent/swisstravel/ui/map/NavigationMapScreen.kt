package com.github.swent.swisstravel.ui.map

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.swent.swisstravel.model.trip.Location
import com.mapbox.geojson.Point
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineApiOptions
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineViewOptions

object NavigationMapScreenTestTags {
  const val MAP = "map"
}

/**
 * Composable that displays a Mapbox map with navigation routes based on provided locations.
 *
 * @param locations List of Location objects representing the points to be displayed on the map.
 */
@Composable
fun NavigationMapScreen(
    locations: List<Location>,
    viewModel: NavigationMapViewModel = viewModel()
) {
  val context = LocalContext.current

  val appCtx = context.applicationContext
  val mapboxNavigation = remember {
    if (MapboxNavigationProvider.isCreated()) MapboxNavigationProvider.retrieve()
    else MapboxNavigationProvider.create(NavigationOptions.Builder(appCtx).build())
  }
  val routeLineApi = remember { MapboxRouteLineApi(MapboxRouteLineApiOptions.Builder().build()) }
  val routeLineViewOptions = remember { MapboxRouteLineViewOptions.Builder(context).build() }
  val routeLineView = remember { MapboxRouteLineView(routeLineViewOptions) }

  LaunchedEffect(Unit) { viewModel.attachMapObjects(mapboxNavigation, routeLineApi) }
  LaunchedEffect(locations) { viewModel.updateLocations(locationsAsPoints(locations)) }

  val ui by viewModel.uiState.collectAsState()
  val mapViewportState = rememberMapViewportState()

  var styleReady by remember { mutableStateOf(false) }

  MapboxMap(
      modifier = Modifier.fillMaxSize().testTag(NavigationMapScreenTestTags.MAP),
      mapViewportState = mapViewportState) {
        MapEffect(Unit) { mapView ->
          mapView.mapboxMap.getStyle { style ->
            routeLineView.initializeLayers(style)
            styleReady = true
          }
        }

        MapEffect(ui.locationsList) {
          val locations = ui.locationsList
          if (locations.isNotEmpty()) {
            val cameraOptions =
                mapViewportState.cameraForCoordinates(
                    locations, coordinatesPadding = EdgeInsets(100.0, 100.0, 100.0, 100.0))
            mapViewportState.setCameraOptions(cameraOptions)
          }
        }

        // Re-render whenever routes change AND style is ready
        val renderKey by viewModel.routeRenderTick.collectAsState()
        MapEffect(styleReady to renderKey) { mapView ->
          if (!styleReady) return@MapEffect
          val api = ui.routeLineApi ?: return@MapEffect
          mapView.mapboxMap.getStyle { style ->
            api.getRouteDrawData { drawData ->
              routeLineView.renderRouteDrawData(style, drawData)
              viewModel.setRouteRendered(true)
            }
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

/** Convert a list of Location to a list of Mapbox Points */
private fun locationsAsPoints(locations: List<Location>) =
    locations.map { Point.fromLngLat(it.coordinate.longitude, it.coordinate.latitude) }
