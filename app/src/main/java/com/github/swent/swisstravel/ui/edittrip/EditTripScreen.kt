package com.github.swent.swisstravel.ui.edittrip

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.swent.swisstravel.ui.composable.PreferenceSelector
import com.github.swent.swisstravel.ui.navigation.TopBar
import com.github.swent.swisstravel.ui.tripcreation.TravelersSelector

@Composable
fun EditTripScreen(
    tripId: String,
    editTripViewModel: EditTripScreenViewModel = viewModel(),
) {
  LaunchedEffect(tripId) { editTripViewModel.loadTrip(tripId) }

  val state by editTripViewModel.state.collectAsState()
  val context = LocalContext.current

  LaunchedEffect(state.errorMsg) {
    if (state.errorMsg != null) {
      Toast.makeText(context, state.errorMsg, Toast.LENGTH_SHORT).show()
      editTripViewModel.clearErrorMsg()
    }
  }

  Scaffold(
      topBar = { TopBar(onClick = {}, title = "") },
      bottomBar = {
        Surface(tonalElevation = 2.dp) {
          Row(
              Modifier.fillMaxWidth().padding(16.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween) {
                IconButton(
                    onClick = { editTripViewModel.save() },
                    enabled = !state.isLoading,
                ) {
                  Icon(imageVector = Icons.Filled.Edit, contentDescription = "Confirm Changes")
                }
              }
        }
      },
      content = { inner ->
        if (state.isLoading) {
          Box(Modifier.fillMaxSize().padding(inner), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
          }
        } else {
          Text("Travelers", style = MaterialTheme.typography.titleMedium)
          TravelersSelector(
              adults = state.adults,
              children = state.children,
              onAdultsChange = editTripViewModel::setAdults,
              onChildrenChange = editTripViewModel::setChildren,
          )

          Text("Preferences", style = MaterialTheme.typography.titleMedium)
          PreferenceSelector(
              isChecked = { pref -> state.selectedPrefs.contains(pref) },
              onCheckedChange = editTripViewModel::togglePref,
          )
        }
      })
}
