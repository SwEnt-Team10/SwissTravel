package com.github.swent.swisstravel.ui.tripcreation

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.ui.navigation.NavigationActions
import com.github.swent.swisstravel.ui.navigation.Screen

@Composable
fun LoadingScreen(
    progress: Float,
    viewModel: TripSettingsViewModel,
    navigationActions: NavigationActions
) {
  val context = LocalContext.current
  LaunchedEffect(key1 = Unit) {
    viewModel.validationEvents.collect { event ->
      when (event) {
        is ValidationEvent.SaveSuccess -> {
          Toast.makeText(context, R.string.trip_saved, Toast.LENGTH_SHORT).show()
          navigationActions.navigateTo(Screen.MyTrips, clearBackStack = true)
        }
        is ValidationEvent.SaveError -> {
          Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
          navigationActions.goBack()
        }
        else -> {
          // Ignore other events
        }
      }
    }
  }

  Column(
      modifier = Modifier.fillMaxSize(),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center) {
        Text(
            text = stringResource(id = R.string.loading_trip),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp))
        Spacer(modifier = Modifier.height(16.dp))
        CircularProgressIndicator(progress = progress)
      }
}
