package com.github.swent.swisstravel.ui.mytrips.tripinfos

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.swent.swisstravel.ui.map.NavigationMap
import com.github.swent.swisstravel.ui.map.NavigationMapScreen

@Composable
fun TripInfoZoomableMap(onFullscreenClick: () -> Unit) {
    Scaffold { pd ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(pd)
        ) {
            NavigationMap()
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomEnd
            ) {
                IconButton(
                    onClick = onFullscreenClick,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Fullscreen,
                        contentDescription = "Basculer en plein écran"
                    )
                }
            }
        }
    }
}
