package com.gymtrainer.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymtrainer.app.data.ExerciseEntity
import com.gymtrainer.app.data.GymRepository
import com.gymtrainer.app.domain.HomePrograms
import com.gymtrainer.app.domain.HomeWorkout
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class Phase { READY, WORK, REST, DONE }

data class GuidedState(
    val workout: HomeWorkout? = null,
    val exercises: Map<String, ExerciseEntity> = emptyMap(),
    val phase: Phase = Phase.READY,
    val index: Int = 0,
    val remaining: Int = 0,        // seconds left in a timed phase; -1 for rep-based work
    val phaseTotal: Int = 1,
    val paused: Boolean = false,
    val elapsed: Int = 0,
    val calories: Int = 0
)

/** Drives a follow-along home workout: get-ready → exercise → rest → … → done. */
class GuidedViewModel(
    private val repo: GymRepository,
    private val workoutKey: String,
    private val defaultRest: Int,
    private val countdown: Int
) : ViewModel() {

    private val _state = MutableStateFlow(GuidedState())
    val state: StateFlow<GuidedState> = _state.asStateFlow()

    /** Lines for the voice coach. */
    private val _speech = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val speech: SharedFlow<String> = _speech.asSharedFlow()

    private var ticker: Job? = null
    private var weightKg = 70.0

    init {
        viewModelScope.launch {
            val w = HomePrograms.resolve(workoutKey) ?: return@launch
            weightKg = repo.getProfile()?.weightKg ?: 70.0
            _state.value = GuidedState(workout = w, exercises = repo.exerciseMap())
            enter(Phase.READY, 0)
            startTicker()
        }
    }

    private val restSeconds get() = _state.value.workout?.restSeconds ?: defaultRest

    private fun name(i: Int): String {
        val s = _state.value
        val id = s.workout?.items?.getOrNull(i)?.exerciseId ?: return ""
        return s.exercises[id]?.name ?: id
    }

    private fun enter(phase: Phase, index: Int) {
        val w = _state.value.workout ?: return
        val item = w.items.getOrNull(index)
        val secs = when (phase) {
            Phase.READY -> countdown
            Phase.WORK -> if (item?.isTimed == true) item.seconds else -1
            Phase.REST -> restSeconds
            Phase.DONE -> 0
        }
        _state.update { it.copy(phase = phase, index = index, remaining = secs, phaseTotal = secs.coerceAtLeast(1), paused = false) }
        when (phase) {
            Phase.READY -> say("Ready to go. Start with ${name(index)}.")
            Phase.WORK -> say(
                if (item?.isTimed == true) "${name(index)}. ${item.seconds} seconds." else "${name(index)}. ${item?.reps} reps."
            )
            Phase.REST -> say("Take a rest. Next, ${name(index + 1)}.")
            Phase.DONE -> say("Well done! Workout complete.")
        }
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = viewModelScope.launch {
            while (true) {
                delay(1000)
                val s = _state.value
                if (s.paused || s.phase == Phase.DONE) continue
                _state.update { it.copy(elapsed = it.elapsed + 1) }
                if (s.remaining > 0) {
                    val left = s.remaining - 1
                    _state.update { it.copy(remaining = left) }
                    if (left in 1..3) say("$left")
                    if (s.phase == Phase.WORK && s.phaseTotal >= 20 && left == s.phaseTotal / 2) say("Half time.")
                    if (left == 0) onPhaseTimeUp()
                }
            }
        }
    }

    private fun onPhaseTimeUp() {
        val s = _state.value
        when (s.phase) {
            Phase.READY -> enter(Phase.WORK, s.index)
            Phase.WORK -> completeExercise()
            Phase.REST -> enter(Phase.WORK, s.index + 1)
            Phase.DONE -> Unit
        }
    }

    /** User finished the current exercise (Done button) or its timer ran out. */
    fun completeExercise() {
        val s = _state.value
        val w = s.workout ?: return
        if (s.index >= w.items.lastIndex) finish() else enter(Phase.REST, s.index)
    }

    fun next() {
        val s = _state.value
        val w = s.workout ?: return
        when (s.phase) {
            Phase.READY -> enter(Phase.WORK, s.index)
            Phase.WORK -> completeExercise()
            Phase.REST -> enter(Phase.WORK, (s.index + 1).coerceAtMost(w.items.lastIndex))
            Phase.DONE -> Unit
        }
    }

    fun previous() {
        val s = _state.value
        val target = when (s.phase) {
            Phase.REST -> s.index
            else -> (s.index - 1).coerceAtLeast(0)
        }
        enter(Phase.WORK, target)
    }

    fun togglePause() = _state.update { it.copy(paused = !it.paused) }

    fun addRest(seconds: Int) = _state.update {
        if (it.phase == Phase.REST) it.copy(remaining = it.remaining + seconds, phaseTotal = it.phaseTotal + seconds) else it
    }

    private fun finish() {
        val s = _state.value
        val w = s.workout ?: return
        val kcal = w.calories(weightKg, s.elapsed)
        _state.update { it.copy(phase = Phase.DONE, remaining = 0, calories = kcal) }
        say("Well done! Workout complete.")
        viewModelScope.launch {
            repo.recordHomeSession(w.title, s.elapsed, kcal)
            if (w.key.startsWith("c:")) {
                val parts = w.key.split(":")
                val day = parts.getOrNull(2)?.toIntOrNull()
                if (day != null) repo.completeChallengeDay(parts[1], day)
            }
        }
    }

    private fun say(text: String) { _speech.tryEmit(text) }
}
