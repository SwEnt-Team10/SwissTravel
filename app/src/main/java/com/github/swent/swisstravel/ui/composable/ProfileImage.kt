package com.github.swent.swisstravel.ui.composable

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.model.image.ImageHelper
import com.github.swent.swisstravel.model.image.ImageRepository
import com.github.swent.swisstravel.model.image.ImageRepositoryFirebase

/**
 * A reusable component that displays a profile picture. AI helped for this composable.
 *
 * It automatically handles the logic to distinguish between:
 * 1. A Web URL (http/https) -> Loaded via Coil.
 * 2. A Firestore UID -> Fetched via ImageRepository.
 * 3. Empty/Null -> Displays default placeholder.
 *
 * @param urlOrUid The URL or Firestore UID of the profile picture.
 * @param modifier Modifier for styling.
 * @param imageRepository The repository for fetching images. (Default is [ImageRepositoryFirebase])
 */
@Composable
fun ProfileImage(
    urlOrUid: String?,
    modifier: Modifier = Modifier,
    imageRepository: ImageRepository = ImageRepositoryFirebase()
) {
  val context = LocalContext.current

  // Check if it looks like a URL
  val isUrl =
      remember(urlOrUid) {
        urlOrUid?.startsWith("http:") == true || urlOrUid?.startsWith("https:") == true
      }

  Box(
      modifier = modifier.background(MaterialTheme.colorScheme.surface),
      contentAlignment = Alignment.Center) {
        if (urlOrUid.isNullOrBlank()) {
          // Case 1: No Image -> Default Placeholder
          PlaceholderProfileImage(modifier)
        } else if (isUrl) {
          // Case 2: Web URL -> Use Coil
          AsyncImage(
              model = ImageRequest.Builder(context).data(urlOrUid).crossfade(true).build(),
              contentDescription = stringResource(R.string.profile_pic_desc),
              placeholder = painterResource(R.drawable.default_profile_pic),
              error = painterResource(R.drawable.default_profile_pic),
              contentScale = ContentScale.Crop,
              modifier = modifier)
        } else {
          // Case 3: Firestore UID -> Fetch Manually
          var bitmap by remember(urlOrUid) { mutableStateOf<Bitmap?>(null) }

          LaunchedEffect(urlOrUid) {
            try {
              val imageObj = imageRepository.getImage(urlOrUid)
              bitmap = ImageHelper.base64ToBitmap(imageObj.base64)
            } catch (e: Exception) {
              // If fetch fails, bitmap stays null, placeholder is shown
              Log.e("ProfileImage", "Fetching profile picture from firestore failed.", e)
            }
          }

          if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = stringResource(R.string.profile_pic_desc),
                contentScale = ContentScale.Crop,
                modifier = modifier)
          } else {
            // Show placeholder while loading or on error
            PlaceholderProfileImage(modifier)
          }
        }
      }
}

/**
 * A composable that displays a placeholder profile image.
 *
 * @param modifier Modifier for styling.
 */
@Composable
private fun PlaceholderProfileImage(modifier: Modifier) {
  Image(
      painter = painterResource(id = R.drawable.default_profile_pic),
      contentDescription = stringResource(R.string.profile_pic_desc),
      contentScale = ContentScale.Crop,
      modifier = modifier)
}
