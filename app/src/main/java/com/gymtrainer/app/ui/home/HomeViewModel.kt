package com.gymtrainer.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymtrainer.app.data.ChallengeProgressEntity
import com.gymtrainer.app.data.GymRepository
import com.gymtrainer.app.data.UserProfileEntity
import com.gymtrainer.app.data.WorkoutPlanEntity
import com.gymtrainer.app.data.WorkoutSessionEntity
import com.gymtrainer.app.domain.BodyArea
import com.gymtrainer.app.domain.Difficulty
import com.gymtrainer.app.domain.HomePrograms
import com.gymtrainer.app.domain.HomeWorkout
import com.gymtrainer.app.domain.Program
import com.gymtrainer.app.domain.TrainingPlace
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar

data class ProgramCard(val id: String, val title: String, val subtitle: String, val done: Int, val total: Int, val nextDay: Int?)
data class DayDot(val label: String, val date: Int, val done: Boolean, val today: Boolean)

data class HomeState(
    val loaded: Boolean = false,
    val name: String = "",
    val place: TrainingPlace = TrainingPlace.BOTH,
    val level: Difficulty = Difficulty.BEGINNER,
    val week: List<DayDot> = emptyList(),
    val workoutsThisWeek: Int = 0,
    val kcalThisWeek: Int = 0,
    val minutesThisWeek: Int = 0,
    val streak: Int = 0,
    val personal: ProgramCard? = null,
    val personalNext: HomeWorkout? = null,
    val challenges: List<ProgramCard> = emptyList(),
    val gymDayId: Long? = null,
    val gymDayName: String? = null,
    val gymDayFocus: String? = null,
    val gymExerciseCount: Int = 0,
    val gymPlanName: String? = null,
    val recommended: List<HomeWorkout> = emptyList()
)

/** Progress summary for any day-based program. */
fun programCard(p: Program, progress: List<ChallengeProgressEntity>): ProgramCard {
    val done = progress.filter { it.challengeId == p.id }.map { it.day }.toSet()
    val next = (1..p.totalDays).firstOrNull { it !in done && !p.isRestDay(it) }
    val completed = (1..p.totalDays).count { it in done || (p.isRestDay(it) && (next == null || it < next)) }
    return ProgramCard(p.id, p.title, p.subtitle, completed, p.totalDays, next)
}

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(private val repo: GymRepository) : ViewModel() {

    private data class Inputs(
        val profile: UserProfileEntity?,
        val plan: WorkoutPlanEntity?,
        val sessions: List<WorkoutSessionEntity>,
        val progress: List<ChallengeProgressEntity>
    )

    val state: StateFlow<HomeState> = combine(
        repo.observeProfile(), repo.observeActivePlan(), repo.observeCompletedSessions(), repo.observeAllChallengeProgress()
    ) { profile, plan, sessions, progress -> Inputs(profile, plan, sessions, progress) }
        .mapLatest { (profile, plan, sessions, progress) ->
            val gymSessions = sessions.filter { it.kind == "GYM" && it.planId == plan?.id }
            val days = plan?.let { repo.days(it.id).filter { d -> !d.isRest } } ?: emptyList()
            val today = if (days.isNotEmpty()) days[gymSessions.size % days.size] else null

            val weekStart = startOfWeek()
            val thisWeek = sessions.filter { it.startedAt >= weekStart }
            val doneDays = thisWeek.map { dayIndexInWeek(it.startedAt) }.toSet()
            val todayIdx = dayIndexInWeek(System.currentTimeMillis())
            val labels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
            val cal = Calendar.getInstance().apply { timeInMillis = weekStart }

            val personal = profile?.let { HomePrograms.personalProgram(it) }
            val personalCard = personal?.let { programCard(it, progress) }
            val spec = profile?.let { HomePrograms.personalSpec(it) }

            HomeState(
                loaded = true,
                name = profile?.name ?: "",
                place = TrainingPlace.from(profile?.trainingPlace),
                level = spec?.level ?: Difficulty.BEGINNER,
                week = labels.mapIndexed { i, l ->
                    val date = cal.get(Calendar.DAY_OF_MONTH); cal.add(Calendar.DAY_OF_YEAR, 1)
                    DayDot(l, date, i in doneDays, i == todayIdx)
                },
                workoutsThisWeek = thisWeek.size,
                kcalThisWeek = thisWeek.sumOf { it.calories },
                minutesThisWeek = thisWeek.sumOf { it.durationSec } / 60,
                streak = streak(sessions),
                personal = personalCard,
                personalNext = personalCard?.nextDay?.let { personal?.workoutFor(it) },
                challenges = HomePrograms.challenges.map { programCard(it, progress) },
                gymDayId = today?.id,
                gymDayName = today?.name,
                gymDayFocus = today?.focus,
                gymExerciseCount = today?.let { repo.dayExercises(it.id).size } ?: 0,
                gymPlanName = plan?.name,
                recommended = HomePrograms.recommended(spec?.level ?: Difficulty.BEGINNER, spec?.focus ?: setOf(BodyArea.FULL_BODY))
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeState())

    private fun startOfWeek(): Long = Calendar.getInstance().apply {
        firstDayOfWeek = Calendar.MONDAY
        set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        if (timeInMillis > System.currentTimeMillis()) add(Calendar.WEEK_OF_YEAR, -1)
    }.timeInMillis

    /** 0 = Monday … 6 = Sunday. */
    private fun dayIndexInWeek(ms: Long): Int {
        val d = Calendar.getInstance().apply { timeInMillis = ms }.get(Calendar.DAY_OF_WEEK)
        return (d + 5) % 7
    }

    private fun streak(sessions: List<WorkoutSessionEntity>): Int {
        if (sessions.isEmpty()) return 0
        val days = sessions.map { epochDay(it.startedAt) }.toSortedSet()
        var cursor = epochDay(System.currentTimeMillis())
        if (cursor !in days) cursor -= 1
        var n = 0
        while (cursor in days) { n++; cursor -= 1 }
        return n
    }

    private fun epochDay(ms: Long): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = ms }
        val offset = cal.get(Calendar.ZONE_OFFSET) + cal.get(Calendar.DST_OFFSET)
        return (ms + offset) / 86_400_000L
    }
}
