package com.github.swent.swisstravel

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.mapbox.geojson.Point
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState

public class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      MapboxMap(
          Modifier.fillMaxSize(),
          mapViewportState =
              rememberMapViewportState {
                setCameraOptions {
                  zoom(2.0)
                  center(Point.fromLngLat(-98.0, 39.5))
                  pitch(0.0)
                  bearing(0.0)
                }
              },
      )
    }
  }
}
