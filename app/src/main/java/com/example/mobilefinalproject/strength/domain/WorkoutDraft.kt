package com.example.mobilefinalproject.strength.domain

import java.util.UUID

class SetDraft(
    val id: String = UUID.randomUUID().toString(),
    var weight: String = "",
    var reps: String = "",
    var note: String = ""
) {
    fun isBlank() = weight.isBlank() && reps.isBlank() && note.isBlank()

    fun values(): WorkoutSet {
        val parsed = parseSet(weight, reps, completed = true)
        return WorkoutSet(id, parsed.weight, parsed.reps, completed = true, note = note.trim())
    }
}

class WorkoutExerciseDraft(
    val name: String,
    val sets: MutableList<SetDraft> = mutableListOf(),
    val targetReps: Int? = null
)

class WorkoutDraft(
    val startedAt: Long,
    val startedAtElapsed: Long,
    var name: String,
    val id: String = UUID.randomUUID().toString(),
    var notes: String = "",
    val exercises: MutableList<WorkoutExerciseDraft> = mutableListOf()
) {
    fun addExercise(input: String): SetDraft {
        val name = validateExerciseName(input)
        require(exercises.none { exerciseKey(it.name) == exerciseKey(name) }) {
            "That exercise is already in the workout. Add a set to it instead."
        }
        val firstSet = SetDraft()
        exercises.add(WorkoutExerciseDraft(name, mutableListOf(firstSet)))
        return firstSet
    }

    fun finish(elapsedNow: Long, completedAt: Long): WorkoutSession {
        require(exercises.isNotEmpty()) { "Add an exercise before saving your workout." }
        val entries = exercises.mapNotNull { exercise ->
            val sets = exercise.sets.mapIndexedNotNull { index, set ->
                if (set.isBlank()) return@mapIndexedNotNull null
                try {
                    set.values()
                } catch (error: IllegalArgumentException) {
                    throw IllegalArgumentException("${exercise.name}, set ${index + 1}. ${error.message}")
                }
            }
            if (sets.isEmpty()) null else WorkoutExercise(exercise.name, sets)
        }
        require(entries.isNotEmpty()) { "Enter reps for at least one set to save your workout." }
        return WorkoutSession(
            id, validateWorkoutName(name), startedAt, completedAt,
            (elapsedNow - startedAtElapsed).coerceAtLeast(0), notes.trim(), entries
        )
    }
}

class RoutineExerciseDraft(val name: String, var sets: String = "3", var reps: String = "8")

class RoutineDraft(
    val id: String = UUID.randomUUID().toString(),
    var name: String = "",
    val exercises: MutableList<RoutineExerciseDraft> = mutableListOf()
) {
    fun addExercise(input: String) {
        val name = validateExerciseName(input)
        require(exercises.none { exerciseKey(it.name) == exerciseKey(name) }) {
            "That exercise is already in the routine."
        }
        exercises.add(RoutineExerciseDraft(name))
    }

    fun toRoutine(): Routine {
        val title = validateRoutineName(name)
        require(exercises.isNotEmpty()) { "Add at least one exercise." }
        return Routine(id, title, exercises.map { exercise ->
            val sets = exercise.sets.trim().toIntOrNull()
            val reps = exercise.reps.trim().toIntOrNull()
            require(sets != null && sets in 1..MAX_PLANNED_SETS) {
                "${exercise.name} needs between 1 and $MAX_PLANNED_SETS planned sets."
            }
            require(reps != null && reps > 0) {
                "${exercise.name} needs a positive whole number of target reps."
            }
            RoutineExercise(exercise.name, sets, reps)
        })
    }
}
