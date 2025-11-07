package com.github.swent.swisstravel.ui.mytrips.tripinfos

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.model.trip.Location
import com.github.swent.swisstravel.ui.map.NavigationMapScreen

/** Test tags for TripInfoZoomableMap composable */
object TripInfoZoomableMapTestTags {
  const val MAP_CONTAINER = "mapContainer"
  const val FULLSCREEN_BUTTON = "fullscreenButton"
}
/**
 * Composable function for the Trip Info Zoomable Map.
 *
 * @param onFullscreenClick Lambda function to be called when the fullscreen button is pressed.
 */
@Composable
fun TripInfoZoomableMap(onFullscreenClick: () -> Unit, locations: List<Location>) {
  Scaffold { pd ->
    Box(modifier = Modifier.fillMaxSize().padding(pd)) {
      Box(modifier = Modifier.fillMaxSize().testTag(TripInfoZoomableMapTestTags.MAP_CONTAINER)) {
        NavigationMapScreen(locations)
      }

      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
        IconButton(
            onClick = onFullscreenClick,
            modifier =
                Modifier.padding(16.dp).testTag(TripInfoZoomableMapTestTags.FULLSCREEN_BUTTON)) {
              Icon(
                  imageVector = Icons.Filled.Fullscreen,
                  contentDescription = stringResource(R.string.fullscreen))
            }
      }
    }
  }
}
