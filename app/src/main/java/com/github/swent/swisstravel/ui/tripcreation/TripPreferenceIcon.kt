package com.github.swent.swisstravel.ui.tripcreation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.model.user.Preference
import com.github.swent.swisstravel.model.user.displayStringRes
import com.github.swent.swisstravel.model.user.toTestTagString

/** Test tags for UI tests to identify components. */
object TripPreferenceIconTestTags {
  const val TRIP_PREFERENCE_ICON = "tripPreferenceIcon"

  fun getTestTag(preference: Preference): String {
    return TRIP_PREFERENCE_ICON + preference.toTestTagString()
  }
}

/**
 * A composable that displays a trip preference icon with a border and background.
 *
 * @param preference The trip preference to display.
 * @param modifier The modifier to be applied to the composable.
 */
@Composable
fun TripPreferenceIcon(preference: Preference, modifier: Modifier = Modifier) {
  val borderWidth = dimensionResource(R.dimen.trip_preference_border_width)
  val shape = RoundedCornerShape(50)
  val containerColor = MaterialTheme.colorScheme.background
  val textColor = MaterialTheme.colorScheme.onBackground
  val borderColor = MaterialTheme.colorScheme.onBackground

  Box(
      modifier =
          modifier
              .testTag(TripPreferenceIconTestTags.getTestTag(preference))
              .border(BorderStroke(borderWidth, borderColor), shape = shape)
              .background(containerColor, shape = shape)
              .padding(
                  horizontal = dimensionResource(R.dimen.trip_preference_icon_horizontal_padding),
                  vertical = dimensionResource(R.dimen.trip_preference_icon_vertical_padding)),
      contentAlignment = Alignment.Center) {
        Text(text = stringResource(preference.displayStringRes()), color = textColor)
      }
}
/** Preview of the TripPreferenceIcon composable. */
@Composable
@Preview
fun TripPreferenceIconPreview() {
  TripPreferenceIcon(preference = Preference.PUBLIC_TRANSPORT)
}
