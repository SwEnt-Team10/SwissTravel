package com.github.swent.swisstravel.ui.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.github.swent.swisstravel.ui.navigation.NavigationActions
import com.github.swent.swisstravel.ui.navigation.Screen
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
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

object NavigationMapScreenTestTags {
  const val ENTER_MAP_BUTTON = "enterMapButton"
  const val EXIT_BUTTON = "exitButton"
  const val MAP = "map"
}

object Locations {
  val EPFL_IC = Point.fromLngLat(6.563349085567107, 46.51823826885176)
  val ZERMATT = Point.fromLngLat(7.747, 46.019)
  val OLYMPIC_MUSEUM = Point.fromLngLat(6.6339, 46.5086)
  val CHUV = Point.fromLngLat(6.6209, 46.5197)
}

@Composable
fun NavigationMapScreen(navigationActions: NavigationActions) {
  Box(modifier = Modifier.fillMaxSize()) {
    NavigationMap()
    Button(
        onClick = { navigationActions.navigateTo(Screen.MyTrips) },
        modifier =
            Modifier.align(Alignment.TopStart).testTag(NavigationMapScreenTestTags.EXIT_BUTTON)) {
          Icon(
              imageVector = Icons.AutoMirrored.Default.ArrowBack,
              contentDescription = "Exit Map Icon")
        }
  }
}

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
  val mapboxNavigation =
      remember(context) {
        if (MapboxNavigationProvider.isCreated()) {
          MapboxNavigationProvider.retrieve()
        } else {
          MapboxNavigationProvider.create(NavigationOptions.Builder(context).build())
        }
      }

  // get the possible routes from origin to destination
  val routeOptions =
      RouteOptions.builder()
          .applyDefaultNavigationOptions()
          .coordinatesList(
              listOf(
                  Locations.EPFL_IC, Locations.OLYMPIC_MUSEUM)) // can add intermediary points here
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
  // set the initial location of the map
  // hardcoded to EPFL for now to see the start of the hardcoded route
  LaunchedEffect(Unit) {
    mapViewportState.setCameraOptions {
      center(Locations.EPFL_IC)
      zoom(14.0)
    }
  }
  MapboxMap(
      modifier = Modifier.fillMaxSize().testTag(NavigationMapScreenTestTags.MAP),
      mapViewportState = mapViewportState) {
        MapEffect(Unit) { mapView ->
          // observer to update the route on the map when routes change
          val routesObserver =
              object : RoutesObserver {
                override fun onRoutesChanged(result: RoutesUpdatedResult) {
                  val alternativesMetadata =
                      mapboxNavigation.getAlternativeMetadataFor(result.navigationRoutes)
                  routeLineApi.setNavigationRoutes(result.navigationRoutes, alternativesMetadata) {
                      routeDrawData ->
                    mapView.mapboxMap.style?.let { style ->
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
  DisposableEffect(Unit) {
    onDispose {
      // mapboxNavigation.unregisterRoutesObserver(routesObserver)
      MapboxNavigationProvider.destroy()
    }
  }
}
