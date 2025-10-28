package com.github.swent.swisstravel.ui.map

import android.app.Application
import androidx.lifecycle.ViewModel
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
class NavigationMapViewModel(application: Application) : ViewModel() {

  /**
   * Backing state that holds the route line draw data (either a success or an error).
   *
   * This is used by the UI to render routes on the map.
   */
  private val _routeLineDrawData = MutableStateFlow<Expected<RouteLineError, RouteSetValue>?>(null)

  /** Public [StateFlow] for observing route line draw data updates. */
  val routeLineDrawData: StateFlow<Expected<RouteLineError, RouteSetValue>?> = _routeLineDrawData

  /** Internal flag indicating whether the route is currently rendered on the map. */
  private val _isRouteRendered = MutableStateFlow(false)

  /** Public [StateFlow] for tracking whether the route is rendered or not. */
  val isRouteRendered: StateFlow<Boolean> = _isRouteRendered

  /**
   * The main Mapbox Navigation instance.
   *
   * If an instance already exists, it is retrieved via [MapboxNavigationProvider.retrieve].
   * Otherwise, a new one is created with default navigation options.
   */
  private val mapboxNavigation =
      if (MapboxNavigationProvider.isCreated()) {
        MapboxNavigationProvider.retrieve()
      } else {
        MapboxNavigationProvider.create(
            NavigationOptions.Builder(application.applicationContext).build())
      }

  /** Configuration options for the [MapboxRouteLineApi]. */
  val routeLineApiOptions = MapboxRouteLineApiOptions.Builder().build()

  /** An instance of MapboxRouteLineAPI, used to generate and update route line draw data. */
  private val routeLineApi = MapboxRouteLineApi(routeLineApiOptions)

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
              mapboxNavigation.getAlternativeMetadataFor(result.navigationRoutes)
          routeLineApi.setNavigationRoutes(result.navigationRoutes, alternativesMetadata) { drawData
            ->
            // update draw data
            if (drawData.value != null) {
              _routeLineDrawData.value = ExpectedFactory.createValue(drawData.value!!)
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
    mapboxNavigation.registerRoutesObserver(routesObserver)
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
            .coordinatesList(listOf(Locations.EPFL_IC, Locations.OLYMPIC_MUSEUM))
            .build()

    val callback =
        object : NavigationRouterCallback {
          override fun onCanceled(routeOptions: RouteOptions, routerOrigin: String) {}

          override fun onFailure(reasons: List<RouterFailure>, routeOptions: RouteOptions) {}

          override fun onRoutesReady(routes: List<NavigationRoute>, routerOrigin: String) {
            mapboxNavigation.setNavigationRoutes(routes)
          }
        }

    // Requesting route (API request)
    mapboxNavigation.requestRoutes(routeOptions, callback)
  }

  /**
   * Updates the current rendering state of the route.
   *
   * This is typically called by the UI after a route has been successfully rendered.
   *
   * @param isRendered `true` if the route is displayed on the map, `false` otherwise.
   */
  fun setRouteRendered(isRendered: Boolean) {
    _isRouteRendered.value = isRendered
  }

  /**
   * Cleans up navigation resources when the ViewModel is cleared.
   * - Unregisters the [routesObserver];
   * - Destroys the [MapboxNavigationProvider] instance to prevent memory leaks.
   */
  override fun onCleared() {
    super.onCleared()
    mapboxNavigation.unregisterRoutesObserver(routesObserver)
    MapboxNavigationProvider.destroy()
  }
}
