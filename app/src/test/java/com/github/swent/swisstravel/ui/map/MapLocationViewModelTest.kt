package com.github.swent.swisstravel.ui.map

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test

class MapLocationViewModelTest {

  @Test
  fun permissionIsFalseByDefault() = runTest {
    val viewModel = MapLocationViewModel()
    Assert.assertFalse(viewModel.permissionGranted.value)
  }

  @Test
  fun setPermissionGrantedTrueUpdatesState() = runTest {
    val viewModel = MapLocationViewModel()
    viewModel.setPermissionGranted(true)
    Assert.assertTrue(viewModel.permissionGranted.value)
  }

  @Test
  fun setPermissionGrantedFalseUpdatesState() = runTest {
    val viewModel = MapLocationViewModel()
    viewModel.setPermissionGranted(true)
    viewModel.setPermissionGranted(false)
    Assert.assertFalse(viewModel.permissionGranted.value)
  }

  @Test
  fun permissionGrantedFlowEmitsCorrectValues() = runTest {
    val viewModel = MapLocationViewModel()
    viewModel.setPermissionGranted(true)
    val value = viewModel.permissionGranted.first()
    Assert.assertTrue(value)
  }
}
