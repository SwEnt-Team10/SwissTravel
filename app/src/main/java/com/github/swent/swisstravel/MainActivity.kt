package com.github.swent.swisstravel

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
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
import com.github.swent.swisstravel.ui.activities.LikedActivitiesScreen
import com.github.swent.swisstravel.ui.activities.SwipeActivitiesScreen
import com.github.swent.swisstravel.ui.authentication.LandingScreen
import com.github.swent.swisstravel.ui.authentication.SignInScreen
import com.github.swent.swisstravel.ui.authentication.SignUpScreen
import com.github.swent.swisstravel.ui.composable.ActivityInfos
import com.github.swent.swisstravel.ui.currenttrip.CurrentTripScreen
import com.github.swent.swisstravel.ui.friends.AddFriendScreen
import com.github.swent.swisstravel.ui.friends.FriendsListScreen
import com.github.swent.swisstravel.ui.friends.FriendsViewModel
import com.github.swent.swisstravel.ui.navigation.BottomNavigationMenu
import com.github.swent.swisstravel.ui.navigation.NavigationActions
import com.github.swent.swisstravel.ui.navigation.NavigationTestTags
import com.github.swent.swisstravel.ui.navigation.Screen
import com.github.swent.swisstravel.ui.navigation.Tab
import com.github.swent.swisstravel.ui.profile.ProfileScreen
import com.github.swent.swisstravel.ui.profile.ProfileSettingsScreen
import com.github.swent.swisstravel.ui.profile.ProfileSettingsViewModel
import com.github.swent.swisstravel.ui.profile.ProfileViewModelFactory
import com.github.swent.swisstravel.ui.profile.selectpinnedpictures.SelectPinnedPicturesScreen
import com.github.swent.swisstravel.ui.profile.selectpinnedpictures.SelectPinnedPicturesViewModel
import com.github.swent.swisstravel.ui.profile.selectpinnedtrips.SelectPinnedTripsScreen
import com.github.swent.swisstravel.ui.profile.selectpinnedtrips.SelectPinnedTripsViewModel
import com.github.swent.swisstravel.ui.theme.SwissTravelTheme
import com.github.swent.swisstravel.ui.trip.edittrip.EditTripScreen
import com.github.swent.swisstravel.ui.trip.tripinfos.DailyViewScreen
import com.github.swent.swisstravel.ui.trip.tripinfos.DailyViewScreenCallbacks
import com.github.swent.swisstravel.ui.trip.tripinfos.TripInfoViewModel
import com.github.swent.swisstravel.ui.trip.tripinfos.photos.AddPhotosScreen
import com.github.swent.swisstravel.ui.trip.tripinfos.photos.EditPhotosScreen
import com.github.swent.swisstravel.ui.tripcreation.ArrivalDepartureScreen
import com.github.swent.swisstravel.ui.tripcreation.FirstDestinationScreen
import com.github.swent.swisstravel.ui.tripcreation.LoadingScreen
import com.github.swent.swisstravel.ui.tripcreation.TripDateScreen
import com.github.swent.swisstravel.ui.tripcreation.TripPreferencesScreen
import com.github.swent.swisstravel.ui.tripcreation.TripSettingsViewModel
import com.github.swent.swisstravel.ui.tripcreation.TripSummaryScreen
import com.github.swent.swisstravel.ui.tripcreation.TripTravelersScreen
import com.github.swent.swisstravel.ui.trips.MyTripsScreen
import com.github.swent.swisstravel.ui.trips.MyTripsViewModel
import com.github.swent.swisstravel.ui.trips.PastTripsScreen
import com.github.swent.swisstravel.ui.trips.SetCurrentTripScreen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

/**
 * Represents navigation data for the app.
 *
 * @property navController The NavHostController used for navigation.
 * @property navigationActions The NavigationActions used for navigation.
 * @property startDestination The starting destination for navigation.
 * @property currentRoute The current route in the navigation graph.
 */
