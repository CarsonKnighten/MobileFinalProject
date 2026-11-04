package com.example.mobilefinalproject.ui.weightloss

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class WeightLossViewModel : ViewModel() {

    private val _analysisText = MutableLiveData<String>().apply {
        value = "Actual weight trend indicates accurate progress relative to intake."
    }
    val analysisText: LiveData<String> = _analysisText
}
