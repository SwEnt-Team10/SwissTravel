package com.github.swent.swisstravel.ui.activities

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.model.trip.activity.Activity
import com.github.swent.swisstravel.ui.composable.ActivityInfos
import com.github.swent.swisstravel.ui.trip.tripinfos.TripInfoViewModel
import com.github.swent.swisstravel.ui.trip.tripinfos.TripInfoViewModelContract

object SwipeActivitiesScreenTestTags {
  const val SWIPE_ACTIVITIES_SCREEN = "swipe_activities_screen"
  const val LIKE_BUTTON = "swipe_activities_screen_like_button"
  const val DISLIKE_BUTTON = "swipe_activities_screen_dislike_button"
  const val BACK_BUTTON = "back_to_daily_view_button"
}

/**
 * Screen to find activities by swiping like/dislike.
 *
 * Done with the help of ChatGPT
 *
 * @param onTripInfo Callback to be called when navigating back to trip info.
 * @param viewModel The ViewModel to use.
 */
@Composable
fun SwipeActivitiesScreen(
    onTripInfo: () -> Unit = {},
    tripInfoVM: TripInfoViewModelContract = viewModel<TripInfoViewModel>(),
) {
  Scaffold(
      topBar = {
        if (tripInfoVM.uiState.collectAsState().value.currentActivity == null) {
          IconButton(
              onClick = onTripInfo,
              modifier = Modifier.testTag(SwipeActivitiesScreenTestTags.BACK_BUTTON)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground)
              }
        }
      }) { pd ->
        Box(
            modifier =
                Modifier.padding(pd)
                    .testTag(SwipeActivitiesScreenTestTags.SWIPE_ACTIVITIES_SCREEN)) {
              SwipeActivitiesStack(tripInfoVM, onTripInfo)
            }
      }
}

/**
 * Stack of cards containing the activities to swipe. This stack contains only 2 cards for
 * optimization reasons (having a card composable for each activity is not useful and would cost
 * more)
 *
 * @param viewModel The ViewModel to use.
 * @param onTripInfo Callback to be called when navigating back to trip info.
 */
@Composable
fun SwipeActivitiesStack(tripInfoVM: TripInfoViewModelContract, onTripInfo: () -> Unit) {
  val state = tripInfoVM.uiState.collectAsState().value
  val current = state.currentActivity
  val next = state.backActivity

  Box(modifier = Modifier.fillMaxSize()) {
    // Back card (next)
    // pre-loaded for fluidity when you swipe the front card
    next.let { activity ->
      if (activity == null) {
        Text(
            stringResource(R.string.no_activities_to_propose),
            modifier = Modifier.align(Alignment.Center))
      } else {
        key(activity.getName()) {
          SwipeableCard(
              activity = activity,
              onSwiped = { liked -> tripInfoVM.swipeActivity(liked) },
              onTripInfo)
        }
      }
    }

    // Current card displayed on the screen
    current.let { activity ->
      if (activity == null) {
        Text(
            stringResource(R.string.no_activities_to_propose),
            modifier = Modifier.align(Alignment.Center))
      } else {
        key(activity.getName()) {
          SwipeableCard(
              activity = activity,
              onSwiped = { liked -> tripInfoVM.swipeActivity(liked) },
              onTripInfo = onTripInfo)
        }
      }
    }
  }
}

/**
 * A swipeable card that allows users to like or dislike an activity.
 *
 * When swiped, the card will both move horizontally and rotate on itself, to give the effect that
 * it rotates away.
 *
 * Done with the help of ChatGPT
 *
 * @param activity The activity to display.
 * @param onSwiped Callback to be called when the activity is swiped.
 */
@Composable
fun SwipeableCard(activity: Activity, onSwiped: (liked: Boolean) -> Unit, onTripInfo: () -> Unit) {
  val swipeState = remember(activity) { mutableStateOf(SwipeState.Idle) }

  // Parameter for rotational movement
  val rotation =
      animateFloatAsState(
          targetValue =
              when (swipeState.value) {
                // rotate clockwise
                SwipeState.Like -> 30f
                // rotate counter-clockwise
                SwipeState.Dislike -> -30f
                else -> 0f
              },
          // duration of the animation
          animationSpec = tween(300))

  // Parameter for horizontal movement
  val offsetX =
      animateDpAsState(
          targetValue =
              when (swipeState.value) {
                // move right
                SwipeState.Like -> 600.dp
                // move left
                SwipeState.Dislike -> (-600).dp
                else -> 0.dp
              },
          // duration of the animation
          animationSpec = tween(300),
          finishedListener = {
            if (swipeState.value != SwipeState.Idle) {
              onSwiped(swipeState.value == SwipeState.Like)
            }
          })

  // Apply rotation and offset to the card
  Box(modifier = Modifier.offset(x = offsetX.value).graphicsLayer { rotationZ = rotation.value }) {
    ActivityInfos(activity = activity, onBack = { onTripInfo() })
  }

  // Like and dislike actions
  Box(
      modifier = Modifier.fillMaxSize().padding(dimensionResource(R.dimen.small_padding)),
      contentAlignment = Alignment.BottomCenter) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(dimensionResource(R.dimen.small_padding)),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
              Button(
                  onClick = { swipeState.value = SwipeState.Dislike },
                  modifier = Modifier.testTag(SwipeActivitiesScreenTestTags.DISLIKE_BUTTON)) {
                    Text(stringResource(R.string.dislike_button))
                  }
              Button(
                  onClick = { swipeState.value = SwipeState.Like },
                  modifier = Modifier.testTag(SwipeActivitiesScreenTestTags.LIKE_BUTTON)) {
                    Text(stringResource(R.string.like_button))
                  }
            }
      }
}

enum class SwipeState {
  Idle,
  Like,
  Dislike
}
