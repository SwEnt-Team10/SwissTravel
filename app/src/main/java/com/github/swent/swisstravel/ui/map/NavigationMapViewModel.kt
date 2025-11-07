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
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.directions.session.RoutesUpdatedResult
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
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
    val mapboxNavigation: MapboxNavigation?,
    val routeLineApi: MapboxRouteLineApi?,
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
class NavigationMapViewModel : ViewModel() {

  private val _routeRenderTick = MutableStateFlow(0)
  val routeRenderTick: StateFlow<Int> = _routeRenderTick

  private val routesObserver =
      object : RoutesObserver {
        override fun onRoutesChanged(result: RoutesUpdatedResult) {
          val nav = _uiState.value.mapboxNavigation ?: return
          val api = _uiState.value.routeLineApi ?: return
          val alt = nav.getAlternativeMetadataFor(result.navigationRoutes)
          api.setNavigationRoutes(result.navigationRoutes, alt) { drawData ->
            if (drawData.value != null) {
              _routeRenderTick.value = _routeRenderTick.value + 1
            }
          }
        }
      }

  private val _uiState =
      MutableStateFlow(
          NavigationMapUIState(
              routeLineDrawData = null,
              isRouteRendered = false,
              locationsList = emptyList(),
              mapboxNavigation = null,
              routeLineApi = null))
  val uiState: StateFlow<NavigationMapUIState> = _uiState

  /** Call once from the Composable after you build Mapbox objects */
  fun attachMapObjects(nav: MapboxNavigation, api: MapboxRouteLineApi) {
    val current = _uiState.value
    if (current.mapboxNavigation === nav && current.routeLineApi === api) return
    _uiState.value = current.copy(mapboxNavigation = nav, routeLineApi = api)
    nav.registerRoutesObserver(routesObserver)
    // request route if we already have points
    if (_uiState.value.locationsList.size >= 2) requestRoute()
  }

  /** Push points when they change */
  fun updateLocations(points: List<Point>) {
    if (points == _uiState.value.locationsList) return
    _uiState.value = _uiState.value.copy(locationsList = points)
    if (points.size >= 2) requestRoute()
  }

  @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
  private fun requestRoute() {
    val nav = _uiState.value.mapboxNavigation ?: return
    val pts = _uiState.value.locationsList
    if (pts.size < 2) return

    val routeOptions =
        RouteOptions.builder().applyDefaultNavigationOptions().coordinatesList(pts).build()

    nav.requestRoutes(
        routeOptions,
        object : NavigationRouterCallback {
          override fun onCanceled(routeOptions: RouteOptions, routerOrigin: String) {}

          override fun onFailure(reasons: List<RouterFailure>, routeOptions: RouteOptions) {
            Log.e("NAV_MAP_VM", "requestRoute failure: $reasons")
          }

          override fun onRoutesReady(routes: List<NavigationRoute>, routerOrigin: String) {
            nav.setNavigationRoutes(routes)
          }
        })
  }

  fun setRouteRendered(isRendered: Boolean) {
    _uiState.value = _uiState.value.copy(isRouteRendered = isRendered)
  }

  override fun onCleared() {
    _uiState.value.mapboxNavigation?.unregisterRoutesObserver(routesObserver)
  }
}
