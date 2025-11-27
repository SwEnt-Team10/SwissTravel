package com.github.swent.swisstravel.ui.trip.tripinfos

import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Attractions
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ZoomInMap
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.model.trip.Location
import com.github.swent.swisstravel.model.trip.TripElement
import com.github.swent.swisstravel.ui.map.MapScreen
import com.github.swent.swisstravel.ui.theme.favoriteIcon
import com.google.firebase.Timestamp
import com.mapbox.geojson.Point
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Test tags for TripInfoScreen composable */
object TripInfoScreenTestTags {
  const val TITLE = "tripInfoScreenTitle"
  const val BACK_BUTTON = "tripInfoScreenBackButton"
  const val FAVORITE_BUTTON = "tripInfoScreenFavoriteButton"
  const val EDIT_BUTTON = "tripInfoScreenEditButton"
  const val LAZY_COLUMN = "tripInfoScreenLazyColumn"
  const val NO_LOCATIONS = "tripInfoScreenNoLocations"
  const val CURRENT_STEP = "tripInfoScreenCurrentStep"
  const val LOCATION_NAME = "tripInfoScreenLocationName"
  const val MAP_CARD = "tripInfoScreenMapCard"
  const val MAP_CONTAINER = "tripInfoScreenMapContainer"
  const val FULLSCREEN_BUTTON = "fullscreenToggle"
  const val LOADING = "loading"
  const val FULLSCREEN_MAP = "fullScreenMap"
  const val FULLSCREEN_EXIT = "fullScreenExit"
  const val PREVIOUS_STEP = "previousStep"
  const val NEXT_STEP = "nextStep"
}

/** UI state holder for the main content of the trip info screen. */
data class TripInfoContentState(
    val ui: TripInfoUIState,
    val schedule: List<TripElement>,
    val currentStepIndex: Int,
    val mapState: MapState
)

/** Event handlers for the main content of the trip info screen. */
data class TripInfoContentCallbacks(
    val onStepChange: (Int) -> Unit,
    val onToggleFullscreen: (Boolean) -> Unit,
    val onToggleNavMode: () -> Unit,
    val onUserLocationUpdate: (Point) -> Unit
)