private data class NavData(
    val navController: NavHostController,
    val navigationActions: NavigationActions,
    val startDestination: String,
    val currentRoute: String?
)

/**
 * Represents whether to show the bottom bar.
 *
 * @property showBottomBar Whether to show the bottom bar.
 * @property myTripsViewModel The MyTripsViewModel used for navigation.
 */
private data class BottomBarShow(
    val showBottomBar: Boolean,
    val myTripsViewModel: MyTripsViewModel
)

/** Provides a singleton instance of the OkHttpClient for making HTTP requests. */
object HttpClientProvider {
  var client: OkHttpClient = OkHttpClient()
}

/** The main activity of the app. */
class MainActivity : ComponentActivity() {
  /**
   * Called when the activity is starting.
   *
   * @param savedInstanceState The previously saved state of the activity.
   */
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

/** Helper to observe the current Firebase user reactively. */
@Composable
fun rememberCurrentUser(): State<FirebaseUser?> {
  val auth = remember { FirebaseAuth.getInstance() }
  val currentUser = remember { mutableStateOf(auth.currentUser) }

  DisposableEffect(auth) {
    val listener =
        FirebaseAuth.AuthStateListener { authState -> currentUser.value = authState.currentUser }
    auth.addAuthStateListener(listener)
    onDispose { auth.removeAuthStateListener(listener) }
  }

  return currentUser
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
 * Retrieves the FriendsViewModel scoped to the FriendsList navigation graph.
 *
 * @param navController The NavHostController used for navigation.
 * @return The FriendsViewModel instance.
 */
@Composable
fun friendsViewModel(navController: NavHostController): FriendsViewModel {
  val currentEntry by navController.currentBackStackEntryAsState()

  val parentEntry =
      remember(currentEntry) {
        runCatching { navController.getBackStackEntry(Screen.FriendsList.name) }.getOrNull()
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
  val myTripsViewModel: MyTripsViewModel = viewModel()
  val myTripsUiState by myTripsViewModel.uiState.collectAsState()

  val currentUser = rememberCurrentUser().value

  val isUserValid =
      (currentUser != null && currentUser.isEmailVerified) || currentUser?.isAnonymous == true

  // Only determine start destination once based on initial state
  val startDestination = remember {
    if (isUserValid) Screen.CurrentTrip.name else Screen.Landing.name
  }

  // Redirect to Landing if user becomes invalid (e.g. logs out)
  LaunchedEffect(isUserValid) {
    if (!isUserValid) {
      FirebaseAuth.getInstance().signOut()
    }
  }

  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = navBackStackEntry?.destination?.route

  val showBottomBar =
      when (currentRoute) {
        Screen.CurrentTrip.route,
        Screen.MyTrips.route,
        Screen.FriendsList.route,
        Screen.Profile.route -> !myTripsUiState.isSelectionMode
        else -> false
      }

  val bottomBarShow = BottomBarShow(showBottomBar, myTripsViewModel)
  val navData = NavData(navController, navigationActions, startDestination, currentRoute)

  SwissTravelScaffold(
      context = context,
      credentialManager = credentialManager,
      navData = navData,
      bottomBarShow = bottomBarShow)
}

/**
 * Handles the main scaffold for the Swiss Travel App.
 *
 * @param context The context of the current state of the application.
 * @param credentialManager The CredentialManager for handling user credentials.
 * @param navData The navigation data for the app.
 * @param bottomBarShow Whether to show the bottom bar.
 */
@Composable
private fun SwissTravelScaffold(
    context: Context,
    credentialManager: CredentialManager,
    navData: NavData,
    bottomBarShow: BottomBarShow
) {
  /* System back button handler */
  BackHandler {
    when {
      // If the current route is authentication then quit the app
      navData.currentRoute == Screen.Landing.route -> {
        (context as? ComponentActivity)?.finish()
      }

      // If the stack is not empty, go back to the previous screen
      navData.navController.previousBackStackEntry != null -> {
        navData.navController.popBackStack()
      }

      // If the stack is empty, do nothing (prevents accidental app exit)
      else -> {
        // Do nothing
      }
    }
  }

  Scaffold(
      bottomBar = {
        if (bottomBarShow.showBottomBar) {
          BottomNavigationMenu(
              selectedTab =
                  when (navData.currentRoute) {
                    Screen.CurrentTrip.route -> Tab.CurrentTrip
                    Screen.MyTrips.route -> Tab.MyTrips
                    Screen.FriendsList.route -> Tab.Friends
                    Screen.Profile.route -> Tab.Profile
                    else -> Tab.CurrentTrip
                  },
              onTabSelected = { tab -> navData.navigationActions.navigateTo(tab.destination) },
              modifier = Modifier.testTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU))
        }
      }) { innerPadding ->
        SwissTravelNavHost(
            context = context,
            navController = navData.navController,
            navigationActions = navData.navigationActions,
            credentialManager = credentialManager,
            startDestination = navData.startDestination,
            modifier = Modifier.padding(innerPadding),
            myTripsViewModel = bottomBarShow.myTripsViewModel)
      }
}

/**
 * Sets up the navigation graph for the Swiss Travel App.
 *
 * @param context The context of the current state of the application.
 * @param navController The NavHostController used for navigation.
 * @param navigationActions The NavigationActions used for navigation.
 * @param credentialManager The CredentialManager for handling user credentials.
 * @param startDestination The starting destination for navigation.
 * @param modifier The modifier to apply to this layout.
 * @param myTripsViewModel The MyTripsViewModel used for navigation.
 */
@Composable
private fun SwissTravelNavHost(
    context: Context,
    navController: NavHostController,
    navigationActions: NavigationActions,
    credentialManager: CredentialManager,
    startDestination: String,
    modifier: Modifier = Modifier,
    myTripsViewModel: MyTripsViewModel
) {
  NavHost(navController = navController, startDestination = startDestination, modifier = modifier) {
    authNavGraph(navigationActions, credentialManager)
    profileNavGraph(navigationActions)
    currentTripNavGraph(navigationActions)
    myTripsNavGraph(context, navigationActions, myTripsViewModel)
    pastTripsNavGraph(navigationActions)
    tripInfoNavGraph(context, navController, navigationActions)
    tripSettingsNavGraph(navController, navigationActions)
    friendsListNavGraph(context, navController, navigationActions)
  }
}

/**
 * Sets up the authentication navigation graph for the Swiss Travel App.
 *
 * @param navigationActions The NavigationActions used for navigation.
 * @param credentialManager The CredentialManager for handling user credentials.
 */
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
          onSignedIn = { navigationActions.navigateTo(Screen.Profile) },
          onPrevious = { navigationActions.navigateTo(Screen.Landing) })
    }
    composable(Screen.SignUp.route) {
      SignUpScreen(
          onSignUpSuccess = { navigationActions.navigateTo(Screen.MyTrips) },
          onPrevious = { navigationActions.navigateTo(Screen.Landing) })
    }
  }
}

