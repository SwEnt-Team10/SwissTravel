package com.github.swent.swisstravel.ui.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.swent.swisstravel.R

object TravelersSelectorTestTag {
  const val ADULT_INCREMENT = "adultIncrement"
  const val ADULT_DECREMENT = "adultDecrement"
  const val CHILD_INCREMENT = "childIncrement"
  const val CHILD_DECREMENT = "childDecrement"
}

/**
 * A composable that displays a selector for the number of adults and children.
 *
 * @param adults The current number of adults.
 * @param children The current number of children.
 * @param onAdultsChange A callback that is invoked when the number of adults changes.
 * @param onChildrenChange A callback that is invoked when the number of children changes.
 * @param modifier The modifier to apply to this composable.
 */
@Composable
fun TravelersSelector(
    adults: Int,
    children: Int,
    onAdultsChange: (Int) -> Unit,
    onChildrenChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
  Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.Center) {
    Counter(
        label = stringResource(R.string.nb_adults),
        count = adults,
        onIncrement = { onAdultsChange(adults + 1) },
        onDecrement = { if (adults > 1) onAdultsChange(adults - 1) },
        enableButton = adults > 1)

    Spacer(modifier = Modifier.height(50.dp))

    Counter(
        label = stringResource(R.string.nb_children),
        count = children,
        onIncrement = { onChildrenChange(children + 1) },
        onDecrement = { if (children > 0) onChildrenChange(children - 1) },
        enableButton = children > 0)
  }
}
