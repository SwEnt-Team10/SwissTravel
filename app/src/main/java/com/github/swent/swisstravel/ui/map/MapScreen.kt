package com.github.swent.swisstravel.ui.map

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.model.trip.Location
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapView
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

private const val PINS_SOURCE_ID = "step-pins-source"
private const val PINS_LAYER_ID = "step-pins-layer"
private const val CIRCLE_COLOR = "#00FF00" // green
private const val CIRCLE_STROKE_COLOR = "#FFFFFF"
private const val CIRCLE_RADIUS = 6.0
private const val CIRCLE_STROKE_WIDTH = 2.0
private const val DEFAULT_ZOOM = 12.0
private val edgeInsets = EdgeInsets(200.0, 200.0, 200.0, 200.0)

/**
 * Shows a full map with the user's location puck, and supports pins and routes drawing
 *
 * @param locations The locations to draw the routes
 * @param drawRoute Whether to draw a route from the first location to the last
 * @param viewModel The view model associated to the map screen
 */
@Suppress("COMPOSE_APPLIER_CALL_MISMATCH")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    locations: List<Location>,
    drawRoute: Boolean,
    onUserLocationUpdate: (Point) -> Unit = {},
    viewModel: MapScreenViewModel = viewModel()
) {
  val context = LocalContext.current
  val appCtx = context.applicationContext

  // Mapbox + route line objects that belong to the screen scope
  val mapboxNavigation = remember {
    if (MapboxNavigationProvider.isCreated()) MapboxNavigationProvider.retrieve()
    else MapboxNavigationProvider.create(NavigationOptions.Builder(appCtx).build())
  }
  val routeLineApi = remember { MapboxRouteLineApi(MapboxRouteLineApiOptions.Builder().build()) }

  LaunchedEffect(Unit) { viewModel.attachMapObjects(mapboxNavigation, routeLineApi) }
  LaunchedEffect(locations) { viewModel.updateLocations(locationsAsPoints(locations)) }

  val ui by viewModel.uiState.collectAsState()
  val mapViewportState = rememberMapViewportState()

  // Permission launcher unchanged
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
          onUserLocationUpdate = onUserLocationUpdate)
      MapOverlays(permissionGranted = ui.permissionGranted, mapViewportState = mapViewportState)
    }
  }
}

/* --------------------------- Split-out content blocks --------------------------- */

/**
 * The inner content of the map screen.
 *
 * @param mapViewportState The state of the map viewport
 * @param drawRoute Whether to draw a route from the first location to the last
 * @param ui The UI state of the map screen
 * @param onUserLocationUpdate A callback to update the user location
 */
@Composable
private fun MapContent(
    mapViewportState: MapViewportState,
    drawRoute: Boolean,
    ui: NavigationMapUIState,
    onUserLocationUpdate: (Point) -> Unit = {}
) {
  val context = LocalContext.current
  val routeLineView = remember {
    val opts = MapboxRouteLineViewOptions.Builder(context).build()
    MapboxRouteLineView(opts)
  }

  // Internal state: not exposed to parent
  var styleReady by remember { mutableStateOf(false) }
  var routeLayersInitialized by remember { mutableStateOf(false) }

  val vm: MapScreenViewModel = viewModel()
  val renderKey by vm.routeRenderTick.collectAsState()

  var lastFitHash by remember { mutableStateOf<Int?>(null) }
  val points: List<Point> = ui.locationsList

  MapboxMap(
      modifier = Modifier.fillMaxSize().testTag(MapScreenTestTags.MAP),
      mapViewportState = mapViewportState) {
        // Pass location to an external function
        MapEffect(ui.permissionGranted) { mapView ->
          if (!ui.permissionGranted) {
            return@MapEffect
          } else {
            mapView.location.addOnIndicatorPositionChangedListener { point ->
              onUserLocationUpdate(point)
            }
          }
        }

        // Style/init
        MapEffect(Unit) { mapView ->
          mapView.mapboxMap.getStyle { style ->
            initRouteLayersOnce(
                style = style,
                routeLineView = routeLineView,
                alreadyInitialized = routeLayersInitialized,
                setInitialized = { routeLayersInitialized = it })
            ensurePinsSourceAndLayer(style)
            styleReady = true
          }
        }

        // Clear route when switching to pins
        MapEffect(styleReady to drawRoute) { mapView ->
          if (!styleReady || drawRoute) return@MapEffect
          val api = ui.routeLineApi ?: return@MapEffect
          val clearValue = api.clearRouteLine()
          mapView.mapboxMap.getStyle { style ->
            routeLineView.renderClearRouteLineValue(style, clearValue)
          }
        }

        // Render route when needed
        MapEffect(styleReady to drawRoute to renderKey) { mapView ->
          if (!styleReady || !drawRoute) return@MapEffect
          val api = ui.routeLineApi ?: return@MapEffect
          mapView.mapboxMap.getStyle { style ->
            api.getRouteDrawData { drawData -> routeLineView.renderRouteDrawData(style, drawData) }
          }
        }

        // Update pins source + visibility
        MapEffect(styleReady to points to drawRoute) { mapView ->
          if (!styleReady) return@MapEffect
          mapView.mapboxMap.getStyle { style -> updatePins(style, points, drawRoute) }
        }

        // Camera fitting
        MapEffect(points to drawRoute) {
          fitCamera(mapViewportState, points, drawRoute, lastFitHash) { lastFitHash = it }
        }

        // Location puck
        MapEffect(ui.permissionGranted) { mapView -> updatePuck(mapView, ui.permissionGranted) }
      }
}

