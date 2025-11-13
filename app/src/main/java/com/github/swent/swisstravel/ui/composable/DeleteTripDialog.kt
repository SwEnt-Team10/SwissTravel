package com.github.swent.swisstravel.ui.composable

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.github.swent.swisstravel.R

object DeleteTripDialogTestTags {
  const val CONFIRM_DELETE_BUTTON = "confirmDelete"
  const val CANCEL_DELETE_BUTTON = "cancelDelete"
}

/**
 * Dialog displayed when the user confirms deletion of selected trips.
 *
 * @param count Number of selected trips.
 * @param onConfirm Invoked when user confirms deletion.
 * @param onCancel Invoked when dialog is dismissed or canceled.
 */
@Composable
fun DeleteTripsDialog(count: Int, onConfirm: () -> Unit, onCancel: () -> Unit) {
  AlertDialog(
      onDismissRequest = onCancel,
      title = { Text(pluralStringResource(R.plurals.confirm_delete_title, count, count)) },
      text = { Text(stringResource(R.string.confirm_delete_message)) },
      confirmButton = {
        TextButton(
            onClick = onConfirm,
            modifier = Modifier.testTag(DeleteTripDialogTestTags.CONFIRM_DELETE_BUTTON)) {
              Text(stringResource(R.string.delete))
            }
      },
      dismissButton = {
        TextButton(
            onClick = onCancel,
            colors =
                ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onBackground),
            modifier = Modifier.testTag(DeleteTripDialogTestTags.CANCEL_DELETE_BUTTON)) {
              Text(stringResource(R.string.cancel))
            }
      },
      containerColor = MaterialTheme.colorScheme.onPrimary)
}
