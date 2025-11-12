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
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.circleLayer
import com.mapbox.maps.extension.style.layers.getLayer
import com.mapbox.maps.extension.style.layers.properties.generated.Visibility
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.sources.getSourceAs
import com.mapbox.maps.plugin.PuckBearing
import com.mapbox.maps.plugin.locationcomponent.createDefault2DPuck
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.ui.maps.route.line.MapboxRouteLineApiExtensions.clearRouteLine
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
fun MapScreen(
    locations: List<Location>,
    drawRoute: Boolean,
    viewModel: MapScreenViewModel = viewModel()
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

  // Attach once
  LaunchedEffect(Unit) { viewModel.attachMapObjects(mapboxNavigation, routeLineApi) }
  LaunchedEffect(locations) { viewModel.updateLocations(locationsAsPoints(locations)) }

  val ui by viewModel.uiState.collectAsState()
  val mapViewportState = rememberMapViewportState()

  val PINS_SOURCE_ID = remember { "step-pins-source" }
  val PINS_LAYER_ID = remember { "step-pins-layer" }

  var styleReady by remember { mutableStateOf(false) }
  // NEW: track whether route-line layers have been initialized for the current style
  var routeLayersInitialized by remember { mutableStateOf(false) }

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

  Scaffold { padding ->
    Box(Modifier.fillMaxSize().padding(padding)) {
      MapboxMap(
          modifier = Modifier.fillMaxSize().testTag(MapScreenTestTags.MAP),
          mapViewportState = mapViewportState) {
            MapEffect(Unit) { mapView ->
              mapView.mapboxMap.getStyle { style ->
                if (!routeLayersInitialized) {
                  routeLineView.initializeLayers(style)
                  routeLayersInitialized = true
                }
                style.getSourceAs<GeoJsonSource>(PINS_SOURCE_ID)
                    ?: geoJsonSource(PINS_SOURCE_ID) {}.also(style::addSource)

                if (style.getLayer(PINS_LAYER_ID) == null) {
                  style.addLayer(
                      circleLayer(PINS_LAYER_ID, PINS_SOURCE_ID) {
                        circleRadius(6.0)
                        circleColor("#00FF00")
                        circleStrokeColor("#FFFFFF")
                        circleStrokeWidth(2.0)
                      })
                }
                styleReady = true
              }
            }

            MapEffect(styleReady to drawRoute) { mapView ->
              if (!styleReady || drawRoute) return@MapEffect
              val api = ui.routeLineApi ?: return@MapEffect
              val clearValue = api.clearRouteLine()
              mapView.mapboxMap.getStyle { style ->
                routeLineView.renderClearRouteLineValue(style, clearValue)
              }
            }

            val renderKey by viewModel.routeRenderTick.collectAsState()
            MapEffect(styleReady to drawRoute to renderKey) { mapView ->
              if (!styleReady || !drawRoute) return@MapEffect
              val api = ui.routeLineApi ?: return@MapEffect
              mapView.mapboxMap.getStyle { style ->
                api.getRouteDrawData { drawData ->
                  routeLineView.renderRouteDrawData(style, drawData)
                }
              }
            }

            MapEffect(styleReady to ui.locationsList to drawRoute) { mapView ->
              if (!styleReady) return@MapEffect
              mapView.mapboxMap.getStyle { style ->
                style
                    .getSourceAs<GeoJsonSource>(PINS_SOURCE_ID)
                    ?.featureCollection(
                        FeatureCollection.fromFeatures(
                            if (!drawRoute) ui.locationsList.map { Feature.fromGeometry(it) }
                            else emptyList()))
                style
                    .getLayer(PINS_LAYER_ID)
                    ?.visibility(if (drawRoute) Visibility.NONE else Visibility.VISIBLE)
              }
            }

            var lastFitHash by remember { mutableStateOf<Int?>(null) }
            val points =
                remember(locations) {
                  locations.map {
                    Point.fromLngLat(it.coordinate.longitude, it.coordinate.latitude)
                  }
                }

            MapEffect(points to drawRoute) {
              if (points.isEmpty()) return@MapEffect
              if (!drawRoute && points.size == 1) {
                mapViewportState.setCameraOptions(
                    CameraOptions.Builder().center(points.first()).zoom(12.0).build())
                lastFitHash = null
              } else {
                val h = points.hashCode()
                if (lastFitHash != h) {
                  lastFitHash = h
                  val cam =
                      mapViewportState.cameraForCoordinates(
                          points, coordinatesPadding = EdgeInsets(200.0, 200.0, 200.0, 200.0))
                  mapViewportState.setCameraOptions(cam)
                }
              }
            }

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
      } else {
        Text(
            text = stringResource(R.string.location_required_to_display),
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp))
      }
    }
  }
}

/** Convert a list of Location to a list of Mapbox Points */
private fun locationsAsPoints(locations: List<Location>) =
    locations.map { Point.fromLngLat(it.coordinate.longitude, it.coordinate.latitude) }
