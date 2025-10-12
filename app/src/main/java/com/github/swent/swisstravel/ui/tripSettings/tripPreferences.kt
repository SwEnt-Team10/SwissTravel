package com.github.swent.swisstravel.ui.tripSettings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.ui.composable.PreferenceSwitch
import com.github.swent.swisstravel.ui.composable.PreferenceToggle
import com.github.swent.swisstravel.ui.theme.SwissTravelTheme

object TripPreferencesTestTags {
  const val DONE = "done"
  const val TRIP_PREFERENCES_SCREEN = "tripPreferencesScreen"
}

/**
 * Screen where users can set their trip preferences.
 *
 * @param viewModel ViewModel to handle the trip settings logic.
 * @param onDone Callback to be invoked when the user is done setting preferences.
 */
@Composable
fun TripPreferencesScreen(viewModel: TripSettingsViewModel = viewModel(), onDone: () -> Unit = {}) {
  val tripSettings by viewModel.tripSettings.collectAsState()
  var prefs by remember { mutableStateOf(tripSettings.preferences) }

  LaunchedEffect(tripSettings.preferences) { prefs = tripSettings.preferences }

  // Light beige-like background as in Figma
  Surface(
      modifier = Modifier.fillMaxSize().testTag(TripPreferencesTestTags.TRIP_PREFERENCES_SCREEN),
      color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween) {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.travellingPreferences),
                    textAlign = TextAlign.Center,
                    style =
                        MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                        ))

                Spacer(modifier = Modifier.height(32.dp))

                PreferenceSwitch(stringResource(R.string.quickTraveler), prefs.quickTraveler) {
                  prefs = prefs.copy(quickTraveler = it)
                }

                PreferenceSwitch(stringResource(R.string.sportyTrip), prefs.sportyLevel) {
                  prefs = prefs.copy(sportyLevel = it)
                }

                PreferenceSwitch(stringResource(R.string.foodyTrip), prefs.foodyLevel) {
                  prefs = prefs.copy(foodyLevel = it)
                }

                PreferenceSwitch(stringResource(R.string.museumsLiker), prefs.museumInterest) {
                  prefs = prefs.copy(museumInterest = it)
                }

                PreferenceToggle(stringResource(R.string.handicappedTraveler), prefs.hasHandicap) {
                  prefs = prefs.copy(hasHandicap = it)
                }

                Spacer(modifier = Modifier.height(24.dp))
              }

              Button(
                  onClick = {
                    viewModel.updatePreferences(prefs)
                    onDone()
                  },
                  colors =
                      ButtonDefaults.buttonColors(
                          containerColor = MaterialTheme.colorScheme.primary),
                  shape = RoundedCornerShape(24.dp),
                  modifier = Modifier.testTag(TripPreferencesTestTags.DONE)) {
                    Text(
                        stringResource(R.string.done),
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium)
                  }
            }
      }
}

@Preview
@Composable
fun TripPreferencesScreenPreview() {
  SwissTravelTheme { TripPreferencesScreen() }
}
