package com.github.swent.swisstravel.ui.geocoding

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.swent.swisstravel.HttpClientProvider
import com.github.swent.swisstravel.model.map.LocationRepository
import com.github.swent.swisstravel.model.map.NominatimLocationRepository
import com.github.swent.swisstravel.model.trip.Location
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Tags used for logging within the AddressTextFieldViewModel. */
object AddressTextFieldViewModelTags {
  const val TAG = "AddressTextFieldViewModel"
}
/**
 * ViewModel for managing the state of an address text field with autocomplete functionality.
 *
 * This ViewModel interacts with a [LocationRepository] to fetch location suggestions based on user
 * input. It maintains the current state of the text field, including the selected location, the
 * current query, and a list of location suggestions.
 */
interface AddressTextFieldViewModelContract {
  val addressState: StateFlow<AddressTextFieldState>
  /**
   * Sets the selected location and updates the location query accordingly.
   *
   * @param location The location to set as selected.
   */
  fun setLocation(location: Location)

  /**
   * Updates the location query and fetches new location suggestions based on the query.
   *
   * @param query The new location query input by the user.
   */
  fun setLocationQuery(query: String)
}
/**
 * Data class representing the state of the address text field.
 *
 * @property selectedLocation The currently selected location, if any.
 * @property locationQuery The current text input in the address field.
 * @property locationSuggestions A list of location suggestions based on the current query.
 */
data class AddressTextFieldState(
    val selectedLocation: Location? = null,
    val locationQuery: String = "",
    val locationSuggestions: List<Location> = emptyList()
)

/**
 * Implementation of [AddressTextFieldViewModelContract] using a [LocationRepository].
 *
 * This ViewModel fetches location suggestions as the user types in the address text field and
 * updates the state accordingly.
 *
 * @param locationRepository The repository used to fetch location suggestions.
 */
open class AddressTextFieldViewModel(
    private val locationRepository: LocationRepository =
        NominatimLocationRepository(HttpClientProvider.client)
) : ViewModel(), AddressTextFieldViewModelContract {

  private val _addressState = MutableStateFlow(AddressTextFieldState())
  override val addressState: StateFlow<AddressTextFieldState> = _addressState.asStateFlow()

  override fun setLocation(location: Location) {
    _addressState.value =
        _addressState.value.copy(selectedLocation = location, locationQuery = location.name)
  }

  override fun setLocationQuery(query: String) {
    _addressState.value = _addressState.value.copy(locationQuery = query)

    if (query.isNotEmpty()) {
      viewModelScope.launch {
        try {
          val results = locationRepository.search(query)
          _addressState.value = _addressState.value.copy(locationSuggestions = results)
        } catch (e: Exception) {
          // Log, doesn't need a string resource
          Log.e(AddressTextFieldViewModelTags.TAG, "Error fetching location suggestions", e)

          _addressState.value = _addressState.value.copy(locationSuggestions = emptyList())
        }
      }
    } else {
      _addressState.value = _addressState.value.copy(locationSuggestions = emptyList())
    }
  }
}
