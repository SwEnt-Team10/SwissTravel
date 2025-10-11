package com.github.swent.swisstravel.ui.map

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** ViewModel to manage location permission state for the map screen. */
class MapLocationViewModel : ViewModel() {
  private val _permissionGranted = MutableStateFlow(false)
  val permissionGranted: StateFlow<Boolean> = _permissionGranted

  /**
   * Update the permission state.
   *
   * @param granted True if location permission is granted, false otherwise.
   */
  fun setPermissionGranted(granted: Boolean) {
    _permissionGranted.value = granted
  }
}
