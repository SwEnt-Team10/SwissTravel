package com.github.swent.swisstravel.ui.tripsettings

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.ui.geocoding.AddressAutocompleteTextField
import com.github.swent.swisstravel.ui.geocoding.AddressTextFieldViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ArrivalDepartureScreen(
    viewModel: TripSettingsViewModel = viewModel(),
    onNext: () -> Unit = {}
) {

  // Use different separate view models for arrival and departure
  val arrivalAddressVm: AddressTextFieldViewModel = viewModel(key = "arrivalAddressVm")
  val departureAddressVm: AddressTextFieldViewModel = viewModel(key = "departureAddressVm")

  val arrivalState by arrivalAddressVm.addressState.collectAsState()
  val departureState by departureAddressVm.addressState.collectAsState()

  // Synch the queries with the view model
  LaunchedEffect(arrivalState.locationQuery) {
    // Update the model at each modification
    viewModel.updateArrivalLocation(arrivalState.locationQuery)
  }
  LaunchedEffect(arrivalState.selectedLocation) {
    arrivalState.selectedLocation?.let { viewModel.updateArrivalLocation(it.name) }
  }
  LaunchedEffect(departureState.locationQuery) {
    viewModel.updateDepartureLocation(departureState.locationQuery)
  }
  LaunchedEffect(departureState.selectedLocation) {
    departureState.selectedLocation?.let { viewModel.updateDepartureLocation(it.name) }
  }

  Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 24.dp),
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

            Spacer(modifier = Modifier.height(32.dp))

            // --- Arrival Destination (autocomplete) ---
            AddressAutocompleteTextField(addressTextFieldViewModel = arrivalAddressVm)

            Spacer(modifier = Modifier.height(32.dp))

            // --- Departure Destination (autocomplete) ---
            AddressAutocompleteTextField(addressTextFieldViewModel = departureAddressVm)

            Spacer(modifier = Modifier.height(32.dp))
          }

          // --- Done button ---
          Button(
              onClick = {
                // save uses the tripSettings LiveData which we keep updated via the LaunchedEffects
                // above
                viewModel.saveTrip()
                onNext()
              },
              colors =
                  ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                Text(
                    stringResource(R.string.done),
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.titleMedium)
              }
        }
  }
}

@Preview
@Composable
fun TripArrivalDeparturePreview() {
  ArrivalDepartureScreen()
}
