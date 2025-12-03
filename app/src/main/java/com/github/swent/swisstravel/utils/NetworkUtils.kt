package com.github.swent.swisstravel.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/**
 * Utility object for handling network connectivity checks. This object was written with the help of
 * AI.
 */
object NetworkUtils {

  /**
   * Checks if the device currently has an active network connection capable of accessing the
   * internet.
   *
   * This method retrieves the [ConnectivityManager] system service and checks the
   * [NetworkCapabilities] of the currently active network. It specifically looks for the
   * [NetworkCapabilities.NET_CAPABILITY_INTERNET] capability, which validates that the network is
   * configured to reach the internet.
   *
   * Note: This requires the `android.permission.ACCESS_NETWORK_STATE` permission in the Manifest.
   *
   * @param context The [Context] used to retrieve the connectivity system service.
   * @return `true` if the device has an active network with verified internet capabilities; `false`
   *   otherwise.
   */
  fun isOnline(context: Context): Boolean {
    val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    // Check if there is an active network
    val network = connectivityManager.activeNetwork ?: return false

    // Get capabilities for the active network
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

    // Check if the network has the INTERNET capability
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
  }
}
