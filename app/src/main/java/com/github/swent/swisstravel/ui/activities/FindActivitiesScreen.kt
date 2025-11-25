package com.github.swent.swisstravel.ui.activities

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.swent.swisstravel.model.trip.Location
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
fun FindActivitiesScreen(
    onTripInfo: () -> Unit,
    tripLocations: List<Location>,
    tripInfoViewModel: TripInfoViewModel = viewModel(),
) {
  val viewModel = FindActivitiesViewModel(tripInfoViewModel)
  val current = viewModel.currentActivity
  Scaffold { pd ->
    Box(modifier = Modifier.padding(pd)) {
      Button(onClick = { onTripInfo() }) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
      }

      // If nothing loaded yet
      if (current.value == null) {
        CircularProgressIndicator(Modifier.align(Alignment.Center))
      } else {
        Box(modifier = Modifier.fillMaxSize()) {

          // Current activity
          SwipeableCard(
              activity = current.value!!,
              onSwiped = { liked ->
                {
                  if (liked) {
                    viewModel.likeCurrentActivity()
                  } else {
                    viewModel.dislikeCurrentActivity()
                  }
                  viewModel.loadNextActivity(tripLocations)
                }
              })
        }
      }
    }
  }
}

/**
 * A card displaying information about an activity.
 *
 * Done with the help of ChatGPT
 *
 * @param activity The activity to display.
 * @param modifier The modifier to be applied to the card.
 */
@Composable
fun ActivityCard(activity: Activity, modifier: Modifier = Modifier) {
  Card(
      modifier = modifier.fillMaxSize(),
  ) {
    ActivityInfos(activity = activity)
  }
}

/**
 * A swipeable card that allows users to like or dislike an activity.
 *
 * Done with the help of ChatGPT
 *
 * @param activity The activity to display.
 * @param onSwiped Callback to be called when the activity is swiped.
 */
@Composable
fun SwipeableCard(activity: Activity, onSwiped: (liked: Boolean) -> Unit) {
  var swipeState = remember { mutableStateOf(SwipeState.Idle) }

  val rotation =
      animateFloatAsState(
          targetValue =
              when (swipeState.value) {
                SwipeState.Like -> 20f
                SwipeState.Dislike -> -20f
                else -> 0f
              },
          animationSpec = tween(300))

  val offsetX =
      animateDpAsState(
          targetValue =
              when (swipeState.value) {
                SwipeState.Like -> 300.dp
                SwipeState.Dislike -> (-300).dp
                else -> 0.dp
              },
          animationSpec = tween(300),
          finishedListener = {
            if (swipeState.value != SwipeState.Idle) {
              onSwiped(swipeState.value == SwipeState.Like)
              swipeState.value = SwipeState.Idle
            }
          })

  Box(modifier = Modifier.offset(x = offsetX.value).graphicsLayer { rotationZ = rotation.value }) {
    ActivityCard(activity)
  }

  // Like & Dislike actions
  Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
    Button(onClick = { swipeState.value = SwipeState.Dislike }) { Text("Dislike") }
    Button(onClick = { swipeState.value = SwipeState.Like }) { Text("Like") }
  }
}

enum class SwipeState {
  Idle,
  Like,
  Dislike
}
