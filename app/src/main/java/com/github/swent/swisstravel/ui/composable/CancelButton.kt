package com.github.swent.swisstravel.ui.composable

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

/** The test tag for the cancel button. */
object CancelButtonTestTag {
  const val CANCEL_BUTTON = "cancelButton"
}

/**
 * A composable that represent a cancel button (a button that looks like a cross).
 *
 * @param onCancel the function to call when you click on the button
 * @param contentDescription the description to add to the button
 */
@Composable
fun CancelButton(onCancel: () -> Unit = {}, contentDescription: String) {
  IconButton(
      modifier = Modifier.testTag(CancelButtonTestTag.CANCEL_BUTTON), onClick = { onCancel() }) {
        Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onBackground)
      }
}