/**
 * A composable that shows a trip's info. It shows the next step the user should do and shows the
 * itinerary on the map It also shows the activities the user has fetched during creation.
 *
 * @param uid The trip's ID.
 * @param tripInfoViewModel The view model to use.
 * @param onMyTrips Called when the user clicks the back button.
 * @param onEditTrip Called when the user clicks the edit button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripInfoScreen(
    uid: String?,
    tripInfoViewModel: TripInfoViewModelContract = viewModel<TripInfoViewModel>(),
    onMyTrips: () -> Unit = {},
    onEditTrip: () -> Unit = {},
    isOnCurrentTripScreen: Boolean = false,
    onActivityClick: (TripElement.TripActivity) -> Unit = {},
    onSwipeActivities: () -> Unit = {},
    onLikedActivities: () -> Unit = {}
) {
  Log.d("NAV_DEBUG", "Entered TripInfo with uid=$uid")

  LaunchedEffect(uid) { tripInfoViewModel.loadTripInfo(uid) }

  val ui by tripInfoViewModel.uiState.collectAsState()
  val context = LocalContext.current

  BackHandler(enabled = ui.fullscreen) { tripInfoViewModel.toggleFullscreen(false) }

  var isComputing by remember { mutableStateOf(false) }
  var schedule by remember { mutableStateOf<List<TripElement>>(emptyList()) }
  var currentStepIndex by rememberSaveable { mutableIntStateOf(0) }
  var currentGpsPoint by remember { mutableStateOf<Point?>(null) }
  var drawFromCurrentPosition by remember { mutableStateOf(false) }

  // State holder for map-related properties
  val mapState =
      remember(schedule, currentStepIndex, currentGpsPoint, drawFromCurrentPosition) {
        MapState(
            locations =
                mapLocationsForStep(
                    schedule = schedule,
                    idx = currentStepIndex,
                    currentGps = currentGpsPoint,
                    drawFromCurrentPosition = drawFromCurrentPosition),
            drawRoute = schedule.isNotEmpty(),
            drawFromCurrentPosition = drawFromCurrentPosition,
            isLoading = isComputing || (ui.locations.isNotEmpty() && schedule.isEmpty()))
      }

  BackHandler(enabled = ui.fullscreen) { tripInfoViewModel.toggleFullscreen(false) }

  // handle VM error toasts
  LaunchedEffect(ui.errorMsg) {
    ui.errorMsg?.let {
      Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
      tripInfoViewModel.clearErrorMsg()
    }
  }

  // compute schedule once trip data available
  LaunchedEffect(ui.locations, ui.activities, ui.tripProfile) {
    if (ui.locations.isEmpty() || ui.tripProfile == null) return@LaunchedEffect
    val tripSegments = ui.routeSegments.map { TripElement.TripSegment(it) }
    val tripActivities = ui.activities.map { TripElement.TripActivity(it) }

    schedule = tripSegments + tripActivities
    schedule = schedule.sortedBy { it.startDate }
    currentStepIndex = 0
    isComputing = false
  }

  Scaffold(
      containerColor = MaterialTheme.colorScheme.background,
      topBar = {
        if (!ui.fullscreen) {
          TripInfoTopAppBar(
              ui = ui,
              isOnCurrentTripScreen = isOnCurrentTripScreen,
              onBack = onMyTrips,
              onToggleFavorite = { tripInfoViewModel.toggleFavorite() },
              onEdit = onEditTrip)
        }
      },

      // button to go to a screen were you can swipe (like/dislike) through activities
      bottomBar = {
        Button(
            onClick = { onSwipeActivities() },
            modifier = Modifier.fillMaxWidth().padding(dimensionResource(R.dimen.small_spacer)),
        ) {
          Text(text = stringResource(R.string.swipe_activities))
        }
      }) { pd ->
        Box(Modifier.fillMaxSize().padding(pd)) {
          val contentState =
              TripInfoContentState(
                  ui = ui,
                  schedule = schedule,
                  currentStepIndex = currentStepIndex,
                  mapState = mapState)
          val callbacks =
              TripInfoContentCallbacks(
                  onStepChange = { newIndex -> currentStepIndex = newIndex },
                  onToggleFullscreen = { tripInfoViewModel.toggleFullscreen(it) },
                  onToggleNavMode = { drawFromCurrentPosition = !drawFromCurrentPosition },
                  onUserLocationUpdate = { currentGpsPoint = it })
          TripInfoContent(contentState, callbacks, isComputing, schedule, onActivityClick)

          // Button to go to the liked activities screen
          Button(
              onClick = { onLikedActivities() },
              modifier =
                  Modifier.align(Alignment.BottomEnd)
                      .padding(dimensionResource(R.dimen.small_spacer))) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = stringResource(R.string.liked_activities))
              }

          // Fullscreen map overlay
          if (ui.fullscreen) {
            FullScreenMap(
                mapState = mapState,
                onExit = { tripInfoViewModel.toggleFullscreen(false) },
                onUserLocationUpdate = { currentGpsPoint = it })
          }
        }
      }
}

/** UI state holder for map properties. */
data class MapState(
    val locations: List<Location>,
    val drawRoute: Boolean,
    val drawFromCurrentPosition: Boolean,
    val isLoading: Boolean
)

/**
 * The main content of the trip info screen, displayed in a LazyColumn.
 *
 * @param contentState The state of the content.
 * @param callbacks The event handlers for the content.
 */