/**
 * Sets up the profile navigation graph for the Swiss Travel App.
 *
 * @param navigationActions The NavigationActions used for navigation. once edit pinned methods are
 *   implemented
 */
private fun NavGraphBuilder.profileNavGraph(navigationActions: NavigationActions) {
  navigation(
      startDestination = Screen.Profile.route,
      route = Screen.Profile.name,
  ) {
    composable(Screen.Profile.route) {
      val uid = rememberCurrentUser().value?.uid ?: ""

      ProfileScreen(
          profileViewModel =
              viewModel(key = uid, factory = ProfileViewModelFactory(requestedUid = uid)),
          onSettings = { navigationActions.navigateTo(Screen.ProfileSettings) },
          onSelectTrip = { navigationActions.navigateTo(Screen.DailyView(it)) },
          onEditPinnedTrips = { navigationActions.navigateTo(Screen.SelectPinnedTrips) },
          onEditPinnedPictures = { navigationActions.navigateTo(Screen.SelectPinnedPictures) })
    }
    composable(Screen.ProfileSettings.route) {
      ProfileSettingsScreen(
          profileSettingsViewModel = ProfileSettingsViewModel(),
          onBack = { navigationActions.goBack() },
          navigationActions = navigationActions)
    }
    composable(Screen.SelectPinnedTrips.route) {
      SelectPinnedTripsScreen(
          selectPinnedTripsViewModel = SelectPinnedTripsViewModel(),
          onBack = { navigationActions.goBack() },
      )
    }
    composable(Screen.SelectPinnedPictures.route) {
      SelectPinnedPicturesScreen(
          selectPinnedPicturesViewModel = SelectPinnedPicturesViewModel(),
          onBack = { navigationActions.goBack() })
    }
  }
}

