package com.github.swent.swisstravel.ui.composable

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.github.swent.swisstravel.ui.trip.tripinfos.photos.EditPhotosScreenTestTags

object CancelButtonTestTag {
    const val CANCEL_BUTTON = "cancelButton"
}

@Composable
fun CancelButton(
    onCancel: () -> Unit = {},
    contentDescription: String
) {
    IconButton(
        modifier = Modifier.testTag(CancelButtonTestTag.CANCEL_BUTTON),
        onClick = { onCancel() }) {
        Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onBackground)
    }
}