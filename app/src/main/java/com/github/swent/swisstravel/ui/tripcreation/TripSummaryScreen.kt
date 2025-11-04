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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.ui.navigation.TopBar
import com.github.swent.swisstravel.ui.tripcreation.ArrivalDepartureTestTags.NEXT_BUTTON
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

    val listState = rememberLazyListState()

    Scaffold(
        topBar = { TopBar(onClick = onPrevious, title = stringResource(R.string.trip_summary)) }
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
                        label = { Text(stringResource(R.string.trip_name_summary)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                }
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
                item {
                    Text(
                        text = stringResource(R.string.number_of_travelers),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                item {
                    val nAdults = state.travelers.adults
                    val stringAdult =
                        if (nAdults == 1) {
                            stringResource(R.string.adult)
                        } else {
                            stringResource(R.string.adults)
                        }
                    Text(
                        text = "$nAdults $stringAdult",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                item {
                    val nChildren = state.travelers.children
                    val stringAdult =
                        if (nChildren == 1) {
                            stringResource(R.string.child)
                        } else {
                            stringResource(R.string.children)
                        }
                    Text(
                        text = "$nChildren $stringAdult",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                item {
                    Text(
                        text = stringResource(R.string.travelling_preferences_summary),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }










                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(
                            onClick = {
                                viewModel.updateName(tripName)
                                viewModel.saveTrip()
                                Toast.makeText(context, R.string.trip_saved, Toast.LENGTH_SHORT).show()
                                onNext()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(
                                stringResource(R.string.create_trip_summary),
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