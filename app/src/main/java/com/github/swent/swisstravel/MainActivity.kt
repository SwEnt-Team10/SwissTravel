package com.github.swent.swisstravel

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import com.github.swent.swisstravel.model.user.UserRepositoryFirebase
import com.github.swent.swisstravel.theme.SampleAppTheme
import com.github.swent.swisstravel.ui.DummyScreen
import com.github.swent.swisstravel.ui.currenttrip.CurrentTripScreen
import com.github.swent.swisstravel.ui.navigation.NavigationActions
import com.github.swent.swisstravel.ui.navigation.Screen
import com.github.swent.swisstravel.ui.profile.ProfileScreen
import com.github.swent.swisstravel.ui.profile.ProfileScreenViewModel
import okhttp3.OkHttpClient

object HttpClientProvider {
  var client: OkHttpClient = OkHttpClient()
}

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      SampleAppTheme {
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
  val startDestination = Screen.Profile.route // TODO Change this
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
  }
}
