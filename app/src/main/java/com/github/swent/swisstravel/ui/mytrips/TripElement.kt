package com.github.swent.swisstravel.ui.mytrips

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.model.trip.Trip

object TripElementTestTags {
  fun getTestTagForTrip(trip: Trip): String = "trip${trip.uid}"
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TripElement(
    trip: Trip,
    onClick: () -> Unit,
    onLongPress: () -> Unit = {},
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false
) {
  Card(
      modifier =
          Modifier.testTag(TripElementTestTags.getTestTagForTrip(trip))
              .combinedClickable(onClick = onClick, onLongClick = onLongPress)
              .fillMaxWidth()
              .height(56.dp)
              .border(
                  width = if (isSelected) 3.dp else 0.dp,
                  color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                  shape = RoundedCornerShape(16.dp)),
      colors =
          CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
      shape = MaterialTheme.shapes.large) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier =
                        Modifier.size(40.dp)
                            .background(MaterialTheme.colorScheme.secondary, CircleShape),
                    contentAlignment = Alignment.Center) {
                      Text(
                          trip.name.first().toString(),
                          color = MaterialTheme.colorScheme.onSecondary)
                    }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = trip.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
              if (isSelectionMode) {
                Checkbox(checked = isSelected, onCheckedChange = null)
              } else {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowRight,
                    contentDescription = stringResource(R.string.go_trip_details),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            }
      }
}
