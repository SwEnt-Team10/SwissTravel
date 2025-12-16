package com.github.swent.swisstravel.ui.trip.tripinfos

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Attractions
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ZoomInMap
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.model.trip.Location
import com.github.swent.swisstravel.model.trip.TripElement
import com.github.swent.swisstravel.model.user.User
import com.github.swent.swisstravel.ui.composable.ProfileImage
import com.github.swent.swisstravel.ui.friends.FriendElement
import com.github.swent.swisstravel.ui.map.MapScreen
import com.github.swent.swisstravel.ui.theme.favoriteIcon
import com.github.swent.swisstravel.utils.NetworkUtils.isOnline
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
  const val SWIPE_ACTIVITIES_BUTTON = "dailyViewScreenSwipeActivitiesButton"
  const val LIKED_ACTIVITIES_BUTTON = "dailyViewScreenLikedActivitiesButton"

  const val SHARE_BUTTON = "dailyViewScreenShareButton"

  fun getTestTagForRemoveCollaborator(uid: String): String = "removeCollaborator${uid}"
}

/**
 * A data class to refactor the callbacks of the DailViewScreen composable
 *
 * @param onMyTrips A callback to navigate back to the list of user's trips.
 * @param onEditTrip A callback to navigate to the trip editing screen.
 * @param onActivityClick A callback invoked when a user clicks on a trip activity for more details.
 * @param onSwipeActivities A callback to navigate to the screen where the user can swipe
 *   activities.
 * @param onLikedActivities A callback to navigate to the screen of liked activities.
 */
data class DailyViewScreenCallbacks(
    val onMyTrips: () -> Unit = {},
    val onEditTrip: () -> Unit = {},
    val onActivityClick: (TripElement.TripActivity) -> Unit = {},
    val onSwipeActivities: () -> Unit = {},
    val onLikedActivities: () -> Unit = {}
)

