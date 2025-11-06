// language: kotlin
package com.github.swent.swisstravel.ui.mytrips.tripinfos

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.CardDefaults
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
object TripInfoTestTags {
  const val BACK_BUTTON = "backButton"
  const val EDIT_BUTTON = "editButton"
  const val FAVORITE_BUTTON = "favoriteButton"
  const val TRIP_CARD = "tripCard"
  const val TOPBAR_TITLE = "topbarTitle"
  const val NO_LOCATIONS_TEXT = "noLocationsText"
  const val FIRST_LOCATION_TEXT = "firstLocationText"
  const val LOCATION_CARD = "locationCard"
  const val MAP_VIEW = "mapView"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripInfoScreen(
    uid: String?,
    tripInfoViewModel: TripInfoViewModel = viewModel(),
    onMyTrips: () -> Unit = {},
    onFullscreenClick: () -> Unit = {},
    onEditTrip: () -> Unit = {}
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
                  modifier = Modifier.testTag(TripInfoTestTags.TOPBAR_TITLE),
                  style = MaterialTheme.typography.titleLarge,
                  color = MaterialTheme.colorScheme.onBackground)
            },
            navigationIcon = {
              IconButton(
                  onClick = { showMap = false },
                  modifier = Modifier.testTag(TripInfoTestTags.BACK_BUTTON)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back_to_my_trips),
                        tint = MaterialTheme.colorScheme.onBackground)
                  }
            },
            actions = {
              val isFavorite = tripInfoUIState.isFavorite
              FavoriteButton(
                  isFavorite = isFavorite,
                  onToggleFavorite = { tripInfoViewModel.toggleFavorite() })
              IconButton(
                  onClick = { onEditTrip() },
                  modifier = Modifier.testTag(TripInfoTestTags.EDIT_BUTTON)) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = stringResource(R.string.edit_trip),
                        tint = MaterialTheme.colorScheme.onBackground)
                  }
            })
      }) { pd ->
        LazyColumn(
            modifier = Modifier.padding(pd).fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally) {
              if (tripInfoUIState.locations.isEmpty()) {
                item {
                  Text(
                      text = stringResource(R.string.no_locations_available),
                      modifier = Modifier.testTag(TripInfoTestTags.NO_LOCATIONS_TEXT))
                }
              } else {
                item {
                  Box(modifier = Modifier.testTag(TripInfoTestTags.LOCATION_CARD)) {
                    Text(
                        text = "${tripInfoUIState.locations[0]}",
                        modifier = Modifier.testTag(TripInfoTestTags.FIRST_LOCATION_TEXT))
                  }
                }

                if (tripInfoUIState.locations.size > 1) {
                  itemsIndexed(tripInfoUIState.locations.drop(1)) { idx, location ->
                    Box(modifier = Modifier.testTag("${TripInfoTestTags.LOCATION_CARD}_$idx")) {
                      StepLocationCard(int = idx + 2, location = location)
                    }
                  }
                }
              }
              item {
                Card(
                    modifier =
                        Modifier.fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .testTag(TripInfoTestTags.TRIP_CARD),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)) {
                      if (showMap) {
                        Box(
                            modifier =
                                Modifier.fillMaxWidth()
                                    .height(200.dp)
                                    .testTag(TripInfoTestTags.MAP_VIEW)) {
                              TripInfoZoomableMap(onFullscreenClick = onFullscreenClick)
                            }
                      }
                    }
              }
            }
      }
}

@Composable
fun FavoriteButton(isFavorite: Boolean, onToggleFavorite: () -> Unit) {
  IconButton(
      onClick = onToggleFavorite, modifier = Modifier.testTag(TripInfoTestTags.FAVORITE_BUTTON)) {
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
