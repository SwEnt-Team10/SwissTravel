package com.github.swent.swisstravel.ui.geocoding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.github.swent.swisstravel.model.map.LocationRepository
import com.github.swent.swisstravel.model.map.MySwitzerlandLocationRepository

/**
 * A [ViewModel] that manages the state for a location autocomplete text field, specifically for
 * destinations, using the [MySwitzerlandLocationRepository].
 *
 * This class extends [AddressTextFieldViewModel] and provides the specific location repository for
 * fetching destination suggestions.
 */
class DestinationTextFieldViewModel(locationRepository: LocationRepository) :
    AddressTextFieldViewModel(MySwitzerlandLocationRepository())

// The following code and description was done by chatGPT
/**
 * Factory class for creating instances of [DestinationTextFieldViewModel] with a custom
 * constructor.
 *
 * In Jetpack Compose (or any Android ViewModel scenario), ViewModels are typically instantiated by
 * the framework using a zero-argument constructor. If your ViewModel requires constructor
 * parameters (such as a repository), you need to provide a [ViewModelProvider.Factory] to instruct
 * the framework how to create it.
 *
 * This factory ensures that each instance of [DestinationTextFieldViewModel] receives a
 * [LocationRepository] when it is created.
 *
 * @property locationRepository The repository to be injected into the
 *   [DestinationTextFieldViewModel].
 *
 * Usage in Compose:
 * ```
 * val destinationVm = viewModel<DestinationTextFieldViewModel>(
 *     key = "destination_$index",
 *     factory = DestinationTextFieldViewModelFactory(MySwitzerlandLocationRepository())
 * )
 * ```
 *
 * This ensures:
 * 1. Each destination input field gets its own ViewModel instance.
 * 2. The ViewModel has access to the necessary repository for fetching location suggestions.
 */
class DestinationTextFieldViewModelFactory(private val locationRepository: LocationRepository) :
    ViewModelProvider.Factory {

  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    if (modelClass.isAssignableFrom(DestinationTextFieldViewModel::class.java)) {
      @Suppress("UNCHECKED_CAST") return DestinationTextFieldViewModel(locationRepository) as T
    }
    throw IllegalArgumentException("Unknown ViewModel class")
  }
}
