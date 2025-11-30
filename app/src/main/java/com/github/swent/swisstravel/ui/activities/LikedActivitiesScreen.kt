package com.github.swent.swisstravel.ui.activities

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.model.trip.activity.Activity
import com.github.swent.swisstravel.ui.trip.tripinfos.TripInfoViewModelContract

object LikedActivitiesScreenTestTags {
  const val SCREEN_TITLE = "liked_activities_screen_title"
  const val BACK_BUTTON = "liked_activities_back_button"
  const val EMPTY_TEXT = "liked_activities_empty_state_text"
  const val LIKED_ACTIVITIES_LIST = "liked_activities_list"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LikedActivitiesScreen(onBack: () -> Unit = {}, tripInfoViewModel: TripInfoViewModelContract) {
  val uiState = tripInfoViewModel.uiState.collectAsState()
  val likedActivities = uiState.value.likedActivities

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
                  itemsIndexed(likedActivities) { idx, activity -> LikedActivityItem(activity) }
                }
          }
        }
      }
}

/**
 * Composable to display a liked activity item in a list.
 *
 * @param activity The activity to display.
 */
@Composable
fun LikedActivityItem(activity: Activity) {
  Card(
      modifier = Modifier.fillMaxWidth(),
      elevation = CardDefaults.cardElevation(dimensionResource(R.dimen.mini_padding))) {
        Column(modifier = Modifier.padding(dimensionResource(R.dimen.small_padding))) {
          Text(activity.getName(), style = MaterialTheme.typography.titleMedium)
          Text(activity.description, style = MaterialTheme.typography.bodyMedium)
        }
      }
}
