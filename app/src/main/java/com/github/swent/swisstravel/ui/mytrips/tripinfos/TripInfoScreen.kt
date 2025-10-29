package com.github.swent.swisstravel.ui.mytrips.tripinfos

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun TripInfoScreen() {
    Scaffold {pd ->
        Text(text = "Salut",
            modifier = Modifier.padding(pd)
        )
    }
}