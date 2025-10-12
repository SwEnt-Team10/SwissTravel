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
import androidx.compose.ui.unit.dp

@Composable
fun DateSelectorRow(label: String, dateText: String, onClick: () -> Unit) {
  Column(modifier = Modifier.fillMaxWidth()) {
    Text(
        text = label,
        style =
            MaterialTheme.typography.titleMedium.copy(
                color = MaterialTheme.colorScheme.onBackground))
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(16.dp),
        border = ButtonDefaults.outlinedButtonBorder,
        colors =
            ButtonDefaults.outlinedButtonColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface)) {
          Text(text = dateText, style = MaterialTheme.typography.bodyLarge)
        }
  }
}
