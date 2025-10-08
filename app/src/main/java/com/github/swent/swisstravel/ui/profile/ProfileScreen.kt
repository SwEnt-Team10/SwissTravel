package com.github.swent.swisstravel.ui.profile

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage

object ProfileScreenTestTags {
  const val PROFILE_PIC = "profilePic"
  const val DROPDOWN_PREFERENCES = "dropdownMenu"
  const val DISPLAY_NAME = "displayName"
  const val EMAIL = "email"
  const val GREETING = "greeting"
}

@Composable
fun ProfileScreen(profileScreenViewModel: ProfileScreenViewModel = viewModel()) {
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
      modifier = Modifier.fillMaxSize().padding(20.dp),
      horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "My Profile",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp))

        AsyncImage(
            model = uiState.profilePicUrl,
            contentDescription = "Profile picture",
            modifier =
                Modifier.size(120.dp).clip(CircleShape).testTag(ProfileScreenTestTags.PROFILE_PIC))

        Text(
            text = "Hello, ${uiState.name}!",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(top = 24.dp).testTag(ProfileScreenTestTags.GREETING))

        Spacer(modifier = Modifier.height(36.dp))

        Text(
            text = "Travel Preferences",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))

        MultiSelectDropdown(
            allPreferences = profileScreenViewModel.allPreferences,
            selectedPreferences = uiState.selectedPreferences,
            onSelectionChanged = { newSelection ->
              profileScreenViewModel.savePreferences(newSelection)
            })

        Spacer(modifier = Modifier.height(36.dp))

        Text(
            text = "Personal Information",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp))

        InfoItem(
            label = "Display Name",
            value = uiState.name,
            modifier = Modifier.testTag(ProfileScreenTestTags.DISPLAY_NAME))
        InfoItem(
            label = "E-Mail",
            value = uiState.email,
            modifier = Modifier.testTag(ProfileScreenTestTags.EMAIL))
      }
}

@Composable
fun InfoItem(label: String, value: String, modifier: Modifier) {
  Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
    Text(
        text = label,
        style =
            MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)))
    Text(
        text = value.ifBlank { "-" },
        style = MaterialTheme.typography.bodyLarge,
        modifier = modifier)
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
    Button(
        onClick = { expanded = !expanded },
        modifier = Modifier.testTag(ProfileScreenTestTags.DROPDOWN_PREFERENCES)) {
          Text("Select Preferences")
        }

    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
      allPreferences.forEach { pref ->
        val isSelected = selectedPreferences.contains(pref)

        DropdownMenuItem(
            text = {
              Row {
                Checkbox(checked = isSelected, onCheckedChange = null)
                Text(pref)
              }
            },
            onClick = {
              val newSelection =
                  if (isSelected) {
                    selectedPreferences - pref
                  } else {
                    selectedPreferences + pref
                  }
              onSelectionChanged(newSelection)
            })
      }
    }
  }
}
