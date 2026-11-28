package com.example.mobilefinalproject.strength.ui

import androidx.navigation.fragment.findNavController
import com.example.mobilefinalproject.R
import com.example.mobilefinalproject.strength.domain.StrengthData
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class StrengthHomeFragment : StrengthFragment() {
    override fun render(data: StrengthData) = with(content) {
        heading("Strength Training", 24f)
        button(if (model.workout == null) "Start Workout" else "Resume Workout", primary = true) {
            if (!isResumed || findNavController().currentDestination?.id != R.id.strength_home) return@button
            model.startWorkout()
            go(R.id.strength_active_workout)
        }

        heading("Recent Workouts")
        if (data.workouts.isEmpty()) textLine("Saved workouts will appear here.")
        for (workout in data.workouts.take(3)) {
            button(workout.name + "\n" + displayDate(workout.completedAt)) {
                go(R.id.strength_workout_detail, recordArgs(workout.id))
            }
        }
        button("Workout History") { go(R.id.strength_history) }
        button("More Strength Tools") {
            MaterialAlertDialogBuilder(context).setTitle("More Strength Tools")
                .setItems(arrayOf("My Routines", "Progress")) { _, which ->
                    go(if (which == 0) R.id.strength_routines else R.id.strength_progress)
                }
                .setNegativeButton("Cancel", null).show()
        }
        Unit
    }
}
