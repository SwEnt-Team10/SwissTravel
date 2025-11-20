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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.algorithm.orderlocations.orderLocations
import com.github.swent.swisstravel.algorithm.tripschedule.scheduleTrip
import com.github.swent.swisstravel.model.trip.Coordinate
import com.github.swent.swisstravel.model.trip.Location
import com.github.swent.swisstravel.model.trip.TripElement
import com.github.swent.swisstravel.ui.map.MapScreen
import com.github.swent.swisstravel.ui.theme.favoriteIcon
import com.google.firebase.Timestamp
import com.mapbox.geojson.Point
import java.time.Instant
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
) {
  Log.d("NAV_DEBUG", "Entered TripInfo with uid=$uid")

  LaunchedEffect(uid) { tripInfoViewModel.loadTripInfo(uid) }

  val ui by tripInfoViewModel.uiState.collectAsState()
  val context = LocalContext.current

  BackHandler(enabled = ui.fullscreen) { tripInfoViewModel.toggleFullscreen(false) }

  var isComputing by remember { mutableStateOf(false) }
  var schedule by remember { mutableStateOf<List<TripElement>>(emptyList()) }
  var computeError by remember { mutableStateOf<String?>(null) }
  var currentStepIndex by rememberSaveable { mutableIntStateOf(0) }
  var currentGpsPoint by remember { mutableStateOf<Point?>(null) }
  var drawFromCurrentPosition by remember { mutableStateOf(false) }
  val mapLocations: List<Location> =
      remember(schedule, currentStepIndex, currentGpsPoint, drawFromCurrentPosition) {
        mapLocationsForStep(
            schedule = schedule,
            idx = currentStepIndex,
            currentGps = currentGpsPoint,
            drawFromCurrentPosition = drawFromCurrentPosition)
      }
  val drawRoute: Boolean = mapLocations.size >= 2

  // handle VM error toasts
  LaunchedEffect(ui.errorMsg) {
    ui.errorMsg?.let {
      Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
      tripInfoViewModel.clearErrorMsg()
    }
  }

  LaunchedEffect(computeError) {
    computeError?.let {
      Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
      computeError = null
    }
  }

  // compute schedule once trip data available
  LaunchedEffect(ui.locations, ui.activities, ui.tripProfile) {
    if (ui.locations.isEmpty() || ui.tripProfile == null) return@LaunchedEffect
    isComputing = true
    computeError = null
    schedule = emptyList()
    currentStepIndex = 0

    val unique = (ui.activities.map { it.location } + ui.locations).distinctBy { it.coordinate }
    val start = ui.locations.first()
    val end = ui.locations.last()

    orderLocations(context, unique, start, end) { ordered ->
      if (ordered.totalDuration < 0) {
        computeError = "Failed to compute route order."
        isComputing = false
        return@orderLocations
      }
      try {
        schedule =
            scheduleTrip(
                tripProfile = requireNotNull(ui.tripProfile),
                ordered = ordered,
                activities = ui.activities)
        currentStepIndex = 0
      } catch (e: Exception) {
        computeError = "Failed to schedule trip: ${e.message}"
      } finally {
        isComputing = false
      }
    }
  }

  Scaffold(
      containerColor = MaterialTheme.colorScheme.background,
      topBar = {
        if (!ui.fullscreen) {
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
                      onClick = { onMyTrips() },
                      modifier = Modifier.testTag(TripInfoScreenTestTags.BACK_BUTTON)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back_to_my_trips),
                            tint = MaterialTheme.colorScheme.onBackground)
                      }
                }
              },
              actions = {
                val isFavorite = ui.isFavorite
                FavoriteButton(
                    isFavorite = isFavorite,
                    onToggleFavorite = { tripInfoViewModel.toggleFavorite() },
                )
                IconButton(
                    onClick = { onEditTrip() },
                    modifier = Modifier.testTag(TripInfoScreenTestTags.EDIT_BUTTON)) {
                      Icon(
                          imageVector = Icons.Outlined.Edit,
                          contentDescription = stringResource(R.string.edit_trip),
                          tint = MaterialTheme.colorScheme.onBackground)
                    }
              })
        }
      }) { pd ->
        Box(Modifier.fillMaxSize().padding(pd)) {
          LazyColumn(
              modifier = Modifier.fillMaxSize().testTag(TripInfoScreenTestTags.LAZY_COLUMN),
              horizontalAlignment = Alignment.Start,
              contentPadding = PaddingValues(bottom = 24.dp)) {
                if (ui.locations.isEmpty()) {
                  item {
                    Text(
                        text = stringResource(R.string.no_locations_available),
                        modifier = Modifier.testTag(TripInfoScreenTestTags.NO_LOCATIONS))
                  }
                } else {
                  item {
                    Column(
                        modifier =
                            Modifier.fillMaxWidth()
                                .padding(start = 16.dp, top = 16.dp, bottom = 8.dp)) {
                          Text(
                              text = stringResource(R.string.current_step),
                              modifier = Modifier.testTag(TripInfoScreenTestTags.CURRENT_STEP),
                              style = MaterialTheme.typography.displaySmall)

                          Text(
                              text =
                                  "Step ${if (schedule.isEmpty()) 0 else currentStepIndex + 1}: " +
                                      currentStepTitle(schedule, currentStepIndex),
                              modifier =
                                  Modifier.padding(top = 4.dp)
                                      .testTag(TripInfoScreenTestTags.LOCATION_NAME),
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

                  if (!ui.fullscreen) {
                    // Map card
                    item {
                      Card(
                          modifier =
                              Modifier.fillMaxWidth()
                                  .padding(horizontal = 20.dp, vertical = 12.dp)
                                  .testTag(TripInfoScreenTestTags.MAP_CARD),
                          shape = RoundedCornerShape(12.dp),
                          colors =
                              CardDefaults.cardColors(
                                  containerColor = MaterialTheme.colorScheme.surface)) {
                            Box(
                                modifier =
                                    Modifier.fillMaxWidth()
                                        .height(200.dp)
                                        .testTag(TripInfoScreenTestTags.MAP_CONTAINER)) {
                                  if (isComputing || schedule.isEmpty()) {
                                    Box(
                                        Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center) {
                                          CircularProgressIndicator(
                                              modifier =
                                                  Modifier.testTag(TripInfoScreenTestTags.LOADING))
                                        }
                                  } else {
                                    MapScreen(
                                        locations = mapLocations,
                                        drawRoute = drawRoute,
                                        onUserLocationUpdate = { p -> currentGpsPoint = p })
                                  }

                                  NavigationModeToggle(
                                      drawFromCurrentPosition = drawFromCurrentPosition,
                                      onToggleNavMode = {
                                        drawFromCurrentPosition = !drawFromCurrentPosition
                                      },
                                      modifier = Modifier.align(Alignment.TopStart))

                                  // Fullscreen button
                                  IconButton(
                                      onClick = { tripInfoViewModel.toggleFullscreen(true) },
                                      modifier =
                                          Modifier.align(Alignment.BottomEnd)
                                              .padding(12.dp)
                                              .testTag(TripInfoScreenTestTags.FULLSCREEN_BUTTON)) {
                                        Icon(
                                            imageVector = Icons.Filled.ZoomOutMap,
                                            contentDescription =
                                                stringResource(R.string.fullscreen),
                                            tint = MaterialTheme.colorScheme.onBackground)
                                      }
                                }
                          }
                    }

                    // Step controls
                    item {
                      Row(
                          modifier =
                              Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                          horizontalArrangement = Arrangement.SpaceBetween,
                          verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(
                                modifier = Modifier.testTag(TripInfoScreenTestTags.PREVIOUS_STEP),
                                onClick = { if (currentStepIndex > 0) currentStepIndex-- },
                                enabled = !isComputing && currentStepIndex > 0) {
                                  Text(stringResource(R.string.previous_step))
                                }

                            Spacer(Modifier.width(8.dp))

                            Button(
                                modifier = Modifier.testTag(TripInfoScreenTestTags.NEXT_STEP),
                                onClick = {
                                  if (currentStepIndex < schedule.lastIndex) currentStepIndex++
                                },
                                enabled =
                                    !isComputing &&
                                        schedule.isNotEmpty() &&
                                        currentStepIndex < schedule.lastIndex) {
                                  Text(stringResource(R.string.next_step))
                                }
                          }
                    }
                  }
                }

                // Steps list
                itemsIndexed(schedule) { idx, el ->
                  val stepNo = idx + 1
                  when (el) {
                    is TripElement.TripActivity -> {
                      StepRow(
                          stepNumber = stepNo,
                          subtitle =
                              el.activity.location.name +
                                  (if (el.activity.description.isBlank()) ""
                                  else " — ${el.activity.description}"),
                          timeRange =
                              "${fmtTime(el.activity.startDate)} – ${fmtTime(el.activity.endDate)}",
                          leadingIcon = {
                            Icon(imageVector = Icons.Filled.Attractions, contentDescription = null)
                          },
                          onClick = { onActivityClick(el) })
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

        // Fullscreen overlay
        if (ui.fullscreen) {
          Box(modifier = Modifier.fillMaxSize().testTag(TripInfoScreenTestTags.FULLSCREEN_MAP)) {
            MapScreen(
                locations = mapLocations,
                drawRoute = drawRoute,
                onUserLocationUpdate = { p -> currentGpsPoint = p })

            NavigationModeToggle(
                drawFromCurrentPosition = drawFromCurrentPosition,
                onToggleNavMode = { drawFromCurrentPosition = !drawFromCurrentPosition },
                modifier = Modifier.align(Alignment.TopStart))

            // Exit fullscreen
            IconButton(
                onClick = { tripInfoViewModel.toggleFullscreen(false) },
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
      }
}

/**
 * Shows a non-clickable row in the step list.
 *
 * @param stepNumber The step number.
 * @param subtitle The subtitle.
 * @param timeRange The time range.
 * @param leadingIcon The leading icon.
 */
@Composable
private fun StepRow(
    stepNumber: Int,
    subtitle: String,
    timeRange: String,
    leadingIcon: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
  ListItem(
      headlineContent = { stringResource(R.string.step_info, stepNumber) },
      supportingContent = {
        Column {
          Text(subtitle, style = MaterialTheme.typography.bodyMedium)
          Text(
              timeRange,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      },
      leadingContent = leadingIcon,
      modifier =
          Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp).let { base ->
            if (onClick != null) base.clickable(onClick = onClick) else base
          })
}

/**
 * A button to mark/unmark a trip as favorite.
 *
 * @param isFavorite Whether the trip is favorite.
 * @param onToggleFavorite Called when the user clicks the button.
 */
@Composable
private fun FavoriteButton(
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
) {
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
 * A button to toggle the navigation mode.
 *
 * @param drawFromCurrentPosition Whether the route should start from the current GPS position
 * @param onToggleNavMode Called when the user clicks the button.
 * @param modifier The modifier to apply.
 */
@Composable
private fun NavigationModeToggle(
    drawFromCurrentPosition: Boolean,
    onToggleNavMode: () -> Unit,
    modifier: Modifier = Modifier
) {
  IconButton(onClick = { onToggleNavMode() }, modifier = modifier.padding(12.dp)) {
    Icon(
        imageVector = if (drawFromCurrentPosition) Icons.Filled.Route else Icons.Outlined.NearMe,
        contentDescription =
            if (drawFromCurrentPosition) stringResource(R.string.draw_from_current_position)
            else stringResource(R.string.draw_from_previous_step),
        tint = MaterialTheme.colorScheme.onBackground)
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
  val i = idx.coerceIn(0, schedule.lastIndex)

  return when (val el = schedule[i]) {
    is TripElement.TripSegment -> {
      val startLocation =
          if (drawFromCurrentPosition) {
            currentGps?.let {
              Location(
                  coordinate = Coordinate(latitude = it.latitude(), longitude = it.longitude()),
                  name = "Your position")
            } ?: el.route.from
          } else {
            el.route.from
          }
      listOf(startLocation, el.route.to)
    }
    is TripElement.TripActivity -> listOf(el.activity.location)
  }
}

/* ---------- Small helpers ---------- */

/**
 * Formats the time into a readable string.
 *
 * @param ts The timestamp to format.
 */
private fun fmtTime(ts: Timestamp?): String {
  val dt =
      ts?.let {
        Instant.ofEpochSecond(it.seconds, it.nanoseconds.toLong())
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
      } ?: return "–"
  return dt.format(DateTimeFormatter.ofPattern("dd LLL yyyy HH:mm"))
}

/**
 * Computes the title of the current step in the schedule.
 *
 * @param schedule The schedule to use.
 * @param idx The index of the current step.
 */
private fun currentStepTitle(schedule: List<TripElement>, idx: Int): String {
  if (schedule.isEmpty()) return "—"
  val i = idx.coerceIn(0, schedule.lastIndex)
  return when (val el = schedule[i]) {
    is TripElement.TripSegment -> "${el.route.from.name} → ${el.route.to.name}"
    is TripElement.TripActivity -> el.activity.location.name
  }
}

/**
 * Computes the time range of the current step in the schedule.
 *
 * @param schedule The schedule to use.
 * @param idx The index of the current step.
 */
private fun currentStepTime(schedule: List<TripElement>, idx: Int): String {
  if (schedule.isEmpty()) return "—"
  val i = idx.coerceIn(0, schedule.lastIndex)
  return when (val el = schedule[i]) {
    is TripElement.TripSegment -> "${fmtTime(el.route.startDate)} – ${fmtTime(el.route.endDate)}"
    is TripElement.TripActivity ->
        "${fmtTime(el.activity.startDate)} – ${fmtTime(el.activity.endDate)}"
  }
}
