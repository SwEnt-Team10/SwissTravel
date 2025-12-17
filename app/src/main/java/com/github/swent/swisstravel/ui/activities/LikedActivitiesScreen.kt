package com.github.swent.swisstravel.ui.activities

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.HeartBroken
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.model.trip.activity.Activity
import com.github.swent.swisstravel.ui.trip.tripinfos.TripInfoViewModelContract
import com.github.swent.swisstravel.ui.tripcreation.LoadingScreen

object LikedActivitiesScreenTestTags {
  const val SCREEN_TITLE = "liked_activities_screen_title"
  const val BACK_BUTTON = "liked_activities_back_button"
  const val EMPTY_TEXT = "liked_activities_empty_state_text"
  const val LIKED_ACTIVITIES_LIST = "liked_activities_list"
  const val SELECT_LIKED_ACTIVITY = "select_liked_activity"
  const val SCHEDULE_BUTTON = "schedule_button"
  const val UNLIKE_BUTTON = "unlike_button"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LikedActivitiesScreen(
    onBack: () -> Unit = {},
    tripInfoVM: TripInfoViewModelContract,
    onUnlike: () -> Unit = {},
    onSchedule: () -> Unit = {},
    onNext: () -> Unit = {}
) {
  val state by tripInfoVM.uiState.collectAsState()
  val likedActivities = state.likedActivities

  LaunchedEffect(state.isScheduling) {
    if (!state.isScheduling && state.savingProgress == 1f) {
      onNext()
    }
  }

  if (state.isScheduling) {
    LoadingScreen(progress = state.savingProgress)
    return
  }

  Scaffold(
      topBar = {
        TopAppBar(
            title = {
              Text(
                  stringResource(R.string.liked_activities),
                  modifier = Modifier.testTag(LikedActivitiesScreenTestTags.SCREEN_TITLE))
            },
            navigationIcon = {
              IconButton(
                  onClick = onBack,
                  modifier = Modifier.testTag(LikedActivitiesScreenTestTags.BACK_BUTTON)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null)
                  }
            })
      },
      bottomBar = {
        LikedActivitiesBottomBar(
            onSchedule = onSchedule,
            onUnlike = onUnlike,
        )
      }) { pd ->
        Box(modifier = Modifier.padding(pd).fillMaxSize()) {
          if (likedActivities.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
              Text(
                  stringResource(R.string.no_liked_activities),
                  modifier = Modifier.testTag(LikedActivitiesScreenTestTags.EMPTY_TEXT))
            }
          } else {
            LazyColumn(
                modifier =
                    Modifier.fillMaxSize()
                        .testTag(LikedActivitiesScreenTestTags.LIKED_ACTIVITIES_LIST),
                verticalArrangement =
                    Arrangement.spacedBy(dimensionResource(R.dimen.smaller_padding)),
                contentPadding = PaddingValues(dimensionResource(R.dimen.small_padding))) {
                  itemsIndexed(likedActivities) { _, activity ->
                    LikedActivityItem(activity, tripInfoVM)
                  }
                }
          }
        }
      }
}

/**
 * Composable to display a liked activity item in a list.
 *
 * @param activity The activity to display.
 * @param tripInfoVM The viewModel of the trip info, used to select, schedule or unlike activities.
 */
@Composable
fun LikedActivityItem(activity: Activity, tripInfoVM: TripInfoViewModelContract) {
  val state = tripInfoVM.uiState.collectAsState().value
  Card(
      modifier = Modifier.fillMaxWidth(),
      elevation = CardDefaults.cardElevation(dimensionResource(R.dimen.mini_padding))) {
        Row {
          Column(modifier = Modifier.weight(1f).padding(dimensionResource(R.dimen.small_padding))) {
            Text(activity.getName(), style = MaterialTheme.typography.titleMedium)
            Text(activity.description, style = MaterialTheme.typography.bodyMedium)
          }
          // each liked activity has a button to select it (to later unlike it or schedule it)
          Checkbox(
              checked = activity in state.selectedLikedActivities,
              onCheckedChange = { checked ->
                if (checked) tripInfoVM.selectLikedActivity(activity)
                else tripInfoVM.deselectLikedActivity(activity)
              },
              modifier =
                  Modifier.testTag(
                          LikedActivitiesScreenTestTags.SELECT_LIKED_ACTIVITY +
                              "_${activity.getName()}")
                      .align(Alignment.CenterVertically))
        }
      }
}

/**
 * The bottomBar composable for the LikedActivitiesScreen
 *
 * @param onSchedule A function used to schedule the selected activities
 * @param onUnlike A function used to unlike the selected activities
 */
@Composable
private fun LikedActivitiesBottomBar(onSchedule: () -> Unit = {}, onUnlike: () -> Unit = {}) {
  Row {
    // Button to schedule the selected activities
    Button(
        onClick = onSchedule,
        modifier =
            Modifier.fillMaxWidth(0.7f)
                .padding(dimensionResource(R.dimen.small_spacer))
                .testTag(LikedActivitiesScreenTestTags.SCHEDULE_BUTTON),
    ) {
      Text(text = stringResource(R.string.schedule_selected_liked_activities))
    }
    // Button to unlike the selected activities
    Button(
        onClick = onUnlike,
        modifier =
            Modifier.padding(dimensionResource(R.dimen.small_spacer))
                .testTag(LikedActivitiesScreenTestTags.UNLIKE_BUTTON)) {
          Icon(imageVector = Icons.Default.HeartBroken, contentDescription = null)
        }
  }
}
