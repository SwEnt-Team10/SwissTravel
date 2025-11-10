package com.github.swent.swisstravel.ui.trip.tripinfos

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.algorithm.orderlocations.OrderedRoute
import com.github.swent.swisstravel.algorithm.orderlocations.orderLocations
import com.github.swent.swisstravel.algorithm.tripschedule.scheduleTrip
import com.github.swent.swisstravel.model.trip.Location
import com.github.swent.swisstravel.model.trip.TripElement
import com.github.swent.swisstravel.ui.map.NavigationMapScreen
import com.github.swent.swisstravel.ui.theme.favoriteIcon
import com.google.firebase.Timestamp
import java.time.*
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
  const val FIRST_LOCATION_BOX = "tripInfoScreenFirstLocationBox"
  const val LOCATION_NAME = "tripInfoScreenLocationName"
  const val MAP_CARD = "tripInfoScreenMapCard"
  const val MAP_CONTAINER = "tripInfoScreenMapContainer"
  const val MAP_BOX = "tripInfoScreenMapBox"
  const val RESET_CHIP = "tripInfoScreenResetChip"

  private const val STEP_PREFIX = "tripInfoScreenStep_"

  fun stepTag(index: Int) = "$STEP_PREFIX$index"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripInfoScreen(
    uid: String?,
    tripInfoViewModel: TripInfoViewModelContract = viewModel<TripInfoViewModel>(),
    onMyTrips: () -> Unit = {},
    onEditTrip: () -> Unit = {},
) {
  LaunchedEffect(uid) { tripInfoViewModel.loadTripInfo(uid) }

  val ui by tripInfoViewModel.uiState.collectAsState()
  val context = LocalContext.current

  var showMap by remember { mutableStateOf(true) }
  var fullscreen by rememberSaveable { mutableStateOf(false) }

  // schedule + ordering state
  var isComputing by remember { mutableStateOf(false) }
  var orderedRoute by remember { mutableStateOf<OrderedRoute?>(null) }
  var schedule by remember { mutableStateOf<List<TripElement>>(emptyList()) }
  var computeError by remember { mutableStateOf<String?>(null) }

  // map preview state (when null → show full trip route)
  var previewLocations by remember { mutableStateOf<List<Location>?>(null) }

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
    isComputing = true
    computeError = null
    schedule = emptyList()
    orderedRoute = null

    val unique = ui.locations.distinctBy { it.coordinate }
    val start = unique.first()
    val end = unique.last()

    orderLocations(context, unique, start, end) { ordered ->
      orderedRoute = ordered
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
      } catch (e: Exception) {
        computeError = "Failed to schedule trip: ${e.message}"
      } finally {
        isComputing = false
      }
    }
  }

  // map locations to feed (preview if set, else all trip locations)
  val mapLocations: List<Location> = previewLocations ?: ui.locations

  LaunchedEffect(showMap) {
    if (!showMap) {
      withFrameNanos {}
      onMyTrips()
    }
  }

  Scaffold(
      containerColor = MaterialTheme.colorScheme.background,
      topBar = {
        if (!fullscreen) {
          TopAppBar(
              title = {
                Text(
                    text = ui.name,
                    modifier = Modifier.testTag(TripInfoScreenTestTags.TITLE),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground)
              },
              navigationIcon = {
                IconButton(
                    onClick = { showMap = false },
                    modifier = Modifier.testTag(TripInfoScreenTestTags.BACK_BUTTON)) {
                      Icon(
                          imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                          contentDescription = stringResource(R.string.back_to_my_trips),
                          tint = MaterialTheme.colorScheme.onBackground)
                    }
              },
              actions = {
                val isFavorite = ui.isFavorite
                FavoriteButton(
                    isFavorite = isFavorite,
                    onToggleFavorite = { tripInfoViewModel.toggleFavorite() },
                    testTag = TripInfoScreenTestTags.FAVORITE_BUTTON)
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
                  // Title + first location (as before)
                  item {
                    Text(
                        text = stringResource(R.string.current_step),
                        modifier =
                            Modifier.fillMaxWidth()
                                .padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                                .testTag(TripInfoScreenTestTags.CURRENT_STEP),
                        style = MaterialTheme.typography.displaySmall)
                  }
                  item {
                    Box(
                        modifier =
                            Modifier.fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .testTag(TripInfoScreenTestTags.FIRST_LOCATION_BOX)) {
                          if (ui.locations.isNotEmpty()) {
                            Text(
                                text = ui.locations[0].name,
                                modifier =
                                    Modifier.align(Alignment.CenterStart)
                                        .testTag(TripInfoScreenTestTags.LOCATION_NAME),
                                style = MaterialTheme.typography.headlineMedium)
                          }
                        }
                  }

                  // Inline map card
                  if (!fullscreen) {
                    item {
                      Card(
                          modifier =
                              Modifier.fillMaxWidth()
                                  .padding(horizontal = 20.dp, vertical = 12.dp)
                                  .testTag(TripInfoScreenTestTags.MAP_CARD),
                          shape = RoundedCornerShape(12.dp)) {
                            Box(
                                modifier =
                                    Modifier.fillMaxWidth()
                                        .height(200.dp)
                                        .testTag(TripInfoScreenTestTags.MAP_CONTAINER)) {
                                  if (showMap) {
                                    NavigationMapScreen(
                                        locations = mapLocations.ifEmpty { ui.locations })
                                  }

                                  // Reset preview chip (shows only when a preview is active)
                                  if (previewLocations != null) {
                                    AssistChip(
                                        onClick = { previewLocations = null },
                                        label = { Text(stringResource(R.string.reset_route)) },
                                        leadingIcon = {
                                          Icon(
                                              imageVector = Icons.Outlined.Refresh,
                                              contentDescription = null)
                                        },
                                        modifier =
                                            Modifier.align(Alignment.TopStart)
                                                .padding(12.dp)
                                                .testTag(TripInfoScreenTestTags.RESET_CHIP))
                                  }

                                  // Fullscreen button (bottom-right)
                                  IconButton(
                                      onClick = { fullscreen = true },
                                      modifier =
                                          Modifier.align(Alignment.BottomEnd)
                                              .padding(12.dp)
                                              .testTag("fullscreenToggle")) {
                                        Icon(
                                            imageVector = Icons.Filled.Fullscreen,
                                            contentDescription =
                                                stringResource(R.string.fullscreen),
                                            tint = MaterialTheme.colorScheme.onBackground)
                                      }
                                }
                          }
                    }
                  }

                  // Schedule block header / loading / error
                  item {
                    when {
                      isComputing -> {
                        Row(
                            modifier =
                                Modifier.fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                              CircularProgressIndicator(
                                  strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                              Spacer(Modifier.width(8.dp))
                              Text(stringResource(R.string.computing_schedule))
                            }
                      }
                      computeError != null -> {
                        Text(
                            text = computeError!!,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                      }
                      schedule.isNotEmpty() -> {
                        Text(
                            text = stringResource(R.string.itinerary),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                      }
                    }
                  }

                  // Steps list (each row advances the “next step”)
                  items(schedule, key = { it.hashCode() }) { el ->
                    when (el) {
                      is TripElement.TripActivity -> {
                        StepRow(
                            title = el.activity.location.name,
                            subtitle = el.activity.description,
                            timeRange =
                                "${fmtTime(el.activity.startDate)} – ${fmtTime(el.activity.endDate)}",
                            onClick = {
                              // preview just this activity’s location
                              previewLocations = listOf(el.activity.location)
                            })
                      }
                      is TripElement.TripSegment -> {
                        StepRow(
                            title = "${el.route.from.name} → ${el.route.to.name}",
                            subtitle =
                                stringResource(R.string.about_minutes, el.route.durationMinutes),
                            timeRange =
                                "${fmtTime(el.route.startDate)} – ${fmtTime(el.route.endDate)}",
                            leadingIcon = { Icon(Icons.Filled.Route, contentDescription = null) },
                            onClick = {
                              // preview only the current segment on the map
                              previewLocations = listOf(el.route.from, el.route.to)
                            })
                      }
                    }
                  }
                }
              }

          // Fullscreen overlay uses SAME preview as inline
          if (fullscreen) {
            Box(modifier = Modifier.fillMaxSize().testTag("FullScreenMap")) {
              NavigationMapScreen(locations = mapLocations.ifEmpty { ui.locations })

              // Exit fullscreen arrow (TOP-LEFT)
              IconButton(
                  onClick = { fullscreen = false },
                  modifier =
                      Modifier.align(Alignment.TopStart).padding(16.dp).testTag("exitFullscreen")) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back_to_my_trips),
                        tint = MaterialTheme.colorScheme.onBackground)
                  }

              // Reset preview in fullscreen too (TOP-RIGHT)
              if (previewLocations != null) {
                AssistChip(
                    onClick = { previewLocations = null },
                    label = { Text(stringResource(R.string.reset_route)) },
                    leadingIcon = {
                      Icon(imageVector = Icons.Outlined.Refresh, contentDescription = null)
                    },
                    modifier = Modifier.align(Alignment.TopEnd).padding(16.dp))
              }
            }
          }
        }
      }
}

/* ---------- Reusable Step row ---------- */

@Composable
private fun StepRow(
    title: String,
    subtitle: String,
    timeRange: String,
    leadingIcon: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
  ListItem(
      headlineContent = { Text(title) },
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
      modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).clickableWithRipple(onClick))
}

@Composable
private fun FavoriteButton(
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    testTag: String? = null
) {
  IconButton(
      onClick = onToggleFavorite,
      modifier = if (testTag != null) Modifier.testTag(testTag) else Modifier) {
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

/* ---------- Small helpers ---------- */

@Composable
private fun Modifier.clickableWithRipple(onClick: () -> Unit): Modifier =
    this.then(
        Modifier.padding(vertical = 2.dp).let { base -> clickable(onClick = onClick).then(base) })

private fun fmtTime(ts: Timestamp?): String {
  val dt =
      ts?.let {
        Instant.ofEpochSecond(it.seconds, it.nanoseconds.toLong())
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
      } ?: return "–"
  return dt.format(DateTimeFormatter.ofPattern("HH:mm"))
}
