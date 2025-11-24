package com.github.swent.swisstravel.ui.profile

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.model.trip.Trip
import com.github.swent.swisstravel.model.user.UserStats
import com.github.swent.swisstravel.ui.composable.TripList
import com.github.swent.swisstravel.ui.navigation.NavigationActions
import com.github.swent.swisstravel.ui.navigation.NavigationTestTags

object ProfileScreenTestTags {
  const val SETTINGS_BUTTON = "settingsButton"
  const val DISPLAY_NAME = "displayName"
  const val PROFILE_PIC = "profilePic"
  const val BIOGRAPHY = "biography"
  const val ACHIEVEMENTS = "achievements"
  const val PINNED_TRIPS = "pinnedTrips"
  const val PINNED_TRIPS_EDIT_BUTTON = "pinnedTripsEditButton"
  const val PINNED_IMAGES = "pinnedImages"
  const val PINNED_IMAGES_EDIT_BUTTON = "pinnedImagesEditButton"
}

/**
 * A screen that shows the user's profile information.
 *
 * @param profileViewModel The view model for this screen.
 * @param onSettings The callback to navigate to the settings screen.
 * @param navigationActions The navigation actions for this screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    uid: String,
    profileViewModel: ProfileViewModel = viewModel(),
    onSettings: () -> Unit = {},
    onSelectTrip: (String) -> Unit = {},
    onEditPinnedTrips: () -> Unit = {},
    navigationActions: NavigationActions? = null,
) {
  val context = LocalContext.current
  val uiState = profileViewModel.uiState.collectAsState().value

  LaunchedEffect(uiState.errorMsg) {
    uiState.errorMsg
        ?.takeIf { it.isNotBlank() }
        ?.let {
          Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
          profileViewModel.clearErrorMsg()
        }
  }

  Scaffold(
      topBar = {
        TopAppBar(
            title = { Text(stringResource(R.string.my_profile)) },
            actions = {
              if (uiState.isOwnProfile) {
                IconButton(
                    onClick = onSettings,
                    modifier = Modifier.testTag(ProfileScreenTestTags.SETTINGS_BUTTON)) {
                      Icon(
                          Icons.Outlined.Settings,
                          contentDescription = stringResource(R.string.settings))
                    }
              }
            },
            modifier = Modifier.testTag(NavigationTestTags.TOP_BAR))
      }) { pd ->
        if (uiState.isLoading) {
          Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
          }
        } else {
          ProfileScreenContent(
              uiState = uiState,
              profileScreenViewModel = profileViewModel,
              onSelectTrip = onSelectTrip,
              modifier = Modifier.padding(pd))
        }
      }
}

/**
 * The content of the profile screen.
 *
 * @param uiState The state of the screen.
 * @param profileScreenViewModel The view model for this screen.
 */
@Composable
private fun ProfileScreenContent(
    uiState: ProfileUIState,
    profileScreenViewModel: ProfileViewModel,
    onSelectTrip: (String) -> Unit,
    onEditPinnedTrips: () -> Unit = {},
    modifier: Modifier
) {
  val scrollState = rememberScrollState()

  Column(
      modifier =
          modifier
              .fillMaxSize()
              .padding(
                  top = dimensionResource(R.dimen.profile_padding_top_bottom),
                  start = dimensionResource(R.dimen.profile_padding_start_end),
                  end = dimensionResource(R.dimen.profile_padding_start_end),
                  bottom = dimensionResource(R.dimen.profile_padding_top_bottom))
              .verticalScroll(scrollState),
      horizontalAlignment = Alignment.CenterHorizontally) {
        ProfileHeader(photoUrl = uiState.profilePicUrl, name = uiState.name)

        // Biography
        Row(
            modifier = Modifier.fillMaxWidth().testTag(ProfileScreenTestTags.BIOGRAPHY),
            verticalAlignment = Alignment.CenterVertically) {
              Text(
                  text = uiState.biography,
                  style = MaterialTheme.typography.bodyMedium,
                  maxLines = 3,
                  overflow = TextOverflow.Ellipsis)
            }

        AchievementsDisplay(uiState.stats)

        PinnedTrips(
            pinnedTrips = uiState.pinnedTrips,
            isOwnProfile = uiState.isOwnProfile,
            onEditPinnedTrips = onEditPinnedTrips,
            onSelectTrip = onSelectTrip)
      }
}

