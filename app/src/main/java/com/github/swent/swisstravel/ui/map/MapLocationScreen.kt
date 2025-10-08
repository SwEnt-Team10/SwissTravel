package com.github.swent.swisstravel.ui.map

import android.app.Activity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.plugin.PuckBearing
import com.mapbox.maps.plugin.locationcomponent.createDefault2DPuck
import com.mapbox.maps.plugin.locationcomponent.location

@Composable
fun MapLocationScreen() {
  val context = LocalContext.current
  val activity = context as? Activity
  val viewModel = remember { MapLocationViewModel(activity) }
  val permissionGranted by viewModel.permissionGranted.collectAsState()
  val mapViewportState = rememberMapViewportState()

  when {
    viewModel.isActivityNull() -> {
      Text("Erreur : impossible d’accéder à l’activité.")
    }
    !permissionGranted -> {
      Column {
        Text("La localisation est nécessaire pour afficher votre position sur la carte.")
        Button(onClick = { viewModel.requestPermission(1001) }) {
          Text("Autoriser la localisation")
        }
      }
    }
    else -> {
      MapboxMap(modifier = Modifier.fillMaxSize(), mapViewportState = mapViewportState) {
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
