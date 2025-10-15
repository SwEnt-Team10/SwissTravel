package com.github.swent.swisstravel.ui.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.swent.swisstravel.R

/** Test tags for UI tests to identify components. */
object ToggleTestTags {
  const val TOGGLE = "toggle"
  const val RADIO_BUTTON = "radioButton"
  const val LABEL = "label"
  const val YES = "yes"
  const val NO = "no"
}

/**
 * A reusable component for a preference toggle with a label.
 *
 * @param label The text label for the toggle.
 * @param Boolean The current state of the toggle.
 * @param onValueChange Callback to be invoked when the toggle state changes.
 */
@Composable
fun PreferenceToggle(label: String, value: Boolean, onValueChange: (Boolean) -> Unit) {
  Column(
      modifier =
          Modifier.fillMaxWidth()
              .padding(vertical = 12.dp)
              .testTag(label + ToggleTestTags.TOGGLE)) {
        Text(
            text = label,
            style =
                MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Medium))

        HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.outlineVariant)

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.selectableGroup().fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically) {
              RadioOption(
                  text = stringResource(R.string.yes),
                  selected = value,
                  onClick = { onValueChange(true) },
                  testTag = ToggleTestTags.YES)

              Spacer(modifier = Modifier.width(24.dp))

              RadioOption(
                  text = stringResource(R.string.no),
                  selected = !value,
                  onClick = { onValueChange(false) },
                  testTag = ToggleTestTags.NO)
            }
      }
}

/**
 * A reusable component for a radio option with a label.
 *
 * @param text The text label for the radio option.
 * @param selected Whether this option is selected.
 * @param onClick Callback to be invoked when the option is clicked.
 */
@Composable
fun RadioOption(text: String, selected: Boolean, onClick: () -> Unit, testTag: String) {
  Row(
      modifier =
          Modifier.selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
              .padding(horizontal = 8.dp)
              .testTag(testTag),
      verticalAlignment = Alignment.CenterVertically) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            modifier = Modifier.testTag(ToggleTestTags.RADIO_BUTTON),
            colors =
                androidx.compose.material3.RadioButtonDefaults.colors(
                    selectedColor = MaterialTheme.colorScheme.primary,
                    unselectedColor = MaterialTheme.colorScheme.outline))
        Text(
            text = text,
            style =
                MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onBackground),
            modifier = Modifier.padding(start = 8.dp).testTag(ToggleTestTags.LABEL))
      }
}
