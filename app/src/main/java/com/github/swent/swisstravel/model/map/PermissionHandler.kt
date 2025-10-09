package com.github.swent.swisstravel.model.map

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * A utility class to handle location permissions in an Android activity.
 *
 * This class provides methods to check if the necessary location permissions are granted and to
 * request them if they are not. It simplifies permission management for activities that require
 * access to fine location data.
 *
 * @param activity The activity context used to check and request permissions.
 */
class PermissionHandler(private val activity: Activity) {
    /**
     * Checks if the ACCESS_FINE_LOCATION permission is granted.
     *
     * @return `true` if the permission is granted, `false` otherwise.
     */
  fun arePermissionsGranted(): Boolean {
    return ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED
  }

  /**
   * Requests the ACCESS_FINE_LOCATION permission from the user.
   *
   * @param requestCode An integer request code that will be returned in the
   *   [Activity.onRequestPermissionsResult] callback when the user responds to the permission
   *   request.
   */
  fun requestPermission(requestCode: Int) {
    ActivityCompat.requestPermissions(
        activity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), requestCode)
  }
}
