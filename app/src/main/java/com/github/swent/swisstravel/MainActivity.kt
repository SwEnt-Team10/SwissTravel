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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.credentials.CredentialManager
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.github.swent.swisstravel.model.trip.TripsRepositoryFirestore
import com.github.swent.swisstravel.model.user.UserRepositoryFirebase
import com.github.swent.swisstravel.ui.authentication.LandingScreen
import com.github.swent.swisstravel.ui.authentication.SignInScreen
import com.github.swent.swisstravel.ui.authentication.SignUpScreen
import com.github.swent.swisstravel.ui.composable.ActivityInfos
import com.github.swent.swisstravel.ui.currenttrip.CurrentTripScreen
import com.github.swent.swisstravel.ui.navigation.BottomNavigationMenu
import com.github.swent.swisstravel.ui.navigation.NavigationActions
import com.github.swent.swisstravel.ui.navigation.NavigationTestTags
import com.github.swent.swisstravel.ui.navigation.Screen
import com.github.swent.swisstravel.ui.navigation.Tab
import com.github.swent.swisstravel.ui.profile.ProfileScreen
import com.github.swent.swisstravel.ui.profile.ProfileScreenViewModel
import com.github.swent.swisstravel.ui.theme.SwissTravelTheme
import com.github.swent.swisstravel.ui.trip.tripinfos.TripInfoScreen
import com.github.swent.swisstravel.ui.tripcreation.ArrivalDepartureScreen
import com.github.swent.swisstravel.ui.tripcreation.FirstDestinationScreen
import com.github.swent.swisstravel.ui.tripcreation.LoadingScreen
import com.github.swent.swisstravel.ui.tripcreation.TripDateScreen
import com.github.swent.swisstravel.ui.tripcreation.TripPreferencesScreen
import com.github.swent.swisstravel.ui.tripcreation.TripSettingsViewModel
import com.github.swent.swisstravel.ui.tripcreation.TripSummaryScreen
import com.github.swent.swisstravel.ui.tripcreation.TripTravelersScreen
import com.github.swent.swisstravel.ui.trips.MyTripsScreen
import com.github.swent.swisstravel.ui.trips.PastTripsScreen
import com.github.swent.swisstravel.ui.trips.SetCurrentTripScreen
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
        runCatching { navController.getBackStackEntry(Screen.TripSettingsDates.name) }.getOrNull()
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
      if (FirebaseAuth.getInstance().currentUser == null) Screen.Landing.name
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

  SwissTravelScaffold(
      context = context,
      navController = navController,
      navigationActions = navigationActions,
      credentialManager = credentialManager,
      startDestination = startDestination,
      showBottomBar = showBottomBar,
      currentRoute = currentRoute)
}

