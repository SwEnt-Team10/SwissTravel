package com.github.swent.swisstravel

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import com.github.swent.swisstravel.model.user.UserRepositoryFirebase
import com.github.swent.swisstravel.ui.DummyScreen
import com.github.swent.swisstravel.ui.currenttrip.CurrentTripScreen
import com.github.swent.swisstravel.ui.map.MapLocationScreen
import com.github.swent.swisstravel.ui.map.MapLocationViewModel
import com.github.swent.swisstravel.ui.navigation.NavigationActions
import com.github.swent.swisstravel.ui.navigation.Screen
import com.github.swent.swisstravel.ui.profile.ProfileScreen
import com.github.swent.swisstravel.ui.profile.ProfileScreenViewModel
import com.github.swent.swisstravel.ui.theme.SwissTravelTheme
import okhttp3.OkHttpClient

object HttpClientProvider {
  var client: OkHttpClient = OkHttpClient()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SwissTravelTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    SwissTravelApp()
                }
            }
        }
    }
}

@Composable
fun SwissTravelApp() {
  val navController = rememberNavController()
  val navigationActions = NavigationActions(navController)
  val startDestination = Screen.Profile.route
  NavHost(navController = navController, startDestination = startDestination) {
    navigation(
        startDestination = Screen.Profile.route,
        route = Screen.Profile.name,
    ) {
      composable(Screen.Profile.route) {
        ProfileScreen(
            profileScreenViewModel =
                ProfileScreenViewModel(userRepository = UserRepositoryFirebase()),
            navigationActions = navigationActions)
      }
    }

    navigation(
        startDestination = Screen.CurrentTrip.route,
        route = Screen.CurrentTrip.name,
    ) {
      composable(Screen.CurrentTrip.route) {
        CurrentTripScreen(navigationActions = navigationActions)
      }
    }

    navigation(
        startDestination = Screen.MyTrips.route,
        route = Screen.MyTrips.name,
    ) {
      composable(Screen.MyTrips.route) {
        DummyScreen(navigationActions = navigationActions) // TODO change this
      }
    }
      navigation(
          startDestination = Screen.Map.route,
          route = Screen.Map.name,
      ) {
          composable(Screen.Map.route) {
              val launcher = rememberLauncherForActivityResult(
                  contract = ActivityResultContracts.RequestPermission()
              ) { isGranted ->
                  // Gère le résultat ici (ex : mettre à jour un ViewModel si besoin)
              }
              MapLocationScreen(
                  navigationActions = navigationActions
              )
          }
      }
  }
}
