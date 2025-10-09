package com.github.swent.swisstravel.ui.profile

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.github.swent.swisstravel.ui.navigation.BottomNavigationMenu
import com.github.swent.swisstravel.ui.navigation.NavigationActions
import com.github.swent.swisstravel.ui.navigation.NavigationTestTags
import com.github.swent.swisstravel.ui.navigation.Tab
import androidx.compose.foundation.verticalScroll
import com.github.swent.swisstravel.ui.theme.onSecondaryContainerLight
import com.github.swent.swisstravel.ui.theme.secondaryContainerLight

object ProfileScreenTestTags {
    const val PROFILE_PIC = "profilePic"
    const val PREFERENCES_LIST = "preferencesList"
    const val DISPLAY_NAME = "displayName"
    const val EMAIL = "email"
    const val GREETING = "greeting"
    const val PERSONAL_INFO = "personalInfo"
    const val PREFERENCES = "preferences"
}

@Composable
fun ProfileScreen(
    profileScreenViewModel: ProfileScreenViewModel = viewModel(),
    navigationActions: NavigationActions? = null
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

  Scaffold(
      topBar = {
          Text(text="My Profile", style = MaterialTheme.typography.headlineSmall,
              modifier = Modifier.padding(bottom = 16.dp))
      },
      bottomBar = {
        BottomNavigationMenu(
            selectedTab = Tab.Profile,
            onTabSelected = { tab -> navigationActions?.navigateTo(tab.destination) },
            modifier = Modifier.testTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU))
      },
      content = { pd ->
        ProfileScreenContent(
            uiState = uiState,
            profileScreenViewModel = profileScreenViewModel,
            modifier = Modifier.padding(pd))
      })
}

@Composable
private fun ProfileScreenContent(
    uiState: ProfileScreenUIState,
    profileScreenViewModel: ProfileScreenViewModel,
    modifier: Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = uiState.profilePicUrl,
            contentDescription = "Profile picture",
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .testTag(ProfileScreenTestTags.PROFILE_PIC)
        )

        Text(
            text = "Hi, ${uiState.name.ifBlank { "User" }}!",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(top = 12.dp).testTag(ProfileScreenTestTags.GREETING)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Personal Info Section
        InfoSection(title = "Personal Information", modifier = Modifier.testTag(ProfileScreenTestTags.PERSONAL_INFO)) {
            InfoItem(label = "Name", value = uiState.name, modifier = Modifier.testTag(ProfileScreenTestTags.DISPLAY_NAME))
            InfoItem(label = "Email", value = uiState.email, modifier = Modifier.testTag(ProfileScreenTestTags.EMAIL))
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Travel Preferences Section
        InfoSection(title = "Travel Preferences", modifier = Modifier.testTag(ProfileScreenTestTags.PREFERENCES_LIST)) {
            profileScreenViewModel.allPreferences.forEach { pref ->
                val isSelected = uiState.selectedPreferences.contains(pref)
                PreferenceToggle(
                    title = pref,
                    checked = isSelected,
                    onCheckedChange = { checked ->
                        val newSelection =
                            if (checked) uiState.selectedPreferences + pref
                            else uiState.selectedPreferences - pref
                        profileScreenViewModel.savePreferences(newSelection)
                    },
                    modifier = Modifier.testTag(ProfileScreenTestTags.PREFERENCES)
                )
            }
        }
    }
}

@Composable
fun InfoSection(
    title: String,
    modifier : Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(secondaryContainerLight)
            .padding(16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            color = onSecondaryContainerLight,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        content()
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
fun PreferenceToggle(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier : Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface
            )
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
