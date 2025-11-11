package com.github.swent.swisstravel.ui.trip.tripinfos

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Card
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.ui.theme.favoriteIcon

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

  private const val STEP_PREFIX = "tripInfoScreenStepLocation_"

  fun stepLocationTag(stepIndex: Int) = "$STEP_PREFIX$stepIndex"
}

/**
 * Screen to show detailed information about a trip.
 *
 * @param uid The unique identifier of the trip to display.
 * @param tripInfoViewModel The ViewModel managing the trip information state.
 * @param onMyTrips Callback invoked when navigating back to the list of trips.
 * @param onFullscreenClick Callback invoked when the fullscreen map button is clicked.
 * @param onEditTrip Callback invoked when the edit trip button is clicked.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripInfoScreen(
    uid: String?,
    tripInfoViewModel: TripInfoViewModelContract = viewModel<TripInfoViewModel>(),
    onMyTrips: () -> Unit = {},
    onFullscreenClick: () -> Unit = {},
    onEditTrip: () -> Unit = {},
    isOnCurrentTripScreen: Boolean = false
) {
  LaunchedEffect(uid) { tripInfoViewModel.loadTripInfo(uid) }

  val tripInfoUIState by tripInfoViewModel.uiState.collectAsState()
  val errorMsg = tripInfoUIState.errorMsg

  val context = LocalContext.current
  var showMap by remember { mutableStateOf(true) }

  LaunchedEffect(errorMsg) {
    if (errorMsg != null) {
      Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
      tripInfoViewModel.clearErrorMsg()
    }
  }
  LaunchedEffect(showMap) {
    if (!showMap) {
      withFrameNanos {}
      onMyTrips()
    }
  }
  Scaffold(
      containerColor = MaterialTheme.colorScheme.background,
      topBar = {
        TopAppBar(
            title = {
              Text(
                  text = tripInfoUIState.name,
                  modifier = Modifier.testTag(TripInfoScreenTestTags.TITLE),
                  style = MaterialTheme.typography.titleLarge,
                  color = MaterialTheme.colorScheme.onBackground)
            },
            navigationIcon = {

              // don't show back button (that goes to MyTripsScreen) when on current trip screen
              if (!isOnCurrentTripScreen) {
                IconButton(
                    onClick = { showMap = false },
                    modifier = Modifier.testTag(TripInfoScreenTestTags.BACK_BUTTON)) {
                      Icon(
                          imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                          contentDescription = stringResource(R.string.back_to_my_trips),
                          tint = MaterialTheme.colorScheme.onBackground)
                    }
              }
            },
            actions = {
              val isFavorite = tripInfoUIState.isFavorite
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
      }) { pd ->
        LazyColumn(
            modifier =
                Modifier.padding(pd).fillMaxSize().testTag(TripInfoScreenTestTags.LAZY_COLUMN),
            horizontalAlignment = Alignment.Start) {
              if (tripInfoUIState.locations.isEmpty()) {
                item {
                  Text(
                      text = stringResource(R.string.no_locations_available),
                      modifier = Modifier.testTag(TripInfoScreenTestTags.NO_LOCATIONS))
                }
              } else {
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
                        Text(
                            text = tripInfoUIState.locations[0].name,
                            modifier =
                                Modifier.align(Alignment.CenterStart)
                                    .testTag(TripInfoScreenTestTags.LOCATION_NAME),
                            style = MaterialTheme.typography.headlineMedium)
                      }
                }
              }
              item {
                Card(
                    modifier =
                        Modifier.fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .testTag(TripInfoScreenTestTags.MAP_CARD),
                    shape = RoundedCornerShape(12.dp)) {
                      Box(modifier = Modifier.testTag(TripInfoScreenTestTags.MAP_CONTAINER)) {
                        if (showMap) {
                          Box(
                              modifier =
                                  Modifier.fillMaxWidth()
                                      .height(200.dp)
                                      .testTag(TripInfoScreenTestTags.MAP_BOX)) {
                                TripInfoZoomableMap(
                                    onFullscreenClick = onFullscreenClick,
                                    locations = tripInfoUIState.locations)
                              }
                        }
                      }
                    }
              }
              if (tripInfoUIState.locations.size > 1) {
                itemsIndexed(tripInfoUIState.locations.drop(1)) { idx, location ->
                  Box(
                      modifier =
                          Modifier.testTag(TripInfoScreenTestTags.stepLocationTag(idx + 2))) {
                        StepLocationCard(stepNumber = idx + 2, location = location)
                      }
                }
              }
            }
      }
}
/**
 * A button to mark/unmark a trip as favorite.
 *
 * @param isFavorite Whether the trip is currently marked as favorite.
 * @param onToggleFavorite Callback invoked when the button is clicked.
 * @param testTag Optional test tag for UI testing.
 */
@Composable
fun FavoriteButton(isFavorite: Boolean, onToggleFavorite: () -> Unit, testTag: String? = null) {
  IconButton(
      onClick = onToggleFavorite,
      modifier = if (testTag != null) Modifier.testTag(testTag) else Modifier) {
        if (isFavorite) {
          Icon(
              imageVector = Icons.Default.Star,
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
