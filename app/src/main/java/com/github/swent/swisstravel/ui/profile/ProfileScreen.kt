package com.github.swent.swisstravel.ui.profile

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PersonRemove
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.model.trip.Trip
import com.github.swent.swisstravel.model.user.UserStats
import com.github.swent.swisstravel.ui.composable.TripList
import com.github.swent.swisstravel.ui.navigation.NavigationTestTags

/** Test tags for the profile screen. */
object ProfileScreenTestTags {
  const val SETTINGS_BUTTON = "settingsButton"
  const val UNFRIEND_BUTTON = "unfriendButton"
  const val DISPLAY_NAME = "displayName"
  const val PROFILE_PIC = "profilePic"
  const val BIOGRAPHY = "biography"
  const val ACHIEVEMENTS = "achievements"
  const val PINNED_TRIPS_TITLE = "pinnedTripsTitle"
  const val PINNED_TRIPS_EDIT_BUTTON = "pinnedTripsEditButton"
  const val PINNED_IMAGES_TITLE = "pinnedImagesTitle"
  const val PINNED_IMAGES_LIST = "pinnedImagesList"
  const val PINNED_IMAGES_EDIT_BUTTON = "pinnedImagesEditButton"
  const val CONFIRM_UNFRIEND_BUTTON = "confirmUnfriendButton"
  const val CANCEL_UNFRIEND_BUTTON = "cancelUnfriendButton"
  const val LOADING_INDICATOR = "loadingIndicator"
}

/** The maximum number of lines for the name. */
private const val NAME_MAX_LINES = 1

/** The maximum number of lines for the biography. */
private const val BIOGRAPHY_MAX_LINES = 3

/**
 * A screen that shows the user's profile information.
 *
 * @param profileViewModel The view model for this screen.
 * @param onBack The callback to navigate back.
 * @param onSettings The callback to navigate to the settings screen.
 * @param onSelectTrip The callback to select a trip.
 * @param onEditPinnedTrips The callback to navigate to the edit pinned trips screen.
 * @param onEditPinnedImages The callback to navigate to the edit pinned images screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    profileViewModel: ProfileViewModel = viewModel(),
    onBack: () -> Unit = {},
    onSettings: () -> Unit = {},
    onSelectTrip: (String) -> Unit = {},
    onEditPinnedTrips: () -> Unit = {},
    onEditPinnedImages: () -> Unit = {},
) {
  val context = LocalContext.current
  val uiState by profileViewModel.uiState.collectAsState()

  LaunchedEffect(uiState.errorMsg) {
    uiState.errorMsg
        ?.takeIf { it.isNotBlank() }
        ?.let {
          Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
          profileViewModel.clearErrorMsg()
        }
  }

  var showUnfriendConfirmation by remember { mutableStateOf(false) }

  if (showUnfriendConfirmation) {
    UnfriendDialog(
        friendName = uiState.name,
        onConfirm = {
          profileViewModel.removeFriend(uiState.uid)
          showUnfriendConfirmation = false
          onBack()
        },
        onCancel = { showUnfriendConfirmation = false })
  }

  Scaffold(
      topBar = {
        ProfileScreenTopBar(
            uiState = uiState,
            onBack = onBack,
            onSettings = onSettings,
            onUnfriend = { showUnfriendConfirmation = true })
      }) { pd ->
        if (uiState.isLoading) {
          Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                modifier = Modifier.testTag(ProfileScreenTestTags.LOADING_INDICATOR))
          }
        } else {
          ProfileScreenContent(
              uiState = uiState,
              onSelectTrip = onSelectTrip,
              onEditPinnedTrips = {
                Toast.makeText(context, "I don't work yet :<", Toast.LENGTH_SHORT).show()
              }, // todo onEditPinnedTrips,
              onEditPinnedImages = {
                Toast.makeText(context, "I don't work yet :<", Toast.LENGTH_SHORT).show()
              }, // todo onEditPinnedImages,
              modifier = Modifier.padding(pd))
        }
      }
}

/**
 * The profile screen Top Bar.
 *
 * @param uiState The state of the screen.
 * @param onBack The callback to navigate back.
 * @param onSettings The callback to navigate to the settings screen.
 * @param onUnfriend The callback to unfriend the user.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileScreenTopBar(
    uiState: ProfileUIState,
    onBack: () -> Unit,
    onSettings: () -> Unit,
    onUnfriend: () -> Unit
) {
  TopAppBar(
      title = {
        if (uiState.isOwnProfile) Text(stringResource(R.string.my_profile)) else Text(uiState.name)
      },
      navigationIcon = {
        if (!uiState.isLoading && !uiState.isOwnProfile) {
          IconButton(
              onClick = onBack,
          ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.back_to_friends_list),
                tint = MaterialTheme.colorScheme.onBackground)
          }
        }
      },
      actions = {
        if (!uiState.isLoading) {
          if (uiState.isOwnProfile) {
            IconButton(
                onClick = onSettings,
                modifier = Modifier.testTag(ProfileScreenTestTags.SETTINGS_BUTTON)) {
                  Icon(
                      imageVector = Icons.Outlined.Settings,
                      contentDescription = stringResource(R.string.settings),
                      tint = MaterialTheme.colorScheme.onBackground)
                }
          } else {
            IconButton(
                onClick = onUnfriend,
                modifier = Modifier.testTag(ProfileScreenTestTags.UNFRIEND_BUTTON)) {
                  Icon(
                      imageVector = Icons.Outlined.PersonRemove,
                      contentDescription = stringResource(R.string.unfriend),
                      tint = MaterialTheme.colorScheme.onBackground)
                }
          }
        }
      },
      modifier = Modifier.testTag(NavigationTestTags.TOP_BAR))
}

/**
 * The content of the profile screen.
 *
 * @param uiState The state of the screen.
 * @param onSelectTrip The callback to select a trip.
 * @param onEditPinnedTrips The callback to navigate to the edit pinned trips screen.
 * @param onEditPinnedImages The callback to navigate to the edit pinned images screen.
 * @param modifier The modifier to apply to the content.
 */
