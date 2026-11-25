package com.gymtrainer.app.domain

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt

// =====================================================================================
//  Coaching engine: progressive overload, deloads, warm-ups and weekly volume.
//  Pure Kotlin (no Android) so every rule is unit-tested.
// =====================================================================================

/** One logged working set. */
data class PastSet(val weightKg: Double, val reps: Int, val effort: Effort)

/** All working sets of one exercise in one session. */
data class PastSession(val sets: List<PastSet>) {
    val topWeight get() = sets.maxOfOrNull { it.weightKg } ?: 0.0
    val topSets get() = sets.filter { it.weightKg == topWeight }
}

enum class TargetAction(val label: String) {
    START("First session"),
    INCREASE("Level up"),
    BUILD_REPS("Add a rep"),
    REPEAT("Repeat"),
    DECREASE("Reset"),
    DELOAD("Deload")
}

/** What to lift today. [weightKg] is null when there's no history (user picks). */
data class Target(val weightKg: Double?, val reps: Int, val action: TargetAction, val reason: String)

object Progression {

    /** Weight jump for one progression step, by equipment and movement. */
    fun increment(equipment: String, muscleGroup: String, compound: Boolean): Double {
        val lower = muscleGroup == "LEGS" || muscleGroup == "GLUTES"
        return when (equipment) {
            "Barbell" -> if (compound && lower) 5.0 else 2.5
            "Dumbbell" -> 2.0
            "Kettlebell" -> 4.0
            "Machine", "Cable" -> if (lower) 5.0 else 2.5
            "Bodyweight" -> 0.0
            else -> if (compound) 2.5 else 1.25
        }
    }

    /** Rounds to a loadable weight (barbell 2.5 kg, dumbbells 1 kg, others 0.5 kg). */
    fun loadable(kg: Double, equipment: String): Double {
        val step = when (equipment) { "Barbell" -> 2.5; "Dumbbell", "Kettlebell" -> 1.0; else -> 0.5 }
        return (kg / step).roundToInt() * step
    }

    /**
     * Double progression: work inside [repMin]–[repMax]. When every top set reaches [repMax] cleanly,
     * add [increment] and drop back to [repMin]. Missing [repMin] twice in a row at the same weight drops 10%.
     * [history] is newest first and must not contain deload sessions.
     */
    fun next(history: List<PastSession>, repMin: Int, repMax: Int, increment: Double, equipment: String = "Barbell"): Target {
        val last = history.firstOrNull { it.sets.isNotEmpty() }
            ?: return Target(null, repMin, TargetAction.START,
                "First time: choose a weight you could lift for ${repMax + 2} reps, then do $repMin–$repMax with good form.")

        val top = last.topSets
        val w = last.topWeight
        val minReps = top.minOf { it.reps }

        // Bodyweight / unloaded: progress reps only.
        if (w <= 0.0 || increment == 0.0) {
            val best = last.sets.maxOf { it.reps }
            return if (best >= repMax) Target(null, repMax + 2, TargetAction.INCREASE,
                "You hit $best reps. Go for ${repMax + 2}, or add load / a harder variation.")
            else Target(null, best + 1, TargetAction.BUILD_REPS, "Beat last time: aim for ${best + 1} reps per set.")
        }

        if (last.sets.any { it.effort == Effort.PAIN }) {
            return Target(loadable(w * 0.8, equipment), repMin, TargetAction.DECREASE,
                "You reported pain last time. Go 20% lighter and stop if it hurts.")
        }

        val clean = top.none { it.effort == Effort.HARD }
        if (top.all { it.reps >= repMax } && clean) {
            return Target(loadable(w + increment, equipment), repMin, TargetAction.INCREASE,
                "You hit $repMax reps on every set. Add ${fmt(increment)} kg and start again at $repMin.")
        }

        val missed = top.count { it.reps < repMin } * 2 >= top.size || top.count { it.effort == Effort.HARD } * 2 > top.size
        if (missed) {
            val prev = history.drop(1).firstOrNull { it.sets.isNotEmpty() }
            val prevMissed = prev != null && prev.topWeight == w && prev.topSets.count { it.reps < repMin } * 2 >= prev.topSets.size
            return if (prevMissed) Target(loadable(w * 0.9, equipment), repMin, TargetAction.DECREASE,
                "Missed $repMin reps twice at ${fmt(w)} kg. Drop 10% and build back up.")
            else Target(w, repMin, TargetAction.REPEAT, "Last time was tough. Repeat ${fmt(w)} kg and own all $repMin reps.")
        }

        val goal = (minReps + 1).coerceIn(repMin, repMax)
        return Target(w, goal, TargetAction.BUILD_REPS,
            "Same weight. Aim for $goal reps on every set (${top.joinToString("/") { "${it.reps}" }} last time).")
    }

