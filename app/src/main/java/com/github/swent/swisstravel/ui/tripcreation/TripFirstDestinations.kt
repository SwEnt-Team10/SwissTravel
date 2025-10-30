package com.github.swent.swisstravel.ui.tripcreation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed // Import itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.model.trip.Coordinate
import com.github.swent.swisstravel.model.trip.Location
import com.github.swent.swisstravel.ui.geocoding.AddressAutocompleteTextField
import com.github.swent.swisstravel.ui.geocoding.AddressTextFieldViewModel // Import the concrete class
import com.github.swent.swisstravel.ui.navigation.TopBar

@Composable
fun FirstDestinationScreen(
    viewModel: TripSettingsViewModel = viewModel(),
    onNext: () -> Unit = {},
    onPrevious: () -> Unit = {}
) {
    val destinations = remember { mutableStateListOf<Location>() }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { TopBar(onClick = { onPrevious() }) }
    ) { paddingValues ->
        Surface(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = stringResource(R.string.first_destinations_title),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(onClick = { destinations.add(Location(coordinate = Coordinate(0.0, 0.0),name="")) }) {
                        Text(stringResource(R.string.add_first_destination))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        // Use itemsIndexed to get the index of each item
                        itemsIndexed(destinations, key = { index, _ -> index }) { index, _ ->
                            // Create a unique ViewModel for each text field using the index as a key
                            val addressVm: AddressTextFieldViewModel = viewModel(key = "destination_$index")

                            AddressAutocompleteTextField(
                                addressTextFieldViewModel = addressVm, // Pass the unique ViewModel instance
                                onLocationSelected = { selectedLocation ->
                                    destinations[index] = selectedLocation
                                },
                                clearOnSelect = false,
                                name = "Destination ${index + 1}"
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(
                        onClick = {
                            viewModel.setDestinations(destinations.toList())
                            onNext()
                        },
                        enabled = destinations.isNotEmpty() && destinations.all { it.name.isNotEmpty() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(
                            stringResource(R.string.next),
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
    }
}
