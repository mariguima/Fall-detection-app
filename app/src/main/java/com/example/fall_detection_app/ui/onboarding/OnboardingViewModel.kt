package com.example.fall_detection_app.ui.onboarding

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class OnboardingViewModel : ViewModel() {
    val requestPermissionsTrigger = MutableLiveData<Boolean>()

    fun triggerPermissionRequest() {
        requestPermissionsTrigger.value = true
    }
}