package com.github.swent.swisstravel

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import com.github.swent.swisstravel.model.user.UserRepositoryFirebase
import com.github.swent.swisstravel.ui.authentication.SignInScreen
import com.github.swent.swisstravel.ui.currenttrip.CurrentTripScreen
import com.github.swent.swisstravel.ui.map.MapLocationScreen
import com.github.swent.swisstravel.ui.mytrips.MyTripsScreen
import com.github.swent.swisstravel.ui.navigation.NavigationActions
import com.github.swent.swisstravel.ui.navigation.Screen
import com.github.swent.swisstravel.ui.profile.ProfileScreen
import com.github.swent.swisstravel.ui.profile.ProfileScreenViewModel
import com.github.swent.swisstravel.ui.theme.SwissTravelTheme
import com.github.swent.swisstravel.ui.tripSettings.TripDateScreen
import com.github.swent.swisstravel.ui.tripSettings.TripPreferencesScreen
import com.github.swent.swisstravel.ui.tripSettings.TripSettingsViewModel
import com.github.swent.swisstravel.ui.tripSettings.TripTravelersScreen
import com.google.firebase.auth.FirebaseAuth
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

/**
 * Retrieves the TripSettingsViewModel scoped to the TripSettings navigation graph.
 *
 * Bug solved with Copilot.
 *
 * @param navController The NavHostController used for navigation.
 * @return The TripSettingsViewModel instance.
 */
@Composable
fun tripSettingsViewModel(navController: NavHostController): TripSettingsViewModel {
  val currentEntry by navController.currentBackStackEntryAsState()

  val parentEntry =
      remember(currentEntry) {
        runCatching { navController.getBackStackEntry(Screen.TripSettings1.name) }.getOrNull()
      }

  return if (parentEntry != null) {
    viewModel(parentEntry)
  } else {
    viewModel()
  }
}

@Composable
fun SwissTravelApp(
    context: Context = LocalContext.current,
    credentialManager: CredentialManager = CredentialManager.create(context)
) {
  val navController = rememberNavController()
  val navigationActions = NavigationActions(navController)
  val startDestination =
      if (FirebaseAuth.getInstance().currentUser == null) Screen.Auth.name
      else Screen.CurrentTrip.name
  NavHost(navController = navController, startDestination = startDestination) {
    navigation(
        startDestination = Screen.Auth.route,
        route = Screen.Auth.name,
    ) {
      composable(Screen.Auth.route) {
        SignInScreen(
            credentialManager = credentialManager,
            onSignedIn = { navigationActions.navigateTo(Screen.Profile) })
      }
    }

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
        MyTripsScreen(
            onSelectTrip = {
              Toast.makeText(context, "I don't work yet! Sorry :(", Toast.LENGTH_SHORT).show()
            },
            onPastTrips = {
              Toast.makeText(context, "I don't work yet! Sorry :(", Toast.LENGTH_SHORT).show()
            },
            navigationActions = navigationActions)
      }
    }
    navigation(
        startDestination = Screen.Map.route,
        route = Screen.Map.name,
    ) {
      composable(Screen.Map.route) { MapLocationScreen(navigationActions = navigationActions) }
    }
    navigation(
        startDestination = Screen.TripSettings1.route,
        route = Screen.TripSettings1.name,
    ) {
      composable(Screen.TripSettings1.route) {
        TripDateScreen(
            viewModel = tripSettingsViewModel(navController),
            onNext = { navigationActions.navigateTo(Screen.TripSettings2) })
      }
      composable(Screen.TripSettings2.route) {
        TripTravelersScreen(
            viewModel = tripSettingsViewModel(navController),
            onNext = { navigationActions.navigateTo(Screen.TripSettings3) })
      }
      composable(Screen.TripSettings3.route) {
        TripPreferencesScreen(
            viewModel = tripSettingsViewModel(navController),
            onDone = { navigationActions.navigateTo(Screen.MyTrips) })
      }
    }
  }
}
