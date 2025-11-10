package com.github.swent.swisstravel

import EditTripScreen
import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.credentials.CredentialManager
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.github.swent.swisstravel.model.user.UserRepositoryFirebase
import com.github.swent.swisstravel.ui.authentication.SignInScreen
import com.github.swent.swisstravel.ui.currenttrip.CurrentTripScreen
import com.github.swent.swisstravel.ui.currenttrip.SetCurrentTripScreen
import com.github.swent.swisstravel.ui.map.MapLocationScreen
import com.github.swent.swisstravel.ui.mytrips.MyTripsScreen
import com.github.swent.swisstravel.ui.mytrips.PastTripsScreen
import com.github.swent.swisstravel.ui.mytrips.tripinfos.TripInfoMapScreen
import com.github.swent.swisstravel.ui.mytrips.tripinfos.TripInfoScreen
import com.github.swent.swisstravel.ui.navigation.BottomNavigationMenu
import com.github.swent.swisstravel.ui.navigation.NavigationActions
import com.github.swent.swisstravel.ui.navigation.NavigationTestTags
import com.github.swent.swisstravel.ui.navigation.Screen
import com.github.swent.swisstravel.ui.navigation.Tab
import com.github.swent.swisstravel.ui.profile.ProfileScreen
import com.github.swent.swisstravel.ui.profile.ProfileScreenViewModel
import com.github.swent.swisstravel.ui.theme.SwissTravelTheme
import com.github.swent.swisstravel.ui.tripcreation.ArrivalDepartureScreen
import com.github.swent.swisstravel.ui.tripcreation.FirstDestinationScreen
import com.github.swent.swisstravel.ui.tripcreation.TripDateScreen
import com.github.swent.swisstravel.ui.tripcreation.TripPreferencesScreen
import com.github.swent.swisstravel.ui.tripcreation.TripSettingsViewModel
import com.github.swent.swisstravel.ui.tripcreation.TripSummaryScreen
import com.github.swent.swisstravel.ui.tripcreation.TripTravelersScreen
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

/**
 * The main composable function for the Swiss Travel App.
 *
 * @param context The context of the current state of the application.
 * @param credentialManager The CredentialManager for handling user credentials.
 */
