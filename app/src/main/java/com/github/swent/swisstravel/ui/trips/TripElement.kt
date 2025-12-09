package com.github.swent.swisstravel.ui.trips

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
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
 * - Normal mode: Shows an arrow icon and supports click navigation.
 * - Selection mode: Replaces the arrow with a checkbox and allows multi-selection.
 * - No Icon mode: Shows a check icon instead of an arrow.
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
    noIcon: Boolean = false,
    collaborators: List<TripsViewModel.CollaboratorUi> = emptyList()
) {
  Card(
      modifier =
          Modifier.testTag(TripElementTestTags.getTestTagForTrip(trip))
              .combinedClickable(onClick = onClick, onLongClick = onLongPress)
              .fillMaxWidth()
              .height(dimensionResource(R.dimen.trip_element_height))
              .border(
                  width =
                      if (isSelected) dimensionResource(R.dimen.trip_element_border)
                      else dimensionResource(R.dimen.empty),
                  color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                  shape = RoundedCornerShape(dimensionResource(R.dimen.trip_element_radius))),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onPrimary),
      shape = MaterialTheme.shapes.large) {
        Row(
            modifier =
                Modifier.fillMaxSize()
                    .padding(horizontal = dimensionResource(R.dimen.trip_element_padding)),
            verticalAlignment = Alignment.CenterVertically) {
              TripNameSection(trip = trip, modifier = Modifier.weight(1f))

              if (collaborators.isNotEmpty()) {
                CollaboratorsPreview(collaborators)
                Spacer(
                    modifier =
                        Modifier.width(
                            dimensionResource(R.dimen.trip_element_collaborators_padding)))
              }

              Spacer(modifier = Modifier.width(dimensionResource(R.dimen.trip_element_width)))

              TripStatusSection(
                  trip = trip,
                  isSelected = isSelected,
                  isSelectionMode = isSelectionMode,
                  noIcon = noIcon)
            }
      }
}

/**
 * Displays the trip name with an icon.
 *
 * @param trip The [Trip] displayed in this element.
 */
@Composable
private fun TripNameSection(trip: Trip, modifier: Modifier) {
  Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
    TripCircle(tripName = trip.name)
    Spacer(modifier = Modifier.width(dimensionResource(R.dimen.trip_element_width)))
    Text(
        text = trip.name,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis)
  }
}

@Composable
private fun CollaboratorsPreview(collaborators: List<TripsViewModel.CollaboratorUi>) {
  Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement =
          Arrangement.spacedBy(
              dimensionResource(R.dimen.trip_element_collaborators_overlap)) // Overlap effect
      ) {
        collaborators.take(3).forEachIndexed { index, user ->
          AsyncImage(
              model = user.avatarUrl.ifBlank { R.drawable.default_profile_pic },
              contentDescription = user.displayName,
              contentScale = ContentScale.Crop,
              modifier =
                  Modifier.size(dimensionResource(R.dimen.trip_element_collaborators_avatar_size))
                      .clip(CircleShape)
                      .border(
                          dimensionResource(R.dimen.trip_element_collaborators_border_size),
                          MaterialTheme.colorScheme.surface,
                          CircleShape)
                      .zIndex((3 - index).toFloat()) // First one on top
              )
        }
        if (collaborators.size > 3) {
          Box(
              modifier =
                  Modifier.size(dimensionResource(R.dimen.trip_element_collaborators_avatar_size))
                      .clip(CircleShape)
                      .background(MaterialTheme.colorScheme.secondaryContainer)
                      .border(
                          dimensionResource(R.dimen.trip_element_collaborators_border_size),
                          MaterialTheme.colorScheme.surface,
                          CircleShape)
                      .zIndex(0f),
              contentAlignment = Alignment.Center) {
                Text(
                    text = "+${collaborators.size - 3}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer)
              }
        }
      }
}

/**
 * Displays a circle with the first letter of the trip name.
 *
 * @param tripName The name of the trip to display.
 */
@Composable
private fun TripCircle(tripName: String) {
  Box(
      modifier =
          Modifier.size(dimensionResource(R.dimen.trip_top_circle_size))
              .background(MaterialTheme.colorScheme.secondary, CircleShape),
      contentAlignment = Alignment.Center) {
        Text(tripName.first().toString(), color = MaterialTheme.colorScheme.onSecondary)
      }
}

/**
 * Displays the trip status icon.
 *
 * @param trip The [Trip] displayed in this element.
 * @param isSelected Whether the trip is currently selected.
 * @param isSelectionMode Whether the UI is currently in selection mode.
 * @param noIcon If true, no icon is displayed when isSelected is false. Otherwise, shows a check
 *   icon.
 */
@Composable
private fun TripStatusSection(
    trip: Trip,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    noIcon: Boolean
) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    if (trip.isFavorite) {
      Icon(
          Icons.Default.Star,
          contentDescription = stringResource(R.string.favorite_icon),
          tint = favoriteIcon)
      Spacer(modifier = Modifier.width(dimensionResource(R.dimen.trip_element_width)))
    }

    when {
      isSelectionMode ->
          Checkbox(
              checked = isSelected,
              onCheckedChange = null,
              modifier =
                  Modifier.testTag(TripElementTestTags.getTestTagForTripCheckbox(trip)).semantics {
                    toggleableState = if (isSelected) ToggleableState.On else ToggleableState.Off
                    role = Role.Checkbox
                  })
      noIcon && isSelected ->
          Icon(
              imageVector = Icons.Filled.Check,
              contentDescription = stringResource(R.string.current_trip_details),
              tint = MaterialTheme.colorScheme.primary)
      !noIcon ->
          Icon(
              imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
              contentDescription = stringResource(R.string.go_trip_details),
              tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
  }
}
