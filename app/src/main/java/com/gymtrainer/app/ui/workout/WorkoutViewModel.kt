package com.gymtrainer.app.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymtrainer.app.data.ExerciseEntity
import com.gymtrainer.app.data.GymRepository
import com.gymtrainer.app.domain.Deload
import com.gymtrainer.app.domain.DeloadStatus
import com.gymtrainer.app.domain.Effort
import com.gymtrainer.app.domain.Progression
import com.gymtrainer.app.domain.Target
import com.gymtrainer.app.domain.TargetAction
import com.gymtrainer.app.domain.Units
import com.gymtrainer.app.ui.Format
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SetEntry(
    val setNumber: Int,
    val weight: String = "",
    val reps: String = "",
    val done: Boolean = false,
    val effort: Effort? = null
)

data class WorkoutExercise(
    val exercise: ExerciseEntity,
    val repMin: Int,
    val repMax: Int,
    val restSeconds: Int,
    val sets: List<SetEntry>,
    val lastSession: String?,
    val target: Target? = null,
    /** Last session's sets, e.g. "60×8 · 60×8 · 60×7". */
    val lastSets: List<String> = emptyList()
)

data class WorkoutSummary(
    val totalSets: Int,
    val totalVolume: Double,
    val durationSec: Int,
    val exercisesDone: Int,
    val deloadStarted: Boolean = false
)

data class WorkoutState(
    val loading: Boolean = true,
    val dayName: String = "",
    val units: Units = Units.METRIC,
    val exercises: List<WorkoutExercise> = emptyList(),
    val currentIndex: Int = 0,
    val finished: Boolean = false,
    val summary: WorkoutSummary? = null,
    val deload: DeloadStatus? = null
)

class WorkoutViewModel(private val repo: GymRepository) : ViewModel() {

    private val _state = MutableStateFlow(WorkoutState())
    val state: StateFlow<WorkoutState> = _state.asStateFlow()

    private val _rest = MutableStateFlow(0)
    val restRemaining: StateFlow<Int> = _rest.asStateFlow()
    private val _restRunning = MutableStateFlow(false)
    val restRunning: StateFlow<Boolean> = _restRunning.asStateFlow()

    private var sessionId: Long = -1
    private var restJob: Job? = null
    private var started = false

    fun start(dayId: Long) {
        if (started) return
        started = true
        viewModelScope.launch {
            val profile = repo.getProfile()
            val units = Units.from(profile?.units)
            val day = repo.day(dayId)
            if (day == null) {
                _state.update { it.copy(loading = false) }
                return@launch
            }
            // Deload applies to the active plan's own days only.
            val status = repo.deloadStatus()
            val onPlan = repo.activePlan()?.id == day.planId
            val deload = status.takeIf { it.active && onPlan }
            sessionId = repo.startSession(day.planId, day)
            if (deload != null) repo.markDeloadSession(sessionId)
            val dayExercises = repo.dayExercises(dayId)
            val list = dayExercises.mapNotNull { de ->
                val ex = repo.exercise(de.exerciseId) ?: return@mapNotNull null
                build(ex, de.repMin, de.repMax, de.restSeconds, de.sets, deload != null, units)
            }
            _state.update {
                it.copy(loading = false, dayName = day.name, units = units, exercises = list, deload = deload)
            }
        }
    }

    /** Builds one exercise with today's target pre-filled (weight, and reps once there's history). */
    private suspend fun build(ex: ExerciseEntity, repMin: Int, repMax: Int, rest: Int, sets: Int, deload: Boolean, units: Units): WorkoutExercise {
        val target = if (ex.timed) null else repo.targetFor(ex, repMin, repMax, deload)
        val weight = target?.weightKg?.let { Format.trim(Format.weightValue(it, units)) } ?: ""
        val reps = if (target != null && target.action != TargetAction.START) target.reps.toString() else ""
        val history = repo.exerciseHistory(ex.id, 1).firstOrNull()?.sets.orEmpty()
        val lastSets = history.map { if (it.weightKg > 0) "${Format.trim(Format.weightValue(it.weightKg, units))}×${it.reps}" else "${it.reps} reps" }
        val count = if (deload) Deload.sets(sets) else sets
        return WorkoutExercise(
            exercise = ex, repMin = repMin, repMax = repMax, restSeconds = rest,
            sets = (1..count).map { SetEntry(setNumber = it, weight = weight, reps = reps) },
            lastSession = null, target = target, lastSets = lastSets
        )
    }

