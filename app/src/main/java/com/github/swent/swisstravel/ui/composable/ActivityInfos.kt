package com.github.swent.swisstravel.ui.composable

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.model.trip.activity.Activity

@Composable
fun ActivityInfos(activity: Activity) {

  Column(modifier = Modifier.fillMaxWidth()) {
    Text(
        text = activity.getName(),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold)

    Spacer(Modifier.height(8.dp))

    Text(text = activity.description, style = MaterialTheme.typography.bodyMedium)

    Spacer(Modifier.height(12.dp))

    if (activity.imageUrls.isNotEmpty()) {
      LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(activity.imageUrls) { url ->
          Image(
              painter = rememberAsyncImagePainter(url),
              contentDescription = stringResource(R.string.activity_image_content_description),
              modifier = Modifier.size(180.dp).clip(RoundedCornerShape(8.dp)),
              contentScale = ContentScale.Crop)
        }
      }
    }

    Spacer(Modifier.height(12.dp))

    Text(
        text = "Takes around ${activity.estimatedTime()} min",
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold)
  }
}
