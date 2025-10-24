package com.github.swent.swisstravel.ui.composable

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.swent.swisstravel.model.user.Preference
import com.github.swent.swisstravel.model.user.displayStringRes
import com.github.swent.swisstravel.model.user.toTestTagString

/** Test tags for the [PreferenceSelector] composable. */
object PreferenceSelectorTestTags {
  const val PREFERENCE_SELECTOR = "preferenceSelector"

  /**
   * Returns the test tag for the given [preference] button.
   *
   * @param preference the preference to get the test tag for
   * @return the test tag for the given preference
   */
  fun getTestTagButton(preference: Preference): String = preference.toTestTagString() + "Button"
}

/**
 * A button that represents a [Preference].
 *
 * @param preference the preference to display
 * @param isChecked whether the preference is checked
 * @param onCheckedChange a function that is called when the user clicks on the button
 */
@Composable
fun PreferenceButton(
    preference: Preference,
    isChecked: Boolean,
    onCheckedChange: (Preference) -> Unit,
) {
  val color =
      if (isChecked) MaterialTheme.colorScheme.primaryContainer
      else MaterialTheme.colorScheme.onBackground
  Button(
      onClick = { onCheckedChange(preference) },
      shape = RoundedCornerShape(50),
      colors =
          ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.onPrimary, contentColor = color),
      border = BorderStroke(Dp.Hairline, color),
      modifier = Modifier.testTag(PreferenceSelectorTestTags.getTestTagButton(preference))) {
        Text(text = stringResource(preference.displayStringRes()))
      }
}

/**
 * A composable that displays a selector of [PreferenceButton]s.
 *
 * @param isChecked a function that returns whether the given preference is checked
 * @param onCheckedChange a function that is called when the user clicks on a preference
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PreferenceSelector(
    isChecked: (Preference) -> Boolean,
    onCheckedChange: (Preference) -> Unit,
) {
  val allPreferences =
      listOf(
          Preference.WELLNESS,
          Preference.SPORTS,
          Preference.HIKE,
          Preference.MUSEUMS,
          Preference.SCENIC_VIEWS,
          Preference.SHOPPING,
          Preference.FOODIE,
          Preference.URBAN,
          Preference.NIGHTLIFE,
          Preference.GROUP,
          Preference.INDIVIDUAL,
          Preference.CHILDREN_FRIENDLY,
          Preference.COUPLE,
          Preference.PUBLIC_TRANSPORT,
          Preference.QUICK)

  FlowRow(
      horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
      verticalArrangement = Arrangement.spacedBy(10.dp),
      modifier =
          Modifier.widthIn(max = 360.dp)
              .wrapContentWidth(Alignment.CenterHorizontally)
              .testTag(PreferenceSelectorTestTags.PREFERENCE_SELECTOR),
      maxItemsInEachRow = 3) {
        for (preference in allPreferences) {
          PreferenceButton(
              preference = preference,
              isChecked = isChecked(preference),
              onCheckedChange = onCheckedChange)
        }
      }
}

/** A preview of the [PreferenceSelector] composable. */
@Preview(showBackground = true)
@Composable
fun PreferenceSelectorPreview() {
  MaterialTheme {
    val selected = remember {
      androidx.compose.runtime.mutableStateOf(listOf(Preference.FOODIE, Preference.SPORTS))
    }

    PreferenceSelector(
        isChecked = { pref -> selected.value.contains(pref) },
        onCheckedChange = { pref ->
          val current = selected.value
          selected.value = if (current.contains(pref)) current - pref else current + pref
        })
  }
}
