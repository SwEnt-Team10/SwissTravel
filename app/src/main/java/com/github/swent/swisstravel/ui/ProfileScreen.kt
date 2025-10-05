package com.github.swent.swisstravel.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage


@Composable
fun ProfileScreen(
    profileScreenViewModel: ProfileScreenViewModel = ProfileScreenViewModel()
) {
    val uiState = profileScreenViewModel.uiState.collectAsState().value
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = uiState.profilePicUrl,
            contentDescription = "Profile picture",
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
        )

        Text(
            text = "Hello, ${uiState.name}!",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(top = 16.dp))

        Text(
            text = "Travel Preferences",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp))

        MultiSelectDropdown(
            allPreferences = profileScreenViewModel.allPreferences,
            selectedPreferences = uiState.selectedPreferences,
            onSelectionChanged = { newSelection -> uiState.selectedPreferences = newSelection }
        )

        Text(
            text = "Personal Informations",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp))



    }
}

@Composable
fun MultiSelectDropdown(
    allPreferences: List<String>,
    selectedPreferences: List<String>,
    onSelectionChanged: (List<String>) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Button(onClick = { expanded = !expanded }) {
            Text("Select Preferences")
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            allPreferences.forEach { pref ->
                // Is this preference already selected?
                val isSelected = selectedPreferences.contains(pref)

                DropdownMenuItem(
                    text = {
                        Row {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = null // handled in onClick below
                            )
                            Text(pref)
                        }
                    },
                    onClick = {
                        val newSelection = if (isSelected) {
                            selectedPreferences - pref
                        } else {
                            selectedPreferences + pref
                        }
                        onSelectionChanged(newSelection)
                    }
                )
            }
        }
    }
}


@Preview
@Composable
fun ProfileScreenPreview() {
    ProfileScreen()
}