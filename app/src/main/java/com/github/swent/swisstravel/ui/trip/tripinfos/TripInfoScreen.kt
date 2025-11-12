package com.github.swent.swisstravel.ui.trip.tripinfos

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Attractions
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.algorithm.orderlocations.orderLocations
import com.github.swent.swisstravel.algorithm.tripschedule.scheduleTrip
import com.github.swent.swisstravel.model.trip.Location
import com.github.swent.swisstravel.model.trip.TripElement
import com.github.swent.swisstravel.ui.map.MapScreen
import com.github.swent.swisstravel.ui.theme.favoriteIcon
import com.google.firebase.Timestamp
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

  var isComputing by remember { mutableStateOf(false) }
  var schedule by remember { mutableStateOf<List<TripElement>>(emptyList()) }
  var computeError by remember { mutableStateOf<String?>(null) }

  var currentStepIndex by rememberSaveable { mutableIntStateOf(0) }

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
    val start = unique.first()
    val end = unique.last()

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

  val mapLocations: List<Location> =
      remember(schedule, currentStepIndex) { mapLocationsForStep(schedule, currentStepIndex) }

  LaunchedEffect(showMap) {
    if (!showMap) {
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

                  if (!fullscreen) {
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
                                  containerColor = MaterialTheme.colorScheme.surface),
                          elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
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
                                  } else if (showMap) {
                                    MapScreen(locations = mapLocations)
                                  }

                                  // Fullscreen button
                                  IconButton(
                                      onClick = { fullscreen = true },
                                      modifier =
                                          Modifier.align(Alignment.BottomEnd)
                                              .padding(12.dp)
                                              .testTag(TripInfoScreenTestTags.FULLSCREEN_BUTTON)) {
                                        Icon(
                                            imageVector = Icons.Filled.Fullscreen,
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
                                onClick = { if (currentStepIndex > 0) currentStepIndex-- },
                                enabled = !isComputing && currentStepIndex > 0) {
                                  Text(stringResource(R.string.previous_step))
                                }

                            Spacer(Modifier.width(8.dp))

                            Button(
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
                            ThumbnailOrIcon(
                                url = el.activity.imageUrls.firstOrNull(),
                                fallbackIcon = Icons.Filled.Attractions,
                                contentDescription =
                                    if (el.activity.imageUrls.isEmpty())
                                        stringResource(R.string.icon)
                                    else stringResource(R.string.image))
                          })
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
        if (fullscreen) {
          Box(modifier = Modifier.fillMaxSize().testTag(TripInfoScreenTestTags.FULLSCREEN_MAP)) {
            MapScreen(locations = mapLocations)

            // Exit fullscreen arrow
            IconButton(
                onClick = { fullscreen = false },
                modifier =
                    Modifier.align(Alignment.TopStart)
                        .padding(16.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .testTag(TripInfoScreenTestTags.FULLSCREEN_EXIT)) {
                  Icon(
                      imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                      contentDescription = stringResource(R.string.back_to_my_trips),
                      tint = MaterialTheme.colorScheme.onPrimary)
                }
          }
        }
      }
}

/* ---------- Helpers to decide which locations to show on the map ---------- */

private fun mapLocationsForStep(schedule: List<TripElement>, idx: Int): List<Location> {
  if (schedule.isEmpty()) return emptyList()
  val i = idx.coerceIn(0, schedule.lastIndex)
  return when (val el = schedule[i]) {
    is TripElement.TripSegment -> listOf(el.route.from, el.route.to)
    is TripElement.TripActivity -> listOf(el.activity.location)
  }
}

/* ---------- Non-clickable Step row ---------- */

@Composable
private fun StepRow(
    stepNumber: Int,
    subtitle: String,
    timeRange: String,
    leadingIcon: (@Composable () -> Unit)? = null
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
      modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp))
}

/** A button to mark/unmark a trip as favorite. */
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

@Composable
private fun ThumbnailOrIcon(
    url: String?,
    fallbackIcon: ImageVector,
    contentDescription: String? = null
) {
  if (url.isNullOrBlank()) {
    Icon(imageVector = fallbackIcon, contentDescription = contentDescription)
  } else {
    AsyncImage(
        model = url,
        contentDescription = contentDescription,
        modifier = Modifier.size(32.dp).clip(CircleShape))
  }
}

/* ---------- Small helpers ---------- */

private fun fmtTime(ts: Timestamp?): String {
  val dt =
      ts?.let {
        Instant.ofEpochSecond(it.seconds, it.nanoseconds.toLong())
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
      } ?: return "–"
  return dt.format(DateTimeFormatter.ofPattern("dd LLL yyyy HH:mm"))
}

private fun currentStepTitle(schedule: List<TripElement>, idx: Int): String {
  if (schedule.isEmpty()) return "—"
  val i = idx.coerceIn(0, schedule.lastIndex)
  return when (val el = schedule[i]) {
    is TripElement.TripSegment -> "${el.route.from.name} → ${el.route.to.name}"
    is TripElement.TripActivity -> el.activity.location.name
  }
}

private fun currentStepTime(schedule: List<TripElement>, idx: Int): String {
  if (schedule.isEmpty()) return "—"
  val i = idx.coerceIn(0, schedule.lastIndex)
  return when (val el = schedule[i]) {
    is TripElement.TripSegment -> "${fmtTime(el.route.startDate)} – ${fmtTime(el.route.endDate)}"
    is TripElement.TripActivity ->
        "${fmtTime(el.activity.startDate)} – ${fmtTime(el.activity.endDate)}"
  }
}
