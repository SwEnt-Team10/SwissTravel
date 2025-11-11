package com.github.swent.swisstravel.ui.map

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.model.trip.Location
import com.mapbox.geojson.Point
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.plugin.PuckBearing
import com.mapbox.maps.plugin.locationcomponent.createDefault2DPuck
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineApiOptions
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineViewOptions

object MapScreenTestTags {
  const val MAP = "map"
}

/**
 * Composable that displays a Mapbox map with navigation routes based on provided locations.
 *
 * @param locations List of Location objects representing the points to be displayed on the map.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(locations: List<Location>, viewModel: MapScreenViewModel = viewModel()) {
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

  val permissionLauncher =
      rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { res
        ->
        val granted =
            res[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                res[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        viewModel.setPermissionGranted(granted)
      }

  LaunchedEffect(Unit) {
    permissionLauncher.launch(
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
  }

  var lastFitHash by remember { mutableStateOf<Int?>(null) }

  Scaffold { padding ->
    Box(Modifier.fillMaxSize().padding(padding)) {
      MapboxMap(
          modifier = Modifier.fillMaxSize().testTag(MapScreenTestTags.MAP),
          mapViewportState = mapViewportState) {
            // Init layers
            MapEffect(Unit) { mapView ->
              mapView.mapboxMap.getStyle { style ->
                routeLineView.initializeLayers(style)
                styleReady = true
              }
            }

            MapEffect(ui.locationsList) {
              val pts = ui.locationsList
              if (pts.isNotEmpty()) {
                val h = pts.hashCode()
                if (lastFitHash != h) {
                  lastFitHash = h
                  val cam =
                      mapViewportState.cameraForCoordinates(
                          pts, coordinatesPadding = EdgeInsets(100.0, 100.0, 100.0, 100.0))
                  mapViewportState.setCameraOptions(cam)
                }
              }
            }

            // Enable/disable location puck based on permission
            MapEffect(ui.permissionGranted) { mapView ->
              mapView.location.updateSettings {
                enabled = ui.permissionGranted
                if (ui.permissionGranted) {
                  locationPuck = createDefault2DPuck(withBearing = true)
                  puckBearing = PuckBearing.COURSE
                  puckBearingEnabled = true
                }
              }
            }

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

      if (ui.permissionGranted) {
        IconButton(
            onClick = { mapViewportState.transitionToFollowPuckState() },
            modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)) {
              Icon(
                  imageVector = Icons.Filled.LocationOn,
                  contentDescription = stringResource(R.string.allow_location),
                  tint = MaterialTheme.colorScheme.onBackground)
            }
      }

      if (!ui.permissionGranted) {
        Text(
            text = stringResource(R.string.location_required_to_display),
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp))
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
