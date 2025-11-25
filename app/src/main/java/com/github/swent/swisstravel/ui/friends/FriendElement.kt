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
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.model.user.User

object FriendElementTestTags {
  fun getTestTagForFriend(user: User): String = "friend${user.uid}"
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FriendElement(userToDisplay: User, onClick: () -> Unit, modifier: Modifier = Modifier) {
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
              FriendNameSection(userToDisplay = userToDisplay)
              FriendArrowSection()
            }
      }
}

@Composable
private fun FriendNameSection(userToDisplay: User) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    FriendCircle(profilePicUrl = userToDisplay.profilePicUrl)
    Spacer(modifier = Modifier.width(dimensionResource(R.dimen.trip_element_width)))
    Box(modifier = Modifier.fillMaxWidth(0.9f)) {
      Text(
          text = userToDisplay.name,
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis)
    }
  }
}

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

@Composable
private fun FriendArrowSection() {
  Icon(
      imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.onSurfaceVariant)
}
