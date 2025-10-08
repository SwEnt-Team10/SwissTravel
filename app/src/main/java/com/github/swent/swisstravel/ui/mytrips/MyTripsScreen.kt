package com.github.swent.swisstravel.ui.mytrips

import android.widget.Toast
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.text.get

object MyTripsScreenTestTags {
    const val TEST_TAG = "testTag"

    fun getTestTagForTrip(trip: Trip): String = "trip${trip.uid}"
}

@Composable
fun MyTripsScreen(
    myTripsScreenViewModel: MyTripsViewModel = viewModel(),
    onSelectTrip: (Trip) -> Unit = {},
    navigationActions: NavigationActions? = null,
) {

    val context = LocalContext.current
    val uiState by myTripsScreenViewModel.uiState.collectAsState()
    val trips = uiState.trips

    // Fetch trips when the screen is recomposed
    LaunchedEffect(Unit) { myTripsScreenViewModel.refreshUIState() }

    // Show error message if fetching todos fails
    LaunchedEffect(uiState.errorMsg) {
        uiState.errorMsg?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            myTripsScreenViewModel.clearErrorMsg()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("My Trips", modifier = Modifier.testTag(NavigationTestTags.TOP_BAR_TITLE))
                },
                actions = {
                    // Past Trips Icon Button
                    IconButton(
                        onClick = { overviewViewModel.signOut(credentialManager) },
                        modifier = Modifier.testTag(OverviewScreenTestTags.LOGOUT_BUTTON)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Log out")
                    }
                })
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onAddTodo() },
                modifier = Modifier.testTag(OverviewScreenTestTags.CREATE_TODO_BUTTON)) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add")
            }
        },
        bottomBar = {
            BottomNavigationMenu(
                selectedTab = Tab.Overview,
                onTabSelected = { tab -> navigationActions?.navigateTo(tab.destination) },
                modifier = Modifier.testTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU))
        },
        content = { pd ->
            if (todos.isNotEmpty()) {
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 8.dp),
                    modifier =
                        Modifier.fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(pd)
                            .testTag(OverviewScreenTestTags.TODO_LIST)) {
                    items(todos.size) { index ->
                        ToDoItem(todo = todos[index], onClick = { onSelectTodo(todos[index]) })
                    }
                }
            } else {
                Text(
                    modifier = Modifier.padding(pd).testTag(OverviewScreenTestTags.EMPTY_TODO_LIST_MSG),
                    text = "You have no ToDo yet.")
            }
        })
}


}