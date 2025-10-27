package com.github.swent.swisstravel.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

/** Object containing test tags for the [TopBar] composable. */
object TopBarTestTags {
  /**
   * Create the test tag for the given [title].
   *
   * @param title the title of the top bar
   * @return the test tag for the given title
   */
  fun getTestTagTitle(title: String): String {
    return "TopBarTitle$title"
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "",
) {
  TopAppBar(
      title = {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.testTag(TopBarTestTags.getTestTagTitle(title)))
      },
      navigationIcon = {
        IconButton(
            onClick = onClick, modifier = Modifier.testTag(NavigationTestTags.TOP_BAR_BUTTON)) {
              Icon(
                  imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                  contentDescription = "Back Arrow",
                  tint = MaterialTheme.colorScheme.onBackground)
            }
      },
      modifier = modifier.testTag(NavigationTestTags.TOP_BAR))
}