/**
 * Sets up the current trip navigation graph for the Swiss Travel App.
 *
 * @param navigationActions The NavigationActions used for navigation.
 */
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

/**
 * Sets up the my trips navigation graph for the Swiss Travel App.
 *
 * @param context The context of the current state of the application.
 * @param navigationActions The NavigationActions used for navigation.
 * @param myTripsViewModel The MyTripsViewModel providing state and business logic.
 */
private fun NavGraphBuilder.myTripsNavGraph(
    context: Context,
    navigationActions: NavigationActions,
    myTripsViewModel: MyTripsViewModel
) {
  navigation(
      startDestination = Screen.MyTrips.route,
      route = Screen.MyTrips.name,
  ) {
    composable(Screen.MyTrips.route) {
      MyTripsScreen(
          myTripsViewModel = myTripsViewModel,
          onSelectTrip = { navigationActions.navigateTo(Screen.DailyView(it)) },
          onPastTrips = { navigationActions.navigateTo(Screen.PastTrips) },
          onCreateTrip = { navigationActions.navigateTo(Screen.TripSettingsDates) },
          onEditCurrentTrip = { navigationActions.navigateTo(Screen.SetCurrentTrip) })
    }
    composable(Screen.SetCurrentTrip.route) {
      SetCurrentTripScreen(
          viewModel = myTripsViewModel,
          title = context.getString(R.string.set_current_trip),
          isSelected = { trip -> trip.isCurrentTrip },
          onClose = { navigationActions.goBack() },
          navigationActions = navigationActions)
    }
  }
}

/**
 * Sets up the past trips navigation graph for the Swiss Travel App.
 *
 * @param navigationActions The NavigationActions used for navigation.
 */
private fun NavGraphBuilder.pastTripsNavGraph(navigationActions: NavigationActions) {
  navigation(
      startDestination = Screen.PastTrips.route,
      route = Screen.PastTrips.name,
  ) {
    composable(Screen.PastTrips.route) {
      PastTripsScreen(
          onBack = { navigationActions.goBack() },
          onSelectTrip = { navigationActions.navigateTo(Screen.DailyView(it)) })
    }
  }
}

/**
 * Sets up the trip info navigation graph for the Swiss Travel App.
 *
 * @param context The context of the current state of the application.
 */
