package com.github.swent.swisstravel.ui.map

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
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
import com.mapbox.maps.Style
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.MapViewportState
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    locations: List<Location>,
    drawRoute: Boolean,
    viewModel: MapScreenViewModel = viewModel()
) {
  val context = LocalContext.current
  val appCtx = context.applicationContext

  // Mapbox + route line objects
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

  val pinsSourceId = remember { "step-pins-source" }
  val pinsLayerId = remember { "step-pins-layer" }

  var styleReady by remember { mutableStateOf(false) }
  var routeLayersInitialized by remember { mutableStateOf(false) }

  // Permission launcher
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
      MapContent(
          mapViewportState = mapViewportState,
          drawRoute = drawRoute,
          ui = ui,
          pinsSourceId = pinsSourceId,
          pinsLayerId = pinsLayerId,
          routeLineView = routeLineView,
          routeLayersInitialized = routeLayersInitialized,
          onRouteLayersInitialized = { routeLayersInitialized = it },
          styleReady = styleReady,
          onStyleReady = { styleReady = it },
          locations = locations)
      MapOverlays(permissionGranted = ui.permissionGranted, mapViewportState = mapViewportState)
    }
  }
}

/* --------------------------- Split-out content blocks --------------------------- */

@Composable
private fun MapContent(
    mapViewportState: MapViewportState,
    drawRoute: Boolean,
    ui: NavigationMapUIState,
    pinsSourceId: String,
    pinsLayerId: String,
    routeLineView: MapboxRouteLineView,
    routeLayersInitialized: Boolean,
    onRouteLayersInitialized: (Boolean) -> Unit,
    styleReady: Boolean,
    onStyleReady: (Boolean) -> Unit,
    locations: List<Location>
) {
  val renderKey by viewModel<MapScreenViewModel>().routeRenderTick.collectAsState()
  var lastFitHash by remember { mutableStateOf<Int?>(null) }
  val points =
      remember(locations) {
        locations.map { Point.fromLngLat(it.coordinate.longitude, it.coordinate.latitude) }
      }

  MapboxMap(
      modifier = Modifier.fillMaxSize().testTag(MapScreenTestTags.MAP),
      mapViewportState = mapViewportState) {
        // 1) Style/init
        MapEffect(Unit) { mapView ->
          mapView.mapboxMap.getStyle { style ->
            initRouteLayersOnce(
                style, routeLineView, routeLayersInitialized, onRouteLayersInitialized)
            ensurePinsSourceAndLayer(style, pinsSourceId, pinsLayerId)
            onStyleReady(true)
          }
        }

        // 2) Clear route when switching to pins
        MapEffect(styleReady to drawRoute) { mapView ->
          if (!styleReady || drawRoute) return@MapEffect
          val api = ui.routeLineApi ?: return@MapEffect
          val clearValue = api.clearRouteLine()
          mapView.mapboxMap.getStyle { style ->
            routeLineView.renderClearRouteLineValue(style, clearValue)
          }
        }

        // 3) Render route when needed
        MapEffect(styleReady to drawRoute to renderKey) { mapView ->
          if (!styleReady || !drawRoute) return@MapEffect
          val api = ui.routeLineApi ?: return@MapEffect
          mapView.mapboxMap.getStyle { style ->
            api.getRouteDrawData { drawData -> routeLineView.renderRouteDrawData(style, drawData) }
          }
        }

        // 4) Update pins source + visibility
        MapEffect(styleReady to ui.locationsList to drawRoute) { mapView ->
          if (!styleReady) return@MapEffect
          mapView.mapboxMap.getStyle { style ->
            updatePins(style, pinsSourceId, pinsLayerId, ui.locationsList, drawRoute)
          }
        }

        // 5) Camera fitting
        MapEffect(points to drawRoute) {
          fitCamera(mapViewportState, points, drawRoute, lastFitHash) { lastFitHash = it }
        }

        // 6) Location puck
        MapEffect(ui.permissionGranted) { mapView -> updatePuck(mapView, ui.permissionGranted) }
      }
}

@Composable
private fun MapOverlays(permissionGranted: Boolean, mapViewportState: MapViewportState) {
  Box(Modifier.fillMaxSize()) {
    if (permissionGranted) {
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

/* --------------------------- Small, focused helpers --------------------------- */

private fun initRouteLayersOnce(
    style: Style,
    routeLineView: MapboxRouteLineView,
    alreadyInitialized: Boolean,
    setInitialized: (Boolean) -> Unit
) {
  if (alreadyInitialized) return
  routeLineView.initializeLayers(style)
  setInitialized(true)
}

private fun ensurePinsSourceAndLayer(style: Style, pinsSourceId: String, pinsLayerId: String) {
  style.getSourceAs<GeoJsonSource>(pinsSourceId)
      ?: geoJsonSource(pinsSourceId) {}.also(style::addSource)
  if (style.getLayer(pinsLayerId) == null) {
    style.addLayer(
        circleLayer(pinsLayerId, pinsSourceId) {
          circleRadius(6.0)
          circleColor("#00FF00") // green
          circleStrokeColor("#FFFFFF")
          circleStrokeWidth(2.0)
        })
  }
}

private fun updatePins(
    style: Style,
    pinsSourceId: String,
    pinsLayerId: String,
    locationsList: List<Point>,
    drawRoute: Boolean
) {
  style
      .getSourceAs<GeoJsonSource>(pinsSourceId)
      ?.featureCollection(
          FeatureCollection.fromFeatures(
              if (!drawRoute) locationsList.map { Feature.fromGeometry(it) } else emptyList()))
  style.getLayer(pinsLayerId)?.visibility(if (drawRoute) Visibility.NONE else Visibility.VISIBLE)
}

private suspend fun fitCamera(
    mapViewportState: MapViewportState,
    points: List<Point>,
    drawRoute: Boolean,
    lastHash: Int?,
    setHash: (Int?) -> Unit
) {
  if (points.isEmpty()) return
  if (!drawRoute && points.size == 1) {
    mapViewportState.setCameraOptions(
        CameraOptions.Builder().center(points.first()).zoom(12.0).build())
    setHash(null)
    return
  }
  val h = points.hashCode()
  if (lastHash != h) {
    setHash(h)
    val cam =
        mapViewportState.cameraForCoordinates(
            points, coordinatesPadding = EdgeInsets(200.0, 200.0, 200.0, 200.0))
    mapViewportState.setCameraOptions(cam)
  }
}

private fun updatePuck(mapView: com.mapbox.maps.MapView, permissionGranted: Boolean) {
  mapView.location.updateSettings {
    enabled = permissionGranted
    if (permissionGranted) {
      locationPuck = createDefault2DPuck(withBearing = true)
      puckBearing = PuckBearing.COURSE
      puckBearingEnabled = true
    }
  }
}

/** Convert a list of Location to a list of Mapbox Points */
private fun locationsAsPoints(locations: List<Location>) =
    locations.map { Point.fromLngLat(it.coordinate.longitude, it.coordinate.latitude) }
