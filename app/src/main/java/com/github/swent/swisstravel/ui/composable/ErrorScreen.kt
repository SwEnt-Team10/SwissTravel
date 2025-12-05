package com.github.swent.swisstravel.ui.composable

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun ErrorScreen(
    message: String,
    messageTestTag: String,
    topBarTitle: String,
    topBarTitleTestTag: String,
    topBarTestTag: String,
    backButtonTestTag: String,
    backButtonDescription: String,
    retryButtonTestTag: String,
    onRetry: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            ErrorScreenTopBar(
                topBarTitle = topBarTitle,
                backButtonDescription = backButtonDescription,
                backButtonTestTag = backButtonTestTag,
                onBack = {
                    onBack()
                },
                topBarTestTag = topBarTestTag,
                topBarTitleTestTag = topBarTitleTestTag
            )
        }
    ) { pd ->
        Column(
            modifier = Modifier.padding(pd)
        ) {
            Text(
                text = message,
                modifier = Modifier.testTag(messageTestTag)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    onRetry()
                },
                modifier = Modifier.testTag(retryButtonTestTag)
            ) {
                Text(text = "Retry")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ErrorScreenTopBar(
    topBarTitle: String,
    backButtonDescription: String,
    backButtonTestTag: String,
    onBack: () -> Unit,
    topBarTestTag: String,
    topBarTitleTestTag: String,
) {
    TopAppBar(
        modifier = Modifier.testTag(topBarTestTag),
        title = {
            Text(
                text = topBarTitle,
                modifier = Modifier.testTag(topBarTitleTestTag)
            )
                },
        navigationIcon = {
            BackButton(
                onBack = { onBack() },
                testTag = backButtonTestTag,
                contentDescription = backButtonDescription
            )
        }
    )
}