/**
 * The profile header section of the profile screen.
 *
 * @param photoUrl The URL of the profile picture.
 */
@Composable
private fun ProfileHeader(photoUrl: String, name: String) {
  Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
    AsyncImage(
        model = photoUrl.ifBlank { R.drawable.default_profile_pic },
        contentDescription = stringResource(R.string.profile_pic_desc),
        modifier =
            Modifier.size(dimensionResource(R.dimen.profile_logo_size))
                .clip(CircleShape)
                .testTag(ProfileSettingsTestTags.PROFILE_PIC))

    Spacer(modifier = Modifier.width(dimensionResource(R.dimen.smaller_spacer)))

    Text(
        text = name,
        style = MaterialTheme.typography.headlineLarge,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis)
  }
}

/**
 * The achievements section of the profile screen.
 *
 * @param stats The user's stats.
 */
@Composable
private fun AchievementsDisplay(stats: UserStats) {
  Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
    Text(
        text = stringResource(R.string.achievements),
        style = MaterialTheme.typography.headlineLarge,
    )
  }

  Spacer(modifier = Modifier.height(dimensionResource(R.dimen.tiny_spacer)))

  Row(
      modifier = Modifier.fillMaxWidth().testTag(ProfileScreenTestTags.ACHIEVEMENTS),
      horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    // TODO TEMPORARY (HENCE HARDCODED STRINGS)
    AchievementItem("Trips", stats.totalTrips.toString())
    AchievementItem("Minutes", stats.totalTravelMinutes.toString())
    AchievementItem("Locations", stats.uniqueLocations.toString())
    AchievementItem("Transport", stats.mostUsedTransportMode?.name ?: "-")
    AchievementItem("Longest (min)", stats.longestRouteSegmentMin.toString())
  }
}

/**
 * A single achievement item in the achievements section.
 *
 * @param label The label for the achievement.
 * @param value The value for the achievement.
 */
@Composable
private fun AchievementItem(label: String, value: String) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(text = value, style = MaterialTheme.typography.headlineMedium)
    Text(text = label, style = MaterialTheme.typography.bodySmall)
  }
}

/**
 * The pinned trips section of the profile screen.
 *
 * @param pinnedTrips The list of pinned trips.
 * @param isOwnProfile Whether the user is their own profile.
 * @param onEditPinnedTrips The callback to navigate to the edit pinned trips screen.
 * @param onSelectTrip The callback to select a trip.
 */
@Composable
private fun PinnedTrips(
    pinnedTrips: List<Trip>,
    isOwnProfile: Boolean,
    onEditPinnedTrips: () -> Unit,
    onSelectTrip: (String) -> Unit,
) {
  Row(
      modifier = Modifier.fillMaxWidth().testTag(ProfileScreenTestTags.PINNED_TRIPS),
      horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            text = stringResource(R.string.pinned_trips),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground)

        if (isOwnProfile) {
          IconButton(
              onClick = onEditPinnedTrips,
              modifier = Modifier.testTag(ProfileScreenTestTags.PINNED_TRIPS_EDIT_BUTTON)) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = stringResource(R.string.edit_pinned_trips))
              }
        }
      }

  TripList(
      trips = pinnedTrips,
      onClickTripElement = { trip -> trip?.let { onSelectTrip(it.uid) } },
      onLongPress = { /* no-op */},
      isSelected = { false },
      isSelectionMode = false,
      noIconTripElement = false,
      emptyListString = "" // TODO
      )
}

/**
 * The pinned images section of the profile screen.
 *
 * @param pinnedImages The list of pinned images.
 * @param isOwnProfile Whether the user is their own profile.
 * @param onEditPinnedImages The callback to navigate to the edit pinned images screen.
 */
@Composable
private fun PinnedImages(
    pinnedImages: List<String>,
    isOwnProfile: Boolean,
    onEditPinnedImages: () -> Unit,
) {
  Row(
      modifier = Modifier.fillMaxWidth().testTag(ProfileScreenTestTags.PINNED_IMAGES),
      horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            text = stringResource(R.string.pinned_images),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground)

        if (isOwnProfile) {
          IconButton(
              onClick = onEditPinnedImages,
              modifier = Modifier.testTag(ProfileScreenTestTags.PINNED_IMAGES_EDIT_BUTTON)) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = stringResource(R.string.edit_pinned_images))
              }
        }
      }

  // TODO
}
