package com.github.swent.swisstravel.ui.composable

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontWeight
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.model.trip.activity.Activity
import com.github.swent.swisstravel.model.trip.activity.WikiImageRepository
import kotlin.collections.emptyList

object ActivityInfosTestTag {
  const val TOP_BAR = "topBar"
  const val BACK_BUTTON = "backButton"
  const val TITLE = "title"
  const val DESCRIPTION = "description"
  const val ESTIMATED_TIME = "estimatedTime"
  const val IMAGES = "imagesList"
  const val TIP = "tip"
}

/**
 * Activity infos screen.
 *
 * @param activity The activity to display.
 * @param onBack Callback to be called when the back button is pressed.
 * @param wikiRepo The wiki image repository to use.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityInfos(
    activity: Activity,
    onBack: () -> Unit = {},
    wikiRepo: WikiImageRepository = WikiImageRepository.default()
) {
  Scaffold(
      topBar = {
        TopAppBar(
            title = {
              Text(activity.getName(), modifier = Modifier.testTag(ActivityInfosTestTag.TITLE))
            },
            modifier = Modifier.testTag(ActivityInfosTestTag.TOP_BAR),
            navigationIcon = {
              IconButton(
                  onClick = onBack, modifier = Modifier.testTag(ActivityInfosTestTag.BACK_BUTTON)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground)
                  }
            })
      }) { pd ->
        val scrollState = rememberScrollState()
        val minutes = activity.estimatedTime()
        val hours = minutes / 60
        val remainingMinutes = minutes % 60

        val durationText =
            when {
              hours > 0 && remainingMinutes > 0 -> "${hours}h $remainingMinutes min"
              hours > 0 -> "${hours}h"
              else -> "$minutes min"
            }

        Column(
            modifier =
                Modifier.fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(pd)
                    .padding(
                        horizontal = dimensionResource(R.dimen.activity_info_main_col_padding)),
            verticalArrangement =
                Arrangement.spacedBy(
                    space = dimensionResource(R.dimen.activity_info_main_col_padding))) {

              // Description
              if (activity.description.isNotBlank()) {
                Column(
                    verticalArrangement =
                        Arrangement.spacedBy(dimensionResource(R.dimen.activity_info_desc_padding)),
                    modifier = Modifier.testTag(ActivityInfosTestTag.DESCRIPTION)) {
                      Text(
                          text = stringResource(R.string.about_activity),
                          style = MaterialTheme.typography.titleMedium,
                          fontWeight = FontWeight.SemiBold)
                      Text(
                          text = activity.description,
                          style = MaterialTheme.typography.bodyMedium,
                      )
                    }
              }

              AssistChip(
                  onClick = {},
                  modifier = Modifier.testTag(ActivityInfosTestTag.ESTIMATED_TIME),
                  leadingIcon = {
                    Icon(imageVector = Icons.Filled.AccessTime, contentDescription = null)
                  },
                  label = { Text(stringResource(R.string.estimated_time, durationText)) })

              ActivityImagesSection(activityName = activity.getName(), wikiRepo = wikiRepo)

              Spacer(Modifier.height(dimensionResource(R.dimen.activity_info_images_padding)))

              Box(
                  modifier =
                      Modifier.fillMaxWidth()
                          .padding(dimensionResource(R.dimen.activity_info_images_padding)),
                  contentAlignment = Alignment.CenterStart) {
                    Text(
                        text = stringResource(R.string.activity_tip),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag(ActivityInfosTestTag.TIP))
                  }
            }
      }
}

/**
 * Activity images section
 *
 * @param activityName The activity name
 * @param wikiRepo The wiki image repository
 * @param modifier Modifier to apply
 */
@Composable
private fun ActivityImagesSection(
    activityName: String,
    wikiRepo: WikiImageRepository,
    modifier: Modifier = Modifier
) {
  val context = LocalContext.current

  var activityUrls by remember { mutableStateOf<List<String>>(emptyList()) }
  var isLoading by remember { mutableStateOf(false) }

  LaunchedEffect(activityName) {
    isLoading = true
    activityUrls =
        try {
          wikiRepo.getImagesByName(activityName)
        } catch (_: Exception) {
          emptyList()
        } finally {
          isLoading = false
        }
  }

  when {
    isLoading -> {
      CircularProgressIndicator()
    }
    activityUrls.isNotEmpty() -> {
      Column(
          verticalArrangement =
              Arrangement.spacedBy(dimensionResource(R.dimen.activity_info_images_padding)),
          modifier = modifier.testTag(ActivityInfosTestTag.IMAGES)) {
            Text(
                text = "Images",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold)

            LazyRow(
                horizontalArrangement =
                    Arrangement.spacedBy(
                        dimensionResource(R.dimen.activity_info_images_horizontal_padding))) {
                  items(activityUrls) { url ->
                    Card(
                        shape =
                            RoundedCornerShape(
                                dimensionResource(R.dimen.activity_info_main_col_padding)),
                        modifier =
                            Modifier.width(dimensionResource(R.dimen.activity_info_images_width))
                                .height(dimensionResource(R.dimen.activity_info_images_height))) {
                          AsyncImage(
                              model =
                                  ImageRequest.Builder(context)
                                      .data(url)
                                      .addHeader(
                                          "User-Agent",
                                          "SwissTravelApp/1.0 (swisstravel.epfl@proton.me)")
                                      .crossfade(true)
                                      .build(),
                              contentDescription = null,
                              modifier =
                                  Modifier.fillMaxSize()
                                      .clip(
                                          RoundedCornerShape(
                                              size =
                                                  dimensionResource(
                                                      R.dimen.activity_info_main_col_padding))),
                              contentScale = ContentScale.Crop,
                          )
                        }
                  }
                }
          }
    }
    else -> {
      Text(
          text = stringResource(R.string.no_image_fallback),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
  }
}