    fun setWeight(exIndex: Int, setIndex: Int, value: String) =
        mutateSet(exIndex, setIndex) { it.copy(weight = value.filter { c -> c.isDigit() || c == '.' }) }

    fun setReps(exIndex: Int, setIndex: Int, value: String) =
        mutateSet(exIndex, setIndex) { it.copy(reps = value.filter { c -> c.isDigit() }) }

    fun completeSet(exIndex: Int, setIndex: Int, effort: Effort) {
        val st = _state.value
        val wex = st.exercises.getOrNull(exIndex) ?: return
        val set = wex.sets.getOrNull(setIndex) ?: return
        val weightKg = Format.toKg(set.weight.toDoubleOrNull() ?: 0.0, st.units)
        val reps = set.reps.toIntOrNull() ?: 0
        viewModelScope.launch {
            repo.logSet(sessionId, wex.exercise, set.setNumber, weightKg, reps, effort.name)
        }
        mutateSet(exIndex, setIndex) { it.copy(done = true, effort = effort) }
        startRest(wex.restSeconds)
    }

    fun addSet(exIndex: Int) {
        _state.update { st ->
            val list = st.exercises.toMutableList()
            val wex = list[exIndex]
            val sets = wex.sets + SetEntry(setNumber = wex.sets.size + 1, weight = wex.sets.lastOrNull()?.weight ?: "",
                reps = wex.target?.takeIf { it.action != TargetAction.START }?.reps?.toString() ?: "")
            list[exIndex] = wex.copy(sets = sets)
            st.copy(exercises = list)
        }
    }

    fun goTo(index: Int) {
        _state.update { it.copy(currentIndex = index.coerceIn(0, (it.exercises.size - 1).coerceAtLeast(0))) }
    }

    fun replaceExercise(exIndex: Int, newExerciseId: String) {
        viewModelScope.launch {
            val newEx = repo.exercise(newExerciseId) ?: return@launch
            val st0 = _state.value
            val old = st0.exercises.getOrNull(exIndex) ?: return@launch
            val built = build(newEx, newEx.repMin, newEx.repMax, newEx.restSeconds, old.sets.size, false, st0.units)
                .let { if (st0.deload != null) it.copy(target = it.target?.let { t -> Progression.deload(t, newEx.repMin, newEx.equipment) }) else it }
            _state.update { st ->
                val list = st.exercises.toMutableList()
                list[exIndex] = built.copy(sets = built.sets.take(old.sets.size).ifEmpty { built.sets })
                st.copy(exercises = list)
            }
        }
    }

    suspend fun alternatives(exId: String): List<ExerciseEntity> = repo.alternativesFor(exId)

    fun finish() {
        viewModelScope.launch {
            repo.finishSession(sessionId)
            val deloadStarted = repo.checkFatigue()
            val logs = repo.setsForSession(sessionId)
            val volume = logs.sumOf { it.weightKg * it.reps }
            val session = repo.session(sessionId)
            _state.update {
                it.copy(
                    finished = true,
                    summary = WorkoutSummary(
                        totalSets = logs.size,
                        totalVolume = volume,
                        durationSec = session?.durationSec ?: 0,
                        exercisesDone = logs.map { l -> l.exerciseId }.distinct().size,
                        deloadStarted = deloadStarted
                    )
                )
            }
        }
    }

    // ---- Rest timer ----
    fun startRest(seconds: Int) {
        restJob?.cancel()
        _rest.value = seconds
        _restRunning.value = true
        restJob = viewModelScope.launch {
            while (_rest.value > 0 && _restRunning.value) {
                delay(1000)
                _rest.update { (it - 1).coerceAtLeast(0) }
            }
            _restRunning.value = false
        }
    }

    fun toggleRest() {
        if (_restRunning.value) {
            _restRunning.value = false
            restJob?.cancel()
        } else if (_rest.value > 0) {
            startRest(_rest.value)
        }
    }

    fun adjustRest(delta: Int) {
        _rest.update { (it + delta).coerceAtLeast(0) }
    }

    fun skipRest() {
        _restRunning.value = false
        restJob?.cancel()
        _rest.value = 0
    }

    private fun mutateSet(exIndex: Int, setIndex: Int, transform: (SetEntry) -> SetEntry) {
        _state.update { st ->
            val list = st.exercises.toMutableList()
            val wex = list.getOrNull(exIndex) ?: return@update st
            val sets = wex.sets.toMutableList()
            val set = sets.getOrNull(setIndex) ?: return@update st
            sets[setIndex] = transform(set)
            list[exIndex] = wex.copy(sets = sets)
            st.copy(exercises = list)
        }
    }
}
