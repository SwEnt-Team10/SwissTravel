package com.github.swent.swisstravel.ui.tripcreation

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.ui.geocoding.AddressTextFieldState
import com.github.swent.swisstravel.ui.geocoding.AddressTextFieldViewModel
import com.github.swent.swisstravel.ui.geocoding.AddressTextFieldViewModelContract
import com.github.swent.swisstravel.ui.geocoding.LocationAutocompleteTextField
import com.github.swent.swisstravel.ui.navigation.TopBar
import com.github.swent.swisstravel.ui.tripcreation.ArrivalDepartureTestTags.NEXT_BUTTON
import com.github.swent.swisstravel.ui.tripcreation.TripFirstDestinationsTestTags.RETURN_BUTTON

/** Test tags for UI tests to identify components within the Arrival/Departure screen. */
object ArrivalDepartureTestTags {
  const val ARRIVAL_TEXTFIELD = "arrival_textfield"
  const val DEPARTURE_TEXTFIELD = "departure_textfield"
  const val NEXT_BUTTON = "next"
}

/**
 * Data class representing the state of the arrival and departure locations.
 *
 * @property arrivalState The state of the arrival location text field.
 * @property departureState The state of the departure location text field.
 */
data class ArrivalDepartureState(
    val arrivalState: AddressTextFieldState,
    val departureState: AddressTextFieldState
)

/**
 * A composable screen that allows users to set the arrival and departure locations for their trip.
 *
 * This screen features two [LocationAutocompleteTextField] components, one for the arrival location
 * and one for the departure location. It uses separate instances of
 * [AddressTextFieldViewModelContract] to manage the state of each text field independently. The
 * selected locations are synchronized with the main [TripSettingsViewModel].
 *
 * @param viewModel The [TripSettingsViewModel] instance that holds the overall trip configuration.
 * @param onNext A callback function to be invoked when the user proceeds to the next step.
 * @param arrivalAddressVm The view model for the arrival address text field. A unique key is used
 *   to ensure a distinct instance from the departure view model.
 * @param departureAddressVm The view model for the departure address text field. A unique key is
 *   used to ensure a distinct instance from the arrival view model.
 * @param isRandomTrip Whether the trip is random or not.
 * @param onRandom Callback to be invoked when the user wants a random trip.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ArrivalDepartureScreen(
    viewModel: TripSettingsViewModel = viewModel(),
    onNext: () -> Unit = {},
    onPrevious: () -> Unit = {},
    arrivalAddressVm: AddressTextFieldViewModel = viewModel(key = "arrivalAddressVm"),
    departureAddressVm: AddressTextFieldViewModel = viewModel(key = "departureAddressVm"),
    isRandomTrip: Boolean = false,
    onRandom: () -> Unit = {}
) {
  val arrivalState by arrivalAddressVm.addressState.collectAsState()
  val departureState by departureAddressVm.addressState.collectAsState()
  val state = ArrivalDepartureState(arrivalState, departureState)
  val isRandomTrip by viewModel.isRandomTrip.collectAsState()
  val context = LocalContext.current

  // Synchronize the selected locations with the TripSettingsViewModel.
  LaunchedEffect(arrivalState.selectedLocation) {
    arrivalState.selectedLocation?.let { viewModel.updateArrivalLocation(it) }
  }
  LaunchedEffect(state.departureState.selectedLocation) {
    if (!isRandomTrip) {
      state.departureState.selectedLocation?.let { viewModel.updateDepartureLocation(it) }
    }
  }

  Scaffold(
      topBar = {
        TopBar(
            onClick = { onPrevious() }, modifier = Modifier.fillMaxWidth().testTag(RETURN_BUTTON))
      }) { paddingValues ->
        Surface(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            color = MaterialTheme.colorScheme.background,
        ) {
          Column(
              modifier =
                  Modifier.fillMaxSize()
                      .padding(dimensionResource(R.dimen.arrival_departure_padding)),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.SpaceBetween) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {

                  // --- Title ---
                  Text(
                      text = stringResource(R.string.arrivalDeparture),
                      textAlign = TextAlign.Center,
                      style =
                          MaterialTheme.typography.headlineMedium.copy(
                              fontWeight = FontWeight.Bold,
                          ))

                  Spacer(modifier = Modifier.height(dimensionResource(R.dimen.medium_spacer)))

                  // --- Arrival Destination (autocomplete) ---
                  LocationAutocompleteTextField(
                      addressTextFieldViewModel = arrivalAddressVm,
                      modifier = Modifier.testTag(ArrivalDepartureTestTags.ARRIVAL_TEXTFIELD),
                      name = stringResource(R.string.arrival_location),
                      showImages = false)
                  Spacer(modifier = Modifier.height(dimensionResource(R.dimen.large_spacer)))

                  // --- Departure Destination (autocomplete) ---
                  if (!isRandomTrip) {
                    LocationAutocompleteTextField(
                        addressTextFieldViewModel = departureAddressVm,
                        modifier = Modifier.testTag(ArrivalDepartureTestTags.DEPARTURE_TEXTFIELD),
                        name = stringResource(R.string.departure_location),
                        showImages = false)
                  }

                  Spacer(modifier = Modifier.height(dimensionResource(R.dimen.medium_spacer)))
                }

                // --- Done button ---
                DoneButton(
                    viewModel = viewModel,
                    isRandomTrip = isRandomTrip,
                    state = state,
                    onNext = onNext,
                    onRandom = onRandom,
                    context = context)
              }
        }
      }
}

/**
 * Button to be displayed at the bottom of the screen.
 *
 * @param modifier Modifier to be applied to the button.
 * @param viewModel ViewModel to handle the trip settings logic.
 * @param state State of the arrival and departure locations.
 * @param isRandomTrip Whether the trip is random or not.
 * @param onNext Callback to be invoked when the user is done setting preferences.
 * @param onRandom Callback to be invoked when the user wants a random trip.
 * @param context Context to be used for the Toast.
 */
@Composable
private fun DoneButton(
    modifier: Modifier = Modifier,
    viewModel: TripSettingsViewModel,
    state: ArrivalDepartureState,
    isRandomTrip: Boolean,
    onNext: () -> Unit,
    onRandom: () -> Unit,
    context: Context
) {

  val buttonText =
      if (isRandomTrip) stringResource(R.string.surprise) else stringResource(R.string.next)

  Button(
      onClick = {
        if (isRandomTrip) {
          viewModel.randomTrip(context)
          onRandom()
        } else {
          onNext()
        }
      },
      enabled =
          if (isRandomTrip) state.arrivalState.selectedLocation != null
          else
              state.arrivalState.selectedLocation != null &&
                  state.departureState.selectedLocation != null,
      colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
      modifier =
          modifier
              .padding(bottom = dimensionResource(R.dimen.medium_padding))
              .testTag(NEXT_BUTTON)) {
        Text(
            text = buttonText,
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.titleMedium)
      }
}
