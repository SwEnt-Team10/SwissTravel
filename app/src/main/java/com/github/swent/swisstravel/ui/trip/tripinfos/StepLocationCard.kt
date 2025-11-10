package com.github.swent.swisstravel.ui.trip.tripinfos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.model.trip.Location

/** Test tags for StepLocationCard composable. */
object StepLocationCardTestTags {
  const val CARD = "StepLocationCard:Card"
  const val STEP_LABEL = "StepLocationCard:StepLabel"
  const val LOCATION_NAME = "StepLocationCard:LocationName"
}
/**
 * A card displaying the step number and location name for a trip step.
 *
 * @param stepNumber The step number.
 * @param location The location of the step.
 */
@Composable
fun StepLocationCard(stepNumber: Int, location: Location) {
  Card(
      modifier =
          Modifier.fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 8.dp)
              .testTag(StepLocationCardTestTags.CARD),
      shape = RoundedCornerShape(8.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onPrimary)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start) {
              Column(
                  verticalArrangement = Arrangement.Center,
                  horizontalAlignment = Alignment.Start,
                  modifier = Modifier) {
                    Text(
                        text = "${stringResource(R.string.step_info)} $stepNumber",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.testTag(StepLocationCardTestTags.STEP_LABEL))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = location.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag(StepLocationCardTestTags.LOCATION_NAME))
                  }

              Spacer(modifier = Modifier.weight(1f))

              Spacer(modifier = Modifier.width(24.dp))
            }
      }
}
