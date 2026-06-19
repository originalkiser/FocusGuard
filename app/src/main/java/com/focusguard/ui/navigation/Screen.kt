package com.focusguard.ui.navigation

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object Dashboard  : Screen("dashboard")
    data object Settings   : Screen("settings")
    data object ContactPicker : Screen("contact_picker")

    data object RuleWizard : Screen("rule_wizard?ruleId={ruleId}") {
        fun withId(id: Long? = null) =
            if (id != null) "rule_wizard?ruleId=$id" else "rule_wizard"
    }
}
