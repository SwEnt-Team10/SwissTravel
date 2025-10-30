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
import androidx.compose.ui.platform.testTag
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
import com.github.swent.swisstravel.ui.tripcreation.TripFirstDestinationsTestTags.ADD_FIRST_DESTINATION
import com.github.swent.swisstravel.ui.tripcreation.TripFirstDestinationsTestTags.FIRST_DESTINATIONS_TITLE
import com.github.swent.swisstravel.ui.tripcreation.TripFirstDestinationsTestTags.NEXT_BUTTON
import com.github.swent.swisstravel.ui.tripcreation.TripFirstDestinationsTestTags.RETURN_BUTTON

object TripFirstDestinationsTestTags {
    const val FIRST_DESTINATIONS_TITLE = "first_destinations_title"
    const val ADD_FIRST_DESTINATION = "add_first_destination"
    const val NEXT_BUTTON = "next_button"
    const val RETURN_BUTTON = "return_button"
}

@Composable
fun FirstDestinationScreen(
    viewModel: TripSettingsViewModel = viewModel(),
    onNext: () -> Unit = {},
    onPrevious: () -> Unit = {},
    //Add the factory as a parameter with a default for production use
    addressViewModelFactory: @Composable (Int) -> AddressTextFieldViewModel = { index ->
        viewModel(key = "destination_$index")
    }
) {
    val destinations = remember { mutableStateListOf<Location>() }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { TopBar(onClick = { onPrevious() }, modifier = Modifier.testTag(RETURN_BUTTON)) }
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
                        modifier = Modifier.testTag(FIRST_DESTINATIONS_TITLE),
                        text = stringResource(R.string.first_destinations_title),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    Spacer(modifier = Modifier.height(32.dp))



                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        itemsIndexed(destinations, key = { index, _ -> index }) { index, _ ->
                            //Use the factory to create the ViewModel
                            val addressVm = addressViewModelFactory(index)

                            AddressAutocompleteTextField(
                                addressTextFieldViewModel = addressVm, // Pass the created ViewModel
                                onLocationSelected = { selectedLocation ->
                                    destinations[index] = selectedLocation
                                },
                                clearOnSelect = false,
                                name = "Destination ${index + 1}"
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        modifier = Modifier.testTag(ADD_FIRST_DESTINATION),
                        onClick = { destinations.add(Location(coordinate = Coordinate(0.0, 0.0), name = "")) },
                        enabled = destinations.isEmpty() || destinations.last().name.isNotEmpty(),
                    ) {
                        Text(stringResource(R.string.add_first_destination))
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(
                        modifier = Modifier.testTag(NEXT_BUTTON),
                        onClick = {
                            destinations.filter({element -> element.name.isNotEmpty()})
                            viewModel.setDestinations(destinations.toList())
                            onNext()
                        },
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
