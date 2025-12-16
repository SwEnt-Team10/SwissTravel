package com.github.swent.swisstravel.ui.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.integerResource
import coil.compose.AsyncImage
import com.github.swent.swisstravel.R

object PhotoGridTestTags {
  /**
   * This function return a test tag for an indexed photo.
   *
   * @Param index the index of the photo
   */
  fun getTestTagForPhoto(index: Int): String = "PhotoIndex$index"
}

/**
 * A shared grid component for displaying photos with optional selection support. Made with the help
 * of AI.
 *
 * @param T The type of the item in the list.
 * @param items The list of items to display.
 * @param modifier The modifier for the grid.
 * @param onClick Callback when an item is clicked. If null, items are not clickable.
 * @param isSelected Callback to determine if an item is selected (shows checkmark overlay).
 * @param modelMapper A function to map the item T to a model supported by Coil (Uri, Bitmap, etc.).
 */
@Composable
fun <T> PhotoGrid(
    items: List<T>,
    modifier: Modifier = Modifier,
    onClick: ((Int) -> Unit)? = null,
    isSelected: ((Int) -> Boolean)? = null,
    modelMapper: (T) -> Any?
) {
  LazyVerticalGrid(
      columns = GridCells.Fixed(integerResource(R.integer.images_on_grid)), modifier = modifier) {
        itemsIndexed(items) { index, item ->
          Box(
              modifier =
                  Modifier.aspectRatio(1f)
                      .then(
                          if (onClick != null) Modifier.clickable { onClick(index) }
                          else Modifier)) {
                AsyncImage(
                    model = modelMapper(item),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier =
                        Modifier.fillMaxSize().testTag(PhotoGridTestTags.getTestTagForPhoto(index)))

                // Selection Overlay (Veil + Checkmark)
                if (isSelected?.invoke(index) == true) {
                  Box(
                      modifier =
                          Modifier.matchParentSize()
                              .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)))
                  Icon(
                      imageVector = Icons.Default.Check,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.primary,
                      modifier =
                          Modifier.matchParentSize()
                              .padding(dimensionResource(R.dimen.check_pading))
                              .wrapContentSize(Alignment.Center))
                }
              }
        }
      }
}
