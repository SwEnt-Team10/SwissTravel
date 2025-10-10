package com.github.swent.swisstravel.ui.mytrips

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

object TripElementTestTags {
  fun getTestTagForTrip(trip: Trip): String = "trip${trip.uid}"
}

@Composable
fun TripElement(trip: Trip, onClick: () -> Unit) {
  Card(
      modifier =
          Modifier.testTag(TripElementTestTags.getTestTagForTrip(trip))
              .clickable(onClick = onClick),
  ) {
    Column(modifier = Modifier) {}
  }
}
