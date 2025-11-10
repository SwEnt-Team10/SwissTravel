package com.github.swent.swisstravel.ui.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.ui.trips.TripSortType

object SortMenuTestTags {
  const val SORT_DROPDOWN_MENU = "SortMenuDropdownMenu"

  fun getTestTagSortOption(type: TripSortType): String {
    return "SortMenuSortOption${type.name}"
  }
}

/**
 * A composable that displays a sort icon button with a dropdown menu for sorting options.
 *
 * @param onClickDropDownMenu Callback when a sorting option is selected from the dropdown menu.
 */
@Composable
fun SortMenu(onClickDropDownMenu: (TripSortType) -> Unit = {}) {
  var expanded by remember { mutableStateOf(false) }
  Box {
    IconButton(
        onClick = { expanded = !expanded },
        modifier = Modifier.testTag(SortMenuTestTags.SORT_DROPDOWN_MENU)) {
          Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = stringResource(R.string.sort))
        }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false },
        modifier = Modifier.background(MaterialTheme.colorScheme.onPrimary)) {
          val sortOptions =
              listOf(
                  TripSortType.START_DATE_ASC to R.string.start_date_asc,
                  TripSortType.START_DATE_DESC to R.string.start_date_desc,
                  TripSortType.END_DATE_ASC to R.string.end_date_asc,
                  TripSortType.END_DATE_DESC to R.string.end_date_desc,
                  TripSortType.NAME_ASC to R.string.name_asc,
                  TripSortType.NAME_DESC to R.string.name_desc,
                  TripSortType.FAVORITES_FIRST to R.string.favorites_first)
          sortOptions.forEach { (type, resId) ->
            DropdownMenuItem(
                modifier =
                    Modifier.testTag(SortMenuTestTags.getTestTagSortOption(type)).semantics(
                        mergeDescendants = true) {},
                text = { Text(stringResource(resId)) },
                onClick = {
                  onClickDropDownMenu(type)
                  expanded = false
                })
          }
        }
  }
}
