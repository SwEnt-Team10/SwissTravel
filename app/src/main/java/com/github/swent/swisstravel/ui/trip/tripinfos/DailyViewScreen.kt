package com.github.swent.swisstravel.ui.trip.tripinfos

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Attractions
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ZoomInMap
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.model.trip.Coordinate
import com.github.swent.swisstravel.model.trip.Location
import com.github.swent.swisstravel.model.trip.TripElement
import com.github.swent.swisstravel.ui.map.MapScreen
import com.github.swent.swisstravel.ui.theme.favoriteIcon
import com.mapbox.geojson.Point
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Object containing test tags for UI testing of the DailyViewScreen. */
object DailyViewScreenTestTags {
  const val TITLE = "dailyViewScreenTitle"
  const val BACK_BUTTON = "dailyViewScreenBackButton"
  const val FAVORITE_BUTTON = "dailyViewScreenFavoriteButton"
  const val EDIT_BUTTON = "dailyViewScreenEditButton"
  const val LAZY_COLUMN = "dailyViewScreenLazyColumn"
  const val NO_LOCATIONS = "dailyViewScreenNoLocations"
  const val DAY_NAVIGATOR = "dailyViewScreenDayNavigator"
  const val PREV_DAY_BUTTON = "dailyViewScreenPrevDayButton"
  const val NEXT_DAY_BUTTON = "dailyViewScreenNextDayButton"
  const val CURRENT_DAY_TEXT = "dailyViewScreenCurrentDayText"
  const val MAP_CARD = "dailyViewScreenMapCard"
  const val MAP_CONTAINER = "dailyViewScreenMapContainer"
  const val FULLSCREEN_BUTTON = "dailyViewScreenFullscreenButton"
  const val LOADING = "dailyViewScreenLoading"
  const val FULLSCREEN_MAP = "dailyViewScreenFullScreenMap"
  const val FULLSCREEN_EXIT = "dailyViewScreenFullScreenExit"
  const val STEP_CARD = "dailyViewScreenStepCard"
}

