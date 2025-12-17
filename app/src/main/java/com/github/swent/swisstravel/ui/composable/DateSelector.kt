package com.github.swent.swisstravel.ui.composable

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import com.github.swent.swisstravel.R

/** Test tags for UI tests to identify components. */
object DateSelectorTestTags {
  const val DATE_SELECTOR = "dateSelector"
  const val DATE = "date"
}

/**
 * A reusable component for a date selector with a label.
 *
 * @param label The text label for the date selector.
 * @param dateText The text to display the selected date.
 * @param onClick Callback to be invoked when the date selector is clicked.
 */
@Composable
fun DateSelectorRow(label: String, dateText: String, onClick: () -> Unit) {
  Column(modifier = Modifier.fillMaxWidth().testTag(label + DateSelectorTestTags.DATE_SELECTOR)) {
    Text(
        text = label,
        style =
            MaterialTheme.typography.titleMedium.copy(
                color = MaterialTheme.colorScheme.onBackground))
    Spacer(modifier = Modifier.height(dimensionResource(R.dimen.tiny_spacer)))
    OutlinedButton(
        onClick = onClick,
        modifier =
            Modifier.fillMaxWidth()
                .height(dimensionResource(R.dimen.date_button_height))
                .testTag(DateSelectorTestTags.DATE),
        shape = RoundedCornerShape(dimensionResource(R.dimen.date_button_radius)),
        colors =
            ButtonDefaults.outlinedButtonColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface)) {
          Text(text = dateText, style = MaterialTheme.typography.bodyLarge)
        }
  }
}
