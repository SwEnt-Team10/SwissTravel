package com.github.swent.swisstravel.ui.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Test tags for UI tests to identify components. */
object CounterTestTags {
  const val COUNTER = "counter"
  const val INCREMENT = "increment"
  const val DECREMENT = "decrement"
  const val COUNT = "count"
}

/**
 * A reusable component for a counter with increment and decrement buttons.
 *
 * @param label The text label for the counter.
 * @param count The current count value.
 * @param onIncrement Callback to be invoked when the increment button is clicked.
 * @param onDecrement Callback to be invoked when the decrement button is clicked.
 */
@Composable
fun Counter(label: String, count: Int, onIncrement: () -> Unit, onDecrement: () -> Unit) {
  Column(
      modifier =
          Modifier.fillMaxWidth()
              .padding(vertical = 12.dp)
              .testTag(label + CounterTestTags.COUNTER)) {
        Text(
            text = label,
            style =
                MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Medium))

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.selectableGroup().fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically) {
              RoundIconButton(
                  text = "–",
                  onClick = onDecrement,
                  enabled = count > 0,
                  testTag = CounterTestTags.DECREMENT)

              Text(
                  text = count.toString(),
                  modifier = Modifier.padding(horizontal = 24.dp).testTag(CounterTestTags.COUNT),
                  style =
                      MaterialTheme.typography.headlineMedium.copy(
                          fontWeight = FontWeight.Bold,
                          color = MaterialTheme.colorScheme.onBackground))

              RoundIconButton(
                  text = "+", onClick = onIncrement, testTag = CounterTestTags.INCREMENT)
            }
      }
}

/**
 * A reusable component for a round icon button.
 *
 * @param text The text to display inside the button.
 * @param onClick Callback to be invoked when the button is clicked.
 * @param enabled Whether the button is enabled or disabled.
 * @param testTag The test tag for UI testing.
 */
@Composable
fun RoundIconButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    testTag: String = ""
) {
  Button(
      onClick = onClick,
      enabled = enabled,
      shape = CircleShape,
      colors =
          ButtonDefaults.buttonColors(
              containerColor =
                  if (enabled) MaterialTheme.colorScheme.primary
                  else MaterialTheme.colorScheme.surfaceVariant,
              contentColor =
                  if (enabled) MaterialTheme.colorScheme.onPrimary
                  else MaterialTheme.colorScheme.onSurfaceVariant),
      modifier = Modifier.size(48.dp).testTag(testTag),
      contentPadding = PaddingValues(0.dp)) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
      }
}
