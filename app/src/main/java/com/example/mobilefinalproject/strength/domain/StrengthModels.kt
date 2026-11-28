package com.example.mobilefinalproject.strength.domain

data class RoutineExercise(val name: String, val sets: Int, val targetReps: Int)

data class Routine(val id: String, val name: String, val exercises: List<RoutineExercise>)

data class WorkoutSet(
    val id: String,
    val weight: Double,
    val reps: Int?,
    val completed: Boolean,
    val note: String = ""
)

data class WorkoutExercise(val name: String, val sets: List<WorkoutSet>)

data class WorkoutSession(
    val id: String,
    val name: String,
    val startedAt: Long,
    val completedAt: Long,
    val durationMs: Long,
    val notes: String,
    val exercises: List<WorkoutExercise>
)

data class StrengthData(
    val favorites: List<String>,
    val routines: List<Routine>,
    val workouts: List<WorkoutSession>
)
