package com.github.swent.swisstravel.ui.tripcreation

import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.ui.navigation.TopBar
import androidx.compose.foundation.layout.Box

@Composable
fun TripSummaryScreen(
    viewModel: TripSettingsViewModel = viewModel(),
    onNext: () -> Unit = {},
    onPrevious: () -> Unit = {}
) {
    val state by viewModel.tripSettings.collectAsState()
    val context = LocalContext.current
    var tripName by rememberSaveable { mutableStateOf("") }
    val startDate = state.date.startDate
    val endDate = state.date.endDate
    val departure = state.arrivalDeparture.departureLocation?.name ?: ""
    val arrival = state.arrivalDeparture.arrivalLocation?.name ?: ""

    val tripSummaryTitle = stringResource(R.string.trip_summary)
    val tripNameLabel = stringResource(R.string.trip_name_summary)
    val numberOfTravelersLabel = stringResource(R.string.number_of_travelers)
    val adultSingular = stringResource(R.string.adult)
    val adultPlural = stringResource(R.string.adults)
    val childSingular = stringResource(R.string.child)
    val childPlural = stringResource(R.string.children)
    val travellingPreferencesLabel = stringResource(R.string.travelling_preferences_summary)
    val arrivalLabel = stringResource(R.string.arrival)
    val departureLabel = stringResource(R.string.departure)
    val placesLabel = stringResource(R.string.places)
    val createTripLabel = stringResource(R.string.create_trip_summary)
    val emptyNameToast = stringResource(R.string.trip_name_required)
    val emptyDeparture = stringResource(R.string.departure_required)
    val emptyArrival = stringResource(R.string.arrival_required)

    val listState = rememberLazyListState()

    Scaffold(
        topBar = { TopBar(onClick = onPrevious, title = tripSummaryTitle) }
    ) { pd ->
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(pd),
            ) {
                item {
                    OutlinedTextField(
                        value = tripName,
                        onValueChange = { new ->
                            tripName = new
                        },
                        label = { Text(tripNameLabel) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                }
                // Summary of dates
                item {
                    Text(
                        text = "From: $startDate",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                item {
                    Text(
                        text = "To: $endDate",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                // Summary of travelers
                item {
                    Text(
                        text = numberOfTravelersLabel,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                item {
                    val nAdults = state.travelers.adults
                    val stringAdult = if (nAdults == 1) adultSingular else adultPlural
                    Text(
                        text = "$nAdults $stringAdult",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                item {
                    val nChildren = state.travelers.children
                    val stringChild = if (nChildren == 1) childSingular else childPlural
                    Text(
                        text = "$nChildren $stringChild",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                // Summary of travelling preferences
                item {
                    Text(
                        text = travellingPreferencesLabel,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                // summary of start and end locations
                item {
                    Text(
                        text = arrivalLabel,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                item {
                    Text(
                        text = arrival,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                item {
                    Text(
                        text = departureLabel,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                item {
                    Text(
                        text = departure,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                // summary of places
                item {
                    Text(
                        text = placesLabel,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                // Create trip button
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(
                            onClick = {
                                if (tripName.isBlank()) {
                                    Toast.makeText(context, emptyNameToast, Toast.LENGTH_SHORT).show()
                                    return@Button
                                } else if (departure == "") {
                                    Toast.makeText(context, emptyDeparture, Toast.LENGTH_SHORT).show()
                                    return@Button
                                } else  if (arrival == "") {
                                    Toast.makeText(context, emptyArrival, Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.updateName(tripName)
                                    viewModel.saveTrip()
                                    Toast.makeText(context, createTripLabel, Toast.LENGTH_SHORT).show()
                                    onNext()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(
                                createTripLabel,
                                color = MaterialTheme.colorScheme.onPrimary,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }
        }
    }
}