    /** Lighter version of [target] for a deload week. */
    fun deload(target: Target, repMin: Int, equipment: String): Target =
        Target(target.weightKg?.let { loadable(it * Deload.WEIGHT_FACTOR, equipment) }, repMin, TargetAction.DELOAD,
            "Deload week: ~10% lighter, fewer sets, stop 3+ reps before failure. Recovery is when you grow.")

    private fun fmt(v: Double) = if (v % 1.0 == 0.0) v.toInt().toString() else v.toString()
}

// ---------------------------------- Deload ----------------------------------

enum class DeloadKind(val label: String) { SCHEDULED("Planned"), FATIGUE("Fatigue"), MANUAL("Your choice") }

data class DeloadStatus(
    val active: Boolean,
    val kind: DeloadKind? = null,
    val sessionsLeft: Int = 0,
    /** Sessions until the next planned deload week (when not active). */
    val nextInSessions: Int? = null
) {
    val reason: String get() = when (kind) {
        DeloadKind.SCHEDULED -> "Every 5th week is lighter so your joints and nervous system catch up."
        DeloadKind.FATIGUE -> "Your last sessions felt too heavy. A lighter week now beats a plateau later."
        DeloadKind.MANUAL -> "You started a lighter week. Keep moving, but leave plenty in the tank."
        null -> ""
    }
}

object Deload {
    /** One deload week every [CYCLE_WEEKS] weeks (4 hard + 1 light). */
    const val CYCLE_WEEKS = 5
    const val WEIGHT_FACTOR = 0.9
    const val SETS_FACTOR = 0.6

    fun sets(normal: Int): Int = ceil(normal * SETS_FACTOR).toInt().coerceAtLeast(1)

    fun isScheduledWeek(week: Int) = week % CYCLE_WEEKS == CYCLE_WEEKS - 1

    /**
     * @param done completed sessions on the active plan, @param days training days per week.
     * @param manualUntil session count until which a fatigue/manual deload runs (or null).
     * @param skippedWeek a planned deload week the user chose to skip (or null).
     */
    fun status(done: Int, days: Int, manualUntil: Int?, manualKind: DeloadKind?, skippedWeek: Int?): DeloadStatus {
        if (days <= 0) return DeloadStatus(false)
        if (manualUntil != null && manualUntil > done) {
            return DeloadStatus(true, manualKind ?: DeloadKind.MANUAL, manualUntil - done)
        }
        val week = done / days
        if (isScheduledWeek(week) && skippedWeek != week) {
            return DeloadStatus(true, DeloadKind.SCHEDULED, days - done % days)
        }
        var w = week + 1
        while (!isScheduledWeek(w)) w++
        return DeloadStatus(false, nextInSessions = w * days - done)
    }

    /** Fatigue = at least 2 of the last 3 sessions had ≥40% of sets "too heavy" or any pain. Newest first. */
    fun fatigued(recentSessions: List<List<Effort>>): Boolean =
        recentSessions.take(3).count { efforts ->
            efforts.isNotEmpty() && (efforts.any { it == Effort.PAIN } ||
                efforts.count { it == Effort.HARD } >= max(1, ceil(efforts.size * 0.4).toInt()))
        } >= 2
}

// ---------------------------------- Warm-ups ----------------------------------

/** One warm-up set. [perSide] is the plate breakdown for barbell lifts. */
data class WarmSet(val weight: Double, val reps: Int, val isBar: Boolean, val perSide: List<Double>)

object WarmUp {
    private val KG_PLATES = listOf(25.0, 20.0, 15.0, 10.0, 5.0, 2.5, 1.25)
    private val LB_PLATES = listOf(45.0, 35.0, 25.0, 10.0, 5.0, 2.5)

    fun bar(imperial: Boolean) = if (imperial) 45.0 else 20.0

