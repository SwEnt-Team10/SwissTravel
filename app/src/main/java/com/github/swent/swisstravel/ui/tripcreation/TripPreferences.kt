package com.github.swent.swisstravel.ui.tripcreation

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.model.user.PreferenceRules
import com.github.swent.swisstravel.ui.composable.PreferenceSelector
import com.github.swent.swisstravel.ui.navigation.TopBar
import kotlinx.coroutines.flow.collectLatest

/** Test tags for UI tests to identify components. */
object TripPreferencesTestTags {
  const val DONE = "done"
  const val TRIP_PREFERENCES_SCREEN = "tripPreferencesScreen"
  const val TRIP_PREFERENCES_TITLE = "tripPreferencesTitle"
  const val TRIP_PREFERENCE_CONTENT = "tripPreferenceContent"
}

/**
 * Screen where users can set their trip preferences.
 *
 * @param viewModel ViewModel to handle the trip settings logic.
 * @param onNext Callback to be invoked when the user is done setting preferences.
 * @param onPrevious Callback to be invoked when the user navigates back.
 * @param isRandomTrip Whether the trip is random or not.
 */
@Composable
fun TripPreferencesScreen(
    viewModel: TripSettingsViewModel = viewModel(),
    onNext: () -> Unit = {},
    onPrevious: () -> Unit = {},
    isRandomTrip: Boolean = false,
) {
  val tripSettings by viewModel.tripSettings.collectAsState()
  val prefs = tripSettings.preferences
  val context = LocalContext.current
  val lazyListState = rememberLazyListState()
  val isScrolledToEnd by rememberIsScrolledToEnd(lazyListState)

  LaunchedEffect(Unit) {
    viewModel.validationEvents.collectLatest { event ->
      when (event) {
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
        // Use a Box to layer the list and the button
        Box(modifier = Modifier.fillMaxSize().padding(pd)) {
          LazyColumn(
              state = lazyListState,
              modifier =
                  Modifier.fillMaxSize().testTag(TripPreferencesTestTags.TRIP_PREFERENCE_CONTENT),
              contentPadding =
                  PaddingValues(
                      start = dimensionResource(R.dimen.mid_padding),
                      end = dimensionResource(R.dimen.mid_padding),
                      top = dimensionResource(R.dimen.mid_padding),
                      // Add padding to the bottom to make space for the sticky button
                      bottom = dimensionResource(R.dimen.huge_padding)),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement =
                  Arrangement.spacedBy(dimensionResource(R.dimen.trip_preference_spacing))) {

                // --- Title ---
                item {
                  Text(
                      modifier = Modifier.testTag(TripPreferencesTestTags.TRIP_PREFERENCES_TITLE),
                      text = stringResource(R.string.travelling_preferences),
                      textAlign = TextAlign.Center,
                      style =
                          MaterialTheme.typography.headlineMedium.copy(
                              fontWeight = FontWeight.Bold,
                          ))
                }

                // --- Preferences ---
                item {
                  PreferenceSelector(
                      isChecked = { pref -> prefs.contains(pref) },
                      onCheckedChange = { preference ->
                        val next = PreferenceRules.toggleWithExclusivity(prefs, preference)
                        viewModel.updatePreferences(next)
                      },
                      isRandomTrip = isRandomTrip)
                }
              }

          // --- Next button (conditionally visible) ---
          Done(Modifier.align(Alignment.BottomCenter), isScrolledToEnd, onNext, context)
        }
      }
}

/**
 * Button to be displayed at the bottom of the screen.
 *
 * @param modifier Modifier to be applied to the button.
 * @param isScrolledToEnd Whether the LazyColumn has been scrolled to the end.
 * @param onNext Callback to be invoked when the user is done setting preferences.
 * @param context Context to be used for the Toast.
 */
@Composable
private fun Done(
    modifier: Modifier = Modifier,
    isScrolledToEnd: Boolean,
    onNext: () -> Unit,
    context: Context
) {
  if (isScrolledToEnd) {
    Button(
        onClick = { onNext() },
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        modifier =
            modifier
                .padding(bottom = dimensionResource(R.dimen.medium_padding))
                .testTag(TripPreferencesTestTags.DONE)) {
          Text(
              text = stringResource(R.string.next),
              color = MaterialTheme.colorScheme.onPrimary,
              style = MaterialTheme.typography.titleMedium)
        }
  }
}

/**
 * Returns a State<Boolean> that is true if the LazyColumn has been scrolled to the end. This is a
 * helper function to keep the main composable clean.
 *
 * @param lazyListState The LazyListState to be observed.
 * @return A State<Boolean> that is true if the LazyColumn has been scrolled to the end.
 */
@Composable
private fun rememberIsScrolledToEnd(lazyListState: LazyListState): State<Boolean> {
  return remember(lazyListState) {
    derivedStateOf {
      val layoutInfo = lazyListState.layoutInfo
      val visibleItemsInfo = layoutInfo.visibleItemsInfo
      if (layoutInfo.totalItemsCount == 0) {
        true
      } else {
        val lastVisibleItem = visibleItemsInfo.lastOrNull() ?: return@derivedStateOf false
        val lastItemVisible = lastVisibleItem.index == layoutInfo.totalItemsCount - 1
        // The bottom of the last item is at or past the viewport's end
        val lastItemBottom = lastVisibleItem.offset + lastVisibleItem.size
        lastItemVisible && lastItemBottom <= layoutInfo.viewportEndOffset
      }
    }
  }
}
