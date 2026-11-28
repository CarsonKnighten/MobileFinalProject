package com.example.mobilefinalproject.strength.data

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import androidx.core.content.contentValuesOf
import com.example.mobilefinalproject.shell.LocalAccount
import com.example.mobilefinalproject.strength.domain.Routine
import com.example.mobilefinalproject.strength.domain.RoutineExercise
import com.example.mobilefinalproject.strength.domain.StrengthData
import com.example.mobilefinalproject.strength.domain.WorkoutExercise
import com.example.mobilefinalproject.strength.domain.WorkoutSession
import com.example.mobilefinalproject.strength.domain.WorkoutSet
import com.example.mobilefinalproject.strength.domain.exerciseKey
import com.example.mobilefinalproject.strength.domain.validateExerciseName
import com.example.mobilefinalproject.strength.domain.validateRoutineName
import com.example.mobilefinalproject.strength.domain.validateWorkoutName
import java.io.File

private const val EXISTING_WORKOUT_DATABASE = "strength_workouts.db"

internal fun strengthDatabaseName(context: Context, account: LocalAccount): String =
    if (account.ownsExistingWorkouts) EXISTING_WORKOUT_DATABASE
    else File(context.noBackupFilesDir, "strength_${account.id}.db").absolutePath

/** Blocking data access. The ViewModel calls these operations on Dispatchers.IO. */
internal class StrengthRepository(context: Context, databaseName: String?) {
    private val helper = StrengthDatabase(context, databaseName)

    fun close() = helper.close()

    fun read(): StrengthData = helper.readableDatabase.transaction {
        val favorites = queryRows("SELECT name FROM favorite ORDER BY exercise_key") { getString(0) }
        val routineExercises = queryRows(
            "SELECT routine_id, name, sets, reps FROM routine_exercise ORDER BY routine_id, position"
        ) {
            getString(0) to RoutineExercise(getString(1), getInt(2), getInt(3))
        }.groupBy({ it.first }, { it.second })
        val routines = queryRows("SELECT id, name FROM routine ORDER BY name COLLATE NOCASE, id") {
            Routine(getString(0), getString(1), routineExercises[getString(0)].orEmpty())
        }
        val sets = queryRows(
            "SELECT workout_exercise_id, id, weight, reps, note FROM workout_set ORDER BY workout_exercise_id, position"
        ) {
            getLong(0) to WorkoutSet(getString(1), getDouble(2), getInt(3), completed = true, note = getString(4))
        }.groupBy({ it.first }, { it.second })
        val workoutExercises = queryRows(
            "SELECT row_id, workout_id, name FROM workout_exercise ORDER BY workout_id, position"
        ) {
            getString(1) to WorkoutExercise(getString(2), sets[getLong(0)].orEmpty())
        }.groupBy({ it.first }, { it.second })
        val workouts = queryRows(
            "SELECT id, name, started_at, completed_at, duration_ms, notes " +
                "FROM workout ORDER BY completed_at DESC, id"
        ) {
            WorkoutSession(getString(0), getString(1), getLong(2), getLong(3), getLong(4),
                getString(5), workoutExercises[getString(0)].orEmpty())
        }
        StrengthData(favorites, routines, workouts)
    }

    fun setFavorite(input: String, favorite: Boolean) {
        val name = validateExerciseName(input)
        val key = exerciseKey(name)
        val db = helper.writableDatabase
        if (favorite) {
            db.insertWithOnConflict("favorite", null, contentValuesOf("exercise_key" to key, "name" to name),
                SQLiteDatabase.CONFLICT_IGNORE)
        } else {
            db.delete("favorite", "exercise_key = ?", arrayOf(key))
        }
    }

    fun saveRoutine(routine: Routine) = helper.writableDatabase.transaction {
        val name = validateRoutineName(routine.name)
        require(routine.exercises.isNotEmpty()) { "Add at least one exercise." }
        val updated = update("routine", contentValuesOf("name" to name), "id = ?", arrayOf(routine.id))
        if (updated == 0) {
            insertOrThrow("routine", null, contentValuesOf("id" to routine.id, "name" to name))
        }
        delete("routine_exercise", "routine_id = ?", arrayOf(routine.id))
        routine.exercises.forEachIndexed { position, entry ->
            insertOrThrow("routine_exercise", null, contentValuesOf(
                "routine_id" to routine.id, "position" to position, "name" to validateExerciseName(entry.name),
                "sets" to entry.sets, "reps" to entry.targetReps
            ))
        }
    }

    fun deleteRoutine(id: String) {
        helper.writableDatabase.delete("routine", "id = ?", arrayOf(id))
    }

    fun saveWorkout(workout: WorkoutSession) = helper.writableDatabase.transaction {
        // A retry with the same draft ID leaves the first committed session intact.
        if (rawQuery("SELECT 1 FROM workout WHERE id = ?", arrayOf(workout.id)).use { it.moveToFirst() }) {
            return@transaction
        }
        require(workout.exercises.isNotEmpty() && workout.exercises.all { exercise ->
            exercise.sets.isNotEmpty() && exercise.sets.all { it.completed }
        }) { "Complete at least one set before finishing your workout." }

        insertOrThrow("workout", null, contentValuesOf(
            "id" to workout.id, "name" to validateWorkoutName(workout.name), "started_at" to workout.startedAt,
            "completed_at" to workout.completedAt, "duration_ms" to workout.durationMs, "notes" to workout.notes
        ))
        workout.exercises.forEachIndexed { position, exercise ->
            val rowId = insertOrThrow("workout_exercise", null, contentValuesOf(
                "workout_id" to workout.id, "position" to position, "name" to validateExerciseName(exercise.name)
            ))
            exercise.sets.forEachIndexed { setPosition, set ->
                insertOrThrow("workout_set", null, contentValuesOf(
                    "id" to set.id, "workout_exercise_id" to rowId, "position" to setPosition,
                    "weight" to set.weight, "reps" to set.reps, "note" to set.note
                ))
            }
        }
    }

    private inline fun <T> SQLiteDatabase.transaction(run: SQLiteDatabase.() -> T): T {
        beginTransaction()
        try {
            return run().also { setTransactionSuccessful() }
        } finally {
            endTransaction()
        }
    }

    private inline fun <T> SQLiteDatabase.queryRows(sql: String, row: Cursor.() -> T): List<T> =
        rawQuery(sql, null).use { cursor ->
            buildList { while (cursor.moveToNext()) add(cursor.row()) }
        }
}