/**
 * A screen that displays the daily itinerary of a trip. It shows a map of the day's route and a
 * list of travel segments and activities. Note : This class was partially written with the help of
 * an AI. If you have any question, don't hesitate to contact the author of the class (@JstnFv)
 *
 * @param uid The unique identifier of the trip to display.
 * @param tripInfoViewModel The ViewModel providing data and logic for the trip info.
 * @param onMyTrips A callback to navigate back to the list of user's trips.
 * @param onEditTrip A callback to navigate to the trip editing screen.
 * @param isOnCurrentTripScreen A boolean indicating if this screen is part of the current trip
 *   flow, which affects the visibility of the back button.
 * @param onActivityClick A callback invoked when a user clicks on a trip activity for more details.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyViewScreen(
    uid: String?,
    tripInfoViewModel: TripInfoViewModelContract = viewModel<TripInfoViewModel>(),
    onMyTrips: () -> Unit = {},
    onEditTrip: () -> Unit = {},
    isOnCurrentTripScreen: Boolean = false,
    onActivityClick: (TripElement.TripActivity) -> Unit = {}
) {
  LaunchedEffect(uid) { tripInfoViewModel.loadTripInfo(uid) }

  val ui by tripInfoViewModel.uiState.collectAsState()
  val context = LocalContext.current

  BackHandler(enabled = ui.fullscreen) { tripInfoViewModel.toggleFullscreen(false) }

  var isComputing by remember { mutableStateOf(false) }
  var schedule by remember { mutableStateOf<List<TripElement>>(emptyList()) }
  var currentDayIndex by rememberSaveable { mutableIntStateOf(0) }
  var currentGpsPoint by remember { mutableStateOf<Point?>(null) }
  var drawFromCurrentPosition by remember { mutableStateOf(false) }
  var selectedStep by remember { mutableStateOf<TripElement?>(null) }

  // Group schedule by day
  val groupedSchedule =
      remember(schedule) {
        schedule
            .groupBy {
              it.startDate.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
            }
            .toSortedMap()
      }
  val days = remember(groupedSchedule) { groupedSchedule.keys.toList() }
  val currentDay = days.getOrNull(currentDayIndex)
  val dailySteps =
      if (currentDay != null) groupedSchedule[currentDay] ?: emptyList() else emptyList()

  // Map state logic
  val mapState =
      remember(dailySteps, currentGpsPoint, drawFromCurrentPosition, selectedStep) {
        val locations =
            if (selectedStep != null) {
              mapLocationsForDay(listOf(selectedStep!!), currentGpsPoint, drawFromCurrentPosition)
            } else {
              mapLocationsForDay(dailySteps, currentGpsPoint, drawFromCurrentPosition)
            }
        MapState(
            locations = locations,
            drawRoute = dailySteps.isNotEmpty(),
            drawFromCurrentPosition = drawFromCurrentPosition,
            isLoading = isComputing || (ui.locations.isNotEmpty() && schedule.isEmpty()))
      }

  LaunchedEffect(ui.errorMsg) {
    ui.errorMsg?.let {
      Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
      tripInfoViewModel.clearErrorMsg()
    }
  }

  LaunchedEffect(ui.locations, ui.activities, ui.tripProfile) {
    if (ui.locations.isEmpty() || ui.tripProfile == null) return@LaunchedEffect
    isComputing = true
    val tripSegments = ui.routeSegments.map { TripElement.TripSegment(it) }
    val tripActivities = ui.activities.map { TripElement.TripActivity(it) }

    val newSchedule = (tripSegments + tripActivities).sortedBy { it.startDate }
    if (schedule != newSchedule) {
      val newGrouped =
          newSchedule.groupBy {
            it.startDate.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
          }
      val newDaysCount = newGrouped.keys.size

      schedule = newSchedule
      // Reset only if necessary, or keep index if within bounds of the NEW schedule
      if (currentDayIndex >= newDaysCount) {
        currentDayIndex = 0
      }
    }
    isComputing = false
  }

  Scaffold(
      containerColor = MaterialTheme.colorScheme.background,
      topBar = {
        if (!ui.fullscreen) {
          DailyViewTopAppBar(
              ui = ui,
              isOnCurrentTripScreen = isOnCurrentTripScreen,
              onBack = onMyTrips,
              onToggleFavorite = { tripInfoViewModel.toggleFavorite() },
              onEdit = onEditTrip)
        }
      }) { pd ->
        Box(Modifier.fillMaxSize().padding(pd)) {
          if (ui.locations.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
              Text(
                  text = stringResource(R.string.no_locations_available),
                  modifier =
                      Modifier.padding(dimensionResource(R.dimen.daily_view_padding))
                          .testTag(DailyViewScreenTestTags.NO_LOCATIONS))
            }
          } else {
            Column(Modifier.fillMaxSize()) {
              // Day Navigator
              DayNavigator(
                  currentDayIndex = currentDayIndex,
                  days = days,
                  onDayChange = { currentDayIndex = it })

              if (!ui.fullscreen) {
                // Map Card
                DailyMapCard(
                    mapState = mapState,
                    onToggleFullscreen = { tripInfoViewModel.toggleFullscreen(true) },
                    onToggleNavMode = { drawFromCurrentPosition = !drawFromCurrentPosition },
                    onUserLocationUpdate = { currentGpsPoint = it },
                    isComputing = isComputing,
                    hasSteps = dailySteps.isNotEmpty())
              }

              // Daily Steps List
              LazyColumn(
                  modifier = Modifier.fillMaxSize().testTag(DailyViewScreenTestTags.LAZY_COLUMN),
                  contentPadding =
                      PaddingValues(
                          bottom =
                              dimensionResource(R.dimen.daily_view_daily_step_bottom_padding))) {
                    itemsIndexed(dailySteps) { idx, el ->
                      DailyStepCard(
                          stepNumber = idx + 1,
                          element = el,
                          isSelected = selectedStep == el,
                          onMapClick = { selectedStep = if (selectedStep == el) null else el },
                          onDetailsClick = {
                            if (el is TripElement.TripActivity) {
                              onActivityClick(el)
                            }
                          })
                    }
                  }
            }
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

/**
 * The top app bar for the Daily View screen. Displays the trip title and action buttons for
 * navigation, favoriting, and editing.
 *
 * @param ui The current UI state of the trip.
 * @param isOnCurrentTripScreen A boolean to control the visibility of the back button.
 * @param onBack Callback for when the back button is clicked.
 * @param onToggleFavorite Callback for when the favorite button is clicked.
 * @param onEdit Callback for when the edit button is clicked.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DailyViewTopAppBar(
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
            modifier = Modifier.testTag(DailyViewScreenTestTags.TITLE),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground)
      },
      navigationIcon = {
        if (!isOnCurrentTripScreen) {
          IconButton(
              onClick = onBack, modifier = Modifier.testTag(DailyViewScreenTestTags.BACK_BUTTON)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back_to_my_trips),
                    tint = MaterialTheme.colorScheme.onBackground)
              }
        }
      },
      actions = {
        IconButton(
            onClick = onToggleFavorite,
            modifier = Modifier.testTag(DailyViewScreenTestTags.FAVORITE_BUTTON)) {
              if (ui.isFavorite) {
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
        IconButton(
            onClick = onEdit, modifier = Modifier.testTag(DailyViewScreenTestTags.EDIT_BUTTON)) {
              Icon(
                  imageVector = Icons.Outlined.Edit,
                  contentDescription = stringResource(R.string.edit_trip),
                  tint = MaterialTheme.colorScheme.onBackground)
            }
      })
}

/**
 * A navigator component to switch between different days of the trip.
 *
 * @param currentDayIndex The index of the currently displayed day.
 * @param days A list of all available dates for the trip.
 * @param onDayChange A callback invoked with the new day index when the user navigates.
 */
