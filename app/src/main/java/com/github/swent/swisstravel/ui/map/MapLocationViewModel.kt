package com.github.swent.swisstravel.ui.map

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.swent.swisstravel.model.map.PermissionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MapLocationViewModel(activity: Activity?) : ViewModel() {
  private val permissionHandler = activity?.let { PermissionHandler(it) }

  private val _permissionGranted =
      MutableStateFlow(permissionHandler?.arePermissionsGranted() ?: false)
  val permissionGranted: StateFlow<Boolean> = _permissionGranted

  fun requestPermission(requestCode: Int) {
    permissionHandler?.requestPermission(requestCode)
    viewModelScope.launch {
      while (permissionHandler?.arePermissionsGranted() != true) {
        kotlinx.coroutines.delay(200)
      }
      _permissionGranted.value = true
    }
  }

  fun isActivityNull() = permissionHandler == null
}
