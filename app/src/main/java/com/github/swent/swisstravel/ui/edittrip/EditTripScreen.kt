import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.model.user.Preference
import com.github.swent.swisstravel.ui.composable.PreferenceSelector
import com.github.swent.swisstravel.ui.composable.PreferenceToggle
import com.github.swent.swisstravel.ui.composable.TravelersSelector
import com.github.swent.swisstravel.ui.edittrip.EditTripScreenViewModel
import com.github.swent.swisstravel.ui.navigation.NavigationTestTags
import com.github.swent.swisstravel.ui.navigation.TopBarTestTags

object EditTripScreenTestTags {
  const val SCREEN = "editTripScreen"
  const val LOADING = "editTripLoading"
  const val TRIP_NAME = "editTripName"
  const val CONFIRM_TOP_BAR = "editTripConfirmTopBar"
  const val CONFIRM_BOTTOM_BAR = "editTripConfirmBottomBar"
  const val DELETE = "editTripDelete"
  const val DELETE_DIALOG = "editTripDeleteDialog"
  const val DELETE_CONFIRM = "editTripDeleteConfirm"
  const val DELETE_CANCEL = "editTripDeleteCancel"
}

/**
 * Screen for editing a trip.
 *
 * @param tripId The ID of the trip to edit.
 * @param onBack Called when the back button is pressed.
 * @param onSaved Called when the trip is saved.
 * @param onDelete Called when the trip is deleted.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTripScreen(
    tripId: String,
    editTripViewModel: EditTripScreenViewModel = viewModel(),
    onBack: () -> Unit,
    onSaved: () -> Unit,
    onDelete: () -> Unit,
) {
  val context = LocalContext.current
  val state by editTripViewModel.state.collectAsState()

  var showDeleteDialog by remember { mutableStateOf(false) }

  LaunchedEffect(tripId) { editTripViewModel.loadTrip(tripId) }

  LaunchedEffect(state.errorMsg) {
    state.errorMsg?.let {
      Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
      editTripViewModel.clearErrorMsg()
    }
  }

  Scaffold(
      modifier = Modifier.testTag(EditTripScreenTestTags.SCREEN),
      topBar = {
        TopAppBar(
            title = {
              Text(
                  text = stringResource(R.string.edit_trip),
                  style = MaterialTheme.typography.titleLarge,
                  color = MaterialTheme.colorScheme.onBackground,
                  modifier =
                      Modifier.testTag(
                          TopBarTestTags.getTestTagTitle(stringResource(R.string.edit_trip))))
            },
            navigationIcon = {
              IconButton(
                  onClick = onBack,
                  modifier = Modifier.testTag(NavigationTestTags.TOP_BAR_BUTTON)) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground)
                  }
            },
            actions = {
              Button(
                  onClick = {
                    editTripViewModel.save()
                    Toast.makeText(context, R.string.trip_saved, Toast.LENGTH_SHORT).show()
                    onSaved()
                  },
                  enabled = !state.isLoading,
                  modifier = Modifier.testTag(EditTripScreenTestTags.CONFIRM_TOP_BAR),
              ) {
                Text(stringResource(R.string.confirm_changes))
              }
            },
            modifier = Modifier.testTag(NavigationTestTags.TOP_BAR))
      },
      bottomBar = {
        Surface(tonalElevation = 2.dp) {
          Box(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
              contentAlignment = Alignment.Center) {
                Button(
                    onClick = {
                      editTripViewModel.save()
                      Toast.makeText(context, R.string.trip_saved, Toast.LENGTH_SHORT).show()
                      onSaved()
                    },
                    enabled = !state.isLoading,
                    shape = RoundedCornerShape(28.dp),
                    modifier =
                        Modifier.fillMaxWidth(0.9f)
                            .testTag(EditTripScreenTestTags.CONFIRM_BOTTOM_BAR)) {
                      Icon(Icons.Filled.Edit, contentDescription = null)
                      Spacer(Modifier.width(8.dp))
                      Text(stringResource(R.string.confirm_changes))
                    }
              }
        }
      }) { inner ->
        if (state.isLoading) {
          Box(
              Modifier.fillMaxSize().padding(inner).testTag(EditTripScreenTestTags.LOADING),
              contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
              }
          return@Scaffold
        }

        Column(
            modifier =
                Modifier.padding(inner)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)) {
              SectionHeader(stringResource(R.string.name))
              OutlinedTextField(
                  value = state.tripName,
                  onValueChange = editTripViewModel::editTripName,
                  modifier = Modifier.fillMaxWidth().testTag(EditTripScreenTestTags.TRIP_NAME),
                  shape = RoundedCornerShape(12.dp),
                  singleLine = true)

              SectionHeader(stringResource(R.string.travelers))
              TravelersSelector(
                  adults = state.adults,
                  children = state.children,
                  onAdultsChange = editTripViewModel::setAdults,
                  onChildrenChange = editTripViewModel::setChildren)

              SectionHeader(stringResource(R.string.preferences))
              PreferenceSelector(
                  isChecked = { pref -> state.selectedPrefs.contains(pref) },
                  onCheckedChange = editTripViewModel::togglePref)

              PreferenceToggle(
                  label = stringResource(R.string.handicapped_traveler),
                  value = state.selectedPrefs.contains(Preference.WHEELCHAIR_ACCESSIBLE),
                  onValueChange = {
                    editTripViewModel.togglePref(Preference.WHEELCHAIR_ACCESSIBLE)
                  })

              Button(
                  onClick = { showDeleteDialog = true },
                  colors =
                      ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                  shape = RoundedCornerShape(28.dp),
                  modifier =
                      Modifier.align(Alignment.CenterHorizontally)
                          .testTag(EditTripScreenTestTags.DELETE)) {
                    Icon(Icons.Filled.Delete, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.delete_trip))
                  }

              if (showDeleteDialog) {
                AlertDialog(
                    modifier = Modifier.testTag(EditTripScreenTestTags.DELETE_DIALOG),
                    onDismissRequest = { showDeleteDialog = false },
                    containerColor = MaterialTheme.colorScheme.onPrimary,
                    confirmButton = {
                      TextButton(
                          onClick = {
                            editTripViewModel.deleteTrip()
                            showDeleteDialog = false
                            Toast.makeText(context, R.string.trip_deleted, Toast.LENGTH_SHORT)
                                .show()
                            onDelete()
                          },
                          modifier = Modifier.testTag(EditTripScreenTestTags.DELETE_CONFIRM)) {
                            Text(stringResource(R.string.confirm))
                          }
                    },
                    dismissButton = {
                      TextButton(
                          onClick = { showDeleteDialog = false },
                          modifier = Modifier.testTag(EditTripScreenTestTags.DELETE_CANCEL)) {
                            Text(stringResource(R.string.cancel))
                          }
                    },
                    title = { Text(stringResource(R.string.confirm_deletion)) },
                    text = { Text(stringResource(R.string.delete_trip_prompt)) })
              }
            }
      }
}

/**
 * A composable that displays a section header.
 *
 * @param text The text to display in the header.
 */
@Composable
private fun SectionHeader(text: String) {
  Column {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    Divider(Modifier.padding(top = 6.dp))
  }
}
