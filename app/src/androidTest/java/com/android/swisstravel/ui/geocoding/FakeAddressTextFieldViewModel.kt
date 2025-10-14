package com.android.swisstravel.ui.geocoding

import com.github.swent.swisstravel.model.trip.Coordinate
import com.github.swent.swisstravel.model.trip.Location
import com.github.swent.swisstravel.ui.geocoding.AddressTextFieldState
import com.github.swent.swisstravel.ui.geocoding.AddressTextFieldViewModelContract
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeAddressTextFieldViewModel : AddressTextFieldViewModelContract {

    private val _state = MutableStateFlow(
        AddressTextFieldState(
            locationQuery = "",
            locationSuggestions = listOf(
                Location(Coordinate(46.5197, 6.6323), "Lausanne"),
                Location(Coordinate(46.2044, 6.1432), "Genève"),
                Location(Coordinate(47.3769, 8.5417), "Zurich")
            )
        )
    )

    override val addressState: StateFlow<AddressTextFieldState> = _state

    override fun setLocationQuery(query: String) {
        _state.value = _state.value.copy(locationQuery = query)
    }

    override fun setLocation(location: Location) {
        _state.value = _state.value.copy(selectedLocation = location)
    }
}