@Composable
private fun ProfileScreenContent(
    uiState: ProfileUIState,
    onSelectTrip: (String) -> Unit,
    onEditPinnedTrips: () -> Unit = {},
    onEditPinnedImages: () -> Unit = {},
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

        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.tiny_spacer)))

        BiographyDisplay(biography = uiState.biography)

        AchievementsDisplay(uiState.stats)

        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.small_spacer)))

        PinnedTrips(
            pinnedTrips = uiState.pinnedTrips,
            isOwnProfile = uiState.isOwnProfile,
            onEditPinnedTrips = onEditPinnedTrips,
            onSelectTrip = onSelectTrip)

        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.small_spacer)))

        PinnedImages(
            pinnedImages = uiState.pinnedImages,
            isOwnProfile = uiState.isOwnProfile,
            onEditPinnedImages = onEditPinnedImages)
      }
}

/**
 * The profile header section of the profile screen.
 *
 * @param photoUrl The URL of the profile picture.
 * @param name The name of the user.
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
                .testTag(ProfileScreenTestTags.PROFILE_PIC))

    Spacer(modifier = Modifier.width(dimensionResource(R.dimen.smaller_spacer)))

    Text(
        text = name,
        style = MaterialTheme.typography.headlineLarge,
        maxLines = NAME_MAX_LINES,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.testTag(ProfileScreenTestTags.DISPLAY_NAME))
  }
}

/**
 * The biography section of the profile screen.
 *
 * @param biography The biography of the user.
 */
@Composable
private fun BiographyDisplay(biography: String) {
  if (!biography.isBlank()) {
    Row(
        modifier = Modifier.fillMaxWidth().testTag(ProfileScreenTestTags.BIOGRAPHY),
        verticalAlignment = Alignment.CenterVertically) {
          Text(
              text = biography,
              style = MaterialTheme.typography.bodyMedium,
              maxLines = BIOGRAPHY_MAX_LINES,
              overflow = TextOverflow.Ellipsis)
        }
    Spacer(modifier = Modifier.height(dimensionResource(R.dimen.tiny_spacer)))
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
      modifier = Modifier.fillMaxWidth().testTag(ProfileScreenTestTags.PINNED_TRIPS_TITLE),
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
      emptyListString =
          if (isOwnProfile) stringResource(R.string.edit_no_pinned_trips)
          else stringResource(R.string.no_pinned_trips),
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
    pinnedImages: List<Uri>,
    isOwnProfile: Boolean,
    onEditPinnedImages: () -> Unit,
) {
  Row(
      modifier = Modifier.fillMaxWidth().testTag(ProfileScreenTestTags.PINNED_IMAGES_TITLE),
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
  // TODO unfinished
  LazyRow(
      modifier = Modifier.fillMaxWidth().testTag(ProfileScreenTestTags.PINNED_IMAGES_LIST),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      contentPadding = PaddingValues(horizontal = 4.dp)) {
        items(pinnedImages) { uri ->
          AsyncImage(
              model = uri,
              contentDescription = null,
              modifier = Modifier.height(120.dp).clip(RoundedCornerShape(16.dp)),
              contentScale = ContentScale.Crop)
        }
      }
}

/**
 * Dialog displayed when the user confirms removal of a friend.
 *
 * @param friendName The name of the friend.
 * @param onConfirm Invoked when user confirms removal.
 * @param onCancel Invoked when dialog is dismissed or canceled.
 */
@Composable
fun UnfriendDialog(friendName: String, onConfirm: () -> Unit, onCancel: () -> Unit) {
  AlertDialog(
      onDismissRequest = onCancel,
      title = { Text(stringResource(R.string.confirm_unfriend_title, friendName)) },
      text = { Text(stringResource(R.string.confirm_unfriend_text)) },
      confirmButton = {
        TextButton(
            onClick = onConfirm,
            modifier = Modifier.testTag(ProfileScreenTestTags.CONFIRM_UNFRIEND_BUTTON)) {
              Text(stringResource(R.string.unfriend))
            }
      },
      dismissButton = {
        TextButton(
            onClick = onCancel,
            colors =
                ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onBackground),
            modifier = Modifier.testTag(ProfileScreenTestTags.CANCEL_UNFRIEND_BUTTON)) {
              Text(stringResource(R.string.cancel))
            }
      },
      containerColor = MaterialTheme.colorScheme.onPrimary)
}
