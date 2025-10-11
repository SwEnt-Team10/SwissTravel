package com.github.swent.swisstravel.ui.geocoding

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.swent.swisstravel.HttpClientProvider
import com.github.swent.swisstravel.model.map.Location
import com.github.swent.swisstravel.model.map.LocationRepository
import com.github.swent.swisstravel.model.map.NominatimLocationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


data class AddressTextFieldState(
    val selectedLocation: Location? = null,
    val locationQuery: String = "",
    val locationSuggestions: List<Location> = emptyList()
)

class AddressTextFieldViewModel : ViewModel() {
    private val locationRepository: LocationRepository =
        NominatimLocationRepository(HttpClientProvider.client)

    private val _addressState = MutableStateFlow(AddressTextFieldState())
    val addressState: StateFlow<AddressTextFieldState> = _addressState.asStateFlow()

    fun setLocation(location: Location) {
        _addressState.value = _addressState.value.copy(selectedLocation = location, locationQuery = location.name)
    }
    fun setLocationQuery(query: String) {
        _addressState.value = _addressState.value.copy(locationQuery = query)

        if (query.isNotEmpty()) {
            viewModelScope.launch {
                try {
                    val results = locationRepository.search(query)
                    _addressState.value = _addressState.value.copy(locationSuggestions = results)
                } catch (e: Exception) {
                    Log.e("AddToDoViewModel", "Error fetching location suggestions", e)
                    _addressState.value = _addressState.value.copy(locationSuggestions = emptyList())
                }
            }
        } else {
            _addressState.value = _addressState.value.copy(locationSuggestions = emptyList())
        }
    }
}