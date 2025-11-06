package com.github.swent.swisstravel.ui.mytrips.tripinfos

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
}

/**
 * Screen that displays the information of a specific trip
 *
 * @param uid the unique identifier of the trip
 * @param tripInfoViewModel the view model that holds the trip information state
 * @param onMyTrips lambda to be called when navigating back to past trips
 * @param onFullscreenClick lambda to be called when the fullscreen button is clicked
 * @param onEditTrip lambda to be called when the edit trip button is clicked
 */
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

    Log.d("TRIP_INFO", "locations = ${tripInfoUIState.locations}")

  LaunchedEffect(errorMsg) {
    if (errorMsg != null) {
      Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
      tripInfoViewModel.clearErrorMsg()
    }
  }

  Scaffold(
      containerColor = MaterialTheme.colorScheme.background,
      topBar = {
        TopAppBar(
            title = {
              Text(
                  text = tripInfoUIState.name,
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
        LaunchedEffect(showMap) {
          if (!showMap) {
            withFrameNanos {}
            onMyTrips()
          }
        }
      }) { pd ->
        Column(
            modifier = Modifier.padding(pd).fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally) {
              Card(
                  modifier =
                      Modifier.fillMaxWidth()
                          .padding(horizontal = 20.dp)
                          .height(270.dp)
                          .testTag(TripInfoTestTags.TRIP_CARD),
                  shape = RoundedCornerShape(12.dp),
                  elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)) {
                    if (showMap) {
                      TripInfoZoomableMap(
                          onFullscreenClick = onFullscreenClick, tripInfoUIState.locations)
                    }
                  }
            }
      }
}

/**
 * Composable that displays a favorite button.
 *
 * @param isFavorite whether the trip is a favorite
 * @param onToggleFavorite lambda to be called when the favorite button is clicked
 */
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
