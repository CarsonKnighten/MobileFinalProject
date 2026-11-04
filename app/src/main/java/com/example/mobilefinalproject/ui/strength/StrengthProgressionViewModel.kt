package com.example.mobilefinalproject.ui.strength

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class StrengthProgressionViewModel : ViewModel() {

    private val _projectionText = MutableLiveData<String>().apply {
        value = "Projected 10% increase in max lifts over the next 12 weeks based on your current trajectory."
    }
    val projectionText: LiveData<String> = _projectionText
}
