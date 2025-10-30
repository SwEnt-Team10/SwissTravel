package com.github.swent.swisstravel.ui.tripcreation

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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.github.swent.swisstravel.model.user.Preference
import com.github.swent.swisstravel.ui.composable.PreferenceSelector
import com.github.swent.swisstravel.ui.composable.PreferenceToggle
import com.github.swent.swisstravel.ui.navigation.TopBar
import kotlinx.coroutines.flow.collectLatest

/** Test tags for UI tests to identify components. */
object TripPreferencesTestTags {
  const val DONE = "done"
  const val TRIP_PREFERENCES_SCREEN = "tripPreferencesScreen"
  const val TRIP_PREFERENCES_TITLE = "tripPreferencesTitle"
}

/**
 * Screen where users can set their trip preferences.
 *
 * @param viewModel ViewModel to handle the trip settings logic.
 * @param onNext Callback to be invoked when the user is done setting preferences.
 */
@Composable
fun TripPreferencesScreen(
    viewModel: TripSettingsViewModel = viewModel(),
    onNext: () -> Unit = {},
    onPrevious: () -> Unit = {}
) {
  val tripSettings by viewModel.tripSettings.collectAsState()
  val prefs = tripSettings.preferences
  val context = LocalContext.current

  LaunchedEffect(Unit) {
    viewModel.validationEvents.collectLatest { event ->
      when (event) {
        is ValidationEvent.SaveSuccess -> {
          onNext()
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
  Scaffold(
      modifier = Modifier.fillMaxSize().testTag(TripPreferencesTestTags.TRIP_PREFERENCES_SCREEN),
      topBar = { TopBar(onClick = { onPrevious() }) }) { pd ->
        Surface(
            modifier = Modifier.fillMaxSize().padding(pd),
            color = MaterialTheme.colorScheme.background) {
              Column(
                  modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 24.dp),
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.SpaceBetween) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {

                      // --- Title ---
                      Text(
                          modifier =
                              Modifier.testTag(TripPreferencesTestTags.TRIP_PREFERENCES_TITLE),
                          text = stringResource(R.string.travelling_preferences),
                          textAlign = TextAlign.Center,
                          style =
                              MaterialTheme.typography.headlineMedium.copy(
                                  fontWeight = FontWeight.Bold,
                              ))

                      Spacer(modifier = Modifier.height(32.dp))

                      // --- Preferences ---
                      PreferenceSelector(
                          isChecked = { pref -> prefs.contains(pref) },
                          onCheckedChange = { preference ->
                            viewModel.updatePreferences(prefs.toggle(preference))
                          })
                      Spacer(modifier = Modifier.height(32.dp))

                      PreferenceToggle(
                          stringResource(R.string.handicapped_traveler),
                          prefs.contains(Preference.WHEELCHAIR_ACCESSIBLE),
                          onValueChange = { _ ->
                            viewModel.updatePreferences(
                                prefs.toggle(Preference.WHEELCHAIR_ACCESSIBLE))
                          })
                    }

                    // --- Next button ---
                    Button(
                        onClick = { onNext() },
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.testTag(TripPreferencesTestTags.DONE)) {
                          Text(
                              stringResource(R.string.next),
                              color = MaterialTheme.colorScheme.onPrimary,
                              style = MaterialTheme.typography.titleMedium)
                        }
                  }
            }
      }
}

/** Toggles the given [preference] in the list. Adds it if it's not present, removes it if it is. */
private fun List<Preference>.toggle(preference: Preference): List<Preference> {
  return this.toMutableList().apply {
    if (!this.contains(preference)) add(preference) else remove(preference)
  }
}
