package com.android.swisstravel.ui

import com.github.swent.swisstravel.ui.map.MapLocationViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class MapLocationViewModelTest {

  @Test
  fun permissionIsFalseByDefault() = runTest {
    val viewModel = MapLocationViewModel()
    assertFalse(viewModel.permissionGranted.value)
  }

  @Test
  fun setPermissionGrantedTrueUpdatesState() = runTest {
    val viewModel = MapLocationViewModel()
    viewModel.setPermissionGranted(true)
    assertTrue(viewModel.permissionGranted.value)
  }

  @Test
  fun setPermissionGrantedFalseUpdatesState() = runTest {
    val viewModel = MapLocationViewModel()
    viewModel.setPermissionGranted(true)
    viewModel.setPermissionGranted(false)
    assertFalse(viewModel.permissionGranted.value)
  }

  @Test
  fun permissionGrantedFlowEmitsCorrectValues() = runTest {
    val viewModel = MapLocationViewModel()
    viewModel.setPermissionGranted(true)
    val value = viewModel.permissionGranted.first()
    assertTrue(value)
  }
}
