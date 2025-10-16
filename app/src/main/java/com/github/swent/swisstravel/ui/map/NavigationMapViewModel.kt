package com.github.swent.swisstravel.ui.map

import android.app.Application
import androidx.lifecycle.ViewModel
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
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

/**
 * ViewModel that encapsulates Mapbox Navigation setup, route requests, and route line data. The
 * composable observes [routeLineDrawData] and renders via MapboxRouteLineView.
 */
class NavigationMapViewModel(application: Application) : ViewModel() {

  private val _routeLineDrawData = MutableStateFlow<Expected<RouteLineError, RouteSetValue>?>(null)
  val routeLineDrawData: StateFlow<Expected<RouteLineError, RouteSetValue>?> = _routeLineDrawData
  private val _isRouteRendered = MutableStateFlow(false)
  val isRouteRendered: StateFlow<Boolean> = _isRouteRendered

  private val mapboxNavigation =
      if (MapboxNavigationProvider.isCreated()) {
        MapboxNavigationProvider.retrieve()
      } else {
        MapboxNavigationProvider.create(
            NavigationOptions.Builder(application.applicationContext).build())
      }
  val routeLineApiOptions = MapboxRouteLineApiOptions.Builder().build()
  private val routeLineApi = MapboxRouteLineApi(routeLineApiOptions)

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

  init {
    // Start observing routes to emit draw data when routes change
    mapboxNavigation.registerRoutesObserver(routesObserver)
    requestRoute()
  }

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

  fun setRouteRendered(isRendered: Boolean) {
    _isRouteRendered.value = isRendered
  }

  override fun onCleared() {
    super.onCleared()
    // Unregister and tear down to match previous DisposableEffect cleanup
    try {
      mapboxNavigation.unregisterRoutesObserver(routesObserver)
    } catch (_: Throwable) {}
    try {
      MapboxNavigationProvider.destroy()
    } catch (_: Throwable) {}
  }
}