@Composable
internal fun TripInfoContent(
    contentState: TripInfoContentState,
    callbacks: TripInfoContentCallbacks,
    isComputing: Boolean,
    schedule: List<TripElement>,
    onActivityClick: (TripElement.TripActivity) -> Unit
) {
  LazyColumn(
      modifier = Modifier.fillMaxSize().testTag(TripInfoScreenTestTags.LAZY_COLUMN),
      horizontalAlignment = Alignment.Start,
      contentPadding = PaddingValues(bottom = 24.dp)) {
        if (contentState.ui.locations.isEmpty()) {
          item {
            Text(
                text = stringResource(R.string.no_locations_available),
                modifier = Modifier.padding(16.dp).testTag(TripInfoScreenTestTags.NO_LOCATIONS))
          }
        } else {
          item {
            CurrentStepHeader(
                schedule = contentState.schedule,
                currentStepIndex = contentState.currentStepIndex,
            )
          }

          if (!contentState.ui.fullscreen) {
            item {
              TripMapCard(
                  mapState = contentState.mapState,
                  onToggleFullscreen = { callbacks.onToggleFullscreen(true) },
                  onToggleNavMode = callbacks.onToggleNavMode,
                  onUserLocationUpdate = callbacks.onUserLocationUpdate,
                  isComputing = isComputing,
                  schedule = schedule)
            }
            item {
              StepControls(
                  currentStepIndex = contentState.currentStepIndex,
                  scheduleSize = contentState.schedule.size,
                  isComputing = contentState.mapState.isLoading,
                  onStepChange = callbacks.onStepChange)
            }
          }
        }

        // Steps list
        itemsIndexed(schedule) { idx, el ->
          val stepNo = idx + 1
          when (el) {
            is TripElement.TripActivity -> {
              StepLocationCard(
                  stepNumber = stepNo,
                  title = el.activity.location.name,
                  timeRange = "${fmtTime(el.activity.startDate)} – ${fmtTime(el.activity.endDate)}",
                  leadingIcon = {
                    Icon(imageVector = Icons.Filled.Attractions, contentDescription = null)
                  },
                  modifier = Modifier.clickable { onActivityClick(el) })
            }
            is TripElement.TripSegment -> {
              StepRow(
                  stepNumber = stepNo,
                  subtitle = "${el.route.from.name} → ${el.route.to.name}",
                  timeRange =
                      stringResource(R.string.about_minutes, el.route.durationMinutes) +
                          " • " +
                          "${fmtTime(el.route.startDate)} – ${fmtTime(el.route.endDate)}",
                  leadingIcon = { Icon(Icons.Filled.Route, contentDescription = null) })
            }
          }
        }
      }
}

/** Top app bar for the trip info screen. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TripInfoTopAppBar(
    ui: TripInfoUIState,
    isOnCurrentTripScreen: Boolean,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onEdit: () -> Unit
) {
  TopAppBar(
      title = {
        Text(
            text = ui.name,
            modifier = Modifier.testTag(TripInfoScreenTestTags.TITLE),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground)
      },
      navigationIcon = {
        if (!isOnCurrentTripScreen) {
          IconButton(
              onClick = onBack, modifier = Modifier.testTag(TripInfoScreenTestTags.BACK_BUTTON)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back_to_my_trips),
                    tint = MaterialTheme.colorScheme.onBackground)
              }
        }
      },
      actions = {
        FavoriteButton(
            isFavorite = ui.isFavorite,
            onToggleFavorite = onToggleFavorite,
        )
        IconButton(
            onClick = onEdit, modifier = Modifier.testTag(TripInfoScreenTestTags.EDIT_BUTTON)) {
              Icon(
                  imageVector = Icons.Outlined.Edit,
                  contentDescription = stringResource(R.string.edit_trip),
                  tint = MaterialTheme.colorScheme.onBackground)
            }
      })
}

/** Displays the current step number and title. */
@Composable
private fun CurrentStepHeader(schedule: List<TripElement>, currentStepIndex: Int) {
  Column(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 16.dp, bottom = 8.dp)) {
    Text(
        text = stringResource(R.string.current_step),
        modifier = Modifier.testTag(TripInfoScreenTestTags.CURRENT_STEP),
        style = MaterialTheme.typography.displaySmall)

    Text(
        text =
            "Step ${if (schedule.isEmpty()) 0 else currentStepIndex + 1}: " +
                currentStepTitle(schedule, currentStepIndex),
        modifier = Modifier.padding(top = 4.dp).testTag(TripInfoScreenTestTags.LOCATION_NAME),
        style = MaterialTheme.typography.headlineMedium)

    val timeLabel = currentStepTime(schedule, currentStepIndex)
    if (timeLabel != "—") {
      Text(
          text = timeLabel,
          modifier = Modifier.padding(top = 2.dp),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
  }
}

/** Displays the map in a card with controls. */
@Composable
private fun TripMapCard(
    mapState: MapState,
    onToggleFullscreen: () -> Unit,
    onToggleNavMode: () -> Unit,
    onUserLocationUpdate: (Point) -> Unit,
    isComputing: Boolean,
    schedule: List<TripElement>
) {
  Card(
      modifier =
          Modifier.fillMaxWidth()
              .padding(horizontal = 20.dp, vertical = 12.dp)
              .testTag(TripInfoScreenTestTags.MAP_CARD),
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Box(
            modifier =
                Modifier.fillMaxWidth()
                    .height(200.dp)
                    .testTag(TripInfoScreenTestTags.MAP_CONTAINER)) {
              if (isComputing || schedule.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                  CircularProgressIndicator(
                      modifier = Modifier.testTag(TripInfoScreenTestTags.LOADING))
                }
              } else {
                MapScreen(
                    locations = mapState.locations,
                    drawRoute = mapState.drawRoute,
                    onUserLocationUpdate = onUserLocationUpdate)
              }

              NavigationModeToggle(
                  drawFromCurrentPosition = mapState.drawFromCurrentPosition,
                  onToggleNavMode = onToggleNavMode,
                  modifier = Modifier.align(Alignment.TopStart))

              // Fullscreen button
              IconButton(
                  onClick = onToggleFullscreen,
                  modifier =
                      Modifier.align(Alignment.BottomEnd)
                          .padding(12.dp)
                          .testTag(TripInfoScreenTestTags.FULLSCREEN_BUTTON)) {
                    Icon(
                        imageVector = Icons.Filled.ZoomOutMap,
                        contentDescription = stringResource(R.string.fullscreen),
                        tint = MaterialTheme.colorScheme.onBackground)
                  }
            }
      }
}

