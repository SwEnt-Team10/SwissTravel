package com.github.swent.swisstravel.ui.map

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.swent.swisstravel.ui.navigation.NavigationActions
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.plugin.PuckBearing
import com.mapbox.maps.plugin.locationcomponent.createDefault2DPuck
import com.mapbox.maps.plugin.locationcomponent.location

object MapLocationScreenTags {
  const val ERROR_TEXT = "errorText"
  const val PERMISSION_TEXT = "permissionText"
  const val PERMISSION_BUTTON = "permissionButton"
  const val MAP = "map"
}

/**
 * Screen that displays a map with the user's current location.
 *
 * Requests location permission if not already granted.
 *
 * @param viewModel ViewModel to manage location permission state.
 * @param isActivityNull True if the activity context is null, false otherwise.
 * @param navigationActions Navigation actions for navigating between screens.
 */
@Composable
fun MapLocationScreen(
    viewModel: MapLocationViewModel = viewModel(),
    isActivityNull: Boolean = false,
    navigationActions: NavigationActions? = null
) {
  val permissionGranted by viewModel.permissionGranted.collectAsState()
  val mapViewportState = rememberMapViewportState()

  val launcher =
      rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) {
          isGranted ->
        viewModel.setPermissionGranted(isGranted)
      }

  Scaffold { contentPadding ->
    when {
      isActivityNull -> {
        Text(
            "Error: unable to access the activity.",
            modifier = Modifier.padding(contentPadding).testTag(MapLocationScreenTags.ERROR_TEXT))
      }
      !permissionGranted -> {
        Column(modifier = Modifier.padding(contentPadding)) {
          Text(
              "Location is required to display your position on the map.",
              modifier = Modifier.testTag(MapLocationScreenTags.PERMISSION_TEXT))
          Button(
              onClick = { launcher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION) },
              modifier = Modifier.testTag(MapLocationScreenTags.PERMISSION_BUTTON)) {
                Text("Allow location")
              }
        }
      }
      else -> {
        MapboxMap(
            modifier =
                Modifier.fillMaxSize().padding(contentPadding).testTag(MapLocationScreenTags.MAP),
            mapViewportState = mapViewportState) {
              MapEffect(Unit) { mapView ->
                mapView.location.updateSettings {
                  locationPuck = createDefault2DPuck(withBearing = true)
                  enabled = true
                  puckBearing = PuckBearing.COURSE
                  puckBearingEnabled = true
                }
                mapViewportState.transitionToFollowPuckState()
              }
            }
      }
    }
  }
}
