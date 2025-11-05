package com.github.swent.swisstravel.ui.geocoding

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
