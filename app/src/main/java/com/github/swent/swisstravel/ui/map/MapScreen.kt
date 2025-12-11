package com.github.swent.swisstravel.ui.map

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.model.trip.Location
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.ViewAnnotationAnchor
import com.mapbox.maps.ViewAnnotationOptions
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.annotation.ViewAnnotation
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
import com.mapbox.maps.viewannotation.geometry
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
 * @param locations The locations to draw the routes (navigation)
 * @param photoEntries List of pairs (Uri, Location) for photo pins
 * @param drawRoute Whether to draw a route from the first location to the last
 * @param viewModel The view model associated to the map screen
 */
@Suppress("COMPOSE_APPLIER_CALL_MISMATCH")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    locations: List<Location>,
    drawRoute: Boolean,
    photoEntries: List<Pair<Uri, Location>> = emptyList(),
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

    // On met à jour les locations de la route (Ligne bleue)
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
                photoEntries = photoEntries,
                onUserLocationUpdate = onUserLocationUpdate)
            MapOverlays(permissionGranted = ui.permissionGranted, mapViewportState = mapViewportState)
        }
    }
}

/* --------------------------- Split-out content blocks --------------------------- */

/**
 * The inner content of the map screen.
 */
@Composable
private fun MapContent(
    mapViewportState: MapViewportState,
    drawRoute: Boolean,
    ui: NavigationMapUIState,
    photoEntries: List<Pair<Uri, Location>>,
    onUserLocationUpdate: (Point) -> Unit = {}
) {
    val context = LocalContext.current
    val routeLineView = remember {
        val opts = MapboxRouteLineViewOptions.Builder(context).build()
        MapboxRouteLineView(opts)
    }

    // Internal state
    var styleReady by remember { mutableStateOf(false) }
    var routeLayersInitialized by remember { mutableStateOf(false) }

    val vm: MapScreenViewModel = viewModel()
    val renderKey by vm.routeRenderTick.collectAsState()

    var lastFitHash by remember { mutableStateOf<Int?>(null) }
    val points: List<Point> = ui.locationsList

    MapboxMap(
        modifier = Modifier.fillMaxSize().testTag(MapScreenTestTags.MAP),
        mapViewportState = mapViewportState,
        // On ne met PAS de paramètre 'style = {}' ici pour éviter les conflits,
        // on le gère manuellement dans le MapEffect ci-dessous.
    ) {

        // 1. Affichage des Pins Photos
        photoEntries.forEach { (uri, location) ->
            key(uri) {
                PhotoPinAnnotation(uri = uri, location = location)
            }
        }

        // 2. Initialisation du Style et des Calques (CRITIQUE)
        MapEffect(Unit) { mapView ->
            // On force le chargement du style OUTDOORS
            mapView.mapboxMap.loadStyleUri(Style.OUTDOORS) { style ->
                // Une fois le style chargé, on initialise tout le reste
                initRouteLayersOnce(
                    style = style,
                    routeLineView = routeLineView,
                    alreadyInitialized = routeLayersInitialized,
                    setInitialized = { routeLayersInitialized = it })

                ensurePinsSourceAndLayer(style)

                // C'est ce flag qui déclenchera l'affichage de la route dans les autres MapEffect
                styleReady = true
            }
        }

        // 3. User Location Listener
        MapEffect(ui.permissionGranted) { mapView ->
            if (ui.permissionGranted) {
                mapView.location.addOnIndicatorPositionChangedListener { point ->
                    onUserLocationUpdate(point)
                }
            }
        }

        // 4. Nettoyage de la route (si on désactive le mode route)
        MapEffect(styleReady to drawRoute) { mapView ->
            if (styleReady && !drawRoute) {
                val api = ui.routeLineApi
                if (api != null) {
                    val clearValue = api.clearRouteLine()
                    mapView.mapboxMap.getStyle { style ->
                        routeLineView.renderClearRouteLineValue(style, clearValue)
                    }
                }
            }
        }

        // 5. Dessin de la route (si styleReady est true)
        MapEffect(styleReady to drawRoute to renderKey) { mapView ->
            if (styleReady && drawRoute) {
                val api = ui.routeLineApi
                if (api != null) {
                    mapView.mapboxMap.getStyle { style ->
                        api.getRouteDrawData { drawData ->
                            routeLineView.renderRouteDrawData(style, drawData)
                        }
                    }
                }
            }
        }

        // 6. Mise à jour des pins de navigation (Start/End)
        MapEffect(styleReady to points to drawRoute) { mapView ->
            if (styleReady) {
                mapView.mapboxMap.getStyle { style ->
                    updatePins(style, points, drawRoute)
                }
            }
        }

        // 7. Centrage de la caméra (Fit Camera)
        MapEffect(points to drawRoute) {
            fitCamera(mapViewportState, points, drawRoute, lastFitHash) { lastFitHash = it }
        }

        // 8. Puck (Position utilisateur)
        MapEffect(ui.permissionGranted) { mapView -> updatePuck(mapView, ui.permissionGranted) }
    }
}

/**
 * Composant personnalisé : Photo Pin "Polaroid"
 */
@Composable
fun PhotoPinAnnotation(uri: Uri, location: Location) {
    val mapPoint = Point.fromLngLat(location.coordinate.longitude, location.coordinate.latitude)

    // 1. On crée la configuration de l'ancre
    val anchorConfig = com.mapbox.maps.ViewAnnotationAnchorConfig.Builder()
        .anchor(com.mapbox.maps.ViewAnnotationAnchor.BOTTOM) // On fixe l'ancre en bas
        .build()

    // 2. On construit les options en passant la liste de configs
    val options = ViewAnnotationOptions.Builder()
        .geometry(mapPoint)
        .allowOverlap(true)
        .variableAnchors(listOf(anchorConfig)) // C'est ici que ça bloquait !
        .build()

    ViewAnnotation(options = options) {
        // Le design "Polaroid"
        Surface(
            modifier = Modifier
                .size(60.dp)
                .shadow(elevation = 6.dp, shape = RoundedCornerShape(4.dp)),
            shape = RoundedCornerShape(4.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Color.LightGray)
        ) {
            Box(modifier = Modifier.padding(3.dp)) {
                AsyncImage(
                    model = uri,
                    contentDescription = location.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(2.dp)),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

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

private fun updatePins(style: Style, locationsList: List<Point>, drawRoute: Boolean) {
    style
        .getSourceAs<GeoJsonSource>(PINS_SOURCE_ID)
        ?.featureCollection(
            FeatureCollection.fromFeatures(
                if (!drawRoute) locationsList.map { Feature.fromGeometry(it) } else emptyList()))
    style.getLayer(PINS_LAYER_ID)?.visibility(if (drawRoute) Visibility.NONE else Visibility.VISIBLE)
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

private fun locationsAsPoints(locations: List<Location>) =
    locations.map { Point.fromLngLat(it.coordinate.longitude, it.coordinate.latitude) }