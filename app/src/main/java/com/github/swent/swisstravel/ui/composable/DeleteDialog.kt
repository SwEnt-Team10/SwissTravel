package com.github.swent.swisstravel.ui.composable

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.github.swent.swisstravel.R

object DeleteDialogTestTags {
  const val CONFIRM_DELETE_BUTTON = "confirmDelete"
  const val CANCEL_DELETE_BUTTON = "cancelDelete"
}

/**
 * Dialog displayed when the user confirms deletion of selected items.
 *
 * @param onConfirm Invoked when user confirms deletion.
 * @param onCancel Invoked when dialog is dismissed or canceled.
 * @param title The title of the dialog.
 * @param text The text of the dialog. "This action is irreversible" by default.
 */
@Composable
fun DeleteDialog(
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    title: String,
    text: String = stringResource(R.string.confirm_delete_message)
) {
  AlertDialog(
      onDismissRequest = onCancel,
      title = { Text(title) },
      text = { Text(text) },
      confirmButton = {
        TextButton(
            onClick = onConfirm,
            modifier = Modifier.testTag(DeleteDialogTestTags.CONFIRM_DELETE_BUTTON)) {
              Text(stringResource(R.string.delete))
            }
      },
      dismissButton = {
        TextButton(
            onClick = onCancel,
            colors =
                ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onBackground),
            modifier = Modifier.testTag(DeleteDialogTestTags.CANCEL_DELETE_BUTTON)) {
              Text(stringResource(R.string.cancel))
            }
      },
      containerColor = MaterialTheme.colorScheme.onPrimary)
}