private fun NavGraphBuilder.tripInfoNavGraph(
    context: Context,
    navController: NavHostController,
    navigationActions: NavigationActions
) {
  navigation(
      startDestination = Screen.DailyView.route,
      route = Screen.TripInfo.name,
  ) {
    composable(Screen.DailyView.route) { navBackStackEntry ->
      val uid = navBackStackEntry.arguments?.getString("uid")
      if (uid == null) {
        Toast.makeText(context, context.getString(R.string.trip_id_missing), Toast.LENGTH_SHORT)
            .show()
        navigationActions.navigateTo(Screen.MyTrips)
        return@composable
      }

      // conserve the ViewModel tied to the TripInfo navigation graph
      val parentEntry =
          remember(navBackStackEntry) { navController.getBackStackEntry(Screen.TripInfo.name) }
      val vm = viewModel<TripInfoViewModel>(parentEntry)

      val refresh = navBackStackEntry.savedStateHandle.get<Boolean>("refresh_trip")
      if (refresh == true) {
        navBackStackEntry.savedStateHandle["refresh_trip"] = false // Reset flag
        vm.loadTripInfo(uid, forceReload = true)
      }

      DailyViewScreen(
          uid = uid,
          tripInfoViewModel = vm,
          callbacks =
              DailyViewScreenCallbacks(
                  onMyTrips = { navigationActions.goBack() },
                  onEditTrip = { navigationActions.navigateToEditTrip(uid) },
                  onActivityClick = { tripActivity ->
                    vm.selectActivity(tripActivity.activity)
                    navigationActions.navigateToActivityInfo(uid)
                  },
                  onSwipeActivities = { navigationActions.navigateTo(Screen.SwipeActivities) },
                  onLikedActivities = { navigationActions.navigateTo(Screen.LikedActivities) }),
          onAddPhotos = { navigationActions.navigateTo(Screen.AddPhotos(uid)) })
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
              onSaved = {
                navController.previousBackStackEntry?.savedStateHandle?.set("refresh_trip", true)
                navController.popBackStack()
              },
              onDelete = { navigationActions.navigateTo(Screen.MyTrips) })
        }

    composable(Screen.SwipeActivities.route) { navBackStackEntry ->
      val parentEntry =
          remember(navBackStackEntry) { navController.getBackStackEntry(Screen.TripInfo.name) }
      SwipeActivitiesScreen(
          onTripInfo = { navController.popBackStack() },
          tripInfoVM = viewModel<TripInfoViewModel>(parentEntry))
    }

    composable(Screen.LikedActivities.route) { navBackStackEntry ->
      val parentEntry =
          remember(navBackStackEntry) { navController.getBackStackEntry(Screen.TripInfo.name) }
      val vm = viewModel<TripInfoViewModel>(parentEntry)
      val errorText = stringResource(R.string.no_activities_selected)
      val scope = rememberCoroutineScope()
      LikedActivitiesScreen(
          onBack = { navController.popBackStack() },
          tripInfoVM = vm,
          onSchedule = {
            if (vm.uiState.value.selectedLikedActivities.isEmpty()) {
              Toast.makeText(context, errorText, Toast.LENGTH_SHORT).show()
            } else {
              scope.launch { vm.scheduleSelectedActivities(context) }
            }
          },
          onUnlike = {
            if (vm.uiState.value.selectedLikedActivities.isEmpty()) {
              Toast.makeText(context, errorText, Toast.LENGTH_SHORT).show()
            } else vm.unlikeSelectedActivities()
          },
          onNext = {
            vm.resetSchedulingState()
            navController.popBackStack()
          })
    }
    composable(Screen.AddPhotos.route) { navBackStackEntry ->
      val tripId = navBackStackEntry.arguments?.getString(stringResource(R.string.trip_id_route))

      tripId?.let {
        AddPhotosScreen(
            onBack = { navController.popBackStack() },
            tripId = tripId,
            onEdit = { navigationActions.navigateTo(Screen.EditPhotos(tripId)) })
      }
          ?: run {
            Log.e(
                stringResource(R.string.add_photos_screen_tag),
                stringResource(R.string.null_trip_id))
            Toast.makeText(context, stringResource(R.string.null_trip_id), Toast.LENGTH_SHORT)
                .show()
          }
    }
    composable(Screen.EditPhotos.route) { navBackStackEntry ->
      val tripId = navBackStackEntry.arguments?.getString(stringResource(R.string.trip_id_route))

      tripId?.let { EditPhotosScreen(onCancel = { navigationActions.goBack() }, tripId = tripId) }
    }
  }
}

