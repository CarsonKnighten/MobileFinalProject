package com.example.mobilefinalproject.strength.ui

import android.os.Bundle
import android.text.InputType
import androidx.core.widget.doAfterTextChanged
import com.example.mobilefinalproject.R
import com.example.mobilefinalproject.strength.domain.StrengthData

class RoutineListFragment : StrengthFragment() {
    override fun render(data: StrengthData) = with(content) {
        heading("My Routines", 24f)
        button("Create Routine", primary = true) {
            model.newRoutine()
            go(R.id.strength_routine_editor)
        }
        if (data.routines.isEmpty()) textLine("Save a routine to reuse its exercises, planned sets, and target reps.")
        for (routine in data.routines) {
            button("${routine.name} · ${routine.exercises.size} exercises") {
                go(R.id.strength_routine_detail, recordArgs(routine.id))
            }
        }
    }
}

class RoutineDetailFragment : StrengthFragment() {
    override fun render(data: StrengthData) = with(content) {
        val routine = data.routines.firstOrNull { it.id == arguments?.getString(RECORD_ID) }
        if (routine == null) {
            textLine("This routine is no longer available.")
            button("Back to Routines") { back() }
            return@with
        }

        heading(routine.name, 24f)
        card {
            heading("Planned Exercises")
            for (entry in routine.exercises) textLine("${entry.name} · ${entry.sets} sets × ${entry.targetReps} reps")
        }
        if (model.workout != null) {
            textLine("Finish or discard the active workout before starting this routine.")
            button("Resume Active Workout", primary = true) { go(R.id.strength_active_workout) }
        } else {
            button("Start Workout", primary = true) {
                validate {
                    model.startWorkout(routine = routine)
                    go(R.id.strength_active_workout)
                }
            }
        }
        button("Edit Routine") {
            model.newRoutine(routine)
            go(R.id.strength_routine_editor)
        }
        button("Delete Routine") {
            confirm("Delete ${routine.name}?", "Saved workouts from this routine will stay in history.", "Delete") {
                model.removeRoutine(routine.id, whenVisible { back() })
            }
        }
    }
}

class RoutineEditorFragment : StrengthFragment() {
    private var exerciseInput = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        exerciseInput = savedInstanceState?.getString("exerciseInput").orEmpty()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("exerciseInput", exerciseInput)
    }

    override fun render(data: StrengthData) = with(content) {
        val draft = model.routineDraft
        if (draft == null) {
            textLine("Reopen the routine editor to start editing.")
            button("Go Back") { back() }
            return@with
        }
        heading(if (data.routines.any { it.id == draft.id }) "Edit Routine" else "Create Routine", 24f)
        field("Routine name", draft.name, limit = 160).doAfterTextChanged { draft.name = it.toString() }
        exerciseEntry(exerciseInput, onChange = { exerciseInput = it }) {
            draft.addExercise(it)
            exerciseInput = ""
        }
        for (entry in draft.exercises) {
            card {
                heading(entry.name)
                val row = formRow()
                row.weightedField("Planned sets", entry.sets, InputType.TYPE_CLASS_NUMBER, 6)
                    .doAfterTextChanged { entry.sets = it.toString() }
                row.weightedField("Target reps", entry.reps, InputType.TYPE_CLASS_NUMBER, 12)
                    .doAfterTextChanged { entry.reps = it.toString() }
                favoriteButton(entry.name, data.favorites)
                button("Remove Exercise") {
                    draft.exercises.remove(entry)
                    redraw()
                }
            }
        }
        button("Save Routine", primary = true) {
            validate { model.saveRoutine(draft.toRoutine(), whenVisible { back() }) }
        }
        Unit
    }
}
