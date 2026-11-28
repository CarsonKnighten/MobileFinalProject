package com.example.mobilefinalproject.strength.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.text.InputType
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.doOnNextLayout
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.example.mobilefinalproject.R
import com.example.mobilefinalproject.strength.domain.SetDraft
import com.example.mobilefinalproject.strength.domain.StrengthData
import com.example.mobilefinalproject.strength.domain.previousSets
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ActiveWorkoutFragment : StrengthFragment(R.layout.strength_workout_editor) {
    private var elapsedLabel: TextView? = null
    private var remainingLabel: TextView? = null
    private var restAction: MaterialButton? = null
    private val saveButton: MaterialButton get() = requireView().findViewById(R.id.save_strength_workout)
    private var exerciseInput = ""
    private var setToShow: String? = null
    private var optionsShown = false
    private val openSetNotes = mutableSetOf<String>()
    private var requestedRestNotifications = false
    private val requestRestNotifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (it) model.scheduleRestNotification()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        exerciseInput = savedInstanceState?.getString("exerciseInput").orEmpty()
        optionsShown = savedInstanceState?.getBoolean("optionsShown") ?: false
        openSetNotes.addAll(savedInstanceState?.getStringArrayList("openSetNotes").orEmpty())
        requestedRestNotifications = savedInstanceState?.getBoolean("requestedRestNotifications") ?: false
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("exerciseInput", exerciseInput)
        outState.putBoolean("optionsShown", optionsShown)
        outState.putStringArrayList("openSetNotes", ArrayList(openSetNotes))
        outState.putBoolean("requestedRestNotifications", requestedRestNotifications)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        saveButton.setOnClickListener { saveWorkout() }
        model.saving.observe(viewLifecycleOwner) { saveButton.isEnabled = !it }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    updateTimers()
                    delay(250)
                }
            }
        }
    }

    override fun onDestroyView() {
        elapsedLabel = null
        remainingLabel = null
        restAction = null
        super.onDestroyView()
    }

    override fun render(data: StrengthData) = with(content) {
        elapsedLabel = null
        remainingLabel = null
        restAction = null
        val draft = model.workout
        saveButton.isVisible = draft != null
        if (draft == null) {
            val finished = model.lastFinishedId
            if (finished == null) textLine("There’s no active workout. Start a new one from Strength Training.")
            else button("Open Workout Summary", primary = true) { showSummary(finished) }
            button("Go Back") { back() }
            return@with
        }

        heading(draft.name.ifBlank { "Strength Workout" }, 24f)
        elapsedLabel = textLine("")
        exerciseEntry(exerciseInput, onChange = { exerciseInput = it }, showFavorites = optionsShown) {
            setToShow = draft.addExercise(it).id
            exerciseInput = ""
        }
        for (exercise in draft.exercises) {
            card {
                heading(exercise.name)
                exercise.targetReps?.let { textLine("Target: $it reps per set") }
                if (optionsShown) {
                    val history = previousSets(data.workouts, exercise.name)
                    if (history.isNotEmpty()) textLine("Last workout · " + history.joinToString("  ·  ") {
                        "${displayWeight(it.weight)} lb × ${it.reps}"
                    })
                    favoriteButton(exercise.name, data.favorites)
                }
                for ((index, set) in exercise.sets.withIndex()) {
                    setEditor(exercise.name, set, index + 1) {
                        exercise.sets.remove(set)
                        openSetNotes.remove(set.id)
                        redraw()
                    }
                }
                button("Add Set") {
                    val newSet = SetDraft()
                    exercise.sets.add(newSet)
                    setToShow = newSet.id
                    hideKeyboard()
                    redraw()
                }
                if (optionsShown) {
                    button("Remove Exercise") {
                        confirm("Remove ${exercise.name}?", "Its sets will be removed from this workout.", "Remove") {
                            draft.exercises.remove(exercise)
                            redraw()
                        }
                    }
                }
            }
        }
        button(if (optionsShown) "Hide Workout Options" else "Workout Options") {
            optionsShown = !optionsShown
            hideKeyboard()
            redraw()
        }
        if (optionsShown) {
            field("Workout name (optional)", draft.name, limit = 160)
                .doAfterTextChanged { draft.name = it.toString() }
            restControls()
            field("Workout notes (optional)", draft.notes, limit = 1000, multiline = true)
                .doAfterTextChanged { draft.notes = it.toString() }
            button("Discard Workout") {
                confirm("Discard this workout?", "Your current sets and notes will be lost.", "Discard") {
                    if (model.discardWorkout()) back()
                }
            }
        }
        updateTimers()
    }

    private fun saveWorkout() {
        validate {
            hideKeyboard()
            val owner = viewLifecycleOwner
            model.finishWorkout { id ->
                if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) showSummary(id)
            }
        }
    }

    private fun LinearLayout.setEditor(exercise: String, set: SetDraft, index: Int, remove: () -> Unit) {
        val editor = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        addView(editor, LinearLayout.LayoutParams(-1, -2))
        editor.renderSet(exercise, set, index, remove)
        if (set.id == setToShow) {
            setToShow = null
            val viewport = requireView().findViewById<ScrollView>(R.id.strength_scroll)
            viewport.doOnNextLayout {
                viewport.post {
                    if (editor.isAttachedToWindow) {
                        editor.requestRectangleOnScreen(Rect(0, 0, editor.width, editor.height), true)
                    }
                }
            }
        }
    }

    private fun LinearLayout.renderSet(exercise: String, set: SetDraft, index: Int, remove: () -> Unit) {
        val header = formRow()
        header.weighted(header.textLine("Set $index"))
        val showNote = set.note.isNotBlank() || set.id in openSetNotes
        val addNote = if (showNote) null else header.button("Add Note") {}
        addNote?.let {
            it.contentDescription = "Add note for $exercise set $index"
            header.weighted(it)
        }
        val row = formRow()
        val weight = row.weightedField("Weight (lb)", set.weight,
            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL, limit = 24, weight = 1.3f)
        val reps = row.weightedField("Reps", set.reps, InputType.TYPE_CLASS_NUMBER, limit = 12)
        weight.contentDescription = "$exercise, set $index, weight in pounds"
        reps.contentDescription = "$exercise, set $index, reps"
        weight.doAfterTextChanged { set.weight = it.toString() }
        reps.doAfterTextChanged { set.reps = it.toString() }
        row.addView(ImageButton(context, null, android.R.attr.borderlessButtonStyle).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            setColorFilter(MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface))
            contentDescription = "Remove $exercise set $index"
            setOnClickListener { remove() }
        }, LinearLayout.LayoutParams(context.dp(48), context.dp(48)))

        if (showNote) setNoteField(exercise, set, index)
        addNote?.setOnClickListener {
            addNote.isVisible = false
            openSetNotes.add(set.id)
            setNoteField(exercise, set, index).requestFocus()
        }
    }

    private fun LinearLayout.setNoteField(exercise: String, set: SetDraft, index: Int) =
        field("Set note (optional)", set.note, limit = 500, multiline = true).apply {
            contentDescription = "$exercise, set $index, note"
            doAfterTextChanged { set.note = it.toString() }
        }

    private fun requestRestNotificationsIfNeeded() {
        if (Build.VERSION.SDK_INT < 33 || requestedRestNotifications ||
            !model.rest.isRunning(SystemClock.elapsedRealtime())
        ) return
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            requestedRestNotifications = true
            requestRestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun LinearLayout.restControls() = card {
        heading("Rest Timer")
        remainingLabel = textLine("")
        val presets = formRow()
        for (seconds in listOf(60, 90)) {
            presets.weighted(presets.button("$seconds seconds") {
                model.selectRest(seconds)
                updateTimers()
            })
        }
        val actions = formRow()
        val action = actions.button("Start Rest", primary = true) {
            model.toggleRest()
            requestRestNotificationsIfNeeded()
            updateTimers()
        }
        restAction = action
        actions.weighted(action)
        actions.weighted(actions.button("Reset") {
            model.resetRest()
            updateTimers()
        })
    }

    private fun updateTimers() {
        val now = SystemClock.elapsedRealtime()
        val rest = model.rest
        model.workout?.let {
            val restStatus = if (rest.isFinished(now)) " · Rest complete" else ""
            elapsedLabel?.show("Elapsed time · ${duration(now - it.startedAtElapsed)}$restStatus")
        }
        val remaining = duration(rest.remaining(now) + 999)
        remainingLabel?.show(when {
            rest.isFinished(now) -> "$remaining · Rest complete"
            rest.isPaused() -> "$remaining remaining · Paused"
            rest.isRunning(now) -> "$remaining remaining"
            else -> "$remaining selected"
        })
        restAction?.show(when {
            rest.isRunning(now) -> "Pause"
            rest.isPaused() -> "Resume"
            else -> "Start Rest"
        })
    }

    private fun TextView.show(value: String) {
        if (text.toString() != value) text = value
    }

    private fun showSummary(id: String) {
        findNavController().navigate(R.id.strength_workout_detail,
            recordArgs(id, summary = true),
            navOptions { popUpTo(R.id.strength_active_workout) { inclusive = true } })
    }
}
