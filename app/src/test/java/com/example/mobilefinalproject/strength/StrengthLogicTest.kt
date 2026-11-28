package com.example.mobilefinalproject.strength

import com.example.mobilefinalproject.strength.domain.PersonalRecord
import com.example.mobilefinalproject.strength.domain.RestTimer
import com.example.mobilefinalproject.strength.domain.RoutineDraft
import com.example.mobilefinalproject.strength.domain.SetDraft
import com.example.mobilefinalproject.strength.domain.WorkoutDraft
import com.example.mobilefinalproject.strength.domain.WorkoutExercise
import com.example.mobilefinalproject.strength.domain.WorkoutSession
import com.example.mobilefinalproject.strength.domain.WorkoutSet
import com.example.mobilefinalproject.strength.domain.exerciseKey
import com.example.mobilefinalproject.strength.domain.parseSet
import com.example.mobilefinalproject.strength.domain.personalRecords
import com.example.mobilefinalproject.strength.domain.previousSets
import com.example.mobilefinalproject.strength.domain.progressPoints
import com.example.mobilefinalproject.strength.domain.validateExerciseName
import com.example.mobilefinalproject.strength.domain.workoutVolume
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class StrengthLogicTest {
    @Test
    fun volumeIncludesOnlyCompletedSets() {
        val workout = session("Bench Press", 1, set(135.0, 8), set(135.0, 8), set(135.0, 6),
            set(300.0, 20, completed = false), set(0.0, 12))
        assertEquals(2970.0, workoutVolume(workout), 0.0)
    }

    @Test
    fun rejectsInvalidWeights() {
        for (value in listOf("-5", "abc", "NaN", "Infinity", "1.2.3", "100001", "0.0001")) {
            assertThrows(value, IllegalArgumentException::class.java) { parseSet(value, "8", true) }
        }
    }

    @Test
    fun validatesRepsAndAllowsBodyweight() {
        for (value in listOf("-1", "0", "1.5", "two", "2147483648", "")) {
            assertThrows(value, IllegalArgumentException::class.java) { parseSet("10", value, true) }
        }
        assertEquals(0.0, parseSet("", "8", true).weight, 0.0)
        assertEquals(27.5, parseSet("27,5", "8", true).weight, 0.0)
        assertEquals(null, parseSet("27.5", "", false).reps)
    }

    @Test
    fun normalizesExerciseNamesAndPreservesDisplayCase() {
        assertEquals("Bench Press", validateExerciseName("  Bench\t  Press  "))
        assertEquals("bench press", exerciseKey("  BENCH   PRESS "))
        assertThrows(IllegalArgumentException::class.java) { validateExerciseName(" ") }
        assertThrows(IllegalArgumentException::class.java) { validateExerciseName("a".repeat(81)) }
        val draft = WorkoutDraft(10, 10, "")
        draft.addExercise("Bench Press")
        assertThrows(IllegalArgumentException::class.java) { draft.addExercise("bench  press") }
    }

    @Test
    fun recordsAndPreviousWorkoutsUseCompletedSetsAndNormalizedNames() {
        val first = session("Bench Press", 1, set(135.0, 8), set(225.0, 5, completed = false))
        val second = session("bench   press", 2, set(115.0, 12), set(110.0, 12))
        val third = session("BENCH PRESS", 3, set(300.0, 5, completed = false))
        val workouts = listOf(first, third, second)
        assertEquals(listOf(PersonalRecord("bench press", "Bench Press", 135.0)), personalRecords(workouts))
        assertEquals(listOf(115.0, 110.0), previousSets(workouts, " Bench Press ").map { it.weight })
        assertTrue(previousSets(workouts, "Squat").isEmpty())
    }

    @Test
    fun progressUsesDailyBestInChronologicalOrder() {
        val dayOne = Instant.parse("2026-11-08T18:00:00Z").toEpochMilli()
        val dayTwo = Instant.parse("2026-11-12T18:00:00Z").toEpochMilli()
        val workouts = listOf(
            session("Bench Press", dayTwo, set(125.0, 8)),
            session("bench press", dayOne + 1000, set(135.0, 8)),
            session("BENCH PRESS", dayOne, set(115.0, 8))
        )
        val points = progressPoints(workouts, "bench Press", ZoneId.of("UTC"))
        assertEquals(listOf(135.0, 125.0), points.map { it.weight })
        assertEquals(listOf(8, 12), points.map { it.date.dayOfMonth })
    }

    @Test
    fun emptyWorkoutCannotFinishAndOnlyCompletedSetsAreSaved() {
        val draft = WorkoutDraft(startedAt = 5000, startedAtElapsed = 1000, name = "")
        assertThrows(IllegalArgumentException::class.java) { draft.finish(2000, 6000) }
        assertTrue(personalRecords(emptyList()).isEmpty())
        assertTrue(progressPoints(emptyList(), "Squat", ZoneId.of("UTC")).isEmpty())
        draft.addExercise("Pull-up")
        draft.addExercise("Squat")
        draft.exercises.first().sets.add(SetDraft(reps = "6"))
        val saved = draft.finish(6000, 10_000)
        assertEquals("Strength Workout", saved.name)
        assertEquals(5000L, saved.durationMs)
        assertEquals(listOf("Pull-up"), saved.exercises.map { it.name })
        assertEquals(6, saved.exercises.single().sets.single().reps)
    }

    @Test
    fun rejectsPartiallyEnteredSetsAndSavesFilledRows() {
        val draft = WorkoutDraft(5000, 1000, "")
        val first = draft.addExercise("Bench Press")
        first.weight = "135"
        assertThrows(IllegalArgumentException::class.java) { draft.finish(2000, 6000) }
        first.reps = "8"
        draft.exercises.single().sets.add(SetDraft(weight = "115", reps = "6"))
        val saved = draft.finish(2000, 6000)
        assertEquals(listOf(8, 6), saved.exercises.single().sets.map { it.reps })
        assertEquals(1770.0, workoutVolume(saved), 0.0)
    }

    @Test
    fun routineValidationRejectsMissingNamesAndInvalidTargets() {
        val draft = RoutineDraft()
        assertThrows(IllegalArgumentException::class.java) { draft.toRoutine() }
        draft.name = "Leg day"
        draft.addExercise("Squat")
        val exercise = draft.exercises.single()
        exercise.sets = "51"
        assertThrows(IllegalArgumentException::class.java) { draft.toRoutine() }
        exercise.sets = "4"
        exercise.reps = "0"
        assertThrows(IllegalArgumentException::class.java) { draft.toRoutine() }
        exercise.reps = "8"
        assertEquals(4, draft.toRoutine().exercises.single().sets)
    }

    @Test
    fun restTimerPauseResumeAndResetUseElapsedTime() {
        val timer = RestTimer()
        assertEquals(60_000L, timer.remaining(500L))
        timer.select(90)
        timer.start(1000L)
        assertEquals(90_000L, timer.remaining(1000L))
        timer.pause(31_000L)
        assertTrue(timer.isPaused())
        assertEquals(60_000L, timer.remaining(70_000L))
        timer.resume(100_000L)
        assertTrue(timer.isRunning(159_000L))
        assertTrue(timer.isFinished(161_000L))
        assertEquals(0L, timer.remaining(161_000L))
        timer.reset()
        assertFalse(timer.isRunning(161_000L))
        assertEquals(90_000L, timer.remaining(161_000L))
    }

    private fun session(name: String, time: Long, vararg sets: WorkoutSet) =
        WorkoutSession("session-$time", "Workout", time - 1000, time, 1000, "",
            listOf(WorkoutExercise(name, sets.toList())))

    private fun set(weight: Double, reps: Int, completed: Boolean = true) =
        WorkoutSet("set-$weight-$reps", weight, reps, completed)
}
