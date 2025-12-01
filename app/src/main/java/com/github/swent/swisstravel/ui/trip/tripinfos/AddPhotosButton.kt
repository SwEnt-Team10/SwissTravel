package com.github.swent.swisstravel.ui.trip.tripinfos

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.github.swent.swisstravel.R

/** Displays a button to go on a screen to add photos */
@Composable
fun AddPhotosButton(onAddPhotos: () -> Unit) {
  IconButton(onClick = onAddPhotos) {
    Icon(
        imageVector = Icons.Outlined.AddPhotoAlternate,
        contentDescription = stringResource(R.string.add_photo))
  }
}
