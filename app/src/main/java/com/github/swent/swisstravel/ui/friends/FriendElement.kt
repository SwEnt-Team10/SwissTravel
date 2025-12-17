//
package com.github.swent.swisstravel.ui.friends

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.model.user.User
import com.github.swent.swisstravel.ui.composable.ProfileImage

object FriendElementTestTags {
  fun getTestTagForFriend(user: User): String = "friend${user.uid}"

  const val ARROW_ICON = "friendArrowIcon"
  const val ACCEPT_BUTTON = "friendAcceptButton"
  const val DECLINE_BUTTON = "friendDeclineButton"
  const val ADD_ICON = "friendAddIcon"
}

/** Data class to hold the state flags for FriendElement. */
data class FriendElementState(
    val isPendingRequest: Boolean = false,
    val shouldAccept: Boolean = false,
    val isAddMode: Boolean = false
)

/** Data class to hold the actions/callbacks for FriendElement. */
data class FriendElementActions(
    val onClick: () -> Unit = {},
    val onAccept: () -> Unit = {},
    val onDecline: () -> Unit = {}
)

/**
 * A single friend element.
 *
 * @param userToDisplay The user to display.
 * @param modifier The modifier to apply to the element.
 * @param state The state flags (pending, accept, addMode).
 * @param actions The callbacks (click, accept, decline).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FriendElement(
    userToDisplay: User,
    modifier: Modifier = Modifier,
    state: FriendElementState = FriendElementState(),
    actions: FriendElementActions = FriendElementActions()
) {
  Card(
      modifier =
          modifier
              .testTag(FriendElementTestTags.getTestTagForFriend(userToDisplay))
              .combinedClickable(onClick = actions.onClick)
              .fillMaxWidth()
              .height(dimensionResource(R.dimen.trip_element_height))
              .border(
                  width = dimensionResource(R.dimen.empty),
                  color = Color.Transparent,
                  shape = RoundedCornerShape(dimensionResource(R.dimen.trip_element_radius))),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onPrimary),
      shape = MaterialTheme.shapes.large) {
        Row(
            modifier =
                Modifier.fillMaxSize()
                    .padding(horizontal = dimensionResource(R.dimen.trip_element_padding)),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
              FriendNameSection(userToDisplay = userToDisplay, modifier = Modifier.weight(1f))
              FriendArrowSection(state, actions)
            }
      }
}

/**
 * A section containing the friend's name and profile picture.
 *
 * @param userToDisplay The user to display.
 * @param modifier The modifier to apply to the section.
 */
@Composable
private fun FriendNameSection(userToDisplay: User, modifier: Modifier = Modifier) {
  val maxLines = 1
  Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
    FriendCircle(profilePicUrl = userToDisplay.profilePicUrl)
    Spacer(modifier = Modifier.width(dimensionResource(R.dimen.trip_element_width)))
    Text(
        text = userToDisplay.name,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f))
  }
}

/**
 * A circle containing the friend's profile picture.
 *
 * @param profilePicUrl The URL of the friend's profile picture.
 */
@Composable
private fun FriendCircle(
    profilePicUrl: String?,
) {
  val imageUrl = profilePicUrl?.takeIf { it.isNotBlank() }

  Box(
      modifier =
          Modifier.size(dimensionResource(R.dimen.trip_top_circle_size))
              .background(MaterialTheme.colorScheme.secondary, CircleShape),
      contentAlignment = Alignment.Center) {
        ProfileImage(
            urlOrUid = imageUrl,
            modifier =
                Modifier.fillMaxSize()
                    .background(MaterialTheme.colorScheme.secondary, CircleShape)
                    .clip(CircleShape))
      }
}

/**
 * A composable containing the icons to accept or decline a friend request, the arrow if its already
 * your friend, or a plus icon if we are adding a friend.
 */
@Composable
fun FriendArrowSection(state: FriendElementState, actions: FriendElementActions) {
  if (state.isAddMode) {
    Icon(
        imageVector = Icons.Default.Add,
        contentDescription = stringResource(R.string.add_friend),
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.testTag(FriendElementTestTags.ADD_ICON))
  } else if (!state.isPendingRequest || !state.shouldAccept) {
    Icon(
        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.testTag(FriendElementTestTags.ARROW_ICON))
  } else {
    Row {
      IconButton(
          onClick = actions.onAccept,
          modifier = Modifier.testTag(FriendElementTestTags.ACCEPT_BUTTON)) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = stringResource(R.string.accept_friend),
                tint = MaterialTheme.colorScheme.primary)
          }

      Spacer(modifier = Modifier.width(dimensionResource(R.dimen.friends_spacer)))
      IconButton(
          onClick = actions.onDecline,
          modifier = Modifier.testTag(FriendElementTestTags.DECLINE_BUTTON)) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.deny_friend),
                tint = MaterialTheme.colorScheme.error)
          }
    }
  }
}
