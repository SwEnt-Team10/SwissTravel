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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.swent.swisstravel.model.user.Preference
import com.github.swent.swisstravel.model.user.displayStringRes

object TripPreferenceIconTestTags {
    const val TRIP_PREFERENCE_ICON = "tripPreferenceIcon"
}

/**
 * A composable that displays a trip preference icon with a border and background.
 *
 * @param preference The trip preference to display.
 * @param modifier The modifier to be applied to the composable.
 */
@Composable
fun TripPreferenceIcon(
    preference: Preference,
    modifier: Modifier = Modifier
) {
    val borderWidth = 1.5.dp
    val shape = RoundedCornerShape(50)
    val containerColor = MaterialTheme.colorScheme.background
    val textColor = MaterialTheme.colorScheme.onBackground
    val borderColor = MaterialTheme.colorScheme.onBackground

    Box(
        modifier = modifier
            .testTag(TripPreferenceIconTestTags.TRIP_PREFERENCE_ICON)
            .border(BorderStroke(borderWidth, borderColor), shape = shape)
            .background(containerColor, shape = shape)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = stringResource(preference.displayStringRes()), color = textColor)
    }
}
/**
 * Preview of the TripPreferenceIcon composable.
 */
@Composable
@Preview
fun TripPreferenceIconPreview() {
    TripPreferenceIcon(preference = Preference.PUBLIC_TRANSPORT)
}
