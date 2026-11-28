package com.example.mobilefinalproject.shell

import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.mobilefinalproject.strength.data.StrengthRepository
import com.example.mobilefinalproject.strength.data.strengthDatabaseName
import com.example.mobilefinalproject.strength.domain.WorkoutExercise
import com.example.mobilefinalproject.strength.domain.WorkoutSession
import com.example.mobilefinalproject.strength.domain.WorkoutSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class LocalAccountsTest {
    @Test
    fun localCredentialsPersistAndWorkoutProfilesStaySeparate() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val storeName = "account-test-${UUID.randomUUID()}.db"
        val password = "Workout account test".toCharArray()
        val files = mutableListOf(File(context.noBackupFilesDir, storeName))
        try {
            val (first, second, third) = LocalAccounts(context, storeName).use { store ->
                val first = requireNotNull(store.register(" Owner@Example.com ", password))
                assertNull(store.register("owner@example.com", password))
                val second = requireNotNull(store.register("second@example.com", password))
                val third = requireNotNull(store.register("third@example.com", password))
                Triple(first, second, third)
            }
            assertTrue(first.ownsExistingWorkouts)
            assertFalse(second.ownsExistingWorkouts)
            assertEquals("strength_workouts.db", strengthDatabaseName(context, first))

            LocalAccounts(context, storeName).use { store ->
                assertEquals(first, store.signIn("OWNER@EXAMPLE.COM", password))
                assertEquals(second, store.signIn("second@example.com", password))
                assertNull(store.signIn("second@example.com", "Wrong password".toCharArray()))
            }

            val secondDatabase = strengthDatabaseName(context, second)
            val thirdDatabase = strengthDatabaseName(context, third)
            assertNotEquals(secondDatabase, thirdDatabase)
            files.add(File(secondDatabase))
            files.add(File(thirdDatabase))
            val workout = WorkoutSession("session", "Account workout", 100L, 200L, 100L, "",
                listOf(WorkoutExercise("Squat", listOf(WorkoutSet("set", 45.0, 8, true)))))
            val writer = StrengthRepository(context, secondDatabase)
            try {
                writer.saveWorkout(workout)
                assertEquals(listOf(workout), writer.read().workouts)
            } finally {
                writer.close()
            }
            val otherAccount = StrengthRepository(context, thirdDatabase)
            try {
                assertTrue(otherAccount.read().workouts.isEmpty())
            } finally {
                otherAccount.close()
            }
        } finally {
            password.fill('\u0000')
            files.forEach { SQLiteDatabase.deleteDatabase(it) }
        }
    }
}