    /** Ramp-up sets for a working weight [work] (in the user's units). Empty when no warm-up is needed. */
    fun plan(work: Double, equipment: String, compound: Boolean, imperial: Boolean): List<WarmSet> {
        if (!compound || work <= 0) return emptyList()
        val k = if (imperial) 2.2 else 1.0
        return when (equipment) {
            "Barbell" -> {
                val bar = bar(imperial)
                if (work < bar * 1.5) return emptyList()
                val step = if (imperial) 5.0 else 2.5
                val out = mutableListOf(WarmSet(bar, 10, true, emptyList()))
                listOf(0.5 to 5, 0.7 to 3, 0.85 to 1).forEach { (pct, reps) ->
                    val wgt = floor(work * pct / step) * step
                    if (wgt > out.last().weight && wgt < work - step / 2) out += WarmSet(wgt, reps, false, plates(wgt, imperial))
                }
                out
            }
            "Dumbbell", "Kettlebell" -> if (work < 14 * k) emptyList() else ramp(work, if (imperial) 5.0 else 2.0, listOf(0.5 to 8, 0.75 to 4))
            "Machine", "Cable" -> if (work < 40 * k) emptyList() else ramp(work, if (imperial) 10.0 else 5.0, listOf(0.5 to 8, 0.75 to 3))
            else -> emptyList()
        }
    }

    private fun ramp(work: Double, step: Double, pcts: List<Pair<Double, Int>>): List<WarmSet> =
        pcts.map { (p, r) -> WarmSet(floor(work * p / step) * step, r, false, emptyList()) }
            .filter { it.weight > 0 && it.weight < work }.distinctBy { it.weight }

    /** Plates per side to load [total] on a standard bar. Leftover that can't be loaded is ignored. */
    fun plates(total: Double, imperial: Boolean): List<Double> {
        var side = (total - bar(imperial)) / 2.0
        if (side <= 0) return emptyList()
        val out = mutableListOf<Double>()
        for (p in if (imperial) LB_PLATES else KG_PLATES) while (side + 1e-6 >= p) { out += p; side -= p }
        return out
    }
}

// ---------------------------------- Weekly volume ----------------------------------

/** Muscles tracked for weekly hard sets (finer than [MuscleGroup]). */
enum class VolumeMuscle(val label: String, val category: GymCategory) {
    CHEST("Chest", GymCategory.CHEST),
    BACK("Back", GymCategory.BACK),
    SHOULDERS("Shoulders", GymCategory.SHOULDERS),
    BICEPS("Biceps", GymCategory.ARMS),
    TRICEPS("Triceps", GymCategory.ARMS),
    QUADS("Quads", GymCategory.LEGS),
    HAMSTRINGS("Hamstrings", GymCategory.LEGS),
    GLUTES("Glutes", GymCategory.GLUTES),
    CALVES("Calves", GymCategory.LEGS),
    CORE("Core", GymCategory.CORE);

    companion object {
        /** Maps free-exercise-db muscle names. Neck/forearms aren't tracked. */
        fun of(name: String): VolumeMuscle? = when (name.trim().lowercase()) {
            "chest" -> CHEST
            "lats", "middle back", "lower back", "traps" -> BACK
            "shoulders" -> SHOULDERS
            "biceps" -> BICEPS
            "triceps" -> TRICEPS
            "quadriceps", "adductors" -> QUADS
            "hamstrings" -> HAMSTRINGS
            "glutes", "abductors" -> GLUTES
            "calves" -> CALVES
            "abdominals" -> CORE
            else -> null
        }
    }
}

enum class VolumeStatus(val label: String) { LOW("Below target"), OK("On target"), HIGH("Very high") }

object Volume {
    /** Set credit per muscle: 1 for a primary muscle, ½ for a secondary one. */
    fun credit(primary: String, secondary: String): Map<VolumeMuscle, Double> {
        val out = linkedMapOf<VolumeMuscle, Double>()
        primary.split(',').mapNotNull { VolumeMuscle.of(it) }.forEach { out[it] = 1.0 }
        secondary.split(',').mapNotNull { VolumeMuscle.of(it) }.forEach { if (it !in out) out[it] = 0.5 }
        return out
    }

    /** Adds up credits for a list of sets, given as (primary, secondary) muscle strings. */
    fun tally(sets: List<Pair<String, String>>): Map<VolumeMuscle, Double> {
        val totals = VolumeMuscle.entries.associateWith { 0.0 }.toMutableMap()
        sets.forEach { (p, s) -> credit(p, s).forEach { (m, c) -> totals[m] = totals.getValue(m) + c } }
        return totals
    }

    /** Weekly hard-set target range per muscle, by training level. */
    fun target(level: Difficulty): IntRange = when (level) {
        Difficulty.BEGINNER -> 6..12
        Difficulty.INTERMEDIATE -> 10..18
        Difficulty.ADVANCED -> 12..22
    }

    fun status(sets: Double, range: IntRange): VolumeStatus = when {
        sets < range.first -> VolumeStatus.LOW
        sets > range.last * 1.25 -> VolumeStatus.HIGH
        else -> VolumeStatus.OK
    }
}
