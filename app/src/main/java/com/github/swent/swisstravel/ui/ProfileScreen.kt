package com.github.swent.swisstravel.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
        // Profile Picture
        AsyncImage(
            model = uiState.profilePicUrl,
            contentDescription = "Profile picture",
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
        )

        Text(
            text = uiState.name,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(top = 16.dp))
    }
}

@Preview
@Composable
fun ProfileScreenPreview() {
    ProfileScreen()
}