/**
 * A screen that displays the daily itinerary of a trip. It shows a map of the day's route and a
 * list of travel segments and activities. Note : This class was partially written with the help of
 * an AI. If you have any question, don't hesitate to contact the author of the class (@JstnFv)
 *
 * @param uid The unique identifier of the trip to display.
 * @param tripInfoViewModel The ViewModel providing data and logic for the trip info.
 * @param isOnCurrentTripScreen A boolean indicating if this screen is part of the current trip
 *   flow, which affects the visibility of the back button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyViewScreen(
    uid: String?,
    tripInfoViewModel: TripInfoViewModelContract = viewModel<TripInfoViewModel>(),
    onAddPhotos: () -> Unit = {},
    isOnCurrentTripScreen: Boolean = false,
    mapContent: @Composable (List<Location>, Boolean, (Point) -> Unit) -> Unit =
        { locations, drawRoute, onUserLocationUpdate ->
          MapScreen(
              locations = locations,
              drawRoute = drawRoute,
              onUserLocationUpdate = onUserLocationUpdate)
        },
    callbacks: DailyViewScreenCallbacks = DailyViewScreenCallbacks()
) {
  LaunchedEffect(uid) { tripInfoViewModel.loadTripInfo(uid) }

  val ui by tripInfoViewModel.uiState.collectAsState()
  // done by AI
  val validPhotoEntries =
      remember(ui.uriLocation) {
        ui.uriLocation
            .filter { entry ->
              entry.value.coordinate.latitude != 0.0 || entry.value.coordinate.longitude != 0.0
            }
            .toList()
      }
  // Done by AI
  val actualMapContent: @Composable (List<Location>, Boolean, (Point) -> Unit) -> Unit =
      { locations, drawRoute, onUserLocationUpdate ->
        MapScreen(
            locations = locations,
            photoEntries = validPhotoEntries,
            drawRoute = drawRoute,
            onUserLocationUpdate = onUserLocationUpdate)
      }

  LaunchedEffect(ui.days, isOnCurrentTripScreen) {
    if (isOnCurrentTripScreen) {
      val today = LocalDate.now()
      val index = ui.days.indexOf(today)
      if (index != -1 && index != ui.currentDayIndex) {
        tripInfoViewModel.setCurrentDayIndex(index)
      }
    }
  }
  val context = LocalContext.current

  BackHandler(enabled = ui.fullscreen) { tripInfoViewModel.toggleFullscreen(false) }

  val currentDay = ui.days.getOrNull(ui.currentDayIndex)
  val dailySteps =
      if (currentDay != null) ui.groupedSchedule[currentDay] ?: emptyList() else emptyList()

  // Map state logic
  val mapState =
      remember(ui.mapLocations, ui.schedule, ui.drawFromCurrentPosition, ui.isComputingSchedule) {
        MapState(
            locations = ui.mapLocations,
            drawRoute = dailySteps.isNotEmpty(),
            drawFromCurrentPosition = ui.drawFromCurrentPosition,
            isLoading =
                ui.isComputingSchedule || (ui.locations.isNotEmpty() && ui.schedule.isEmpty()))
      }

  var showShareDialog by remember { mutableStateOf(false) }

  LaunchedEffect(ui.errorMsg) {
    ui.errorMsg?.let {
      Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
      tripInfoViewModel.clearErrorMsg()
    }
  }

  LaunchedEffect(ui.uid) {
    if (ui.currentUserIsOwner) {
      tripInfoViewModel.loadCollaboratorData()
    }
  }

  Scaffold(
      containerColor = MaterialTheme.colorScheme.background,
      topBar = {
        if (!ui.fullscreen) {
          DailyViewTopAppBar(
              ui = ui,
              isOnCurrentTripScreen = isOnCurrentTripScreen,
              onBack = callbacks.onMyTrips,
              onToggleFavorite = { tripInfoViewModel.toggleFavorite() },
              onEdit = callbacks.onEditTrip,
              onAddPhotos = { onAddPhotos() },
              onShare = { showShareDialog = true })
        }
      },
      bottomBar = {
        if (ui.currentUserIsOwner && !ui.fullscreen)
            DailyViewBottomBar(
                onSwipeActivities = callbacks.onSwipeActivities,
                onLikedActivities = callbacks.onLikedActivities)
      }) { pd ->
        Box(Modifier.fillMaxSize().padding(pd)) {
          if (ui.locations.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
              Column(
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.Center) {
                    if (isOnline(context)) {
                      Text(
                          text = stringResource(R.string.no_locations_available),
                          modifier =
                              Modifier.padding(dimensionResource(R.dimen.daily_view_padding))
                                  .testTag(DailyViewScreenTestTags.NO_LOCATIONS))
                    } else {
                      CircularProgressIndicator(
                          modifier = Modifier.testTag(DailyViewScreenTestTags.LOADING))
                      Spacer(
                          modifier =
                              Modifier.height(dimensionResource(R.dimen.medium_large_spacer)))
                      Text(
                          text = stringResource(R.string.loading_from_cache),
                          modifier =
                              Modifier.padding(dimensionResource(R.dimen.daily_view_padding))
                                  .testTag(DailyViewScreenTestTags.NO_LOCATIONS)
                                  .align(Alignment.CenterHorizontally))
                    }
                  }
            }
          } else {
            Column(Modifier.fillMaxSize()) {
              // Day Navigator
              DayNavigator(
                  currentDayIndex = ui.currentDayIndex,
                  days = ui.days,
                  onDayChange = { tripInfoViewModel.setCurrentDayIndex(it) })

              if (!ui.fullscreen) {
                // Map Card
                DailyMapCard(
                    mapState = mapState,
                    onToggleFullscreen = { tripInfoViewModel.toggleFullscreen(true) },
                    onToggleNavMode = {
                      tripInfoViewModel.setDrawFromCurrentPosition(!ui.drawFromCurrentPosition)
                    },
                    onUserLocationUpdate = { tripInfoViewModel.updateUserLocation(it) },
                    isComputing = ui.isComputingSchedule,
                    hasSteps = dailySteps.isNotEmpty(),
                    mapContent = actualMapContent)
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
                          isSelected = ui.selectedStep == el,
                          onMapClick = {
                            tripInfoViewModel.setSelectedStep(
                                if (ui.selectedStep == el) null else el)
                          },
                          onDetailsClick = {
                            if (el is TripElement.TripActivity) {
                              callbacks.onActivityClick(el)
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
                onUserLocationUpdate = { tripInfoViewModel.updateUserLocation(it) },
                mapContent = actualMapContent)
          }

          if (showShareDialog) {
            ShareTripDialog(
                onDismiss = { showShareDialog = false },
                collaborators = ui.collaborators,
                availableFriends = ui.availableFriends,
                onRemoveCollaborator = { user ->
                  tripInfoViewModel.removeCollaborator(user)
                  Toast.makeText(
                          context, "Friend successfully removed from the trip!", Toast.LENGTH_SHORT)
                      .show()
                },
                onAddCollaborator = { user ->
                  tripInfoViewModel.addCollaborator(user)
                  Toast.makeText(
                          context, "Friend successfully added to the trip!", Toast.LENGTH_SHORT)
                      .show()
                })
          }
        }
      }
}

/**
 * The share trip dialog. Allows users to share the trip with their friends
 *
 * @param onDismiss Callback to dismiss the dialog.
 * @param collaborators List of collaborators for the trip.
 * @param availableFriends List of available friends to share the trip with.
 * @param onRemoveCollaborator Callback to remove a collaborator from the trip.
 * @param onAddCollaborator Callback to add a collaborator to the trip
 */
