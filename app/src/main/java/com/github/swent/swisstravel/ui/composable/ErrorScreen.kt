package com.github.swent.swisstravel.ui.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

object ErrorScreenTestTags {
  const val ERROR_SCREEN_TOP_BAR = "errorScreenTopBar"
  const val ERROR_SCREEN_TOP_BAR_TITLE = "errorScreenTopBarTitle"
  const val ERROR_MESSAGE = "errorMessage"
  const val RETRY_BUTTON = "retryButton"
}

@Composable
fun ErrorScreen(
    message: String,
    topBarTitle: String,
    backButtonDescription: String,
    onRetry: () -> Unit,
    onBack: () -> Unit
) {
  Scaffold(
      topBar = {
        ErrorScreenTopBar(
            topBarTitle = topBarTitle,
            backButtonDescription = backButtonDescription,
            onBack = { onBack() })
      }) { pd ->
        Column(
            modifier = Modifier.padding(pd).fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
              Text(text = message, modifier = Modifier.testTag(ErrorScreenTestTags.ERROR_MESSAGE))
              Spacer(modifier = Modifier.height(16.dp))
              IconButton(
                  onClick = { onRetry() },
                  modifier = Modifier.testTag(ErrorScreenTestTags.RETRY_BUTTON)) {
                    Icon(imageVector = Icons.Filled.Replay, contentDescription = "Retry")
                  }
            }
      }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ErrorScreenTopBar(
    topBarTitle: String,
    backButtonDescription: String,
    onBack: () -> Unit,
) {
  TopAppBar(
      modifier = Modifier.testTag(ErrorScreenTestTags.ERROR_SCREEN_TOP_BAR),
      title = {
        Text(
            text = topBarTitle,
            modifier = Modifier.testTag(ErrorScreenTestTags.ERROR_SCREEN_TOP_BAR_TITLE))
      },
      navigationIcon = {
        BackButton(onBack = { onBack() }, contentDescription = backButtonDescription)
      })
}

@Preview
@Composable
private fun ErrorScreenPreview() {
  ErrorScreen(
      message = "It didn't work",
      backButtonDescription = "preview",
      onRetry = {},
      onBack = {},
      topBarTitle = "Preview")
}