@SuppressLint("SuspiciousIndentation")
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

  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = navBackStackEntry?.destination?.route

  val showBottomBar =
      when (currentRoute) {
        Screen.CurrentTrip.route,
        Screen.MyTrips.route,
        Screen.Profile.route -> true
        else -> false
      }

  /* System back button handler */
  BackHandler {
    when {
      /* If the current route is authentication then quit the app */
      currentRoute == Screen.Auth.route -> {
        (context as? ComponentActivity)?.finish()
      }

      /* If the stack is not empty, go back to the previous screen */
      navController.previousBackStackEntry != null -> {
        navController.popBackStack()
      }

      /* If the stack is empty, do nothing */
      else -> {
        // Do nothing (prevents accidental app exit)
      }
    }
  }

  Scaffold(
      bottomBar = {
        if (showBottomBar) {
          BottomNavigationMenu(
              selectedTab =
                  when (currentRoute) {
                    Screen.CurrentTrip.route -> Tab.CurrentTrip
                    Screen.MyTrips.route -> Tab.MyTrips
                    Screen.Profile.route -> Tab.Profile
                    else -> Tab.CurrentTrip
                  },
              onTabSelected = { tab -> navigationActions.navigateTo(tab.destination) },
              modifier = Modifier.testTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU))
        }
      }) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)) {

              // Sign-in screen
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

              // Profile screen
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

              // Current trip screen
              navigation(
                  startDestination = Screen.CurrentTrip.route,
                  route = Screen.CurrentTrip.name,
              ) {
                composable(Screen.CurrentTrip.route) {
                  CurrentTripScreen(
                      navigationActions = navigationActions,
                      isLoggedIn = FirebaseAuth.getInstance().currentUser != null)
                }
              }

              // My Trips screen
              navigation(
                  startDestination = Screen.MyTrips.route,
                  route = Screen.MyTrips.name,
              ) {
                composable(Screen.MyTrips.route) {
                  MyTripsScreen(
                      onSelectTrip = { navigationActions.navigateTo(Screen.TripInfo(it)) },
                      onPastTrips = { navigationActions.navigateTo(Screen.PastTrips) },
                      onCreateTrip = { navigationActions.navigateTo(Screen.TripSettings1) },
                      onEditCurrentTrip = { navigationActions.navigateTo(Screen.SetCurrentTrip) },
                      navigationActions = navigationActions)
                }
                // Set Current Trip Screen
                composable(Screen.SetCurrentTrip.route) {
                  SetCurrentTripScreen(
                      title = context.getString(R.string.set_current_trip),
                      isSelected = { trip -> trip.isCurrentTrip },
                      onClose = { navigationActions.goBack() },
                      navigationActions = navigationActions)
                }
              }

              // Past Trips Screen
              navigation(
                  startDestination = Screen.PastTrips.route,
                  route = Screen.PastTrips.name,
              ) {
                composable(Screen.PastTrips.route) {
                  PastTripsScreen(
                      onBack = { navigationActions.goBack() },
                      onSelectTrip = { navigationActions.navigateTo(Screen.TripInfo(it)) },
                      navigationActions = navigationActions)
                }
              }

              // Trip Info screen
              navigation(
                  startDestination = Screen.TripInfo.route,
                  route = Screen.TripInfo.name,
              ) {
                composable(Screen.TripInfo.route) { naveBackStackEntry ->
                  val uid = naveBackStackEntry.arguments?.getString("uid")
                  if (uid == null) {
                    Toast.makeText(context, "Trip ID is missing", Toast.LENGTH_SHORT).show()
                    navigationActions.navigateTo(Screen.MyTrips)
                    return@composable
                  }
                  TripInfoScreen(
                      uid,
                      onMyTrips = { navigationActions.goBack() },
                      onFullscreenClick = { navigationActions.navigateTo(Screen.TripInfoMap) },
                      onEditTrip = { navigationActions.navigateToEditTrip(uid) })
                }

                // Map screen
                composable(Screen.TripInfoMap.route) {
                  TripInfoMapScreen(onBack = { navigationActions.goBack() }, viewModel())
                }

                // Edit Trip screen
                composable(
                    route = Screen.EditTrip.route,
                    arguments = listOf(navArgument("tripId") { type = NavType.StringType })) {
                        navBackStackEntry ->
                      val tripId = requireNotNull(navBackStackEntry.arguments?.getString("tripId"))
                      EditTripScreen(
                          tripId = tripId,
                          onBack = { navController.popBackStack() },
                          onSaved = { navController.popBackStack() },
                          onDelete = { navigationActions.navigateTo(Screen.MyTrips) })
                    }
              }

              // Map location screen
              navigation(
                  startDestination = Screen.Map.route,
                  route = Screen.Map.name,
              ) {
                composable(Screen.Map.route) { MapLocationScreen() }
              }

              // Trip settings screens
              navigation(
                  startDestination = Screen.TripSettings1.route,
                  route = Screen.TripSettings1.name,
              ) {
                composable(Screen.TripSettings1.route) {
                  TripDateScreen(
                      viewModel = tripSettingsViewModel(navController),
                      onNext = { navigationActions.navigateTo(Screen.TripSettings2) },
                      onPrevious = { navigationActions.goBack() })
                }
                composable(Screen.TripSettings2.route) {
                  TripTravelersScreen(
                      viewModel = tripSettingsViewModel(navController),
                      onNext = { navigationActions.navigateTo(Screen.TripSettings3) },
                      onPrevious = { navigationActions.goBack() })
                }
                composable(Screen.TripSettings3.route) {
                  TripPreferencesScreen(
                      viewModel = tripSettingsViewModel(navController),
                      onNext = {
                        navigationActions.navigateTo(Screen.TripSettingsArrivalDeparture)
                      },
                      onPrevious = { navigationActions.goBack() })
                }
                composable(Screen.TripSettingsArrivalDeparture.route) {
                  ArrivalDepartureScreen(
                      viewModel = tripSettingsViewModel(navController),
                      onNext = {
                        navigationActions.navigateTo(Screen.TripSettingsFirstDestination)
                      },
                      onPrevious = { navigationActions.goBack() })
                }
                composable(Screen.TripSettingsFirstDestination.route) {
                  FirstDestinationScreen(
                      viewModel = tripSettingsViewModel(navController),
                      onNext = { navigationActions.navigateTo(Screen.TripSummary) },
                      onPrevious = { navigationActions.goBack() })
                }
                composable(Screen.TripSummary.route) {
                  TripSummaryScreen(
                      viewModel = tripSettingsViewModel(navController),
                      onNext = { navigationActions.navigateTo(Screen.MyTrips, true) },
                      onPrevious = { navigationActions.goBack() })
                }
              }
            }
      }
}
