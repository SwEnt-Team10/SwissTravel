package com.github.swent.swisstravel.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage


@Composable
fun ProfileScreen(
    profileScreenViewModel: ProfileScreenViewModel = ProfileScreenViewModel()
) {
    val context = LocalContext.current
    val uiState = profileScreenViewModel.uiState.collectAsState().value

    LaunchedEffect(uiState.errorMsg) {
        val msg = uiState.errorMsg
        if (!msg.isNullOrBlank()) {
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            profileScreenViewModel.clearErrorMsg()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "My Profile",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )

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

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Travel Preferences",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        MultiSelectDropdown(
            allPreferences = profileScreenViewModel.allPreferences,
            selectedPreferences = uiState.selectedPreferences,
            onSelectionChanged = { newSelection -> profileScreenViewModel.savePreferences(newSelection) }
        )

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Personal Informations",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))



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
                val isSelected = selectedPreferences.contains(pref)

                DropdownMenuItem(
                    text = {
                        Row {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = null
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