package com.example.mobilefinalproject.strength

import android.database.sqlite.SQLiteConstraintException
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.mobilefinalproject.strength.data.StrengthDatabase
import com.example.mobilefinalproject.strength.data.StrengthRepository
import com.example.mobilefinalproject.strength.domain.Routine
import com.example.mobilefinalproject.strength.domain.RoutineExercise
import com.example.mobilefinalproject.strength.domain.WorkoutExercise
import com.example.mobilefinalproject.strength.domain.WorkoutSession
import com.example.mobilefinalproject.strength.domain.WorkoutSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class StrengthRepositoryTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun savesAndReopensWorkoutsNotesAndSetsWithoutDuplicates() {
        val name = "strength-test-${UUID.randomUUID()}.db"
        try {
            val first = workout("first")
            val second = workout("second", 300L)
            useRepository(name) {
                saveWorkout(first)
                saveWorkout(second)
                saveWorkout(first.copy(notes = "Duplicate attempt"))
            }
            useRepository(name) {
                assertEquals(listOf(second, first), read().workouts)
            }
        } finally {
            context.deleteDatabase(name)
        }
    }

    @Test
    fun supportsRoutineUpdatesAndNormalizedFavorites() = useRepository {
        val routine = Routine("routine", "Upper Body", listOf(RoutineExercise("Bench Press", 3, 8)))
        saveRoutine(routine)
        assertEquals(routine, read().routines.single())
        saveWorkout(workout("session"))
        val revised = routine.copy(name = "Push", exercises = listOf(RoutineExercise("Push-up", 2, 12)))
        saveRoutine(revised)
        setFavorite("Bench Press", true)
        setFavorite("  BENCH   PRESS ", true)
        assertEquals(listOf("Bench Press"), read().favorites)
        assertEquals(revised, read().routines.single())
        assertEquals("Bench Press", read().workouts.single().exercises.single().name)
        setFavorite("bench press", false)
        deleteRoutine(routine.id)
        assertTrue(read().favorites.isEmpty())
        assertTrue(read().routines.isEmpty())
        assertEquals(1, read().workouts.size)
    }

    @Test
    fun rollsBackWorkoutIfAnySetIsInvalid() = useRepository {
        val first = workout("first")
        val replacement = first.copy(exercises = listOf(
            WorkoutExercise("Bench Press", listOf(WorkoutSet("bad-set", -10.0, 5, completed = true)))
        ))
        assertThrows(SQLiteConstraintException::class.java) { saveWorkout(replacement) }
        assertTrue(read().workouts.isEmpty())
        saveWorkout(first)
        assertEquals(first, read().workouts.single())
    }

    @Test
    fun upgradingAddsBlankNotesToExistingSets() {
        val name = "strength-upgrade-${UUID.randomUUID()}.db"
        try {
            context.openOrCreateDatabase(name, 0, null).use { legacy ->
                legacy.execSQL(
                    "CREATE TABLE workout_set (id TEXT PRIMARY KEY, workout_exercise_id INTEGER NOT NULL, " +
                        "position INTEGER NOT NULL, weight REAL NOT NULL, reps INTEGER NOT NULL)"
                )
                legacy.execSQL("INSERT INTO workout_set VALUES ('old-set', 1, 0, 135.0, 8)")
                legacy.version = 1
            }
            StrengthDatabase(context, name).use { helper ->
                helper.readableDatabase.rawQuery("SELECT id, weight, reps, note FROM workout_set", null).use {
                    assertTrue(it.moveToFirst())
                    assertEquals("old-set", it.getString(0))
                    assertEquals(135.0, it.getDouble(1), 0.0)
                    assertEquals(8, it.getInt(2))
                    assertEquals("", it.getString(3))
                }
            }
        } finally {
            context.deleteDatabase(name)
        }
    }

    private fun workout(id: String, time: Long = 200L) = WorkoutSession(
        id, "Test Workout", 50L, time, time - 50L, "Felt stronger",
        listOf(WorkoutExercise("Bench Press", listOf(WorkoutSet("$id-set", 135.0, 8, true, "Controlled reps"))))
    )

    private fun useRepository(name: String? = null, test: StrengthRepository.() -> Unit) {
        val repository = StrengthRepository(context, name)
        try {
            repository.test()
        } finally {
            repository.close()
        }
    }
}
