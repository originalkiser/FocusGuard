package com.focusguard.ui

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.focusguard.service.FocusGuardForegroundService
import com.focusguard.ui.navigation.FocusGuardNavGraph
import com.focusguard.ui.navigation.Screen
import com.focusguard.ui.theme.FocusGuardTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        startFilteringService()

        val onboardingDone = prefs.getBoolean("onboarding_complete", false)

        setContent {
            FocusGuardTheme {
                val navController = rememberNavController()
                FocusGuardNavGraph(
                    navController = navController,
                    startDestination = if (onboardingDone) Screen.Dashboard.route
                                       else Screen.Onboarding.route
                )
            }
        }
    }

    private fun startFilteringService() {
        try {
            startForegroundService(
                Intent(this, FocusGuardForegroundService::class.java).apply {
                    action = FocusGuardForegroundService.ACTION_START
                }
            )
        } catch (e: Exception) {
            // Service start failure must never crash the Activity.
            // The app is still usable; background persistence just won't apply.
            Log.e("FocusGuard", "Could not start foreground service", e)
        }
    }
}
