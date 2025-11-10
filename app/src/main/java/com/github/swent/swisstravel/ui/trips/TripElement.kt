package com.github.swent.swisstravel.ui.trips

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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.model.trip.Trip
import com.github.swent.swisstravel.ui.theme.favoriteIcon

/**
 * Contains helper methods for generating unique test tags used in [TripElement].
 *
 * These tags are used in UI tests to find and assert elements like trip cards or checkboxes.
 */
object TripElementTestTags {

  /** Returns a unique test tag for the given [trip] card. */
  fun getTestTagForTrip(trip: Trip): String = "trip${trip.uid}"

  /** Returns a unique test tag for the checkbox of the given [trip]. */
  fun getTestTagForTripCheckbox(trip: Trip): String = "checkbox${trip.uid}"
}

/**
 * A composable representing a single trip item in the "My Trips" list.
 *
 * Displays the trip name with an icon and supports both normal and selection modes:
 * - **Normal mode:** Shows an arrow icon and supports click navigation.
 * - **Selection mode:** Replaces the arrow with a checkbox and allows multi-selection.
 *
 * @param trip The [Trip] displayed in this element.
 * @param onClick Called when the element is tapped.
 * @param onLongPress Called when the element is long-pressed (e.g., to enter selection mode).
 * @param isSelected Whether the trip is currently selected.
 * @param isSelectionMode Whether the UI is currently in selection mode.
 * @param noIcon If true, no icon is displayed when isSelected is false. Otherwise, shows a check
 *   icon.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TripElement(
    trip: Trip,
    onClick: () -> Unit,
    onLongPress: () -> Unit = {},
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    noIcon: Boolean = false
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
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onPrimary),
      shape = MaterialTheme.shapes.large) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                // Circle with the first letter of the trip name
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
                    color = MaterialTheme.colorScheme.onSurface)
              }
              Row(verticalAlignment = Alignment.CenterVertically) {
                if (trip.isFavorite) {
                  // Favorite Icon
                  Icon(
                      Icons.Default.Star,
                      contentDescription = stringResource(R.string.favorite_icon),
                      tint = favoriteIcon)
                  Spacer(modifier = Modifier.width(16.dp))
                }
                if (isSelectionMode) {
                  // Checkbox shown only during selection mode
                  Checkbox(
                      checked = isSelected,
                      onCheckedChange = null, // Managed externally via ViewModel
                      modifier =
                          Modifier.testTag(TripElementTestTags.getTestTagForTripCheckbox(trip))
                              .semantics {
                                // Add semantics so UI tests (e.g., assertIsOn/assertIsOff) can
                                // detect
                                // checked state
                                toggleableState =
                                    if (isSelected) ToggleableState.On else ToggleableState.Off
                                role = Role.Checkbox
                              })
                }
                if (noIcon) {
                  if (isSelected) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = stringResource(R.string.current_trip_details),
                        tint = MaterialTheme.colorScheme.primary)
                  }
                  /* Else display nothing */
                } else {
                  // Arrow icon for normal (non-selection) mode
                  Icon(
                      Icons.AutoMirrored.Filled.KeyboardArrowRight,
                      contentDescription = stringResource(R.string.go_trip_details),
                      tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
              }
            }
      }
}
