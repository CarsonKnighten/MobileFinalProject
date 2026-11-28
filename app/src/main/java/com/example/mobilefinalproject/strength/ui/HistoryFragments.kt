package com.example.mobilefinalproject.strength.ui

import androidx.navigation.fragment.findNavController
import com.example.mobilefinalproject.R
import com.example.mobilefinalproject.strength.domain.StrengthData
import com.example.mobilefinalproject.strength.domain.workoutVolume

class WorkoutHistoryFragment : StrengthFragment() {
    override fun render(data: StrengthData) = with(content) {
        heading("Workout History", 24f)
        if (data.workouts.isEmpty()) textLine("Complete a workout to see it here.")
        else textLine("${data.workouts.size} workouts · ${duration(data.workouts.sumOf { it.durationMs })} total time")
        for (workout in data.workouts) {
            card {
                heading(workout.name)
                textLine(displayDate(workout.completedAt))
                textLine("${duration(workout.durationMs)} · ${workout.exercises.size} exercises")
                button("View Workout") { go(R.id.strength_workout_detail, recordArgs(workout.id)) }
            }
        }
    }
}

class WorkoutDetailFragment : StrengthFragment() {
    override fun render(data: StrengthData) = with(content) {
        val workout = data.workouts.firstOrNull { it.id == arguments?.getString(RECORD_ID) }
        if (workout == null) {
            textLine("This workout is no longer available.")
            button("Go Back") { back() }
            return@with
        }
        val summary = arguments?.getBoolean(IS_SUMMARY) == true
        if (summary) heading("Workout Saved", 24f)
        heading(workout.name, if (summary) 18f else 24f)
        if (summary) button("Back to Workouts") { findNavController().popBackStack(R.id.strength_home, false) }
        card {
            textLine(displayDate(workout.completedAt))
            textLine("Duration · ${duration(workout.durationMs)}")
            val sets = workout.exercises.sumOf { entry -> entry.sets.size }
            textLine("Sets logged · $sets")
            heading("Training Volume · ${displayWeight(workoutVolume(workout))} lb")
            textLine("Volume uses completed sets and added weight.")
        }
        heading("Workout Notes")
        textLine(workout.notes.ifBlank { "No notes were entered." })
        for (entry in workout.exercises) {
            card {
                heading(entry.name)
                entry.sets.forEachIndexed { index, set ->
                    textLine("Set ${index + 1} · ${displayWeight(set.weight)} lb × ${set.reps}")
                    if (set.note.isNotBlank()) textLine(set.note)
                }
            }
        }
    }
}
