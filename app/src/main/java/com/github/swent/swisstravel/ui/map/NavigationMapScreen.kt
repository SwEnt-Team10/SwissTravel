package com.github.swent.swisstravel.ui.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.common.location.Location
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.locationcomponent.createDefault2DPuck
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.replay.route.ReplayProgressObserver
import com.mapbox.navigation.core.replay.route.ReplayRouteMapper
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.ui.maps.camera.NavigationCamera
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider
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
}

@Composable
fun SampleMenu(navController: NavController) {
  Scaffold { paddingValues ->
    Box(modifier = Modifier.padding(paddingValues)) {
      Button(
          onClick = { navController.navigate("nav-map") },
          modifier = Modifier.testTag(NavigationMapScreenTestTags.ENTER_BUTTON)) {
            Text("Enter Navigation")
          }
    }
  }
}

@Composable
fun MapScreen(navController: NavController) {
  val context = LocalContext.current
  var hasPermission by remember {
    mutableStateOf(
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED)
  }

  val permissionLauncher =
      rememberLauncherForActivityResult(
          contract = ActivityResultContracts.RequestMultiplePermissions()) { perms ->
            hasPermission = perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            if (!hasPermission) {
              Toast.makeText(
                      context,
                      "Location permission denied. Enable it in settings.",
                      Toast.LENGTH_LONG)
                  .show()
            }
          }

  LaunchedEffect(Unit) {
    if (!hasPermission) {
      permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION))
    }
  }

  if (hasPermission) {
    BottomSheet(navController = navController)
  } else {
    Box(Modifier.fillMaxSize()) {
      Button(
          onClick = {
            permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION))
          },
          modifier = Modifier.testTag(NavigationMapScreenTestTags.PERMISSION_BUTTON)) {
            Text("Grant location permission")
          }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheet(navController: NavController) {
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
          NavigationMapScreen(navController = navController)
        }
      }
}

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
@Composable
fun NavigationMapScreen(navController: NavController) {
  val context = LocalContext.current
  val density = LocalDensity.current

  // Map + Navigation state holders
  val mapViewState = remember { mutableStateOf<MapView?>(null) }
  val navigationLocationProvider = remember { NavigationLocationProvider() }
  val replayRouteMapper = remember { ReplayRouteMapper() }

  // Route line + camera state
  var viewportDataSource by remember { mutableStateOf<MapboxNavigationViewportDataSource?>(null) }
  var navigationCamera by remember { mutableStateOf<NavigationCamera?>(null) }
  val routeLineApi by remember {
    mutableStateOf(MapboxRouteLineApi(MapboxRouteLineApiOptions.Builder().build()))
  }
  val routeLineView by remember {
    mutableStateOf(MapboxRouteLineView(MapboxRouteLineViewOptions.Builder(context).build()))
  }

  // Start and End positions
  val startPoint = Point.fromLngLat(-122.43539772352648, 37.77440680146262)
  val endPoint = Point.fromLngLat(-122.42409811526268, 37.76556957793795)

  // Create MapboxNavigation once for this screen
  val mapboxNavigation = remember {
    MapboxNavigationProvider.create(NavigationOptions.Builder(context).build())
  }

  // Observers
  val routesObserver = remember {
    com.mapbox.navigation.core.directions.session.RoutesObserver { routeUpdate ->
      val mv = mapViewState.value ?: return@RoutesObserver
      if (routeUpdate.navigationRoutes.isNotEmpty()) {
        // draw the route
        routeLineApi.setNavigationRoutes(routeUpdate.navigationRoutes) { drawData ->
          mv.mapboxMap.style?.let { style -> routeLineView.renderRouteDrawData(style, drawData) }
        }

        // update viewport and go to overview
        viewportDataSource?.onRouteChanged(routeUpdate.navigationRoutes.first())
        viewportDataSource?.evaluate()
        navigationCamera?.requestNavigationCameraToOverview()
      }
    }
  }

  val locationObserver = remember {
    object : LocationObserver {
      override fun onNewRawLocation(rawLocation: Location) {}

      override fun onNewLocationMatcherResult(locationMatcherResult: LocationMatcherResult) {
        val enhanced = locationMatcherResult.enhancedLocation
        navigationLocationProvider.changePosition(
            location = enhanced, keyPoints = locationMatcherResult.keyPoints)
        viewportDataSource?.onLocationChanged(enhanced)
        viewportDataSource?.evaluate()
        navigationCamera?.requestNavigationCameraToFollowing()
      }
    }
  }

  // Build the MapView inside Compose
  Box(
      Modifier.fillMaxSize()
          .scrollable(
              rememberScrollableState {
                // view world deltas should be reflected in compose world
                // components that participate in nested scrolling
                it
              },
              Orientation.Vertical)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
              MapView(ctx).apply {
                mapboxMap.setCamera(
                    CameraOptions.Builder()
                        .center(Point.fromLngLat(-122.43539772352648, 37.77440680146262))
                        .zoom(14.0)
                        .build())
                // Enable location puck using NavigationLocationProvider
                location.apply {
                  setLocationProvider(navigationLocationProvider)
                  locationPuck = LocationPuck2D() // replaced later with default 2D puck as well
                  enabled = true
                }
                mapViewState.value = this

                // Viewport, padding, and camera
                viewportDataSource =
                    MapboxNavigationViewportDataSource(mapboxMap).also { vds ->
                      val top = with(density) { 180.dp.toPx().toDouble() }
                      val left = with(density) { 40.dp.toPx().toDouble() }
                      val bottom = with(density) { 150.dp.toPx().toDouble() }
                      val right = with(density) { 40.dp.toPx().toDouble() }
                      vds.followingPadding = EdgeInsets(top, left, bottom, right)
                    }
                navigationCamera = NavigationCamera(mapboxMap, camera, viewportDataSource!!)
              }
            },
            update = { /* no-op */},
        )
        Button(
            onClick = { navController.navigate("menu-example") },
            modifier =
                Modifier.align(Alignment.TopEnd).testTag(NavigationMapScreenTestTags.EXIT_BUTTON)) {
              Text("Exit Navigation")
            }
      }

  // Wire up Navigation lifecycle with Compose
  DisposableEffect(Unit) {
    // Register observers and kick off a replayed trip session
    val mv = mapViewState.value
    if (mv != null) {
      // ensure the nice default puck
      mv.location.apply {
        setLocationProvider(navigationLocationProvider)
        locationPuck = createDefault2DPuck()
        enabled = true
      }
    }

    val replayProgressObserver = ReplayProgressObserver(mapboxNavigation.mapboxReplayer)
    mapboxNavigation.registerRoutesObserver(routesObserver)
    mapboxNavigation.registerLocationObserver(locationObserver)
    mapboxNavigation.registerRouteProgressObserver(replayProgressObserver)
    mapboxNavigation.startReplayTripSession()

    // Request a simple 2-point route and push replay events
    val origin = Point.fromLngLat(-122.43539772352648, 37.77440680146262)
    val destination = Point.fromLngLat(-122.42409811526268, 37.76556957793795)

    @SuppressLint("MissingPermission")
    fun requestRoute() {
      mapboxNavigation.requestRoutes(
          RouteOptions.builder()
              .applyDefaultNavigationOptions()
              .coordinatesList(listOf(origin, destination))
              .layersList(listOf(mapboxNavigation.getZLevel(), null))
              .build(),
          object : NavigationRouterCallback {
            override fun onCanceled(routeOptions: RouteOptions, routerOrigin: String) {}

            override fun onFailure(reasons: List<RouterFailure>, routeOptions: RouteOptions) {}

            override fun onRoutesReady(routes: List<NavigationRoute>, routerOrigin: String) {
              mapboxNavigation.setNavigationRoutes(routes)

              // Simulate user movement along the route
              val replayData =
                  replayRouteMapper.mapDirectionsRouteGeometry(routes.first().directionsRoute)
              mapboxNavigation.mapboxReplayer.pushEvents(replayData)
              mapboxNavigation.mapboxReplayer.seekTo(replayData.first())
              mapboxNavigation.mapboxReplayer.play()
            }
          })
    }

    requestRoute()

    onDispose {
      // Unregister and clean up
      mapboxNavigation.unregisterRoutesObserver(routesObserver)
      mapboxNavigation.unregisterLocationObserver(locationObserver)
      mapboxNavigation.stopTripSession()
      MapboxNavigationProvider.destroy() // releases the singleton instance
      mapViewState.value = null
    }
  }
}
