package com.example.mobilefinalproject.ui.workout

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class WorkoutLoggingViewModel : ViewModel() {

    private val _recentWorkouts = MutableLiveData<String>().apply {
        value = "No workouts logged yet today."
    }
    val recentWorkouts: LiveData<String> = _recentWorkouts
}
