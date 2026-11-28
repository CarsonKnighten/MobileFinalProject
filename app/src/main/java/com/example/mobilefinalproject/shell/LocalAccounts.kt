package com.example.mobilefinalproject.shell

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Patterns
import androidx.core.content.contentValuesOf
import java.io.Closeable
import java.io.File
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Locale
import java.util.UUID
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

internal const val MIN_PASSWORD_LENGTH = 8
internal const val MAX_PASSWORD_LENGTH = 128
private const val HASH_ITERATIONS = 600_000

internal fun normalizedEmail(input: String): String? =
    input.trim().lowercase(Locale.ROOT).takeIf { it.length <= 254 && Patterns.EMAIL_ADDRESS.matcher(it).matches() }

internal class LocalAccounts(context: Context, databaseName: String = "local_accounts.db") : Closeable {
    private val helper = AccountDatabase(context, File(context.noBackupFilesDir, databaseName).absolutePath)

    fun register(email: String, password: CharArray): LocalAccount? {
        val normalized = requireNotNull(normalizedEmail(email))
        require(password.size in MIN_PASSWORD_LENGTH..MAX_PASSWORD_LENGTH)
        val salt = ByteArray(16).also(SecureRandom()::nextBytes)
        val verifier = passwordHash(password, salt, HASH_ITERATIONS)

        val database = helper.writableDatabase
        database.beginTransaction()
        try {
            val firstAccount = database.rawQuery("SELECT 1 FROM account LIMIT 1", null).use { !it.moveToFirst() }
            val account = LocalAccount(UUID.randomUUID().toString(), normalized, firstAccount)
            val row = database.insertWithOnConflict("account", null, contentValuesOf(
                "id" to account.id,
                "email" to account.email,
                "salt" to salt,
                "verifier" to verifier,
                "iterations" to HASH_ITERATIONS,
                "owns_existing_workouts" to account.ownsExistingWorkouts
            ), SQLiteDatabase.CONFLICT_IGNORE)
            database.setTransactionSuccessful()
            return account.takeIf { row != -1L }
        } finally {
            database.endTransaction()
        }
    }

    fun signIn(email: String, password: CharArray): LocalAccount? {
        val normalized = normalizedEmail(email) ?: return null
        if (password.size !in MIN_PASSWORD_LENGTH..MAX_PASSWORD_LENGTH) return null
        return helper.readableDatabase.rawQuery(
            "SELECT id, salt, verifier, iterations, owns_existing_workouts FROM account WHERE email = ?",
            arrayOf(normalized)
        ).use { cursor ->
            if (!cursor.moveToFirst()) return null
            val actual = passwordHash(password, cursor.getBlob(1), cursor.getInt(3))
            if (!MessageDigest.isEqual(cursor.getBlob(2), actual)) return null
            LocalAccount(cursor.getString(0), normalized, cursor.getInt(4) == 1)
        }
    }

    override fun close() = helper.close()

    private fun passwordHash(password: CharArray, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(password, salt, iterations, 256)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }
}

private class AccountDatabase(context: Context, path: String) : SQLiteOpenHelper(
    context.applicationContext, path, null, 1
) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE account (
                id TEXT PRIMARY KEY NOT NULL, email TEXT UNIQUE NOT NULL,
                salt BLOB NOT NULL, verifier BLOB NOT NULL, iterations INTEGER NOT NULL,
                owns_existing_workouts INTEGER NOT NULL CHECK(owns_existing_workouts IN (0, 1))
            )
        """.trimIndent())
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        error("Missing local account migration from $oldVersion to $newVersion")
    }
}
