package com.github.swent.swisstravel.ui.composable

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
      colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = color),
      border = BorderStroke(Dp.Hairline, color),
      modifier = Modifier.testTag(PreferenceSelectorTestTags.getTestTagButton(preference))) {
        Text(text = stringResource(preference.displayStringRes()))
      }
}

/**
 * A row of [PreferenceButton]s.
 *
 * @param prefs the list of preferences to display
 * @param isChecked a function that returns whether the given preference is checked
 * @param onCheckedChange a function that is called when the user clicks on a preference
 */
@Composable
fun PreferenceSelectorRow(
    prefs: List<Preference>,
    isChecked: (Preference) -> Boolean,
    onCheckedChange: (Preference) -> Unit,
) {
  Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
    for (preference in prefs) {
      PreferenceButton(
          preference = preference,
          isChecked = isChecked(preference),
          onCheckedChange = onCheckedChange,
      )
    }
  }
}

/**
 * A composable that displays a selector of [PreferenceButton]s.
 *
 * @param isChecked a function that returns whether the given preference is checked
 * @param onCheckedChange a function that is called when the user clicks on a preference
 */
@Composable
fun PreferenceSelector(
    isChecked: (Preference) -> Boolean,
    onCheckedChange: (Preference) -> Unit,
) {
  Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(10.dp),
      modifier = Modifier.testTag(PreferenceSelectorTestTags.PREFERENCE_SELECTOR)) {
        val firstRow = listOf(Preference.WELLNESS, Preference.SPORTS, Preference.HIKE)
        val secondRow = listOf(Preference.MUSEUMS, Preference.SCENIC_VIEWS)
        val thirdRow = listOf(Preference.SHOPPING, Preference.FOODIE)
        val fourthRow = listOf(Preference.URBAN, Preference.NIGHTLIFE)
        val fifthRow = listOf(Preference.GROUP, Preference.INDIVIDUAL)
        val sixthRow = listOf(Preference.CHILDREN_FRIENDLY, Preference.COUPLE)
        val seventhRow = listOf(Preference.PUBLIC_TRANSPORT, Preference.QUICK)

        PreferenceSelectorRow(
            prefs = firstRow, isChecked = isChecked, onCheckedChange = onCheckedChange)
        PreferenceSelectorRow(
            prefs = secondRow, isChecked = isChecked, onCheckedChange = onCheckedChange)
        PreferenceSelectorRow(
            prefs = thirdRow, isChecked = isChecked, onCheckedChange = onCheckedChange)
        PreferenceSelectorRow(
            prefs = fourthRow, isChecked = isChecked, onCheckedChange = onCheckedChange)
        PreferenceSelectorRow(
            prefs = fifthRow, isChecked = isChecked, onCheckedChange = onCheckedChange)
        PreferenceSelectorRow(
            prefs = sixthRow, isChecked = isChecked, onCheckedChange = onCheckedChange)
        PreferenceSelectorRow(
            prefs = seventhRow, isChecked = isChecked, onCheckedChange = onCheckedChange)
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