/**
 * Sets up the activity info route for the Swiss Travel App.
 *
 * @param context The context of the current state of the application.
 */
@Composable
private fun ActivityInfoRoute(
    context: Context,
    navController: NavHostController,
    navigationActions: NavigationActions,
    backStackEntry: androidx.navigation.NavBackStackEntry
) {
  val tripId = backStackEntry.arguments?.getString("uid")
  if (tripId == null) {
    Toast.makeText(context, context.getString(R.string.trip_id_missing), Toast.LENGTH_SHORT).show()
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

/**
 * Sets up the trip settings navigation graph for the Swiss Travel App.
 *
 * @param navController The NavHostController used for navigation.
 */
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
      val vm = tripSettingsViewModel(navController)
      TripTravelersScreen(
          viewModel = vm,
          onNext = {
            vm.setRandomTrip(false)
            navigationActions.navigateTo(Screen.TripSettingsPreferences)
          },
          onRandom = {
            vm.setRandomTrip(true)
            navigationActions.navigateTo(Screen.TripSettingsPreferences)
          },
          onPrevious = { navigationActions.goBack() })
    }
    composable(Screen.TripSettingsPreferences.route) {
      val vm = tripSettingsViewModel(navController)
      val isRandomTrip by vm.isRandomTrip.collectAsState()
      TripPreferencesScreen(
          viewModel = vm,
          onNext = { navigationActions.navigateTo(Screen.TripSettingsArrivalDeparture) },
          onPrevious = { navigationActions.goBack() },
          isRandomTrip = isRandomTrip)
    }
    composable(Screen.TripSettingsArrivalDeparture.route) {
      val vm = tripSettingsViewModel(navController)
      val isRandomTrip by vm.isRandomTrip.collectAsState()
      ArrivalDepartureScreen(
          viewModel = vm,
          onNext = { navigationActions.navigateTo(Screen.TripSettingsFirstDestination) },
          onPrevious = { navigationActions.goBack() },
          isRandomTrip = isRandomTrip,
          onRandom = { navigationActions.navigateTo(Screen.Loading) })
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

/**
 * The navigation graph for the friends list and add friend screens.
 *
 * @param navController The NavHostController used for navigation.
 * @param navigationActions The NavigationActions used for navigation actions.
 */
private fun NavGraphBuilder.friendsListNavGraph(
    context: Context,
    navController: NavHostController,
    navigationActions: NavigationActions
) {
  navigation(
      startDestination = Screen.FriendsList.route,
      route = Screen.FriendsList.name,
  ) {
    // Friends List Screen
    composable(Screen.FriendsList.route) {
      FriendsListScreen(
          onSelectFriend = { navigationActions.navigateTo(Screen.FriendProfile(it)) },
          onAddFriend = { navigationActions.navigateTo(Screen.AddFriend) })
    }

    // Add Friend Screen
    composable(Screen.AddFriend.route) {
      val vm = friendsViewModel(navController)

      AddFriendScreen(friendsViewModel = vm, onBack = { navigationActions.goBack() })
    }

    // Friend Profile Screen
    composable(
        route = Screen.FriendProfile.route,
        arguments = listOf(navArgument("uid") { type = NavType.StringType })) { entry ->
          val uid = entry.arguments?.getString("uid")
          if (uid == null) {
            Toast.makeText(
                    context, context.getString(R.string.user_uid_missing), Toast.LENGTH_SHORT)
                .show()
            navigationActions.goBack()
            return@composable
          }

          ProfileScreen(
              profileViewModel =
                  viewModel(key = uid, factory = ProfileViewModelFactory(requestedUid = uid)),
              onBack = { navigationActions.goBack() },
              onSelectTrip = { navigationActions.navigateTo(Screen.DailyView(it)) },
              friendsViewModel = friendsViewModel(navController))
        }
  }
}
