package com.github.swent.swisstravel.ui.tripcreation

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.ui.navigation.TopBar
import com.github.swent.swisstravel.ui.tripcreation.ArrivalDepartureTestTags.NEXT_BUTTON

/**
 * A screen to edit the name of a trip.
 *
 * @param viewModel The [TripSettingsViewModel] to use for this screen.
 * @param onNext A callback to invoke when the user has finished editing the trip name.
 * @param onPrevious A callback to invoke when the user wants to go back to the previous screen.
 */
@Composable
fun TripNameScreen(
    viewModel: TripSettingsViewModel = viewModel(),
    onNext: () -> Unit = {},
    onPrevious: () -> Unit = {}
) {
  val state by viewModel.tripSettings.collectAsState()
  val context = LocalContext.current

  Scaffold(
      topBar = { TopBar(onClick = onPrevious, title = "") },
  ) { padding ->
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
      Column(
          modifier = Modifier.fillMaxSize().padding(padding),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.SpaceBetween) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {

              // --- Title ---
              Text(
                  text = stringResource(R.string.trip_name),
                  textAlign = TextAlign.Center,
                  style =
                      MaterialTheme.typography.headlineMedium.copy(
                          fontWeight = FontWeight.Bold,
                      ))

              Spacer(modifier = Modifier.height(32.dp))

              OutlinedTextField(
                  value = state.name,
                  onValueChange = viewModel::updateName,
                  modifier = Modifier.fillMaxWidth().testTag(EditTripScreenTestTags.TRIP_NAME),
                  shape = RoundedCornerShape(12.dp),
                  singleLine = true)

              // --- Done button ---
              Button(
                  modifier = Modifier.testTag(NEXT_BUTTON),
                  onClick = {
                    viewModel.saveTrip()
                    Toast.makeText(context, R.string.trip_saved, Toast.LENGTH_SHORT).show()
                    onNext()
                  },
                  colors =
                      ButtonDefaults.buttonColors(
                          containerColor = MaterialTheme.colorScheme.primary)) {
                    Text(
                        stringResource(R.string.done),
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.titleMedium)
                  }
            }
          }
    }
  }
}