/**
 * The map overlays (i.e the buttons)
 *
 * @param permissionGranted Whether the location permission is granted
 * @param mapViewportState The state of the map viewport
 */
@Composable
private fun MapOverlays(permissionGranted: Boolean, mapViewportState: MapViewportState) {
  Box(Modifier.fillMaxSize()) {
    if (permissionGranted) {
      IconButton(
          onClick = { mapViewportState.transitionToFollowPuckState() },
          modifier =
              Modifier.align(Alignment.BottomStart)
                  .padding(dimensionResource(R.dimen.map_overlay_padding))) {
            Icon(
                imageVector = Icons.Outlined.MyLocation,
                contentDescription = stringResource(R.string.allow_location),
                tint = MaterialTheme.colorScheme.onBackground)
          }
    } else {
      Text(
          text = stringResource(R.string.location_required_to_display),
          modifier =
              Modifier.align(Alignment.BottomCenter)
                  .padding(dimensionResource(R.dimen.map_overlay_padding)))
    }
  }
}

/* --------------------------- Small helpers --------------------------- */

/**
 * A function to initialize the routing layers
 *
 * @param style The style to add to the layers
 * @param routeLineView The route line view to initialize
 * @param alreadyInitialized Whether the layers are already initialized
 * @param setInitialized A callback to set the layers as initialized
 */
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

/**
 * A function to create the necessary layers for the pins
 *
 * @param style The style to add to the layers
 */
private fun ensurePinsSourceAndLayer(style: Style) {
  style.getSourceAs<GeoJsonSource>(PINS_SOURCE_ID)
      ?: geoJsonSource(PINS_SOURCE_ID) {}.also(style::addSource)
  if (style.getLayer(PINS_LAYER_ID) == null) {
    style.addLayer(
        circleLayer(PINS_LAYER_ID, PINS_SOURCE_ID) {
          circleRadius(CIRCLE_RADIUS)
          circleColor(CIRCLE_COLOR)
          circleStrokeColor(CIRCLE_STROKE_COLOR)
          circleStrokeWidth(CIRCLE_STROKE_WIDTH)
        })
  }
}

/**
 * A function to update the state of the pins
 *
 * @param style The style to update
 * @param locationsList The list of locations to update
 * @param drawRoute Whether to draw a route
 */
private fun updatePins(style: Style, locationsList: List<Point>, drawRoute: Boolean) {
  style
      .getSourceAs<GeoJsonSource>(PINS_SOURCE_ID)
      ?.featureCollection(
          FeatureCollection.fromFeatures(
              if (!drawRoute) locationsList.map { Feature.fromGeometry(it) } else emptyList()))
  style.getLayer(PINS_LAYER_ID)?.visibility(if (drawRoute) Visibility.NONE else Visibility.VISIBLE)
}

/**
 * A function to fit the camera depending on the points and if the routes are drawn
 *
 * @param mapViewportState The state of the map viewport
 * @param points The list of points to fit
 * @param drawRoute Whether to draw a route
 * @param lastHash The last hash of the points
 * @param setHash A callback to set the hash of the points
 */
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
        CameraOptions.Builder().center(points.first()).zoom(DEFAULT_ZOOM).build())
    setHash(null)
    return
  }
  val h = points.hashCode()
  if (lastHash != h) {
    setHash(h)
    val cam = mapViewportState.cameraForCoordinates(points, coordinatesPadding = edgeInsets)
    mapViewportState.setCameraOptions(cam)
  }
}

/**
 * A function to update the location puck
 *
 * @param mapView The map view to update
 */
private fun updatePuck(mapView: MapView, permissionGranted: Boolean) {
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
