package com.example.mobilefinalproject.strength

import android.Manifest
import android.os.Build
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withHint
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.mobilefinalproject.MainActivity
import com.example.mobilefinalproject.R
import com.example.mobilefinalproject.shell.AppSession
import com.example.mobilefinalproject.shell.testAccount
import com.example.mobilefinalproject.strength.data.StrengthRepository
import com.example.mobilefinalproject.strength.data.strengthDatabaseName
import com.example.mobilefinalproject.strength.domain.WorkoutExercise
import com.example.mobilefinalproject.strength.domain.WorkoutSession
import com.example.mobilefinalproject.strength.domain.WorkoutSet
import org.hamcrest.Matchers.containsString
import org.junit.Test
import org.junit.runner.RunWith
import java.text.NumberFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class StrengthFlowTest {
    @Test
    fun savesTypedWorkoutAndReusesFavoriteAfterActivityRestart() {
        val unique = UUID.randomUUID().toString().take(6)
        val name = "Test Session $unique"
        val exercise = "Bench Press $unique"
        val notes = "Test notes $unique"
        val setNote = "Smooth first set $unique"
        val volume = NumberFormat.getNumberInstance().format(1080)
        if (Build.VERSION.SDK_INT >= 33) {
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            instrumentation.uiAutomation.grantRuntimePermission(
                instrumentation.targetContext.packageName, Manifest.permission.POST_NOTIFICATIONS
            )
        }
        launchDashboard().use { activity ->
            openStrength()
            tap("Start Workout")
            onView(withId(R.id.save_strength_workout)).check(matches(isDisplayed()))
            fill("Exercise name", exercise)
            tap("Add Exercise")
            onView(withText("Set 1")).check(matches(isDisplayed()))

            tapSaveWorkout()
            onView(withText("Enter reps for at least one set to save your workout.")).inRoot(isDialog())
                .check(matches(isDisplayed()))
            onView(withText("OK")).inRoot(isDialog()).perform(click())
            fill("Weight (lb)", "-135")
            fill("Reps", "8")
            tapSaveWorkout()
            onView(withText("$exercise, set 1. Enter a non-negative weight in pounds.")).inRoot(isDialog())
                .check(matches(isDisplayed()))
            onView(withText("OK")).inRoot(isDialog()).perform(click())
            fill("Weight (lb)", "135")
            onView(withContentDescription("Add note for $exercise set 1")).perform(scrollTo(), click())
            fill("Set note (optional)", setNote)
            tap("Add Set")
            onView(withText("Set 2")).check(matches(isDisplayed()))
            onView(withContentDescription("$exercise, set 2, reps"))
                .perform(scrollTo(), replaceText("6"), closeSoftKeyboard())
            tap("Add Set")
            onView(withContentDescription("Remove $exercise set 3")).perform(scrollTo(), click())
            tap("Add Set")
            tap("Workout Options")
            fill("Workout name (optional)", name)
            onView(withContentDescription("Add $exercise to favorites")).perform(scrollTo(), click())
            tap("90 seconds")
            tap("Start Rest")
            tap("Pause")

            activity.recreate()
            tap("Resume")
            tap("Reset")
            fill("Workout notes (optional)", notes)
            tapSaveWorkout()
            visible("Workout Saved")
            visible("Sets logged · 2")
            visible("Training Volume · $volume lb")
            visible(notes)
            visible("Set 1 · 135 lb × 8")
            visible(setNote)
            visible("Set 2 · 0 lb × 6")
            tap("Back to Workouts")
            visible("Recent Workouts")
        }

        launchDashboard().use {
            openStrength()
            onView(withText(containsString("$name\n"))).perform(scrollTo(), click())
            visible(setNote)
            pressBack()
            tap("Workout History")
            onView(withText(name)).perform(scrollTo()).check(matches(isDisplayed()))
            pressBack()
            openMore("Progress")
            visible("$exercise · 135 lb")
            pressBack()
            openMore("My Routines")
            tap("Create Routine")
            val routine = "Test Routine $unique"
            fill("Routine name", routine)
            tap("Favorite Exercises")
            onView(withText(exercise)).inRoot(isDialog()).perform(click())
            tap("Save Routine")
            tap("$routine · 1 exercises")
            tap("Start Workout")
            visible("Target: 8 reps per set")
            tap("Workout Options")
            onView(withText(containsString("135 lb × 8"))).perform(scrollTo()).check(matches(isDisplayed()))
        }
    }

    @Test
    fun progressDisplaysChartForTwoSavedDates() {
        val exercise = "Chart Press ${UUID.randomUUID().toString().take(6)}"
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repository = StrengthRepository(context, strengthDatabaseName(context, testAccount))
        try {
            for ((day, weight) in listOf(3L to 115.0, 1L to 135.0)) {
                val id = UUID.randomUUID().toString()
                val date = LocalDate.now().minusDays(day).atStartOfDay(ZoneId.systemDefault())
                    .toInstant().toEpochMilli()
                val name = if (day == 1L) exercise else exercise.lowercase(Locale.ROOT)
                repository.saveWorkout(WorkoutSession(
                    id, "Chart test session", date - 1000, date, 1000, "",
                    listOf(WorkoutExercise(name, listOf(WorkoutSet("$id-set", weight, 8, true))))
                ))
            }
        } finally {
            repository.close()
        }
        launchDashboard().use {
            openStrength()
            openMore("Progress")
            visible("Heaviest Lift by Date")
            visible("$exercise · 135 lb")
            onView(withText(containsString("Exercise · "))).perform(scrollTo(), click())
            onView(withText(exercise)).inRoot(isDialog()).perform(click())
            onView(withContentDescription(containsString("Line chart of highest added weight per day")))
                .perform(scrollTo()).check(matches(isDisplayed()))
            visible("Latest Sessions by Day")
        }
    }

    @Test
    fun phoneNavigationOpensRemainingDestinations() {
        launchDashboard().use {
            onView(withText("Starter Log")).check(doesNotExist())
            onView(withText("Starter Progress")).check(doesNotExist())
            onView(withId(R.id.nav_weight_loss)).perform(click())
            onView(withId(R.id.button_save_weight_log)).check(matches(isDisplayed()))
            onView(withId(R.id.nav_strength_training)).perform(click())
            visible("Recent Workouts")
            onView(withId(R.id.nav_dashboard)).perform(click())
            onView(withId(R.id.open_strength_training)).check(matches(isDisplayed()))
        }
    }

    private fun openStrength() {
        onView(withId(R.id.open_strength_training)).perform(click())
        visible("Recent Workouts")
    }

    private fun launchDashboard(): ActivityScenario<MainActivity> {
        AppSession.signIn(testAccount)
        return ActivityScenario.launch(MainActivity::class.java)
    }

    private fun fill(hint: String, value: String) {
        onView(withHint(hint)).perform(scrollTo(), replaceText(value), closeSoftKeyboard())
    }

    private fun openMore(option: String) {
        tap("More Strength Tools")
        onView(withText(option)).inRoot(isDialog()).perform(click())
    }

    private fun tapSaveWorkout() {
        onView(withId(R.id.save_strength_workout)).perform(click())
    }

    private fun tap(text: String) {
        onView(withText(text)).perform(scrollTo(), click())
    }

    private fun visible(text: String) {
        onView(withText(text)).perform(scrollTo()).check(matches(isDisplayed()))
    }
}
