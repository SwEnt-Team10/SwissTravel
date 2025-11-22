package com.github.swent.swisstravel.ui.trip.tripinfos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.github.swent.swisstravel.R

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
 * @param title The title of the location.
 * @param timeRange The time range of the location.
 * @param modifier The modifier to apply to the card.
 * @param leadingIcon The leading icon to display on the card.
 */
@Composable
fun StepLocationCard(
    stepNumber: Int,
    title: String,
    timeRange: String,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
  Card(
      modifier =
          modifier
              .fillMaxWidth()
              .padding(
                  horizontal = dimensionResource(R.dimen.step_location_card_horizontal_padding),
                  vertical = dimensionResource(R.dimen.step_location_card_vertical_padding))
              .testTag(StepLocationCardTestTags.CARD),
      shape = RoundedCornerShape(dimensionResource(R.dimen.step_location_card_radius)),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onPrimary)) {
        Row(
            modifier =
                Modifier.fillMaxWidth()
                    .padding(dimensionResource(R.dimen.step_location_card_padding)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start) {

              // Icon on the left (if provided)
              if (leadingIcon != null) {
                Box(
                    modifier =
                        Modifier.padding(
                            end =
                                dimensionResource(R.dimen.step_location_card_horizontal_padding))) {
                      leadingIcon()
                    }
              }

              Column(
                  verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.Start) {

                    // "Step X"
                    Text(
                        text = stringResource(R.string.step_info, stepNumber),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.testTag(StepLocationCardTestTags.STEP_LABEL))

                    Spacer(
                        modifier =
                            Modifier.height(
                                dimensionResource(R.dimen.step_location_card_micro_spacer)))

                    // Title (location / route)
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag(StepLocationCardTestTags.LOCATION_NAME))

                    // Time range
                    if (timeRange.isNotBlank()) {
                      Spacer(
                          modifier =
                              Modifier.height(
                                  dimensionResource(R.dimen.step_location_card_micro_spacer)))
                      Text(
                          text = timeRange,
                          style = MaterialTheme.typography.bodySmall,
                          color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                  }

              Spacer(modifier = Modifier.weight(1f))
            }
      }
}
