package com.focusguard.ui.onboarding

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val prefs: SharedPreferences
) : ViewModel() {

    fun completeOnboarding() {
        prefs.edit().putBoolean("onboarding_complete", true).apply()
    }
}
