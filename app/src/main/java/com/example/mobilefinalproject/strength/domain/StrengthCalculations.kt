package com.example.mobilefinalproject.strength.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private fun WorkoutSet.isCompletedWork() = completed && (reps ?: 0) > 0

fun workoutVolume(workout: WorkoutSession): Double =
    workout.exercises.sumOf { exercise ->
        exercise.sets.filter(WorkoutSet::isCompletedWork).sumOf { it.weight * (it.reps ?: 0) }
    }

data class PersonalRecord(val exerciseKey: String, val name: String, val weight: Double)
data class ProgressPoint(val date: LocalDate, val weight: Double)

fun personalRecords(workouts: List<WorkoutSession>): List<PersonalRecord> {
    val records = mutableMapOf<String, PersonalRecord>()
    for (workout in workouts) {
        for (exercise in workout.exercises) {
            val best = exercise.sets.filter(WorkoutSet::isCompletedWork).maxOfOrNull { it.weight } ?: continue
            val key = exerciseKey(exercise.name)
            val prior = records[key]
            if (prior == null || best > prior.weight) {
                records[key] = PersonalRecord(key, exercise.name, best)
            }
        }
    }
    return records.values.sortedBy { it.exerciseKey }
}

fun progressPoints(
    workouts: List<WorkoutSession>, name: String, zone: ZoneId
): List<ProgressPoint> {
    val key = exerciseKey(name)
    return workouts.mapNotNull { workout ->
        val best = completedSets(workout, key).maxOfOrNull { it.weight } ?: return@mapNotNull null
        ProgressPoint(Instant.ofEpochMilli(workout.completedAt).atZone(zone).toLocalDate(), best)
    }.groupBy { it.date }.map { (_, points) -> points.maxBy { it.weight } }.sortedBy { it.date }
}

fun previousSets(workouts: List<WorkoutSession>, name: String): List<WorkoutSet> {
    val key = exerciseKey(name)
    return workouts.sortedByDescending { it.completedAt }.asSequence()
        .map { completedSets(it, key) }.firstOrNull { it.isNotEmpty() }.orEmpty()
}

private fun completedSets(workout: WorkoutSession, key: String): List<WorkoutSet> =
    workout.exercises.filter { exerciseKey(it.name) == key }
        .flatMap { exercise -> exercise.sets.filter(WorkoutSet::isCompletedWork) }
