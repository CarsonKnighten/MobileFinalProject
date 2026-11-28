package com.example.mobilefinalproject.strength

import android.app.Application
import android.database.sqlite.SQLiteException
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.mobilefinalproject.shell.AppSession
import com.example.mobilefinalproject.strength.data.StrengthRepository
import com.example.mobilefinalproject.strength.data.strengthDatabaseName
import com.example.mobilefinalproject.strength.domain.RestTimer
import com.example.mobilefinalproject.strength.domain.Routine
import com.example.mobilefinalproject.strength.domain.RoutineDraft
import com.example.mobilefinalproject.strength.domain.RoutineExerciseDraft
import com.example.mobilefinalproject.strength.domain.SetDraft
import com.example.mobilefinalproject.strength.domain.StrengthData
import com.example.mobilefinalproject.strength.domain.WorkoutDraft
import com.example.mobilefinalproject.strength.domain.WorkoutExerciseDraft
import com.example.mobilefinalproject.strength.domain.validateWorkoutName
import com.example.mobilefinalproject.strength.timer.RestTimerService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StrengthViewModel(application: Application) : AndroidViewModel(application) {
    private val databaseName = strengthDatabaseName(application, requireNotNull(AppSession.account))
    private val dataState = MutableLiveData<StrengthData>()
    val data: LiveData<StrengthData> = dataState
    private val errorState = MutableLiveData<String?>()
    val error: LiveData<String?> = errorState
    private val savingState = MutableLiveData(false)
    val saving: LiveData<Boolean> = savingState

    var workout: WorkoutDraft? = null
        private set
    var lastFinishedId: String? = null
        private set
    var routineDraft: RoutineDraft? = null
        private set
    val rest = RestTimer()

    init {
        refresh()
    }

    fun clearError() {
        errorState.value = null
    }

    fun selectRest(seconds: Int) {
        rest.select(seconds)
        RestTimerService.cancel(getApplication())
    }

    fun toggleRest() {
        val now = SystemClock.elapsedRealtime()
        if (rest.isRunning(now)) {
            rest.pause(now)
            RestTimerService.cancel(getApplication())
        } else {
            if (rest.isPaused()) rest.resume(now) else rest.start(now)
            scheduleRestNotification()
        }
    }

    fun scheduleRestNotification() {
        val now = SystemClock.elapsedRealtime()
        if (rest.isRunning(now)) RestTimerService.schedule(getApplication(), now + rest.remaining(now))
    }

    fun resetRest() {
        rest.reset()
        RestTimerService.cancel(getApplication())
    }

    override fun onCleared() {
        RestTimerService.cancel(getApplication())
        super.onCleared()
    }

    fun refresh() = write()

    fun setFavorite(name: String, favorite: Boolean) = write(
        change = { setFavorite(name, favorite) }
    )

    fun newRoutine(routine: Routine? = null) {
        routineDraft = if (routine == null) RoutineDraft() else RoutineDraft(
            routine.id, routine.name, routine.exercises.map {
                RoutineExerciseDraft(it.name, it.sets.toString(), it.targetReps.toString())
            }.toMutableList()
        )
    }

    fun saveRoutine(routine: Routine, done: () -> Unit) =
        write(change = { saveRoutine(routine) }, after = done)

    fun removeRoutine(id: String, done: () -> Unit) =
        write(change = { deleteRoutine(id) }, after = done)

    fun startWorkout(routine: Routine? = null) {
        if (workout != null) return
        val title = validateWorkoutName(routine?.name.orEmpty())
        val draft = WorkoutDraft(System.currentTimeMillis(), SystemClock.elapsedRealtime(), title)
        for (plan in routine?.exercises.orEmpty()) {
            val sets = MutableList(plan.sets) { SetDraft() }
            draft.exercises.add(WorkoutExerciseDraft(plan.name, sets, plan.targetReps))
        }
        lastFinishedId = null
        resetRest()
        workout = draft
    }

    fun discardWorkout(): Boolean {
        if (savingState.value == true) return false
        workout = null
        resetRest()
        return true
    }

    fun finishWorkout(done: (String) -> Unit) {
        if (savingState.value == true) return
        val draft = workout ?: return
        val session = draft.finish(SystemClock.elapsedRealtime(), System.currentTimeMillis())
        write(
            change = { saveWorkout(session) },
            committed = {
                workout = null
                lastFinishedId = session.id
                resetRest()
            },
            after = { done(session.id) }
        )
    }

    private fun write(
        change: StrengthRepository.() -> Unit = {},
        committed: () -> Unit = {},
        after: () -> Unit = {}
    ) {
        if (savingState.value == true) return
        savingState.value = true
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    val repository = StrengthRepository(getApplication(), databaseName)
                    try {
                        repository.change()
                        repository.read()
                    } finally {
                        repository.close()
                    }
                }
                committed()
                dataState.value = result
                after()
            } catch (error: IllegalArgumentException) {
                errorState.value = error.message ?: "Check the values you entered."
            } catch (error: SQLiteException) {
                Log.e("StrengthTraining", "Database operation failed", error)
                errorState.value = "Couldn’t read or save strength data. Try again."
            } finally {
                savingState.value = false
            }
        }
    }
}
