package com.github.swent.swisstravel

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHost
import com.github.swent.swisstravel.resources.C
import com.github.swent.swisstravel.theme.SampleAppTheme
import okhttp3.OkHttpClient
import com.github.swent.swisstravel.ui.map.MapboxComposeApp
import com.mapbox.geojson.Point
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.github.swent.swisstravel.ui.map.MenuExample


object HttpClientProvider {
  var client: OkHttpClient = OkHttpClient()
}

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      SwissTravel()
    }
  }
}

@Preview
@Composable
fun SwissTravel() {
  val navController = rememberNavController()

  NavHost(
    navController = navController,
    startDestination = "menu-example",
    builder = {
        composable("menu-example") {
            MenuExample(navController)
        }
        composable("nav-map") {
            MapboxComposeApp(navController)
        }
    }
  )
}
