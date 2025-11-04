package com.github.swent.swisstravel.ui.map

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import com.github.swent.swisstravel.model.trip.Location
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.directions.session.RoutesUpdatedResult
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineApiOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineError
import com.mapbox.navigation.ui.maps.route.line.model.RouteSetValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object Locations {
  val EPFL_IC = Point.fromLngLat(6.563349085567107, 46.51823826885176)
  val ZERMATT = Point.fromLngLat(7.747, 46.019)
  val OLYMPIC_MUSEUM = Point.fromLngLat(6.6339, 46.5086)
  val CHUV = Point.fromLngLat(6.6209, 46.5197)
}

data class NavigationMapUIState(
    val routeLineDrawData: Expected<RouteLineError, RouteSetValue>? = null,
    val isRouteRendered: Boolean = false,
    val locationsList: List<Point>,
    val mapboxNavigation: MapboxNavigation,
    val routeLineApi: MapboxRouteLineApi,
)

/**
 * ViewModel responsible for configuring and managing the Mapbox Navigation instance.
 *
 * It handles:
 * - Initializing the [MapboxNavigationProvider];
 * - Requesting navigation routes;
 * - Observing route updates via [RoutesObserver];
 * - Exposing route line draw data to the UI.
 *
 * The UI layer observes [routeLineDrawData] and [isRouteRendered] to reactively update the map view
 * when routes change or when rendering is complete.
 */
class NavigationMapViewModel(application: Application, locationsList: List<Location>) :
    ViewModel() {

  /** ... */
  private val _uiState =
      MutableStateFlow<NavigationMapUIState>(
          NavigationMapUIState(
              locationsList = locationsAsPoints(locationsList),
              mapboxNavigation =
                  if (MapboxNavigationProvider.isCreated()) {
                    MapboxNavigationProvider.retrieve()
                  } else {
                    MapboxNavigationProvider.create(
                        NavigationOptions.Builder(application.applicationContext).build())
                  },
              routeLineApi = MapboxRouteLineApi(MapboxRouteLineApiOptions.Builder().build())))

  /** ... */
  val uiState: StateFlow<NavigationMapUIState> = _uiState

  /**
   * A [RoutesObserver] that listens for changes in the current navigation routes.
   *
   * When routes are updated, this observer updates the route line draw data through [routeLineApi]
   * and emits it via [_routeLineDrawData].
   */
  private val routesObserver =
      object : RoutesObserver {
        override fun onRoutesChanged(result: RoutesUpdatedResult) {
          val alternativesMetadata =
              _uiState.value.mapboxNavigation.getAlternativeMetadataFor(result.navigationRoutes)
          _uiState.value.routeLineApi.setNavigationRoutes(
              result.navigationRoutes, alternativesMetadata) { drawData ->
                // update draw data
                if (drawData.value != null) {
                  _uiState.value =
                      _uiState.value.copy(
                          routeLineDrawData = ExpectedFactory.createValue(drawData.value!!))
                }
              }
        }
      }

  /**
   * Initializes the ViewModel:
   * - Registers the [routesObserver] to listen for route changes;
   * - Immediately triggers a default route request.
   */
  init {
    // Start observing routes to emit draw data when routes change
    Log.d("NAV_MAP_VM", "initialize a VM")
    _uiState.value.mapboxNavigation.registerRoutesObserver(routesObserver)
    requestRoute()
  }

  /**
   * Requests a navigation route between two points
   *
   * Once routes are ready, they are set on the [mapboxNavigation] instance, triggering the
   * [routesObserver] to update the route line data.
   */
  @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
  fun requestRoute() {
    val routeOptions =
        RouteOptions.builder()
            .applyDefaultNavigationOptions()
            .coordinatesList(_uiState.value.locationsList)
            .build()

    val callback =
        object : NavigationRouterCallback {
          override fun onCanceled(routeOptions: RouteOptions, routerOrigin: String) {}

          override fun onFailure(reasons: List<RouterFailure>, routeOptions: RouteOptions) {}

          override fun onRoutesReady(routes: List<NavigationRoute>, routerOrigin: String) {
            _uiState.value.mapboxNavigation.setNavigationRoutes(routes)
          }
        }

    // Requesting route (API request)
    _uiState.value.mapboxNavigation.requestRoutes(routeOptions, callback)
  }

  /**
   * Updates the current rendering state of the route.
   *
   * This is typically called by the UI after a route has been successfully rendered.
   *
   * @param isRendered `true` if the route is displayed on the map, `false` otherwise.
   */
  fun setRouteRendered(isRendered: Boolean) {
    _uiState.value = _uiState.value.copy(isRouteRendered = isRendered)
  }

  /**
   * Cleans up navigation resources when the ViewModel is cleared.
   * - Unregisters the [routesObserver];
   * - Destroys the [MapboxNavigationProvider] instance to prevent memory leaks.
   */
  override fun onCleared() {
    super.onCleared()
    _uiState.value.mapboxNavigation.unregisterRoutesObserver(routesObserver)
    MapboxNavigationProvider.destroy()
  }
}

/**
 * Converts a list of locations (from the tripInfoViewModel) to a list of Points (what the mapbox
 * api needs)
 */
fun locationsAsPoints(locations: List<Location>): List<Point> {
  return locations.map { location ->
    Point.fromLngLat(location.coordinate.longitude, location.coordinate.latitude)
  }
}