@Composable
private fun DayNavigator(currentDayIndex: Int, days: List<LocalDate>, onDayChange: (Int) -> Unit) {
  val currentDay = days.getOrNull(currentDayIndex)
  val dateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d")

  Row(
      modifier =
          Modifier.fillMaxWidth()
              .padding(
                  horizontal =
                      dimensionResource(R.dimen.daily_view_day_navigation_horizontal_padding),
                  vertical = dimensionResource(R.dimen.daily_view_day_navigation_vertical_padding))
              .testTag(DailyViewScreenTestTags.DAY_NAVIGATOR),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            onClick = { if (currentDayIndex > 0) onDayChange(currentDayIndex - 1) },
            enabled = currentDayIndex > 0,
            modifier = Modifier.testTag(DailyViewScreenTestTags.PREV_DAY_BUTTON)) {
              Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Day")
            }

        Text(
            text = currentDay?.format(dateFormatter) ?: "No Days",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.testTag(DailyViewScreenTestTags.CURRENT_DAY_TEXT))

        IconButton(
            onClick = { if (currentDayIndex < days.size - 1) onDayChange(currentDayIndex + 1) },
            enabled = currentDayIndex < days.size - 1,
            modifier = Modifier.testTag(DailyViewScreenTestTags.NEXT_DAY_BUTTON)) {
              Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Day")
            }
      }
}

/**
 * A card that displays the map for the current day's itinerary.
 *
 * @param mapState The state object for the map, containing locations and drawing options.
 * @param onToggleFullscreen Callback to enter fullscreen map view.
 * @param onToggleNavMode Callback to toggle navigation from the user's current position.
 * @param onUserLocationUpdate Callback for when the user's location is updated.
 * @param isComputing A boolean to show a loading indicator if the schedule is being processed.
 * @param hasSteps A boolean indicating if there are any steps to display on the map.
 */
@Composable
private fun DailyMapCard(
    mapState: MapState,
    onToggleFullscreen: () -> Unit,
    onToggleNavMode: () -> Unit,
    onUserLocationUpdate: (Point) -> Unit,
    isComputing: Boolean,
    hasSteps: Boolean
) {
  Card(
      modifier =
          Modifier.fillMaxWidth()
              .padding(
                  horizontal =
                      dimensionResource(R.dimen.daily_view_day_navigation_horizontal_padding),
                  vertical = dimensionResource(R.dimen.daily_view_day_navigation_vertical_padding))
              .testTag(DailyViewScreenTestTags.MAP_CARD),
      shape = RoundedCornerShape(dimensionResource(R.dimen.daily_view_map_corner_radius)),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Box(
            modifier =
                Modifier.fillMaxWidth()
                    .height(dimensionResource(R.dimen.daily_view_map_height))
                    .testTag(DailyViewScreenTestTags.MAP_CONTAINER)) {
              if (isComputing || !hasSteps) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                  CircularProgressIndicator(
                      modifier = Modifier.testTag(DailyViewScreenTestTags.LOADING))
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

              IconButton(
                  onClick = onToggleFullscreen,
                  modifier =
                      Modifier.align(Alignment.BottomEnd)
                          .padding(dimensionResource(R.dimen.daily_view_map_toggle_button_padding))
                          .testTag(DailyViewScreenTestTags.FULLSCREEN_BUTTON)) {
                    Icon(
                        imageVector = Icons.Filled.ZoomOutMap,
                        contentDescription = stringResource(R.string.fullscreen),
                        tint = MaterialTheme.colorScheme.onBackground)
                  }
            }
      }
}

/**
 * A card representing a single step in the daily itinerary, which can be a travel segment or an
 * activity.
 *
 * @param stepNumber The sequential number of the step in the day.
 * @param element The [TripElement] data for this step.
 * @param isSelected A boolean indicating if this card is currently selected, which changes its
 *   appearance.
 * @param onMapClick A callback invoked when the user clicks the map icon on a travel segment.
 * @param onDetailsClick A callback invoked when the user clicks on an activity card.
 */
