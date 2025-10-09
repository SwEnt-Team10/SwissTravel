package com.github.swent.swisstravel

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHost
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.github.swent.swisstravel.ui.map.MapScreen
import com.github.swent.swisstravel.ui.map.SampleMenu
import okhttp3.OkHttpClient

object HttpClientProvider {
  var client: OkHttpClient = OkHttpClient()
}

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent { SwissTravel() }
  }
}

@Preview
@Composable
fun SwissTravel() {
  val navController = rememberNavController()

  NavHost(
      navController = navController,
      startDestination = "menu",
      builder = {
        composable("menu") { SampleMenu(navController) }
        composable("nav-map") { MapScreen(navController) }
      })
}