/** Displays the "Previous" and "Next" step buttons. */
@Composable
private fun StepControls(
    currentStepIndex: Int,
    scheduleSize: Int,
    isComputing: Boolean,
    onStepChange: (Int) -> Unit
) {
  Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(
            modifier = Modifier.testTag(TripInfoScreenTestTags.PREVIOUS_STEP),
            onClick = { if (currentStepIndex > 0) onStepChange(currentStepIndex - 1) },
            enabled = !isComputing && currentStepIndex > 0) {
              Text(stringResource(R.string.previous_step))
            }

        Spacer(Modifier.width(8.dp))

        Button(
            modifier = Modifier.testTag(TripInfoScreenTestTags.NEXT_STEP),
            onClick = {
              if (currentStepIndex < scheduleSize - 1) onStepChange(currentStepIndex + 1)
            },
            enabled = !isComputing && scheduleSize > 0 && currentStepIndex < scheduleSize - 1) {
              Text(stringResource(R.string.next_step))
            }
      }
}

/** A single row representing a step in the trip plan. */
@Composable
private fun StepRow(
    stepNumber: Int,
    subtitle: String,
    timeRange: String,
    leadingIcon: @Composable () -> Unit
) {
  ListItem(
      headlineContent = { Text("Step $stepNumber: $subtitle") },
      supportingContent = { Text(timeRange) },
      leadingContent = leadingIcon)
}

/**
 * A button to mark/unmark a trip as favorite.
 *
 * @param isFavorite Whether the trip is favorite.
 * @param onToggleFavorite Called when the user clicks the button.
 */
@Composable
private fun FavoriteButton(isFavorite: Boolean, onToggleFavorite: () -> Unit) {
  IconButton(
      onClick = onToggleFavorite,
      modifier = Modifier.testTag(TripInfoScreenTestTags.FAVORITE_BUTTON)) {
        if (isFavorite) {
          Icon(
              imageVector = Icons.Filled.Star,
              contentDescription = stringResource(R.string.unfavorite_icon),
              tint = favoriteIcon)
        } else {
          Icon(
              imageVector = Icons.Outlined.StarOutline,
              contentDescription = stringResource(R.string.favorite_icon_empty),
              tint = MaterialTheme.colorScheme.onBackground)
        }
      }
}

