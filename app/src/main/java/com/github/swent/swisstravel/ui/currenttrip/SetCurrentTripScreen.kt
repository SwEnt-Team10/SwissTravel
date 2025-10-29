package com.github.swent.swisstravel.ui.currenttrip

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.ui.mytrips.MyTripsViewModel
import com.github.swent.swisstravel.ui.navigation.TopBar

object SetCurrentTripTestTags {
  const val SET_CURRENT_TRIP_SCREEN = "setCurrentTripScreen"
}

@Composable
fun SetCurrentTripScreen(viewModel: MyTripsViewModel = viewModel(), onPrevious: () -> Unit = {}) {
  Scaffold(
      topBar = { TopBar(onClick = onPrevious, title = stringResource(R.string.select_a_trip)) },
      modifier = Modifier.testTag(SetCurrentTripTestTags.SET_CURRENT_TRIP_SCREEN)) { pd ->
        Box(modifier = Modifier.padding(pd)) // placeholder waiting for the sorting of lists
  }
}
