package com.github.swent.swisstravel.ui.edittrip

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import com.github.swent.swisstravel.ui.tripcreation.TravelersSelector

@Composable
fun EditTripScreen(
    tripId: String,
    editTripViewModel: EditTripScreenViewModel = viewModel(),
    onBack: () -> Unit,
    onSavedOrDelete: () -> Unit,
) {
  val context = LocalContext.current
  val state by editTripViewModel.state.collectAsState()

  var showDeleteDialog by remember { mutableStateOf(false) }

  LaunchedEffect(tripId) { editTripViewModel.loadTrip(tripId) }

  LaunchedEffect(state.errorMsg) {
    state.errorMsg?.let {
      Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
      editTripViewModel.clearErrorMsg()
    }
  }

  Scaffold(
      topBar = { TopBar(onClick = onBack, title = "") },
      bottomBar = {
        Surface(tonalElevation = 2.dp) {
          Box(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
              contentAlignment = Alignment.Center) {
                Button(
                    onClick = {
                      editTripViewModel.save()
                      Toast.makeText(context, R.string.trip_saved, Toast.LENGTH_SHORT).show()
                      onSavedOrDelete()
                    },
                    enabled = !state.isLoading,
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier.fillMaxWidth(0.9f)) {
                      Icon(Icons.Filled.Edit, contentDescription = null)
                      Spacer(Modifier.width(8.dp))
                      Text(stringResource(R.string.confirm_changes))
                    }
              }
        }
      }) { inner ->
        if (state.isLoading) {
          Box(Modifier.fillMaxSize().padding(inner), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
          }
          return@Scaffold
        }

        Column(
            modifier =
                Modifier.padding(inner)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)) {
              Text(
                  text = "Editing ${state.tripName.ifBlank { "{TripName}" }} Info",
                  style = MaterialTheme.typography.headlineLarge,
                  fontWeight = FontWeight.ExtraBold,
                  textAlign = TextAlign.Center,
                  modifier = Modifier.fillMaxWidth())

              SectionHeader(stringResource(R.string.travelers))
              TravelersSelector(
                  adults = state.adults,
                  children = state.children,
                  onAdultsChange = editTripViewModel::setAdults,
                  onChildrenChange = editTripViewModel::setChildren)

              SectionHeader(stringResource(R.string.preferences))
              PreferenceSelector(
                  isChecked = { pref -> state.selectedPrefs.contains(pref) },
                  onCheckedChange = editTripViewModel::togglePref)

              PreferenceToggle(
                  label = stringResource(R.string.handicapped_traveler),
                  value = state.selectedPrefs.contains(Preference.WHEELCHAIR_ACCESSIBLE),
                  onValueChange = {
                    editTripViewModel.togglePref(Preference.WHEELCHAIR_ACCESSIBLE)
                  })

              Button(
                  onClick = { showDeleteDialog = true },
                  colors =
                      ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                  shape = RoundedCornerShape(28.dp),
                  modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Icon(Icons.Filled.Delete, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.delete_trip))
                  }

              Spacer(Modifier.height(100.dp))
            }

        if (showDeleteDialog) {
          AlertDialog(
              onDismissRequest = { showDeleteDialog = false },
              confirmButton = {
                TextButton(
                    onClick = {
                      editTripViewModel.deleteTrip()
                      showDeleteDialog = false
                      Toast.makeText(context, R.string.trip_deleted, Toast.LENGTH_SHORT).show()
                      onSavedOrDelete()
                    }) {
                      Text(stringResource(R.string.confirm))
                    }
              },
              dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                  Text(stringResource(R.string.cancel))
                }
              },
              title = { Text(stringResource(R.string.confirm_deletion)) },
              text = { Text(stringResource(R.string.delete_trip_prompt)) })
        }
      }
}

@Composable
private fun SectionHeader(text: String) {
  Column {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    HorizontalDivider(Modifier.padding(top = 6.dp))
  }
}
