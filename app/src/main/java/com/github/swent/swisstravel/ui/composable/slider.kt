package com.github.swent.swisstravel.ui.composable

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * A reusable component for a preference slider with a label.
 *
 * (Not used in the current implementation but kept for potential future use.)
 *
 * @param label The text label for the switch.
 * @param value The current state of the switch.
 * @param onValueChange Callback to be invoked when the slider state changes.
 */
@Composable
fun PreferenceSlider(label: String, value: Int, onValueChange: (Int) -> Unit) {
  Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
    Text(
        text = label,
        style =
            MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Medium))
    HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.outlineVariant)
    Slider(
        value = value.toFloat(),
        onValueChange = { onValueChange(it.toInt()) },
        valueRange = 1f..5f,
        steps = 3,
        colors =
            SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant))
  }
}
