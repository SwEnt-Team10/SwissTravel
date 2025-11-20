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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.github.swent.swisstravel.model.trip.activity.Activity

object ActivityInfosTestTag {
  const val TOP_BAR = "topBar"
  const val BACK_BUTTON = "backButton"
  const val TITLE = "title"
  const val DESCRIPTION = "description"
  const val ESTIMATED_TIME = "estimatedTime"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityInfos(
    activity: Activity,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
  val context = LocalContext.current

  Scaffold(
      topBar = {
        TopAppBar(
            title = { Text(activity.getName()) },
            navigationIcon = {
              IconButton(onClick = onBack, modifier = modifier) {
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
            modifier = modifier.fillMaxSize().verticalScroll(scrollState).padding(pd),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {

              // Description
              if (activity.description.isNotBlank()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                  Text(
                      text = "About this activity",
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
                  leadingIcon = {
                    Icon(imageVector = Icons.Filled.AccessTime, contentDescription = null)
                  },
                  label = { Text("Estimated time: $durationText") })

              if (activity.imageUrls.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                  Text(
                      text = "Images",
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.SemiBold)

                  LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(activity.imageUrls) { url ->
                      Card(
                          shape = RoundedCornerShape(16.dp),
                          modifier = Modifier.width(220.dp).height(150.dp)) {
                            AsyncImage(
                                model =
                                    ImageRequest.Builder(context).data(url).crossfade(true).build(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)),
                                contentScale = ContentScale.Crop,
                            )
                          }
                    }
                  }
                }
              } else {
                Text(
                    text = "No images available for this activity.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
              }

              Spacer(Modifier.height(8.dp))

              Box(
                  modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                  contentAlignment = Alignment.CenterStart) {
                    Text(
                        text = "Tip: Check weather & opening hours before going.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                  }
            }
      }
}
