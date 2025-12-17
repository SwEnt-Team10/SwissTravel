package com.github.swent.swisstravel.ui.tripcreation

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.swent.swisstravel.R

/** A set of test tag for this screen. */
object LoadingTestTags {
  const val LOADING = "loadingScreen"
  const val LOADING_TEXT = "loadingText"
  const val PROGRESS_BAR = "progressBar"
}

/**
 * A screen that is displayed while a trip is being created.
 *
 * @param progress The progress of the trip creation process.
 * @param viewModel The view model for the trip settings screen.
 * @param onSuccess Callback to be invoked when the trip creation is successful.
 * @param onFailure Callback to be invoked when the trip creation fails.
 */
@Composable
fun LoadingScreen(
    progress: Float = 0F,
    viewModel: TripSettingsViewModel = viewModel(),
    onSuccess: () -> Unit = {},
    onFailure: () -> Unit = {}
) {
  val context = LocalContext.current
  LaunchedEffect(key1 = Unit) {
    viewModel.validationEvents.collect { event ->
      when (event) {
        is ValidationEvent.SaveSuccess -> {
          Toast.makeText(context, R.string.trip_saved, Toast.LENGTH_SHORT).show()
          onSuccess()
        }
        is ValidationEvent.SaveError -> {
          Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
          onFailure()
        }
        else -> {
          // Ignore other events
        }
      }
    }
  }

  Column(
      modifier = Modifier.fillMaxSize().testTag(LoadingTestTags.LOADING),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center) {
        Text(
            text = stringResource(id = R.string.loading_trip),
            style = MaterialTheme.typography.titleLarge,
            modifier =
                Modifier.padding(dimensionResource(R.dimen.loading_padding))
                    .testTag(LoadingTestTags.LOADING_TEXT))
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.mid_spacer)))
        LinearProgressIndicator(
            progress = { progress },
            drawStopIndicator = {},
            modifier = Modifier.testTag(LoadingTestTags.PROGRESS_BAR))
      }
}
