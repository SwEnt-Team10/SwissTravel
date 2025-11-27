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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.swent.swisstravel.model.trip.activity.Activity
import com.github.swent.swisstravel.ui.composable.ActivityInfos
import com.github.swent.swisstravel.ui.trip.tripinfos.TripInfoViewModel

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
    onTripInfo: () -> Unit,
    tripInfoViewModel: TripInfoViewModel,
) {
  val viewModel = remember { SwipeActivitiesViewModel(tripInfoViewModel) }
  Scaffold { pd ->
    Box(modifier = Modifier.padding(pd)) {
      // Back button, with zIndex = 1f, for it to be on top of the activity cards
      Button(onClick = { onTripInfo() }, modifier = Modifier.zIndex(1f)) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
      }

      SwipeActivitiesStack(viewModel)
    }
  }
}

@Composable
fun SwipeActivitiesStack(viewModel: SwipeActivitiesViewModel) {
  val current = viewModel.uiState.collectAsState().value.currentActivity
  val next = viewModel.uiState.collectAsState().value.backActivity

  Box(modifier = Modifier.fillMaxSize()) {
    // Back card (next)
    next.let { activity ->
      if (activity == null) {
        Text("No more activities to show!", modifier = Modifier.align(Alignment.Center))
      } else {
        key(activity.getName()) {
          SwipeableCard(activity = activity, onSwiped = { liked -> viewModel.swipeActivity(liked) })
        }
      }
    }

    // Front card (current)
    current.let { activity ->
      if (activity == null) {
        Text("No more activities to show!", modifier = Modifier.align(Alignment.Center))
      } else {
        key(activity.getName()) {
          SwipeableCard(activity = activity, onSwiped = { liked -> viewModel.swipeActivity(liked) })
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
fun SwipeableCard(activity: Activity, onSwiped: (liked: Boolean) -> Unit) {
  val swipeState = remember(activity) { mutableStateOf(SwipeState.Idle) }

  // Parameter for rotational movement
  val rotation =
      animateFloatAsState(
          targetValue =
              when (swipeState.value) {
                // rotate clockwise
                SwipeState.Like -> 20f
                // rotate counter-clockwise
                SwipeState.Dislike -> -20f
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
                SwipeState.Like -> 400.dp
                // move left
                SwipeState.Dislike -> (-400).dp
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
    ActivityInfos(activity = activity)
  }

  // Like and dislike actions
  Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.BottomCenter) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
          Button(onClick = { swipeState.value = SwipeState.Dislike }) { Text("Dislike") }
          Button(onClick = { swipeState.value = SwipeState.Like }) { Text("Like") }
        }
  }
}

enum class SwipeState {
  Idle,
  Like,
  Dislike
}
