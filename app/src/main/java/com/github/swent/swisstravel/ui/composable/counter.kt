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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun counter(label: String, count: Int, onIncrement: () -> Unit, onDecrement: () -> Unit) {
  Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
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
          RoundIconButton(text = "–", onClick = onDecrement, enabled = count > 0)

          Text(
              text = count.toString(),
              modifier = Modifier.padding(horizontal = 24.dp),
              style =
                  MaterialTheme.typography.headlineMedium.copy(
                      fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground))

          RoundIconButton(text = "+", onClick = onIncrement)
        }
  }
}

@Composable
fun RoundIconButton(text: String, onClick: () -> Unit, enabled: Boolean = true) {
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
      modifier = Modifier.size(48.dp),
      contentPadding = PaddingValues(0.dp)) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
      }
}