@Composable
private fun SwissTravelScaffold(
    context: Context,
    navController: NavHostController,
    navigationActions: NavigationActions,
    credentialManager: CredentialManager,
    startDestination: String,
    showBottomBar: Boolean,
    currentRoute: String?
) {
  /* System back button handler */
  BackHandler {
    when {
      // If the current route is authentication then quit the app
      currentRoute == Screen.Landing.route -> {
        (context as? ComponentActivity)?.finish()
      }

      // If the stack is not empty, go back to the previous screen
      navController.previousBackStackEntry != null -> {
        navController.popBackStack()
      }

      // If the stack is empty, do nothing (prevents accidental app exit)
      else -> {
        // Do nothing
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
        SwissTravelNavHost(
            context = context,
            navController = navController,
            navigationActions = navigationActions,
            credentialManager = credentialManager,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding))
      }
}

@Composable
private fun SwissTravelNavHost(
    context: Context,
    navController: NavHostController,
    navigationActions: NavigationActions,
    credentialManager: CredentialManager,
    startDestination: String,
    modifier: Modifier = Modifier
) {
  NavHost(navController = navController, startDestination = startDestination, modifier = modifier) {
    authNavGraph(navigationActions, credentialManager)
    profileNavGraph(navigationActions)
    currentTripNavGraph(navigationActions)
    myTripsNavGraph(context, navigationActions)
    pastTripsNavGraph(navigationActions)
    tripInfoNavGraph(context, navController, navigationActions)
    tripSettingsNavGraph(navController, navigationActions)
  }
}

private fun NavGraphBuilder.authNavGraph(
    navigationActions: NavigationActions,
    credentialManager: CredentialManager
) {
  navigation(
      startDestination = Screen.Landing.route,
      route = Screen.Landing.name,
  ) {
    composable(Screen.Landing.route) {
      LandingScreen(
          onSignInClick = { navigationActions.navigateTo(Screen.Auth) },
          onSignUpClick = { navigationActions.navigateTo(Screen.SignUp) })
    }
    composable(Screen.Auth.route) {
      SignInScreen(
          credentialManager = credentialManager,
          onSignedIn = { navigationActions.navigateTo(Screen.Profile) })
    }
    composable(Screen.SignUp.route) {
      SignUpScreen(
          onSignUpSuccess = { navigationActions.navigateTo(Screen.MyTrips) },
          onPrevious = { navigationActions.navigateTo(Screen.Landing) })
    }
  }
}

private fun NavGraphBuilder.profileNavGraph(navigationActions: NavigationActions) {
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
}

private fun NavGraphBuilder.currentTripNavGraph(navigationActions: NavigationActions) {
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
}

private fun NavGraphBuilder.myTripsNavGraph(
    context: Context,
    navigationActions: NavigationActions
) {
  navigation(
      startDestination = Screen.MyTrips.route,
      route = Screen.MyTrips.name,
  ) {
    composable(Screen.MyTrips.route) {
      MyTripsScreen(
          onSelectTrip = { navigationActions.navigateTo(Screen.TripInfo(it)) },
          onPastTrips = { navigationActions.navigateTo(Screen.PastTrips) },
          onCreateTrip = { navigationActions.navigateTo(Screen.TripSettingsDates) },
          onEditCurrentTrip = { navigationActions.navigateTo(Screen.SetCurrentTrip) })
    }
    composable(Screen.SetCurrentTrip.route) {
      SetCurrentTripScreen(
          title = context.getString(R.string.set_current_trip),
          isSelected = { trip -> trip.isCurrentTrip },
          onClose = { navigationActions.goBack() },
          navigationActions = navigationActions)
    }
  }
}

private fun NavGraphBuilder.pastTripsNavGraph(navigationActions: NavigationActions) {
  navigation(
      startDestination = Screen.PastTrips.route,
      route = Screen.PastTrips.name,
  ) {
    composable(Screen.PastTrips.route) {
      PastTripsScreen(
          onBack = { navigationActions.goBack() },
          onSelectTrip = { navigationActions.navigateTo(Screen.TripInfo(it)) })
    }
  }
}

private fun NavGraphBuilder.tripInfoNavGraph(
    context: Context,
    navController: NavHostController,
    navigationActions: NavigationActions
) {
  navigation(
      startDestination = Screen.TripInfo.route,
      route = Screen.TripInfo.name,
  ) {
    composable(Screen.TripInfo.route) { navBackStackEntry ->
      val uid = navBackStackEntry.arguments?.getString("uid")
      if (uid == null) {
        Toast.makeText(context, "Trip ID is missing", Toast.LENGTH_SHORT).show()
        navigationActions.navigateTo(Screen.MyTrips)
        return@composable
      }

      val vm = navigationActions.tripInfoViewModel(navController)

      TripInfoScreen(
          uid = uid,
          tripInfoViewModel = vm,
          onMyTrips = { navigationActions.goBack() },
          onEditTrip = { navigationActions.navigateToEditTrip(uid) },
          onActivityClick = { tripActivity ->
            vm.selectActivity(tripActivity.activity)
            navigationActions.navigateToActivityInfo(uid)
          })
    }

    composable(
        route = Screen.ActivityInfo.route,
        arguments = listOf(navArgument("uid") { type = NavType.StringType })) { backStackEntry ->
          ActivityInfoRoute(
              context = context,
              navController = navController,
              navigationActions = navigationActions,
              backStackEntry = backStackEntry)
        }

    composable(
        route = Screen.EditTrip.route,
        arguments = listOf(navArgument("tripId") { type = NavType.StringType })) { navBackStackEntry
          ->
          val tripId = requireNotNull(navBackStackEntry.arguments?.getString("tripId"))
          EditTripScreen(
              tripId = tripId,
              onBack = { navController.popBackStack() },
              onSaved = { navController.popBackStack() },
              onDelete = { navigationActions.navigateTo(Screen.MyTrips) })
        }
  }
}

@Composable
private fun ActivityInfoRoute(
    context: Context,
    navController: NavHostController,
    navigationActions: NavigationActions,
    backStackEntry: androidx.navigation.NavBackStackEntry
) {
  val tripId = backStackEntry.arguments?.getString("uid")
  if (tripId == null) {
    Toast.makeText(context, "Trip ID is missing", Toast.LENGTH_SHORT).show()
    navigationActions.goBack()
    return
  }

  val vm = navigationActions.tripInfoViewModel(navController)
  val ui by vm.uiState.collectAsState()
  val activity = ui.selectedActivity

  var hasChecked by remember { mutableStateOf(false) }

  LaunchedEffect(activity) {
    if (!hasChecked) {
      hasChecked = true
      if (activity == null) {
        Toast.makeText(context, "Activity not found", Toast.LENGTH_SHORT).show()
        navigationActions.goBack()
      }
    }
  }

  if (activity != null) {
    ActivityInfos(activity = activity, onBack = { navigationActions.goBack() })
  }
}

private fun NavGraphBuilder.tripSettingsNavGraph(
    navController: NavHostController,
    navigationActions: NavigationActions
) {
  navigation(
      startDestination = Screen.TripSettingsDates.route,
      route = Screen.TripSettingsDates.name,
  ) {
    composable(Screen.TripSettingsDates.route) {
      TripDateScreen(
          viewModel = tripSettingsViewModel(navController),
          onNext = { navigationActions.navigateTo(Screen.TripSettingsTravelers) },
          onPrevious = { navigationActions.goBack() })
    }
    composable(Screen.TripSettingsTravelers.route) {
      TripTravelersScreen(
          viewModel = tripSettingsViewModel(navController),
          onNext = { navigationActions.navigateTo(Screen.TripSettingsPreferences) },
          onPrevious = { navigationActions.goBack() })
    }
    composable(Screen.TripSettingsPreferences.route) {
      TripPreferencesScreen(
          viewModel = tripSettingsViewModel(navController),
          onNext = { navigationActions.navigateTo(Screen.TripSettingsArrivalDeparture) },
          onPrevious = { navigationActions.goBack() })
    }
    composable(Screen.TripSettingsArrivalDeparture.route) {
      ArrivalDepartureScreen(
          viewModel = tripSettingsViewModel(navController),
          onNext = { navigationActions.navigateTo(Screen.TripSettingsFirstDestination) },
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
          onNext = { navigationActions.navigateTo(Screen.Loading) },
          onPrevious = { navigationActions.goBack() })
    }
    composable(Screen.Loading.route) {
      val viewModel = tripSettingsViewModel(navController)
      val loadingProgress by viewModel.loadingProgress.collectAsState()
      LoadingScreen(
          progress = loadingProgress,
          viewModel = viewModel,
          onSuccess = {
            navigationActions.resetTo(Screen.MyTrips)
            navigationActions.navigateTo(Screen.MyTrips, true)
          },
          onFailure = { navigationActions.goBack() })
    }
  }
}
