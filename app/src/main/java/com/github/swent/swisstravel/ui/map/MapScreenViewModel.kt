package com.github.swent.swisstravel.ui.map

import android.util.Log
import androidx.lifecycle.ViewModel
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.bindgen.Expected
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineError
import com.mapbox.navigation.ui.maps.route.line.model.RouteSetValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

const val DRIVING_TRAFFIC = "driving-traffic"

/**
 * Represents the UI state for the NavigationMapScreen.
 *
 * @property routeLineDrawData The current draw data for the route line (used internally by Mapbox).
 * @property isRouteRendered True if the route has been rendered on the map.
 * @property locationsList List of Points representing navigation locations.
 * @property mapboxNavigation The active MapboxNavigation instance.
 * @property routeLineApi The MapboxRouteLineApi instance used to render routes.
 * @property permissionGranted True if location permission is granted, false otherwise.
 * @property currentLocation The current location displayed on the map.
 */
data class NavigationMapUIState(
    val routeLineDrawData: Expected<RouteLineError, RouteSetValue>? = null,
    val isRouteRendered: Boolean = false,
    val locationsList: List<Point>,
    val photoPinsList: List<Point> = emptyList(), // done by AI
    val mapboxNavigation: MapboxNavigation?,
    val routeLineApi: MapboxRouteLineApi?,
    val permissionGranted: Boolean = false,
    val currentLocation: Point? = null
)

/**
 * ViewModel responsible for managing Mapbox Navigation and route line rendering.
 *
 * Responsibilities:
 * - Initializing MapboxNavigation objects
 * - Requesting navigation routes for a list of locations
 * - Observing route updates via RoutesObserver
 * - Exposing reactive state to the UI for route drawing and camera updates
 */
class MapScreenViewModel : ViewModel() {

  private val _routeRenderTick = MutableStateFlow(0)

  /** Incremented every time a route draw data update occurs, to trigger re-renders in Compose */
  val routeRenderTick: StateFlow<Int> = _routeRenderTick

  /** Internal routes observer that updates the route line API when routes change */
  private val routesObserver = RoutesObserver { result ->
    val nav = _uiState.value.mapboxNavigation ?: return@RoutesObserver
    val alt = nav.getAlternativeMetadataFor(result.navigationRoutes)
    val api = _uiState.value.routeLineApi ?: return@RoutesObserver
    api.setNavigationRoutes(result.navigationRoutes, alt) { drawData ->
      if (drawData.value != null) {
        _routeRenderTick.value += 1
      }
    }
  }

  /** Backing state for the UI */
  private val _uiState =
      MutableStateFlow(
          NavigationMapUIState(
              routeLineDrawData = null,
              isRouteRendered = false,
              locationsList = emptyList(),
              mapboxNavigation = null,
              routeLineApi = null))
  val uiState: StateFlow<NavigationMapUIState> = _uiState

  /**
   * Call once from the Composable after creating MapboxNavigation and RouteLineApi objects.
   * Registers the routes observer and requests the route if enough points exist.
   *
   * @param nav The MapboxNavigation instance
   * @param api The MapboxRouteLineApi instance
   */
  fun attachMapObjects(nav: MapboxNavigation, api: MapboxRouteLineApi) {
    val current = _uiState.value
    if (current.mapboxNavigation === nav && current.routeLineApi === api) return
    _uiState.value = current.copy(mapboxNavigation = nav, routeLineApi = api)
    nav.registerRoutesObserver(routesObserver)
    // request route if we already have points
    if (_uiState.value.locationsList.size >= 2) requestRoute()
  }

  fun updatePhotoPins(points: List<Point>) {
    if (points == _uiState.value.photoPinsList) return
    _uiState.value = _uiState.value.copy(photoPinsList = points)
  }

  /**
   * Update the list of navigation points. Automatically requests a new route if there are at least
   * 2 points.
   *
   * @param points List of Points representing locations
   */
  fun updateLocations(points: List<Point>) {
    if (points == _uiState.value.locationsList) return
    _uiState.value = _uiState.value.copy(locationsList = points)
    if (points.size >= 2) requestRoute()
  }

  /**
   * Requests a navigation route from Mapbox for the current points. Private because it should only
   * be called internally when locations change or objects attach.
   *
   * @param profile The navigation profile to use (e.g. "driving-traffic")
   */
  @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
  private fun requestRoute(profile: String = DRIVING_TRAFFIC) {
    val nav = _uiState.value.mapboxNavigation ?: return
    val pts = _uiState.value.locationsList
    if (pts.size < 2) return

    val routeOptions =
        RouteOptions.builder()
            .applyDefaultNavigationOptions()
            .profile(profile)
            .coordinatesList(pts)
            .build()

    nav.requestRoutes(
        routeOptions,
        object : NavigationRouterCallback {
          override fun onCanceled(routeOptions: RouteOptions, routerOrigin: String) {
            // no-op should never happen
          }

          override fun onFailure(reasons: List<RouterFailure>, routeOptions: RouteOptions) {
            Log.e("NAV_MAP_VM", "requestRoute failure: $reasons")
            // Fallback to walking if driving fails
            if (profile == "DRIVING_TRAFFIC") {
              Log.d("NAV_MAP_VM", "Trying walking route as fallback")
              requestRoute("walking")
            }
          }

          override fun onRoutesReady(routes: List<NavigationRoute>, routerOrigin: String) {
            if (routes.isEmpty()) {
              Log.d("NAV_MAP_VM", "No route found for $profile")
              // Fallback to walking if driving failed
              if (profile == DRIVING_TRAFFIC) {
                Log.d("NAV_MAP_VM", "Trying walking route as fallback")
                requestRoute("walking")
              }
              return
            }
            nav.setNavigationRoutes(routes)
          }
        })
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
   * Update the permission state.
   *
   * @param granted True if location permission is granted, false otherwise.
   */
  fun setPermissionGranted(granted: Boolean) {
    _uiState.value = _uiState.value.copy(permissionGranted = granted)
  }

  /** Cleanup: unregisters routes observer when ViewModel is cleared */
  override fun onCleared() {
    _uiState.value.mapboxNavigation?.unregisterRoutesObserver(routesObserver)
  }
}
