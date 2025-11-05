package com.github.swent.swisstravel.ui.geocoding

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.swent.swisstravel.model.map.LocationRepository
import com.github.swent.swisstravel.model.map.MySwitzerlandLocationRepository
import com.github.swent.swisstravel.model.trip.Location
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * A [ViewModel] that manages the state for a location autocomplete text field.
 *
 * This class implements the [AddressTextFieldViewModelContract] and is responsible for:
 * - Holding the current state of the text field, including the user's query and the selected
 *   location.
 * - Fetching location suggestions from a [LocationRepository] as the user types.
 * - Exposing the state to the UI via a [StateFlow].
 *
 * @param locationRepository The data source for fetching location suggestions. Defaults to
 *   [MySwitzerlandLocationRepository], but can be replaced for testing purposes.
 */
class DestinationTextFieldViewModel(
    private val locationRepository: LocationRepository = MySwitzerlandLocationRepository()
) : ViewModel(), AddressTextFieldViewModelContract {

  /** The private, mutable state holder for the address text field's UI state. */
  private val _addressState = MutableStateFlow(AddressTextFieldState())

  /**
   * The public, read-only [StateFlow] representing the current state of the address text field. UI
   * components should observe this flow to react to state changes.
   */
  override val addressState: StateFlow<AddressTextFieldState> = _addressState.asStateFlow()

  /**
   * Sets the selected location and updates the text field's query to match the location's name.
   *
   * This is typically called when a user selects a suggestion from the dropdown list.
   *
   * @param location The [Location] object that was selected.
   */
  override fun setLocation(location: Location) {
    _addressState.value =
        _addressState.value.copy(selectedLocation = location, locationQuery = location.name)
  }

  /**
   * Updates the location query based on user input and triggers a search for new suggestions.
   *
   * If the query is not empty, it launches a coroutine in the [viewModelScope] to fetch location
   * suggestions from the [locationRepository]. If the query is empty, it clears the existing
   * suggestions. Any errors during the fetch are logged, and the suggestions are cleared to prevent
   * displaying stale or incorrect data.
   *
   * @param query The new text entered by the user in the text field.
   */
  override fun setLocationQuery(query: String) {
    _addressState.value = _addressState.value.copy(locationQuery = query)

    if (query.isNotEmpty()) {
      viewModelScope.launch {
        try {
          val results = locationRepository.search(query)
          _addressState.value = _addressState.value.copy(locationSuggestions = results)
        } catch (e: Exception) {
          // Log the error; a user-facing message is not critical for suggestions.
          Log.e(AddressTextFieldViewModelTags.TAG, "Error fetching location suggestions", e)
          _addressState.value = _addressState.value.copy(locationSuggestions = emptyList())
        }
      }
    } else {
      // Clear suggestions if the query is empty
      _addressState.value = _addressState.value.copy(locationSuggestions = emptyList())
    }
  }
}
