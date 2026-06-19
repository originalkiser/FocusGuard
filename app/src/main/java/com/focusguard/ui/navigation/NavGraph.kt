package com.focusguard.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.focusguard.ui.contacts.ContactPickerScreen
import com.focusguard.ui.dashboard.DashboardScreen
import com.focusguard.ui.onboarding.OnboardingScreen
import com.focusguard.ui.rules.RuleWizardScreen
import com.focusguard.ui.rules.RuleWizardViewModel
import com.focusguard.ui.settings.SettingsScreen

@Composable
fun FocusGuardNavGraph(
    navController: NavHostController,
    startDestination: String
) {
    NavHost(navController = navController, startDestination = startDestination) {

        composable(Screen.Onboarding.route) {
            OnboardingScreen(onComplete = {
                navController.navigate(Screen.Dashboard.route) {
                    popUpTo(Screen.Onboarding.route) { inclusive = true }
                }
            })
        }

        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onCreateRule = { navController.navigate(Screen.RuleWizard.withId()) },
                onEditRule   = { id -> navController.navigate(Screen.RuleWizard.withId(id)) },
                onOpenSettings = { navController.navigate(Screen.Settings.route) }
            )
        }

        composable(
            route = "rule_wizard?ruleId={ruleId}",
            arguments = listOf(navArgument("ruleId") {
                type = NavType.LongType
                defaultValue = -1L
            })
        ) { backStackEntry ->
            val ruleId = backStackEntry.arguments?.getLong("ruleId")?.takeIf { it != -1L }
            val viewModel: RuleWizardViewModel = hiltViewModel()

            // Receive selected contacts returned by ContactPickerScreen
            val pickerResult by backStackEntry.savedStateHandle
                .getStateFlow<ArrayList<String>?>("contact_picker_result", null)
                .collectAsState()
            LaunchedEffect(pickerResult) {
                pickerResult?.let {
                    viewModel.updateSelectedContacts(it.toSet())
                    backStackEntry.savedStateHandle.remove<ArrayList<String>>("contact_picker_result")
                }
            }

            RuleWizardScreen(
                ruleId = ruleId,
                onNavigateBack = { navController.popBackStack() },
                onPickContacts = {
                    // Stash current selection so picker pre-checks already-selected contacts
                    backStackEntry.savedStateHandle["pre_selected_contacts"] =
                        ArrayList(viewModel.uiState.value.selectedContactKeys)
                    navController.navigate(Screen.ContactPicker.route)
                },
                viewModel = viewModel
            )
        }

        composable(Screen.ContactPicker.route) {
            val previousSsh = navController.previousBackStackEntry?.savedStateHandle
            val preSelected = previousSsh
                ?.get<ArrayList<String>>("pre_selected_contacts")
                ?.toSet() ?: emptySet()

            ContactPickerScreen(
                preSelectedKeys = preSelected,
                onDone = { selected ->
                    previousSsh?.set("contact_picker_result", ArrayList(selected))
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