@Composable
private fun DailyStepCard(
    stepNumber: Int,
    element: TripElement,
    isSelected: Boolean,
    onMapClick: () -> Unit,
    onDetailsClick: () -> Unit
) {
  val timeFormatter = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
  val startTime = timeFormatter.format(element.startDate.toDate().toInstant())
  val endTime = timeFormatter.format(element.endDate.toDate().toInstant())

  Card(
      modifier =
          Modifier.fillMaxWidth()
              .padding(
                  horizontal =
                      dimensionResource(R.dimen.daily_view_day_navigation_horizontal_padding),
                  vertical = dimensionResource(R.dimen.daily_view_day_navigation_vertical_padding))
              .clickable(
                  enabled = element is TripElement.TripActivity,
                  onClick = onDetailsClick) // Card click goes to details
              .testTag(DailyViewScreenTestTags.STEP_CARD),
      shape = RoundedCornerShape(dimensionResource(R.dimen.daily_view_map_corner_radius)),
      elevation =
          CardDefaults.cardElevation(
              defaultElevation =
                  if (isSelected) {
                    dimensionResource(R.dimen.daily_view_card_elevation_on)
                  } else {
                    dimensionResource(R.dimen.daily_view_card_elevation_off)
                  }),
      colors =
          CardDefaults.cardColors(
              containerColor =
                  if (isSelected) MaterialTheme.colorScheme.primaryContainer
                  else MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.fillMaxWidth()) {
          Row(
              modifier =
                  Modifier.padding(dimensionResource(R.dimen.daily_view_date_padding))
                      .fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier =
                        Modifier.padding(
                            end = dimensionResource(R.dimen.daily_view_date_end_padding))) {
                      Text(
                          text = startTime,
                          style = MaterialTheme.typography.bodyMedium,
                          fontWeight = FontWeight.Bold)
                      Text(
                          text = "|",
                          style = MaterialTheme.typography.bodySmall,
                          modifier =
                              Modifier.padding(
                                  vertical =
                                      dimensionResource(R.dimen.daily_view_date_separator_padding)))
                      Text(
                          text = endTime,
                          style = MaterialTheme.typography.bodyMedium,
                          color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                Column(modifier = Modifier.weight(1f)) {
                  Text(
                      text =
                          when (element) {
                            is TripElement.TripActivity -> element.activity.location.name
                            is TripElement.TripSegment -> "Travel to ${element.route.to.name}"
                          },
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold)

                  if (element is TripElement.TripSegment) {
                    Text(
                        text =
                            stringResource(R.string.about_minutes, element.route.durationMinutes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                  }
                }

                if (element is TripElement.TripSegment) {
                  // Map Button only for Segments
                  IconButton(onClick = onMapClick) {
                    Icon(
                        imageVector = Icons.Filled.Route,
                        contentDescription = "Show on Map",
                        tint =
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface)
                  }
                } else {
                  // Details icon for Activity
                  Icon(
                      imageVector = Icons.Filled.Attractions,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.primary)
                }
              }
        }
      }
}

/**
 * A fullscreen map overlay.
 *
 * @param mapState The state for the map to display.
 * @param onExit Callback to exit the fullscreen mode.
 * @param onUserLocationUpdate Callback that provides updates to the user's location.
 */
@Composable
private fun FullScreenMap(
    mapState: MapState,
    onExit: () -> Unit,
    onUserLocationUpdate: (Point) -> Unit
) {
  Box(modifier = Modifier.fillMaxSize().testTag(DailyViewScreenTestTags.FULLSCREEN_MAP)) {
    MapScreen(
        locations = mapState.locations,
        drawRoute = mapState.drawRoute,
        onUserLocationUpdate = onUserLocationUpdate)
    IconButton(
        onClick = onExit,
        modifier =
            Modifier.align(Alignment.BottomEnd)
                .padding(dimensionResource(R.dimen.daily_view_map_toggle_button_padding))
                .testTag(DailyViewScreenTestTags.FULLSCREEN_EXIT)) {
          Icon(
              imageVector = Icons.Filled.ZoomInMap,
              contentDescription = stringResource(R.string.back_to_my_trips))
        }
  }
}

/**
 * Prepares the list of locations to be displayed on the map for a given day.
 *
 * @param dailySteps The list of [TripElement]s for the current day.
 * @param currentGps The user's current GPS position, if available.
 * @param drawFromCurrentPosition A boolean indicating whether to include the user's current
 *   location as the starting point.
 * @return A distinct list of [Location]s to be plotted on the map.
 */
private fun mapLocationsForDay(
    dailySteps: List<TripElement>,
    currentGps: Point?,
    drawFromCurrentPosition: Boolean
): List<Location> {
  if (dailySteps.isEmpty()) return emptyList()

  val locations = mutableListOf<Location>()

  // Add current location if requested
  if (drawFromCurrentPosition && currentGps != null) {
    locations.add(
        Location(
            name = "Current Location",
            coordinate = Coordinate(currentGps.latitude(), currentGps.longitude())))
  }

  dailySteps.forEach { step ->
    when (step) {
      is TripElement.TripActivity -> {
        // Do not add activity locations to the main map
      }
      is TripElement.TripSegment -> {
        locations.add(step.route.from)
        locations.add(step.route.to)
      }
    }
  }

  return locations.distinct()
}
