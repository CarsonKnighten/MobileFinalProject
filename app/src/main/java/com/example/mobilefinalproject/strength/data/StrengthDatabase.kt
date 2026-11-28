package com.example.mobilefinalproject.strength.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.mobilefinalproject.strength.domain.MAX_PLANNED_SETS
import com.example.mobilefinalproject.strength.domain.MAX_WEIGHT

internal class StrengthDatabase(context: Context, name: String?) : SQLiteOpenHelper(
    context.applicationContext, name, null, 2
) {
    override fun onConfigure(db: SQLiteDatabase) {
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE favorite (
                exercise_key TEXT PRIMARY KEY NOT NULL, name TEXT NOT NULL
            )
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE routine (id TEXT PRIMARY KEY NOT NULL, name TEXT NOT NULL)
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE routine_exercise (
                routine_id TEXT NOT NULL REFERENCES routine(id) ON DELETE CASCADE,
                position INTEGER NOT NULL, name TEXT NOT NULL,
                sets INTEGER NOT NULL CHECK(sets BETWEEN 1 AND $MAX_PLANNED_SETS),
                reps INTEGER NOT NULL CHECK(reps > 0),
                PRIMARY KEY (routine_id, position)
            )
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE workout (
                id TEXT PRIMARY KEY NOT NULL, name TEXT NOT NULL, started_at INTEGER NOT NULL,
                completed_at INTEGER NOT NULL, duration_ms INTEGER NOT NULL CHECK(duration_ms >= 0),
                notes TEXT NOT NULL
            )
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE workout_exercise (
                row_id INTEGER PRIMARY KEY,
                workout_id TEXT NOT NULL REFERENCES workout(id) ON DELETE CASCADE,
                position INTEGER NOT NULL, name TEXT NOT NULL, UNIQUE (workout_id, position)
            )
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE workout_set (
                id TEXT PRIMARY KEY NOT NULL,
                workout_exercise_id INTEGER NOT NULL REFERENCES workout_exercise(row_id) ON DELETE CASCADE,
                position INTEGER NOT NULL, weight REAL NOT NULL CHECK(weight BETWEEN 0 AND $MAX_WEIGHT),
                reps INTEGER NOT NULL CHECK(reps > 0), note TEXT NOT NULL DEFAULT '',
                UNIQUE (workout_exercise_id, position)
            )
        """.trimIndent())
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion == 1 && newVersion == 2) {
            db.execSQL("ALTER TABLE workout_set ADD COLUMN note TEXT NOT NULL DEFAULT ''")
            return
        }
        error("Missing strength database migration from $oldVersion to $newVersion")
    }
}