/**
 * A toggle button to switch between drawing the route from the start of the step or from the user's
 * current GPS location.
 *
 * @param drawFromCurrentPosition Whether the route should start from the current GPS position
 * @param onToggleNavMode Called when the user clicks the button.
 * @param modifier The modifier to apply.
 */
@Composable
fun NavigationModeToggle(
    drawFromCurrentPosition: Boolean,
    onToggleNavMode: () -> Unit,
    modifier: Modifier = Modifier
) {
  IconButton(onClick = onToggleNavMode, modifier = modifier.padding(12.dp)) {
    Icon(
        imageVector = Icons.Outlined.NearMe,
        contentDescription = "Toggle Navigation Mode",
        tint =
            if (drawFromCurrentPosition) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onBackground)
  }
}

/** Displays the map in a fullscreen overlay. */
@Composable
private fun FullScreenMap(
    mapState: MapState,
    onExit: () -> Unit,
    onUserLocationUpdate: (Point) -> Unit
) {
  Box(modifier = Modifier.fillMaxSize().testTag(TripInfoScreenTestTags.FULLSCREEN_MAP)) {
    MapScreen(
        locations = mapState.locations,
        drawRoute = mapState.drawRoute,
        onUserLocationUpdate = onUserLocationUpdate)
    IconButton(
        onClick = onExit,
        modifier =
            Modifier.align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag(TripInfoScreenTestTags.FULLSCREEN_EXIT)) {
          Icon(
              imageVector = Icons.Filled.ZoomInMap,
              contentDescription = stringResource(R.string.back_to_my_trips))
        }
  }
}

/**
 * Builds the list of locations that should be displayed on the map for the currently active step in
 * the trip schedule.
 *
 * For a TripSegment, if the user's current GPS position is available, the route will start from the
 * GPS location and end at the segment's destination. Otherwise, the original segment "from → to"
 * route is used.
 *
 * For a TripActivity, only the activity's location is returned.
 *
 * @param schedule The full list of trip elements forming the schedule.
 * @param idx The index of the currently selected step.
 * @param currentGps The current GPS position of the user, or null if unavailable.
 * @param drawFromCurrentPosition Whether the route should start from the current GPS position.
 * @return A list of one or two locations describing what should be drawn on the map.
 */
private fun mapLocationsForStep(
    schedule: List<TripElement>,
    idx: Int,
    currentGps: Point?,
    drawFromCurrentPosition: Boolean
): List<Location> {
  if (schedule.isEmpty()) return emptyList()

  val currentStep = schedule.getOrNull(idx) ?: return emptyList()

  return when (currentStep) {
    is TripElement.TripActivity -> listOf(currentStep.activity.location)
    is TripElement.TripSegment -> {
      if (drawFromCurrentPosition && currentGps != null) {
        listOf(
            Location(
                name = "Current Location",
                coordinate =
                    com.github.swent.swisstravel.model.trip.Coordinate(
                        currentGps.latitude(), currentGps.longitude())),
            currentStep.route.to)
      } else {
        listOf(currentStep.route.from, currentStep.route.to)
      }
    }
  }
}

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())

private fun fmtTime(ts: Timestamp?): String =
    ts?.toInstant()?.let { timeFormatter.format(it) } ?: "—"

private fun currentStepTitle(schedule: List<TripElement>, index: Int): String {
  if (schedule.isEmpty()) return "No steps planned."
  return when (val step = schedule.getOrNull(index)) {
    is TripElement.TripActivity -> step.activity.location.name
    is TripElement.TripSegment -> "Travel to ${step.route.to.name}"
    else -> "End of trip."
  }
}

private fun currentStepTime(schedule: List<TripElement>, index: Int): String {
  if (schedule.isEmpty()) return "—"
  return when (val step = schedule.getOrNull(index)) {
    is TripElement.TripActivity ->
        "${fmtTime(step.activity.startDate)} - ${fmtTime(step.activity.endDate)}"
    is TripElement.TripSegment ->
        "${fmtTime(step.route.startDate)} - ${fmtTime(step.route.endDate)}"
    else -> "—"
  }
}
