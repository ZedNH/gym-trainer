package com.gymtrainer.app.ui.train

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymtrainer.app.data.CUSTOM_PLAN_ID
import com.gymtrainer.app.data.GymRepository
import com.gymtrainer.app.data.TEMPLATE_PLAN_ID
import com.gymtrainer.app.data.WorkoutDayEntity
import com.gymtrainer.app.data.WorkoutPlanEntity
import com.gymtrainer.app.data.WorkoutSessionEntity
import com.gymtrainer.app.domain.DeloadStatus
import com.gymtrainer.app.domain.Difficulty
import com.gymtrainer.app.domain.HomePrograms
import com.gymtrainer.app.domain.Volume
import com.gymtrainer.app.domain.VolumeMuscle
import com.gymtrainer.app.domain.VolumeStatus
import java.util.Calendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ExLine(val id: String, val name: String, val scheme: String, val muscle: String)
data class DayView(val id: Long, val name: String, val focus: String, val exercises: List<ExLine>) {
    val sets get() = exercises.sumOf { it.scheme.substringBefore(" ").toIntOrNull() ?: 0 }
}
data class TrainState(
    val loaded: Boolean = false,
    val hasPlan: Boolean = false,
    val planName: String = "",
    val notes: String = "",
    val weeks: Int = 0,
    val days: List<DayView> = emptyList(),
    /** Index into [days] of the next session in the rotation. */
    val nextIndex: Int = 0,
    /** Completed gym sessions on the active plan. */
    val sessionsDone: Int = 0,
    /** Gym catalog workout title → runnable template day id. */
    val templateIds: Map<String, Long> = emptyMap(),
    val custom: List<DayView> = emptyList(),
    val deload: DeloadStatus = DeloadStatus(false),
    /** Hard sets per muscle this week. */
    val volume: Map<VolumeMuscle, Double> = emptyMap(),
    val level: Difficulty = Difficulty.BEGINNER
) {
    val volumeRange get() = Volume.target(level)
    val lowMuscles get() = VolumeMuscle.entries.filter { Volume.status(volume[it] ?: 0.0, volumeRange) == VolumeStatus.LOW }
    val next: DayView? get() = days.getOrNull(nextIndex)
}

/** Monday 00:00 of the week [weeksAgo] weeks back (0 = this week, -1 = next week). */
fun weekStart(weeksAgo: Int): Long = Calendar.getInstance().apply {
    firstDayOfWeek = Calendar.MONDAY
    set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    if (timeInMillis > System.currentTimeMillis()) add(Calendar.WEEK_OF_YEAR, -1)
    add(Calendar.WEEK_OF_YEAR, -weeksAgo)
}.timeInMillis

@OptIn(ExperimentalCoroutinesApi::class)
class TrainViewModel(private val repo: GymRepository) : ViewModel() {

    private val planDays = repo.observeActivePlan().flatMapLatest { plan ->
        if (plan == null) flowOf<Pair<WorkoutPlanEntity?, List<WorkoutDayEntity>>>(null to emptyList())
        else repo.observeDays(plan.id).mapLatest { plan to it }
    }

    private val tick = MutableStateFlow(0)

    private data class Inputs(
        val plan: Pair<WorkoutPlanEntity?, List<WorkoutDayEntity>>,
        val templates: List<WorkoutDayEntity>,
        val custom: List<WorkoutDayEntity>,
        val sessions: List<WorkoutSessionEntity>
    )

    val state: StateFlow<TrainState> = combine(
        planDays,
        repo.observeDays(TEMPLATE_PLAN_ID),
        repo.observeDays(CUSTOM_PLAN_ID),
        repo.observeCompletedSessions(),
        tick
    ) { plan, templates, custom, sessions, _ -> Inputs(plan, templates, custom, sessions) }
        .mapLatest { (planPair, templates, custom, sessions) ->
            val (plan, days) = planPair
            val training = days.filter { !it.isRest }
            val done = sessions.count { it.kind == "GYM" && it.planId == plan?.id }
            TrainState(
                loaded = true,
                hasPlan = plan != null,
                planName = plan?.name ?: "",
                notes = plan?.notes ?: "",
                weeks = plan?.durationWeeks ?: 0,
                days = training.map { view(it) },
                nextIndex = if (training.isEmpty()) 0 else done % training.size,
                sessionsDone = done,
                templateIds = templates.associate { it.name to it.id },
                custom = custom.map { view(it) },
                deload = repo.deloadStatus(),
                volume = repo.weeklyVolume(weekStart(0), weekStart(-1)),
                level = repo.getProfile()?.let { HomePrograms.personalSpec(it).level } ?: Difficulty.BEGINNER
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TrainState())

    private suspend fun view(day: WorkoutDayEntity): DayView {
        val lines = repo.dayExercises(day.id).map { de ->
            val ex = repo.exercise(de.exerciseId)
            val scheme = if (ex?.timed == true) "${de.sets} × ${de.repMin}s"
            else if (de.repMin == de.repMax) "${de.sets} × ${de.repMin}" else "${de.sets} × ${de.repMin}–${de.repMax}"
            ExLine(de.exerciseId, ex?.name ?: de.exerciseId, scheme, ex?.muscleGroup ?: "")
        }
        return DayView(day.id, day.name, day.focus, lines)
    }

    fun regenerate(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            val profile = repo.getProfile() ?: return@launch
            repo.generateAndSavePlan(profile)
            withContext(Dispatchers.Main) { onDone() }
        }
    }

    fun startDeload() { viewModelScope.launch { repo.startDeload(); tick.value++ } }
    fun skipDeload() { viewModelScope.launch { repo.skipDeload(); tick.value++ } }

    fun deleteRoutine(dayId: Long) {
        viewModelScope.launch { repo.deleteRoutine(dayId) }
    }
}
