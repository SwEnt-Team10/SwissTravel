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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.model.user.User

object FriendElementTestTags {
  fun getTestTagForFriend(user: User): String = "friend${user.uid}"
}

/**
 * A single friend element.
 *
 * @param userToDisplay The user to display.
 * @param onClick The function to call when the element is clicked.
 * @param modifier The modifier to apply to the element.
 * @param isPendingRequest Whether the request is pending.
 * @param shouldAccept Whether the request should be accepted.
 * @param onAccept The function to call when the request is accepted.
 * @param onDecline The function to call when the request is declined.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FriendElement(
    userToDisplay: User,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPendingRequest: Boolean = false,
    shouldAccept: Boolean = false,
    onAccept: () -> Unit = {},
    onDecline: () -> Unit = {},
) {
  Card(
      modifier =
          modifier
              .testTag(FriendElementTestTags.getTestTagForFriend(userToDisplay))
              .combinedClickable(onClick = onClick)
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
              FriendArrowSection(isPendingRequest, shouldAccept, onAccept, onDecline)
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
  Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
    FriendCircle(profilePicUrl = userToDisplay.profilePicUrl)
    Spacer(modifier = Modifier.width(dimensionResource(R.dimen.trip_element_width)))
    Text(
        text = userToDisplay.name,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
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
        AsyncImage(
            model = imageUrl ?: R.drawable.default_profile_pic,
            contentDescription = null,
            modifier =
                Modifier.fillMaxSize()
                    .background(MaterialTheme.colorScheme.secondary, CircleShape)
                    .clip(CircleShape),
            contentScale = ContentScale.Crop)
      }
}

/**
 * A composable containing the icons to accept or decline a friend request or the arrow its already
 * your friend.
 *
 * @param isPendingRequest Whether the request is pending.
 * @param shouldAccept Whether the request should be accepted.
 */
@Composable
fun FriendArrowSection(
    isPendingRequest: Boolean,
    shouldAccept: Boolean,
    onAccept: () -> Unit = {},
    onDecline: () -> Unit = {},
) {
  if (!isPendingRequest || !shouldAccept) {
    Icon(
        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant)
  } else {
    Row {
      IconButton(onClick = onAccept) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = stringResource(R.string.accept_friend),
            tint = MaterialTheme.colorScheme.primary)
      }

      Spacer(modifier = Modifier.width(20.dp))
      IconButton(onClick = onDecline) {
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = stringResource(R.string.deny_friend),
            tint = MaterialTheme.colorScheme.error)
      }
    }
  }
}