@Composable
private fun ShareTripDialog(
    onDismiss: () -> Unit,
    collaborators: List<User>,
    availableFriends: List<User>,
    onRemoveCollaborator: (User) -> Unit,
    onAddCollaborator: (User) -> Unit
) {
  AlertDialog(
      onDismissRequest = onDismiss,
      title = { Text(stringResource(R.string.share_trip)) },
      text = {
        Column {
          // --- CURRENT COLLABORATORS SECTION ---
          if (collaborators.isNotEmpty()) {
            Text(
                text = stringResource(R.string.current_collaborators),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier =
                    Modifier.padding(
                        bottom = dimensionResource(R.dimen.trip_element_collaborators_padding)))

            LazyColumn(
                modifier =
                    Modifier.heightIn(
                        max = dimensionResource(R.dimen.share_dialog_collaborators_list_height)),
                verticalArrangement =
                    Arrangement.spacedBy(
                        dimensionResource(R.dimen.trip_element_collaborators_padding))) {
                  items(collaborators) { collaborator ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween) {
                          // Profile Pic & Name
                          Row(
                              verticalAlignment = Alignment.CenterVertically,
                              modifier = Modifier.weight(1f)) {
                                ProfileImage(
                                    urlOrUid = collaborator.profilePicUrl,
                                    modifier =
                                        Modifier.size(
                                                dimensionResource(R.dimen.trip_top_circle_size))
                                            .clip(CircleShape))
                                Spacer(
                                    modifier =
                                        Modifier.width(
                                            dimensionResource(R.dimen.share_dialog_spacer)))
                                Text(
                                    text = collaborator.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis)
                              }

                          // Remove Button
                          IconButton(
                              onClick = { onRemoveCollaborator(collaborator) },
                              modifier =
                                  Modifier.testTag(
                                      DailyViewScreenTestTags.getTestTagForRemoveCollaborator(
                                          collaborator.uid))) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = stringResource(R.string.delete),
                                    tint = MaterialTheme.colorScheme.error)
                              }
                        }
                  }
                }
            // Divider between sections
            HorizontalDivider(
                modifier =
                    Modifier.padding(vertical = dimensionResource(R.dimen.share_dialog_spacer)))
          }

          // --- ADD FRIENDS SECTION ---
          Text(
              text = stringResource(R.string.add_friend),
              style = MaterialTheme.typography.titleSmall,
              color = MaterialTheme.colorScheme.primary,
              modifier =
                  Modifier.padding(
                      bottom = dimensionResource(R.dimen.trip_element_collaborators_padding)))

          if (availableFriends.isEmpty()) {
            Text(
                text = stringResource(R.string.no_friends_to_share),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
          } else {
            LazyColumn(
                modifier =
                    Modifier.heightIn(
                        max = dimensionResource(R.dimen.share_dialog_friends_list_height)),
                verticalArrangement =
                    Arrangement.spacedBy(
                        dimensionResource(R.dimen.trip_element_collaborators_padding))) {
                  items(availableFriends) { friend ->
                    FriendElement(userToDisplay = friend, onClick = { onAddCollaborator(friend) })
                  }
                }
          }
        }
      },
      confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.ok)) } })
}

