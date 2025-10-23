package com.github.swent.swisstravel.ui.tripsettings

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.model.user.Preference
import com.github.swent.swisstravel.ui.composable.PreferenceSwitch
import com.github.swent.swisstravel.ui.composable.PreferenceToggle
import kotlinx.coroutines.flow.collectLatest

/** Test tags for UI tests to identify components. */
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
  val prefs = tripSettings.preferences
  val context = LocalContext.current

  LaunchedEffect(Unit) {
    viewModel.validationEvents.collectLatest { event ->
      when (event) {
        is ValidationEvent.SaveSuccess -> {
          Toast.makeText(context, R.string.trip_saved, Toast.LENGTH_SHORT).show()
          onDone()
        }
        is ValidationEvent.SaveError -> {
          Toast.makeText(context, "${R.string.error}: ${event.message}", Toast.LENGTH_LONG).show()
        }
        else -> {
          // Other events are not handled here
        }
      }
    }
  }

  LaunchedEffect(prefs) { viewModel.updatePreferences(prefs) }

  Surface(
      modifier = Modifier.fillMaxSize().testTag(TripPreferencesTestTags.TRIP_PREFERENCES_SCREEN),
      color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween) {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {

                // --- Title ---
                Text(
                    text = stringResource(R.string.travellingPreferences),
                    textAlign = TextAlign.Center,
                    style =
                        MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                        ))

                Spacer(modifier = Modifier.height(32.dp))

                // --- Preferences ---
                PreferenceSwitch(
                    stringResource(R.string.quickTraveler),
                    prefs.contains(Preference.QUICK),
                    onCheckedChange = { checked ->
                      viewModel.updatePreferences(prefs.toggle(Preference.QUICK, checked))
                    })

                PreferenceSwitch(
                    stringResource(R.string.sportyTrip),
                    prefs.contains(Preference.SPORTS),
                    onCheckedChange = { checked ->
                      viewModel.updatePreferences(prefs.toggle(Preference.SPORTS, checked))
                    })

                PreferenceSwitch(
                    stringResource(R.string.foodyTrip),
                    prefs.contains(Preference.FOODIE),
                    onCheckedChange = { checked ->
                      viewModel.updatePreferences(prefs.toggle(Preference.FOODIE, checked))
                    })

                PreferenceSwitch(
                    stringResource(R.string.museumsLiker),
                    prefs.contains(Preference.MUSEUMS),
                    onCheckedChange = { checked ->
                      viewModel.updatePreferences(prefs.toggle(Preference.MUSEUMS, checked))
                    })

                PreferenceToggle(
                    stringResource(R.string.handicappedTraveler),
                    prefs.contains(Preference.WHEELCHAIR_ACCESSIBLE),
                    onValueChange = { checked ->
                      viewModel.updatePreferences(
                          prefs.toggle(Preference.WHEELCHAIR_ACCESSIBLE, checked))
                    })

                Spacer(modifier = Modifier.height(24.dp))
              }

              // --- Done button ---
              Button(
                  onClick = { viewModel.saveTrip() },
                  colors =
                      ButtonDefaults.buttonColors(
                          containerColor = MaterialTheme.colorScheme.primary),
                  modifier = Modifier.testTag(TripPreferencesTestTags.DONE)) {
                    Text(
                        stringResource(R.string.done),
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium)
                  }
            }
      }
}

/** Toggles the given [preference] in the list. Adds it if it's not present, removes it if it is. */
private fun List<Preference>.toggle(preference: Preference, checked: Boolean): List<Preference> {
  return this.toMutableList().apply { if (checked) add(preference) else remove(preference) }
}
