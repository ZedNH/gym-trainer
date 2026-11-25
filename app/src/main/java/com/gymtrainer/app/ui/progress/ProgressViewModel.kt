package com.gymtrainer.app.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymtrainer.app.data.GymRepository
import com.gymtrainer.app.data.PersonalRecordEntity
import com.gymtrainer.app.domain.MuscleGroup
import com.gymtrainer.app.domain.Units
import com.gymtrainer.app.ui.Format
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

data class SessionRow(val name: String, val date: String, val duration: String, val home: Boolean, val calories: Int)

data class ProgressState(
    val totalKcal: Int = 0,
    val totalMinutes: Int = 0,
    val bmi: Double = 0.0,
    val units: Units = Units.METRIC,
    val weightSeries: List<Float> = emptyList(),
    val latestWeight: String = "—",
    val records: List<PersonalRecordEntity> = emptyList(),
    val sessions: List<SessionRow> = emptyList(),
    val totalSessions: Int = 0,
    val weeklySets: List<Pair<String, Float>> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
class ProgressViewModel(private val repo: GymRepository) : ViewModel() {

    val state: StateFlow<ProgressState> = combine(
        repo.observeMeasurements(),
        repo.observeRecords(),
        repo.observeCompletedSessions(),
        repo.observeProfile()
    ) { measures, records, sessions, profile ->
        Quad(measures, records, sessions, profile)
    }.mapLatest { (measures, records, sessions, profile) ->
        val units = Units.from(profile?.units)
        val weights = measures.filter { it.type == "WEIGHT" }.sortedBy { it.date }
        val series = weights.map { Format.weightValue(it.value, units).toFloat() }
        val latest = weights.lastOrNull()?.let { Format.weight(it.value, units) } ?: "—"

        // Weekly sets by muscle group (last 7 days).
        val weekAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)
        val sets = repo.allSets().filter { it.timestamp >= weekAgo }
        val byGroup = linkedMapOf<String, Int>()
        for (s in sets) {
            val ex = repo.exercise(s.exerciseId) ?: continue
            val label = MuscleGroup.from(ex.muscleGroup).label
            byGroup[label] = (byGroup[label] ?: 0) + 1
        }
        val weekly = byGroup.entries.map { (if (it.key == "Shoulders") "Delts" else it.key) to it.value.toFloat() }

        val latestKg = weights.lastOrNull()?.value ?: profile?.weightKg ?: 0.0
        ProgressState(
            totalKcal = sessions.sumOf { it.calories },
            totalMinutes = sessions.sumOf { it.durationSec } / 60,
            bmi = com.gymtrainer.app.ui.tools.Calc.bmi(latestKg, profile?.heightCm ?: 0.0),
            units = units,
            weightSeries = series,
            latestWeight = latest,
            records = records.take(8),
            sessions = sessions.take(30).map {
                SessionRow(it.dayName, Format.date(it.startedAt), Format.duration(it.durationSec), it.kind == "HOME", it.calories)
            },
            totalSessions = sessions.size,
            weeklySets = weekly
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProgressState())

    fun addWeight(displayValue: Double) {
        viewModelScope.launch {
            val units = Units.from(repo.getProfile()?.units)
            repo.addMeasurement("WEIGHT", Format.toKg(displayValue, units))
        }
    }

    private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
}