/**
 * The bottom bar for the Daily View screen, containing buttons for swiping activities and accessing
 * liked activities.
 *
 * @param onSwipeActivities Callback for when the swipe activities button is clicked.
 * @param onLikedActivities Callback for when the liked activities button is clicked.
 */
@Composable
private fun DailyViewBottomBar(
    onSwipeActivities: () -> Unit = {},
    onLikedActivities: () -> Unit = {}
) {
  Row {
    // button to go to a screen were you can swipe (like/dislike) through activities
    Button(
        onClick = onSwipeActivities,
        modifier =
            Modifier.fillMaxWidth(0.7f)
                .padding(dimensionResource(R.dimen.small_spacer))
                .testTag(DailyViewScreenTestTags.SWIPE_ACTIVITIES_BUTTON),
    ) {
      Text(text = stringResource(R.string.swipe_activities))
    }
    // Button to go to the liked activities screen
    Button(
        onClick = onLikedActivities,
        modifier =
            Modifier.padding(dimensionResource(R.dimen.small_spacer))
                .testTag(DailyViewScreenTestTags.LIKED_ACTIVITIES_BUTTON)) {
          Icon(
              imageVector = Icons.Filled.Favorite,
              contentDescription = stringResource(R.string.liked_activities))
        }
  }
}

/**
 * The top app bar for the Daily View screen. Displays the trip title and action buttons for
 * navigation, favoriting, editing, and sharing.
 *
 * @param ui The current UI state of the trip.
 * @param isOnCurrentTripScreen A boolean to control the visibility of the back button.
 * @param onBack Callback for when the back button is clicked.
 * @param onToggleFavorite Callback for when the favorite button is clicked.
 * @param onEdit Callback for when the edit button is clicked.
 * @param onAddPhotos Callback for when the add photos button is clicked.
 * @param onShare Callback for when the share button is clicked.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DailyViewTopAppBar(
    ui: TripInfoUIState,
    isOnCurrentTripScreen: Boolean,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onEdit: () -> Unit,
    onAddPhotos: () -> Unit = {},
    onShare: () -> Unit = {}
) {
  TopAppBar(
      title = {
        Text(
            text = ui.name,
            modifier = Modifier.testTag(DailyViewScreenTestTags.TITLE),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis)
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
        if (ui.currentUserIsOwner) {
          IconButton(
              onClick = onShare,
              modifier = Modifier.testTag(DailyViewScreenTestTags.SHARE_BUTTON)) {
                Icon(
                    imageVector = Icons.Filled.Share,
                    contentDescription = stringResource(R.string.share_trip),
                    tint = MaterialTheme.colorScheme.onBackground)
              }
          AddPhotosButton(onAddPhotos = { onAddPhotos() })
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
              Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous Day")
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
              Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next Day")
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
    hasSteps: Boolean,
    mapContent: @Composable (List<Location>, Boolean, (Point) -> Unit) -> Unit
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
                mapContent(mapState.locations, mapState.drawRoute, onUserLocationUpdate)
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
    onUserLocationUpdate: (Point) -> Unit,
    mapContent: @Composable (List<Location>, Boolean, (Point) -> Unit) -> Unit
) {
  Box(modifier = Modifier.fillMaxSize().testTag(DailyViewScreenTestTags.FULLSCREEN_MAP)) {
    mapContent(mapState.locations, mapState.drawRoute, onUserLocationUpdate)
    IconButton(
        onClick = onExit,
        modifier =
            Modifier.align(Alignment.BottomEnd)
                .padding(dimensionResource(R.dimen.daily_view_map_button_padding))
                .testTag(DailyViewScreenTestTags.FULLSCREEN_EXIT)) {
          Icon(
              imageVector = Icons.Filled.ZoomInMap,
              contentDescription = stringResource(R.string.back_to_my_trips))
        }
  }
}
