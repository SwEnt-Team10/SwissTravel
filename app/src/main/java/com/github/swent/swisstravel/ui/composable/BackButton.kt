package com.github.swent.swisstravel.ui.composable

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

object BackButtonTestTag {
  const val BACK_BUTTON = "backButton"
}

@Composable
fun BackButton(onBack: () -> Unit, contentDescription: String) {
  IconButton(onClick = { onBack() }, modifier = Modifier.testTag(BackButtonTestTag.BACK_BUTTON)) {
    Icon(
        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
        contentDescription = contentDescription,
        tint = MaterialTheme.colorScheme.onBackground)
  }
}
