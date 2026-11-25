package com.gymtrainer.app.domain

import com.gymtrainer.app.data.UserProfileEntity
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.random.Random

/** One step of a guided workout: either reps or a timed hold. */
data class HomeItem(val exerciseId: String, val reps: Int = 0, val seconds: Int = 0) {
    val isTimed get() = seconds > 0
    fun label(): String =
        if (isTimed) String.format(Locale.US, "%02d:%02d", seconds / 60, seconds % 60) else "x$reps"
}

enum class BodyArea(val label: String) {
    FULL_BODY("Full Body"), ABS("Abs"), CHEST("Chest"), ARM("Arms"), LEG("Legs & Butt"), SHOULDER_BACK("Back & Shoulders")
}

/** Browsable workout categories (home). */
enum class WorkoutCategory(val label: String, val blurb: String) {
    FULL_BODY("Full Body", "Total-body strength"),
    ABS("Abs & Core", "Strong, stable midsection"),
    CHEST("Chest", "Push-up progressions"),
    ARM("Arms", "Triceps, biceps, shoulders"),
    LEG("Legs & Butt", "Glutes, quads, hamstrings"),
    SHOULDER_BACK("Back & Shoulders", "Posture and upper back"),
    HIIT("HIIT", "Short, intense intervals"),
    CARDIO("Cardio", "Raise your heart rate"),
    MOBILITY("Mobility", "Stretch, move better, recover"),
    DUMBBELL("Dumbbell", "One pair of dumbbells"),
    KETTLEBELL_BANDS("Bands & Kettlebell", "Minimal kit, maximum results"),
    LOW_IMPACT("Low Impact", "Joint-friendly, no jumping"),
    QUICK("Quick", "Done in 15 minutes or less")
}

data class HomeWorkout(
    val key: String,
    val title: String,
    val subtitle: String,
    val level: Difficulty,
    val area: BodyArea?,
    val items: List<HomeItem>,
    val restSeconds: Int? = null,     // null = use the user's setting
    val stretch: Boolean = false,     // lighter MET for calorie estimates
    val category: WorkoutCategory = WorkoutCategory.FULL_BODY
) {
    fun estimatedSeconds(defaultRest: Int): Int {
        val work = items.sumOf { if (it.isTimed) it.seconds else it.reps * 3 }
        val rest = (restSeconds ?: defaultRest) * (items.size - 1).coerceAtLeast(0)
        return work + rest
    }

    fun estimatedMinutes(defaultRest: Int) = (estimatedSeconds(defaultRest) / 60.0).roundToInt().coerceAtLeast(1)

    fun calories(weightKg: Double, seconds: Int): Int {
        val met = if (stretch) 2.5 else if (category == WorkoutCategory.HIIT || category == WorkoutCategory.CARDIO) 8.0 else 6.0
        return (met * 3.5 * weightKg / 200.0 * (seconds / 60.0)).roundToInt()
    }
}

enum class ProgramKind { CHALLENGE, PLAN, PERSONAL }

/** A multi-day program (30-day challenge or multi-week plan) with progress tracked per day. */
class Program(
    val id: String,
    val title: String,
    val subtitle: String,
    val level: Difficulty?,
    val kind: ProgramKind,
    val totalDays: Int,
    val sessionsPerWeek: Int,
    val minutesPerSession: Int,
    val goalLabel: String,
    private val rest: (Int) -> Boolean,
    private val builder: (Int) -> HomeWorkout?
) {
    val weeks get() = ceil(totalDays / 7.0).toInt()
    fun isRestDay(day: Int) = rest(day)
    fun workoutFor(day: Int): HomeWorkout? = if (day !in 1..totalDays || rest(day)) null else builder(day)
}

// ============================================================================
// Pools for generated sessions
// ============================================================================

private enum class Role { WARMUP, STRENGTH, CORE, CARDIO, STRETCH }

private data class PoolEx(
    val id: String,
    val role: Role,
    val areas: Set<BodyArea>,
    val min: Difficulty,
    val max: Difficulty = Difficulty.ADVANCED,
    val reps: Int = 0,
    val secs: Int = 0,
    val avoid: Set<Limitation> = emptySet(),
    val dumbbell: Boolean = false
)

private val B = Difficulty.BEGINNER
private val I = Difficulty.INTERMEDIATE
private val A = Difficulty.ADVANCED
private val K = Limitation.KNEE
private val S = Limitation.SHOULDER
private val BK = Limitation.BACK
private val W = Limitation.WRIST
private val ALL = setOf(BodyArea.FULL_BODY)
private fun a(vararg x: BodyArea) = x.toSet()

private val POOL: List<PoolEx> = listOf(
    // Warm-up (timed)
    PoolEx("Arm_Circles", Role.WARMUP, ALL, B, secs = 30),
    PoolEx("Shoulder_Circles", Role.WARMUP, ALL, B, secs = 30),
    PoolEx("Standing_Hip_Circles", Role.WARMUP, ALL, B, secs = 30),
    PoolEx("Torso_Rotation", Role.WARMUP, ALL, B, secs = 30),
    PoolEx("Knee_Circles", Role.WARMUP, ALL, B, secs = 30),
    PoolEx("x_Jumping_Jacks", Role.WARMUP, ALL, B, secs = 30, avoid = setOf(K)),
    PoolEx("x_High_Knees", Role.WARMUP, ALL, B, secs = 30, avoid = setOf(K)),
    PoolEx("Inchworm", Role.WARMUP, ALL, B, secs = 30, avoid = setOf(W)),

    // Cardio (timed)
    PoolEx("x_Jumping_Jacks", Role.CARDIO, ALL, B, secs = 30, avoid = setOf(K)),
    PoolEx("x_High_Knees", Role.CARDIO, ALL, B, secs = 30, avoid = setOf(K)),
    PoolEx("Mountain_Climbers", Role.CARDIO, a(BodyArea.ABS, BodyArea.FULL_BODY), B, secs = 30, avoid = setOf(W)),
    PoolEx("Single_Leg_Butt_Kick", Role.CARDIO, ALL, B, secs = 30, avoid = setOf(K)),
    PoolEx("Star_Jump", Role.CARDIO, ALL, I, secs = 30, avoid = setOf(K)),
    PoolEx("Fast_Skipping", Role.CARDIO, ALL, I, secs = 30, avoid = setOf(K)),
    PoolEx("Scissors_Jump", Role.CARDIO, a(BodyArea.LEG), I, secs = 30, avoid = setOf(K)),
    PoolEx("x_Burpees", Role.CARDIO, ALL, I, secs = 30, avoid = setOf(K, W, S)),
    PoolEx("Lateral_Bound", Role.CARDIO, a(BodyArea.LEG), I, secs = 30, avoid = setOf(K)),
    PoolEx("Split_Jump", Role.CARDIO, a(BodyArea.LEG), A, secs = 30, avoid = setOf(K)),
    PoolEx("Knee_Tuck_Jump", Role.CARDIO, ALL, A, secs = 30, avoid = setOf(K)),
    PoolEx("Step-up_with_Knee_Raise", Role.CARDIO, a(BodyArea.LEG), B, secs = 40),

    // Core
    PoolEx("Crunches", Role.CORE, a(BodyArea.ABS), B, reps = 12),
    PoolEx("Air_Bike", Role.CORE, a(BodyArea.ABS), B, reps = 12),
    PoolEx("x_Lying_Leg_Raise", Role.CORE, a(BodyArea.ABS), B, reps = 10, avoid = setOf(BK)),
    PoolEx("Reverse_Crunch", Role.CORE, a(BodyArea.ABS), B, reps = 10),
    PoolEx("Dead_Bug", Role.CORE, a(BodyArea.ABS), B, reps = 10),
    PoolEx("Elbow_to_Knee", Role.CORE, a(BodyArea.ABS), B, reps = 12),
    PoolEx("Oblique_Crunches_-_On_The_Floor", Role.CORE, a(BodyArea.ABS), B, reps = 12),
    PoolEx("Tuck_Crunch", Role.CORE, a(BodyArea.ABS), B, reps = 12),
    PoolEx("Cross-Body_Crunch", Role.CORE, a(BodyArea.ABS), B, reps = 12),
    PoolEx("Alternate_Heel_Touchers", Role.CORE, a(BodyArea.ABS), B, reps = 16),
    PoolEx("Leg_Pull-In", Role.CORE, a(BodyArea.ABS), B, reps = 10),
    PoolEx("Plank", Role.CORE, a(BodyArea.ABS, BodyArea.FULL_BODY), B, secs = 30, avoid = setOf(W)),
    PoolEx("Side_Bridge", Role.CORE, a(BodyArea.ABS), B, secs = 25),
    PoolEx("Flutter_Kicks", Role.CORE, a(BodyArea.ABS), B, secs = 30, avoid = setOf(BK)),
    PoolEx("Russian_Twist", Role.CORE, a(BodyArea.ABS), I, reps = 16, avoid = setOf(BK)),
    PoolEx("Sit-Up", Role.CORE, a(BodyArea.ABS), I, reps = 12, avoid = setOf(BK)),
    PoolEx("Jackknife_Sit-Up", Role.CORE, a(BodyArea.ABS), I, reps = 10, avoid = setOf(BK)),
    PoolEx("Bottoms_Up", Role.CORE, a(BodyArea.ABS), I, reps = 10),
    PoolEx("Seated_Leg_Tucks", Role.CORE, a(BodyArea.ABS), I, reps = 12),
    PoolEx("Side_Jackknife", Role.CORE, a(BodyArea.ABS), I, reps = 10),
    PoolEx("Spider_Crawl", Role.CORE, a(BodyArea.ABS, BodyArea.FULL_BODY), I, reps = 10, avoid = setOf(W)),
    PoolEx("Butt-Ups", Role.CORE, a(BodyArea.ABS), I, reps = 12, avoid = setOf(W)),
    PoolEx("Frog_Sit-Ups", Role.CORE, a(BodyArea.ABS), A, reps = 12, avoid = setOf(BK)),

    // Strength — chest
    PoolEx("Incline_Push-Up", Role.STRENGTH, a(BodyArea.CHEST), B, I, reps = 10, avoid = setOf(W)),
    PoolEx("x_Knee_Push-Ups", Role.STRENGTH, a(BodyArea.CHEST, BodyArea.ARM), B, I, reps = 8, avoid = setOf(W)),
    PoolEx("Incline_Push-Up_Wide", Role.STRENGTH, a(BodyArea.CHEST), B, I, reps = 8, avoid = setOf(W)),
    PoolEx("Isometric_Chest_Squeezes", Role.STRENGTH, a(BodyArea.CHEST), B, secs = 30),
    PoolEx("Pushups", Role.STRENGTH, a(BodyArea.CHEST, BodyArea.ARM, BodyArea.FULL_BODY), I, reps = 10, avoid = setOf(W, S)),
    PoolEx("Push-Up_Wide", Role.STRENGTH, a(BodyArea.CHEST), I, reps = 10, avoid = setOf(W, S)),
    PoolEx("Push_Up_to_Side_Plank", Role.STRENGTH, a(BodyArea.CHEST, BodyArea.ABS, BodyArea.FULL_BODY), I, reps = 8, avoid = setOf(W, S)),
    PoolEx("Push-Ups_With_Feet_Elevated", Role.STRENGTH, a(BodyArea.CHEST, BodyArea.SHOULDER_BACK), A, reps = 10, avoid = setOf(W, S)),
    PoolEx("Plyo_Push-up", Role.STRENGTH, a(BodyArea.CHEST), A, reps = 8, avoid = setOf(W, S)),
    PoolEx("Clock_Push-Up", Role.STRENGTH, a(BodyArea.CHEST), A, reps = 8, avoid = setOf(W, S)),

    // Strength — arms
    PoolEx("Standing_Towel_Triceps_Extension", Role.STRENGTH, a(BodyArea.ARM), B, reps = 12),
    PoolEx("Bench_Dips", Role.STRENGTH, a(BodyArea.ARM), B, reps = 10, avoid = setOf(S, W)),
    PoolEx("Body_Tricep_Press", Role.STRENGTH, a(BodyArea.ARM), I, reps = 10, avoid = setOf(W)),
    PoolEx("Push-Ups_-_Close_Triceps_Position", Role.STRENGTH, a(BodyArea.ARM, BodyArea.CHEST), I, reps = 10, avoid = setOf(W, S)),
    PoolEx("Body-Up", Role.STRENGTH, a(BodyArea.ARM), A, reps = 8, avoid = setOf(W)),

    // Strength — back & shoulders
    PoolEx("Superman", Role.STRENGTH, a(BodyArea.SHOULDER_BACK), B, reps = 10),
    PoolEx("Inchworm", Role.STRENGTH, a(BodyArea.SHOULDER_BACK, BodyArea.FULL_BODY), B, reps = 6, avoid = setOf(W)),
    PoolEx("x_Pike_Push-Ups", Role.STRENGTH, a(BodyArea.SHOULDER_BACK, BodyArea.ARM), I, reps = 8, avoid = setOf(S, W)),
    PoolEx("Hyperextensions_With_No_Hyperextension_Bench", Role.STRENGTH, a(BodyArea.SHOULDER_BACK), I, reps = 12, avoid = setOf(BK)),

    // Strength — legs & glutes
    PoolEx("Bodyweight_Squat", Role.STRENGTH, a(BodyArea.LEG, BodyArea.FULL_BODY), B, reps = 12),
    PoolEx("Chair_Squat", Role.STRENGTH, a(BodyArea.LEG), B, I, reps = 12),
    PoolEx("Butt_Lift_Bridge", Role.STRENGTH, a(BodyArea.LEG), B, reps = 12),
    PoolEx("x_Donkey_Kicks", Role.STRENGTH, a(BodyArea.LEG), B, reps = 12),
    PoolEx("x_Calf_Raises", Role.STRENGTH, a(BodyArea.LEG), B, reps = 15),
    PoolEx("Glute_Kickback", Role.STRENGTH, a(BodyArea.LEG), B, reps = 12),
    PoolEx("Leg_Lift", Role.STRENGTH, a(BodyArea.LEG), B, reps = 12),
    PoolEx("x_Wall_Sit", Role.STRENGTH, a(BodyArea.LEG), B, secs = 30, avoid = setOf(K)),
    PoolEx("x_Bodyweight_Lunges", Role.STRENGTH, a(BodyArea.LEG, BodyArea.FULL_BODY), B, reps = 10, avoid = setOf(K)),
    PoolEx("Single_Leg_Glute_Bridge", Role.STRENGTH, a(BodyArea.LEG), I, reps = 10),
    PoolEx("Kneeling_Squat", Role.STRENGTH, a(BodyArea.LEG), I, reps = 12, avoid = setOf(K)),
    PoolEx("Freehand_Jump_Squat", Role.STRENGTH, a(BodyArea.LEG, BodyArea.FULL_BODY), I, reps = 10, avoid = setOf(K)),
    PoolEx("x_Burpees", Role.STRENGTH, a(BodyArea.FULL_BODY), I, reps = 8, avoid = setOf(K, W, S)),
    PoolEx("Natural_Glute_Ham_Raise", Role.STRENGTH, a(BodyArea.LEG), A, reps = 6, avoid = setOf(K)),

    // Strength — dumbbells (used when the profile has dumbbells)
    PoolEx("Goblet_Squat", Role.STRENGTH, a(BodyArea.LEG, BodyArea.FULL_BODY), B, reps = 12, dumbbell = true),
    PoolEx("Plie_Dumbbell_Squat", Role.STRENGTH, a(BodyArea.LEG), B, reps = 12, dumbbell = true),
    PoolEx("Dumbbell_Lunges", Role.STRENGTH, a(BodyArea.LEG), I, reps = 10, avoid = setOf(K), dumbbell = true),
    PoolEx("Stiff-Legged_Dumbbell_Deadlift", Role.STRENGTH, a(BodyArea.LEG, BodyArea.SHOULDER_BACK), I, reps = 10, avoid = setOf(BK), dumbbell = true),
    PoolEx("Standing_Dumbbell_Calf_Raise", Role.STRENGTH, a(BodyArea.LEG), B, reps = 15, dumbbell = true),
    PoolEx("Standing_Dumbbell_Press", Role.STRENGTH, a(BodyArea.SHOULDER_BACK, BodyArea.ARM), B, reps = 10, avoid = setOf(S), dumbbell = true),
    PoolEx("Side_Lateral_Raise", Role.STRENGTH, a(BodyArea.SHOULDER_BACK), B, reps = 12, avoid = setOf(S), dumbbell = true),
    PoolEx("Bent_Over_Two-Dumbbell_Row", Role.STRENGTH, a(BodyArea.SHOULDER_BACK), B, reps = 10, avoid = setOf(BK), dumbbell = true),
    PoolEx("Dumbbell_Shrug", Role.STRENGTH, a(BodyArea.SHOULDER_BACK), B, reps = 12, dumbbell = true),
    PoolEx("Dumbbell_Bicep_Curl", Role.STRENGTH, a(BodyArea.ARM), B, reps = 12, dumbbell = true),
    PoolEx("Hammer_Curls", Role.STRENGTH, a(BodyArea.ARM), B, reps = 12, dumbbell = true),
    PoolEx("Standing_Dumbbell_Triceps_Extension", Role.STRENGTH, a(BodyArea.ARM), B, reps = 10, avoid = setOf(S), dumbbell = true),
    PoolEx("Tricep_Dumbbell_Kickback", Role.STRENGTH, a(BodyArea.ARM), B, reps = 12, dumbbell = true),
    PoolEx("Reverse_Flyes", Role.STRENGTH, a(BodyArea.SHOULDER_BACK), I, reps = 12, avoid = setOf(BK), dumbbell = true),

    // Stretch / mobility (timed)
    PoolEx("Childs_Pose", Role.STRETCH, a(BodyArea.SHOULDER_BACK, BodyArea.FULL_BODY), B, secs = 30),
    PoolEx("x_Cobra_Stretch", Role.STRETCH, a(BodyArea.ABS, BodyArea.FULL_BODY), B, secs = 30, avoid = setOf(BK)),
    PoolEx("Cat_Stretch", Role.STRETCH, a(BodyArea.SHOULDER_BACK, BodyArea.FULL_BODY), B, secs = 30),
    PoolEx("Runners_Stretch", Role.STRETCH, a(BodyArea.LEG, BodyArea.FULL_BODY), B, secs = 30),
    PoolEx("Quad_Stretch", Role.STRETCH, a(BodyArea.LEG), B, secs = 30),
    PoolEx("Seated_Floor_Hamstring_Stretch", Role.STRETCH, a(BodyArea.LEG), B, secs = 30),
    PoolEx("Shoulder_Stretch", Role.STRETCH, a(BodyArea.SHOULDER_BACK, BodyArea.ARM, BodyArea.CHEST), B, secs = 30),
    PoolEx("Triceps_Stretch", Role.STRETCH, a(BodyArea.ARM), B, secs = 30),
    PoolEx("Spinal_Stretch", Role.STRETCH, a(BodyArea.SHOULDER_BACK, BodyArea.ABS), B, secs = 30),
    PoolEx("Kneeling_Hip_Flexor", Role.STRETCH, a(BodyArea.LEG, BodyArea.ABS), B, secs = 30),
    PoolEx("Upper_Back_Stretch", Role.STRETCH, a(BodyArea.SHOULDER_BACK), B, secs = 30),
    PoolEx("Calf_Stretch_Hands_Against_Wall", Role.STRETCH, a(BodyArea.LEG), B, secs = 30),
    PoolEx("Knee_Across_The_Body", Role.STRETCH, a(BodyArea.LEG, BodyArea.SHOULDER_BACK), B, secs = 30),
    PoolEx("Hug_Knees_To_Chest", Role.STRETCH, a(BodyArea.SHOULDER_BACK, BodyArea.FULL_BODY), B, secs = 30),
    PoolEx("Chest_And_Front_Of_Shoulder_Stretch", Role.STRETCH, a(BodyArea.CHEST), B, secs = 30),
    PoolEx("Standing_Lateral_Stretch", Role.STRETCH, a(BodyArea.ABS), B, secs = 30),
    PoolEx("Upward_Stretch", Role.STRETCH, ALL, B, secs = 30),
    PoolEx("Ankle_On_The_Knee", Role.STRETCH, a(BodyArea.LEG), B, secs = 30),
    PoolEx("Side_Lying_Groin_Stretch", Role.STRETCH, a(BodyArea.LEG), B, secs = 30),
    PoolEx("Worlds_Greatest_Stretch", Role.STRETCH, ALL, I, secs = 30),
    PoolEx("90_90_Hamstring", Role.STRETCH, a(BodyArea.LEG), I, secs = 30),
    PoolEx("Dancers_Stretch", Role.STRETCH, a(BodyArea.SHOULDER_BACK), I, secs = 30),
    PoolEx("The_Straddle", Role.STRETCH, a(BodyArea.LEG), I, secs = 30),
    PoolEx("Windmills", Role.STRETCH, ALL, I, secs = 30)
)

private fun rank(d: Difficulty) = d.ordinal

/** Goals for generated multi-week home plans. */
enum class PlanGoal(val label: String, val desc: String) {
    LOSE_WEIGHT("Lose Weight", "Burn calories with fat-burning circuits"),
    BUILD_MUSCLE("Build Muscle", "Progressive bodyweight strength"),
    TONE("Tone & Shape", "Strength + core to sculpt your body"),
    FLEXIBILITY("Flexibility", "Daily mobility and stretching"),
    GENERAL("Stay Fit", "Balanced fitness for every day")
}

data class PlanSpec(
    val goal: PlanGoal,
    val level: Difficulty,
    val days: Int,
    val minutes: Int,
    val focus: Set<BodyArea>,
    val avoid: Set<Limitation>,
    val dumbbells: Boolean,
    val weeks: Int
) {
    fun encode(prefix: String) = listOf(
        prefix, goal.name, level.name, days, minutes,
        BodyArea.entries.filter { it in focus }.sumOf { 1 shl it.ordinal },
        Limitation.entries.filter { it in avoid }.sumOf { 1 shl it.ordinal },
        if (dumbbells) 1 else 0, weeks
    ).joinToString(".")

    companion object {
        fun decode(id: String): PlanSpec? = runCatching {
            val p = id.split(".")
            PlanSpec(
                PlanGoal.valueOf(p[1]), Difficulty.valueOf(p[2]), p[3].toInt(), p[4].toInt(),
                BodyArea.entries.filter { p[5].toInt() and (1 shl it.ordinal) != 0 }.toSet(),
                Limitation.entries.filter { p[6].toInt() and (1 shl it.ordinal) != 0 }.toSet(),
                p[7] == "1", p[8].toInt()
            )
        }.getOrNull()
    }
}

/** Builds day-by-day sessions for a [PlanSpec]. Deterministic: same spec + day → same workout. */
object PlanBuilder {

    private val trainingDays = mapOf(
        1 to setOf(1), 2 to setOf(1, 4), 3 to setOf(1, 3, 5), 4 to setOf(1, 2, 4, 5),
        5 to setOf(1, 2, 3, 5, 6), 6 to setOf(1, 2, 3, 4, 5, 6), 7 to (1..7).toSet()
    )

    fun isRest(spec: PlanSpec, day: Int): Boolean {
        val weekday = (day - 1) % 7 + 1
        return weekday !in trainingDays.getValue(spec.days.coerceIn(1, 7))
    }

    private fun sessionIndex(spec: PlanSpec, day: Int) = (1 until day).count { !isRest(spec, it) }

    private fun focusFor(spec: PlanSpec, session: Int): Set<BodyArea> {
        if (spec.goal == PlanGoal.FLEXIBILITY) return ALL
        val rotation: List<Set<BodyArea>> = if (spec.focus.isEmpty() || spec.focus == ALL) {
            listOf(ALL, a(BodyArea.LEG, BodyArea.ABS), a(BodyArea.CHEST, BodyArea.ARM, BodyArea.SHOULDER_BACK), ALL, a(BodyArea.ABS, BodyArea.LEG))
        } else {
            val f = spec.focus.filter { it != BodyArea.FULL_BODY }
            // Each focus area gets its own day, with a full-body day every third session.
            (f.map { setOf(it) } + listOf(ALL)).let { if (it.size == 1) listOf(ALL) else it }
        }
        return rotation[session % rotation.size]
    }

    private fun titleFor(areas: Set<BodyArea>, goal: PlanGoal): String = when {
        goal == PlanGoal.FLEXIBILITY -> "Mobility Flow"
        areas == ALL -> "Full Body"
        areas.size == 1 -> areas.first().label
        BodyArea.LEG in areas && BodyArea.ABS in areas -> "Lower Body & Core"
        BodyArea.CHEST in areas -> "Upper Body"
        else -> areas.joinToString(" & ") { it.label }
    }

    private fun matches(p: PoolEx, spec: PlanSpec, areas: Set<BodyArea>): Boolean {
        if (p.avoid.any { it in spec.avoid }) return false
        if (p.dumbbell && !spec.dumbbells) return false
        if (rank(p.min) > rank(spec.level) || rank(p.max) < rank(spec.level)) return false
        return areas == ALL || p.areas.any { it in areas } || BodyArea.FULL_BODY in p.areas && areas.size > 1
    }

    private fun pick(role: Role, spec: PlanSpec, areas: Set<BodyArea>, n: Int, rnd: Random, used: MutableSet<String>): List<PoolEx> {
        var cands = POOL.filter { it.role == role && it.id !in used && matches(it, spec, areas) }
        if (cands.size < n) {
            // Widen to any area for this role (keeps level and limitation rules).
            cands = (cands + POOL.filter { it.role == role && it.id !in used && matches(it, spec, ALL) }).distinctBy { it.id }
        }
        // Prefer the dumbbell moves when available, they progress better.
        val sorted = cands.shuffled(rnd).sortedByDescending { if (it.dumbbell) 1 else 0 }
        return sorted.take(n).also { list -> used += list.map { it.id } }
    }

    private fun dose(p: PoolEx, spec: PlanSpec, week: Int): HomeItem {
        val lvl = when (spec.level) { Difficulty.BEGINNER -> 1.0; Difficulty.INTERMEDIATE -> 1.35; Difficulty.ADVANCED -> 1.75 }
        val prog = 1.0 + 0.1 * week
        return if (p.secs > 0) {
            val base = when (spec.level) { Difficulty.BEGINNER -> 30; Difficulty.INTERMEDIATE -> 40; Difficulty.ADVANCED -> 45 }
            val s = if (p.role == Role.STRETCH || p.role == Role.WARMUP) 30 + 5 * (week / 2) else base + 5 * week
            HomeItem(p.id, seconds = (s.coerceAtMost(75) / 5) * 5)
        } else {
            HomeItem(p.id, reps = (p.reps * lvl * prog).roundToInt().coerceIn(4, 40))
        }
    }

    private fun restFor(spec: PlanSpec): Int {
        val base = when (spec.goal) {
            PlanGoal.LOSE_WEIGHT -> 15; PlanGoal.BUILD_MUSCLE -> 35; PlanGoal.FLEXIBILITY -> 8
            PlanGoal.TONE, PlanGoal.GENERAL -> 25
        }
        return base + if (spec.level == Difficulty.BEGINNER) 10 else 0
    }

    fun session(id: String, spec: PlanSpec, day: Int): HomeWorkout? {
        if (isRest(spec, day)) return null
        val session = sessionIndex(spec, day)
        val week = (day - 1) / 7
        val areas = focusFor(spec, session)
        val rnd = Random((id.hashCode() * 31 + day).toLong())
        val used = mutableSetOf<String>()
        val rest = restFor(spec)

        val warm = pick(Role.WARMUP, spec, ALL, 2, rnd, used)
        val cool = pick(Role.STRETCH, spec, areas, 2, rnd, used)

        val budget = spec.minutes * 60 - (warm.size + cool.size) * (30 + 8)
        val perItem = 40 + rest
        val n = (budget / perItem).coerceIn(5, 24)

        val main: List<PoolEx> = when (spec.goal) {
            PlanGoal.FLEXIBILITY -> pick(Role.STRETCH, spec, ALL, n.coerceAtMost(14), rnd, used)
            PlanGoal.LOSE_WEIGHT -> {
                val k = (n.coerceAtMost(8) + 1) / 2
                val s = pick(Role.STRENGTH, spec, areas, k, rnd, used)
                val c = pick(Role.CARDIO, spec, ALL, k, rnd, used)
                val core = pick(Role.CORE, spec, areas, 1, rnd, used)
                s.zip(c) { x, y -> listOf(x, y) }.flatten() + core
            }
            PlanGoal.BUILD_MUSCLE -> {
                val coreN = if (BodyArea.ABS in areas) 3 else 1
                pick(Role.STRENGTH, spec, areas, (n.coerceAtMost(9) - coreN).coerceAtLeast(3), rnd, used) +
                    pick(Role.CORE, spec, areas, coreN, rnd, used)
            }
            PlanGoal.TONE, PlanGoal.GENERAL -> {
                val coreN = if (BodyArea.ABS in areas) 3 else 2
                pick(Role.STRENGTH, spec, areas, (n.coerceAtMost(9) - coreN - 1).coerceAtLeast(3), rnd, used) +
                    pick(Role.CORE, spec, areas, coreN, rnd, used) +
                    pick(Role.CARDIO, spec, ALL, 1, rnd, used)
            }
        }
        val rounds = if (spec.goal == PlanGoal.FLEXIBILITY || main.isEmpty()) 1
        else (n.toDouble() / main.size).roundToInt().coerceIn(1, 3)

        val items = warm.map { dose(it, spec, week) } +
            (1..rounds).flatMap { main.map { m -> dose(m, spec, week) } } +
            cool.map { dose(it, spec, week) }

        val cat = when {
            spec.goal == PlanGoal.FLEXIBILITY -> WorkoutCategory.MOBILITY
            spec.goal == PlanGoal.LOSE_WEIGHT -> WorkoutCategory.HIIT
            areas == ALL -> WorkoutCategory.FULL_BODY
            BodyArea.LEG in areas -> WorkoutCategory.LEG
            BodyArea.ABS in areas -> WorkoutCategory.ABS
            else -> WorkoutCategory.CHEST
        }
        val roundsTxt = if (rounds > 1) " • $rounds rounds" else ""
        return HomeWorkout(
            key = "c:$id:$day",
            title = "Week ${week + 1} · ${titleFor(areas, spec.goal)}",
            subtitle = "Day $day of ${spec.weeks * 7}$roundsTxt",
            level = spec.level, area = null, items = items, restSeconds = rest,
            stretch = spec.goal == PlanGoal.FLEXIBILITY, category = cat
        )
    }

    fun program(id: String, spec: PlanSpec, title: String, subtitle: String, kind: ProgramKind) = Program(
        id = id, title = title, subtitle = subtitle, level = spec.level, kind = kind,
        totalDays = spec.weeks * 7, sessionsPerWeek = spec.days, minutesPerSession = spec.minutes,
        goalLabel = spec.goal.label,
        rest = { isRest(spec, it) }, builder = { session(id, spec, it) }
    )
}

// ============================================================================
// Catalog
// ============================================================================

object HomePrograms {

    private fun r(id: String, n: Int) = HomeItem(id, reps = n)
    private fun s(id: String, sec: Int) = HomeItem(id, seconds = sec)

    private const val JJ = "x_Jumping_Jacks"
    private const val SQUAT = "Bodyweight_Squat"
    private const val PUSH = "Pushups"
    private const val KNEE_PUSH = "x_Knee_Push-Ups"
    private const val LUNGE = "x_Bodyweight_Lunges"
    private const val PLANK = "Plank"
    private const val BRIDGE = "Butt_Lift_Bridge"
    private const val MC = "Mountain_Climbers"
    private const val CRUNCH = "Crunches"
    private const val BIKE = "Air_Bike"
    private const val TWIST = "Russian_Twist"
    private const val LEG_RAISE = "x_Lying_Leg_Raise"
    private const val COBRA = "x_Cobra_Stretch"
    private const val SIDE_PLANK = "Side_Bridge"
    private const val HK = "x_High_Knees"

    private fun w(
        key: String, title: String, sub: String, level: Difficulty, area: BodyArea?, cat: WorkoutCategory,
        vararg items: HomeItem, rest: Int? = null, stretch: Boolean = false
    ) = HomeWorkout(key, title, sub, level, area, items.toList(), rest, stretch, cat)

    private val FB = WorkoutCategory.FULL_BODY
    private val AB = WorkoutCategory.ABS
    private val CH = WorkoutCategory.CHEST
    private val AR = WorkoutCategory.ARM
    private val LG = WorkoutCategory.LEG
    private val SB = WorkoutCategory.SHOULDER_BACK

    val workouts: List<HomeWorkout> = listOf(
        // ---------- Full body ----------
        w("full_beg", "Full Body Beginner", "Get moving with simple bodyweight basics", B, BodyArea.FULL_BODY, FB,
            s(JJ, 30), r(SQUAT, 12), r(KNEE_PUSH, 8), s(MC, 20), r(LUNGE, 10), r(BRIDGE, 12), s(PLANK, 20), r("Superman", 10), s(COBRA, 20)),
        w("full_int", "Full Body Intermediate", "Build strength and burn calories", I, BodyArea.FULL_BODY, FB,
            s(JJ, 40), r(SQUAT, 16), r(PUSH, 12), s(MC, 30), r(LUNGE, 14), r("x_Burpees", 8), r(BRIDGE, 16), s(PLANK, 40),
            s(HK, 30), r("Superman", 14), s(SIDE_PLANK, 25), s(COBRA, 25)),
        w("full_adv", "Full Body Advanced", "High-intensity total body session", A, BodyArea.FULL_BODY, FB,
            s(JJ, 60), r("Freehand_Jump_Squat", 16), r(PUSH, 18), r("x_Burpees", 14), s(MC, 45), r(LUNGE, 20),
            r("Push_Up_to_Side_Plank", 10), s(HK, 45), r("Single_Leg_Glute_Bridge", 14), s(PLANK, 60),
            s(SIDE_PLANK, 40), r("Superman", 18), r("Inchworm", 8), s(COBRA, 30)),

        // ---------- Abs ----------
        w("abs_beg", "Abs Beginner", "Core basics for a stronger midsection", B, BodyArea.ABS, AB,
            s(JJ, 20), r(CRUNCH, 12), r(BIKE, 12), r(TWIST, 12), r(LEG_RAISE, 10), s(MC, 20), s(PLANK, 20), s(COBRA, 20)),
        w("abs_int", "Abs Intermediate", "Sculpt your core", I, BodyArea.ABS, AB,
            s(JJ, 30), r(CRUNCH, 18), r(BIKE, 18), r("Reverse_Crunch", 14), r(TWIST, 20), r(LEG_RAISE, 14),
            s(MC, 30), r("Elbow_to_Knee", 16), s(SIDE_PLANK, 25), s(PLANK, 40), s(COBRA, 25)),
        w("abs_adv", "Abs Advanced", "Intense six-pack session", A, BodyArea.ABS, AB,
            s(JJ, 40), r("Sit-Up", 20), r(BIKE, 26), r("Reverse_Crunch", 20), r(TWIST, 28), r(LEG_RAISE, 20),
            s("Flutter_Kicks", 40), s(MC, 45), r("Dead_Bug", 16), r("Oblique_Crunches_-_On_The_Floor", 20),
            s(SIDE_PLANK, 40), s(PLANK, 60), s(COBRA, 30)),

        // ---------- Chest ----------
        w("chest_beg", "Chest Beginner", "Push-up progressions for beginners", B, BodyArea.CHEST, CH,
            s(JJ, 30), r("Incline_Push-Up", 10), r(KNEE_PUSH, 8), r("Incline_Push-Up_Wide", 8), r(KNEE_PUSH, 8),
            s("Dynamic_Chest_Stretch", 20), s("Chest_And_Front_Of_Shoulder_Stretch", 20)),
        w("chest_int", "Chest Intermediate", "Build a stronger chest", I, BodyArea.CHEST, CH,
            s(JJ, 30), r(PUSH, 12), r("Push-Up_Wide", 12), r("Push-Ups_-_Close_Triceps_Position", 10),
            r("Incline_Push-Up", 14), r("Push_Up_to_Side_Plank", 8), r(PUSH, 12), s("Isometric_Wipers", 30),
            s("Dynamic_Chest_Stretch", 25), s("Chest_And_Front_Of_Shoulder_Stretch", 25)),
        w("chest_adv", "Chest Advanced", "Max push-up volume", A, BodyArea.CHEST, CH,
            s(JJ, 40), r(PUSH, 18), r("Push-Ups_With_Feet_Elevated", 14), r("Push-Up_Wide", 16), r("Plyo_Push-up", 10),
            r("Push-Ups_-_Close_Triceps_Position", 14), r("Push_Up_to_Side_Plank", 12), r(PUSH, 16),
            r("Clock_Push-Up", 8), s("Dynamic_Chest_Stretch", 30), s("Chest_And_Front_Of_Shoulder_Stretch", 30)),

        // ---------- Arms ----------
        w("arm_beg", "Arms Beginner", "Tone arms with no equipment", B, BodyArea.ARM, AR,
            s("Arm_Circles", 30), r("Bench_Dips", 10), r(KNEE_PUSH, 8), r("Standing_Towel_Triceps_Extension", 12),
            s(PLANK, 20), s("Triceps_Stretch", 20), s("Standing_Biceps_Stretch", 20)),
        w("arm_int", "Arms Intermediate", "Stronger triceps, shoulders and biceps", I, BodyArea.ARM, AR,
            s("Arm_Circles", 30), r("Bench_Dips", 14), r("Push-Ups_-_Close_Triceps_Position", 10), r("Body_Tricep_Press", 10),
            r("x_Pike_Push-Ups", 8), r("Standing_Towel_Triceps_Extension", 16), s(PLANK, 40),
            s("Triceps_Stretch", 25), s("Standing_Biceps_Stretch", 25)),
        w("arm_adv", "Arms Advanced", "Burn out every arm muscle", A, BodyArea.ARM, AR,
            s("Arm_Circles", 40), r("Bench_Dips", 20), r("Push-Ups_-_Close_Triceps_Position", 16), r("Body_Tricep_Press", 14),
            r("x_Pike_Push-Ups", 14), r("Push_Up_to_Side_Plank", 10), r("Standing_Towel_Triceps_Extension", 20),
            s(PLANK, 60), s("Triceps_Stretch", 30), s("Standing_Biceps_Stretch", 30)),

        // ---------- Legs ----------
        w("leg_beg", "Legs & Butt Beginner", "Shape your legs and glutes", B, BodyArea.LEG, LG,
            s(JJ, 30), r(SQUAT, 12), r(LUNGE, 10), r(BRIDGE, 12), r("x_Donkey_Kicks", 12), r("x_Calf_Raises", 15),
            s("x_Wall_Sit", 20), s("Quad_Stretch", 20), s("Runners_Stretch", 20)),
        w("leg_int", "Legs & Butt Intermediate", "Firm and strong lower body", I, BodyArea.LEG, LG,
            s(JJ, 40), r(SQUAT, 18), r(LUNGE, 16), r("Freehand_Jump_Squat", 10), r(BRIDGE, 18), r("x_Donkey_Kicks", 16),
            r("Single_Leg_Glute_Bridge", 10), r("x_Calf_Raises", 20), s("x_Wall_Sit", 40), s("Quad_Stretch", 25), s("Runners_Stretch", 25)),
        w("leg_adv", "Legs & Butt Advanced", "Explosive lower-body burner", A, BodyArea.LEG, LG,
            s(JJ, 60), r("Freehand_Jump_Squat", 20), r(LUNGE, 24), r(SQUAT, 24), r("Step-up_with_Knee_Raise", 16),
            r("Single_Leg_Glute_Bridge", 16), r("x_Donkey_Kicks", 22), s(HK, 40), r("x_Calf_Raises", 30),
            s("x_Wall_Sit", 60), s("Quad_Stretch", 30), s("Runners_Stretch", 30)),

        // ---------- Back & shoulders ----------
        w("back_beg", "Back & Shoulders Beginner", "Better posture, stronger back", B, BodyArea.SHOULDER_BACK, SB,
            s("Arm_Circles", 30), r("Superman", 10), s("Shoulder_Circles", 20), r(KNEE_PUSH, 8), r("Inchworm", 5),
            s("Cat_Stretch", 20), s("Childs_Pose", 25)),
        w("back_int", "Back & Shoulders Intermediate", "Strong shoulders and upper back", I, BodyArea.SHOULDER_BACK, SB,
            s("Arm_Circles", 30), r("Superman", 14), r("x_Pike_Push-Ups", 8), r("Inchworm", 8), r("Hyperextensions_With_No_Hyperextension_Bench", 12),
            s(SIDE_PLANK, 25), r("Push_Up_to_Side_Plank", 8), s("Cat_Stretch", 25), s("Childs_Pose", 30)),
        w("back_adv", "Back & Shoulders Advanced", "Bulletproof shoulders and back", A, BodyArea.SHOULDER_BACK, SB,
            s("Arm_Circles", 40), r("Superman", 20), r("x_Pike_Push-Ups", 14), r("Inchworm", 12),
            r("Hyperextensions_With_No_Hyperextension_Bench", 18), r("Push_Up_to_Side_Plank", 12), s(SIDE_PLANK, 40),
            s(PLANK, 60), s("Cat_Stretch", 30), s("Childs_Pose", 30)),

        // ---------- HIIT ----------
        w("hiit_beg", "HIIT Starter", "Gentle intervals to build your engine", B, null, WorkoutCategory.HIIT,
            s(JJ, 30), s(SQUAT, 30), s(HK, 20), s(KNEE_PUSH, 30), s("Single_Leg_Butt_Kick", 30), s(BRIDGE, 30), s(MC, 20), s(PLANK, 20),
            rest = 30),
        w("hiit_int", "HIIT Burn", "40 seconds on, 20 seconds off", I, null, WorkoutCategory.HIIT,
            s(JJ, 40), s("Freehand_Jump_Squat", 40), s(MC, 40), s(PUSH, 40), s(HK, 40), s(LUNGE, 40),
            s("Star_Jump", 40), s(BIKE, 40), s("x_Burpees", 40), s(PLANK, 40), rest = 20),
        w("hiit", "HIIT Inferno", "Short, all-out intervals", A, null, WorkoutCategory.HIIT,
            s(JJ, 40), s("x_Burpees", 30), s(MC, 40), s("Freehand_Jump_Squat", 30), s(HK, 40),
            s("Star_Jump", 30), s(MC, 40), s("x_Burpees", 30), s("Split_Jump", 30), s("Knee_Tuck_Jump", 20), s(PLANK, 45), rest = 15),

        // ---------- Cardio ----------
        w("cardio_beg", "Low-Impact Cardio", "No jumping — easy on the joints", B, null, WorkoutCategory.CARDIO,
            s("Arm_Circles", 30), s("Step-up_with_Knee_Raise", 40), s(SQUAT, 40), s("Torso_Rotation", 30),
            s("x_Calf_Raises", 40), s(MC, 30), s("Standing_Hip_Circles", 30), s("Step-up_with_Knee_Raise", 40), s(SQUAT, 40),
            rest = 15),
        w("cardio_int", "Cardio Blast", "Keep moving, keep burning", I, null, WorkoutCategory.CARDIO,
            s(JJ, 45), s(HK, 45), s("Single_Leg_Butt_Kick", 45), s(MC, 40), s("Star_Jump", 40), s("Fast_Skipping", 45),
            s(JJ, 45), s(HK, 45), s("Scissors_Jump", 40), s(MC, 40), rest = 15),
        w("cardio_adv", "Cardio Endurance", "Long intervals for serious stamina", A, null, WorkoutCategory.CARDIO,
            s(JJ, 60), s(HK, 60), s("x_Burpees", 45), s("Lateral_Bound", 45), s(MC, 60), s("Split_Jump", 40),
            s("Fast_Skipping", 60), s("Star_Jump", 45), s("Knee_Tuck_Jump", 30), s(MC, 60), rest = 15),

        // ---------- Stretch & mobility ----------
        w("warmup", "Warm Up", "5 minutes to prepare your body", B, null, WorkoutCategory.MOBILITY,
            s("Arm_Circles", 30), s("Shoulder_Circles", 30), s("Standing_Hip_Circles", 30), s(JJ, 30),
            s("Knee_Circles", 30), s("Ankle_Circles", 30), s(HK, 30), rest = 10),
        w("morning", "Morning Wake-Up", "Gentle mobility to start your day", B, null, WorkoutCategory.MOBILITY,
            s("Upward_Stretch", 30), s("Standing_Lateral_Stretch", 30), s("Standing_Toe_Touches", 30), s("Torso_Rotation", 30),
            s("Arm_Circles", 30), s(SQUAT, 30), s("Worlds_Greatest_Stretch", 30), rest = 10, stretch = true),
        w("stretch", "Stretching & Cool Down", "Relax and recover after training", B, null, WorkoutCategory.MOBILITY,
            s("Childs_Pose", 30), s(COBRA, 30), s("Cat_Stretch", 30), s("Runners_Stretch", 30), s("Quad_Stretch", 30),
            s("Seated_Floor_Hamstring_Stretch", 30), s("Shoulder_Stretch", 30), s("Triceps_Stretch", 30), s("Spinal_Stretch", 30),
            rest = 10, stretch = true),
        w("backmob", "Lower-Back Mobility", "Gentle moves for a stiff back (stop if it hurts)", B, null, WorkoutCategory.MOBILITY,
            s("Cat_Stretch", 40), s("Childs_Pose", 40), s("Hug_Knees_To_Chest", 40), s("Knee_Across_The_Body", 40),
            s("Pelvic_Tilt_Into_Bridge", 30), s("Spinal_Stretch", 40), s("Kneeling_Hip_Flexor", 40), rest = 10, stretch = true),
        w("mob_int", "Full-Body Flexibility", "Open hips, hamstrings and shoulders", I, null, WorkoutCategory.MOBILITY,
            s("Worlds_Greatest_Stretch", 40), s("Runners_Stretch", 40), s("90_90_Hamstring", 40), s("Kneeling_Hip_Flexor", 40),
            s("Side_Lying_Groin_Stretch", 40), s("Upper_Back_Stretch", 40), s("Chest_And_Front_Of_Shoulder_Stretch", 40),
            s("Dancers_Stretch", 40), s("Childs_Pose", 40), rest = 10, stretch = true),
        w("mob_adv", "Deep Mobility Flow", "Longer holds for advanced flexibility", A, null, WorkoutCategory.MOBILITY,
            s("Inchworm", 40), s("Worlds_Greatest_Stretch", 50), s("The_Straddle", 50), s("Windmills", 40), s("90_90_Hamstring", 50),
            s("Ankle_On_The_Knee", 50), s("Side_Lying_Groin_Stretch", 50), s("Dancers_Stretch", 50), s("Upper_Back_Stretch", 50),
            s(COBRA, 40), s("Childs_Pose", 60), rest = 10, stretch = true),

        // ---------- Dumbbell ----------
        w("db_beg", "Dumbbell Total Body", "Simple moves with one pair of dumbbells", B, null, WorkoutCategory.DUMBBELL,
            s(JJ, 30), r("Goblet_Squat", 12), r("Standing_Dumbbell_Press", 10), r("Bent_Over_Two-Dumbbell_Row", 10),
            r("Plie_Dumbbell_Squat", 12), r("Dumbbell_Bicep_Curl", 12), r("Tricep_Dumbbell_Kickback", 12), s("Childs_Pose", 30)),
        w("db_int", "Dumbbell Upper Body", "Chest, back, shoulders and arms", I, null, WorkoutCategory.DUMBBELL,
            s("Arm_Circles", 30), r("Standing_Dumbbell_Press", 12), r("Bent_Over_Two-Dumbbell_Row", 12), r("Side_Lateral_Raise", 12),
            r(PUSH, 12), r("Hammer_Curls", 12), r("Standing_Dumbbell_Triceps_Extension", 12), r("Reverse_Flyes", 12),
            r("Dumbbell_Shrug", 15), s("Shoulder_Stretch", 30)),
        w("db_adv", "Dumbbell Strength Circuit", "Heavy full-body circuit", A, null, WorkoutCategory.DUMBBELL,
            s(JJ, 45), r("Goblet_Squat", 16), r("Dumbbell_Lunges", 14), r("Stiff-Legged_Dumbbell_Deadlift", 12),
            r("Standing_Dumbbell_Press", 12), r("Bent_Over_Two-Dumbbell_Row", 14), r("Dumbbell_Clean", 10),
            r("Side_Lateral_Raise", 15), r("Hammer_Curls", 14), r("Standing_Dumbbell_Triceps_Extension", 14),
            r("Standing_Dumbbell_Calf_Raise", 20), s("Runners_Stretch", 30)),

        // ---------- Quick ----------
        w("quick_beg", "5-Minute Energizer", "A quick boost — no excuses", B, null, WorkoutCategory.QUICK,
            s(JJ, 30), s(SQUAT, 30), s(KNEE_PUSH, 30), s(MC, 30), s(BRIDGE, 30), s(PLANK, 30), rest = 15),
        w("seven", "7-Minute Workout", "The classic 12-move circuit", I, null, WorkoutCategory.QUICK,
            s(JJ, 30), s("x_Wall_Sit", 30), s(PUSH, 30), s(CRUNCH, 30), s("Step-up_with_Knee_Raise", 30), s(SQUAT, 30),
            s("Bench_Dips", 30), s(PLANK, 30), s(HK, 30), s(LUNGE, 30), s("Push_Up_to_Side_Plank", 30), s(SIDE_PLANK, 30),
            rest = 10),
        w("quick_abs", "10-Minute Abs", "Fast core burner", I, null, WorkoutCategory.QUICK,
            s(MC, 40), s(CRUNCH, 40), s(BIKE, 40), s("Reverse_Crunch", 40), s(TWIST, 40), s("Flutter_Kicks", 40),
            s(SIDE_PLANK, 30), s(PLANK, 45), s("Elbow_to_Knee", 40), s(LEG_RAISE, 40), rest = 15),
        w("quick_adv", "15-Minute Total Burn", "Everything, fast", A, null, WorkoutCategory.QUICK,
            s(JJ, 45), s("x_Burpees", 40), s("Freehand_Jump_Squat", 40), s(PUSH, 40), s(MC, 45), s(LUNGE, 45),
            s("Push_Up_to_Side_Plank", 40), s(HK, 45), s(BIKE, 45), s("Split_Jump", 30), s(PLANK, 60), s("x_Burpees", 40),
            s(COBRA, 30), rest = 15),
        w("quick_int20", "20-Minute Strength", "Bodyweight strength in 20 minutes", I, null, WorkoutCategory.QUICK,
            s(JJ, 40), r(SQUAT, 20), r(PUSH, 12), r(LUNGE, 16), r("Superman", 15), r(BRIDGE, 20), r("Push_Up_to_Side_Plank", 10),
            r(SQUAT, 20), r(PUSH, 12), s(PLANK, 45), s(COBRA, 30), rest = 20),

        // ---------- Bands & kettlebell ----------
        w("kb_beg", "Kettlebell Basics", "Learn the swing and friends", B, null, WorkoutCategory.KETTLEBELL_BANDS,
            s("Arm_Circles", 30), r("Goblet_Squat", 12), r("One-Arm_Kettlebell_Swings", 12), r("Kettlebell_One-Legged_Deadlift", 8),
            r("Two-Arm_Kettlebell_Military_Press", 10), r("Two-Arm_Kettlebell_Row", 10), r("Kettlebell_Halo", 8), s(PLANK, 30)),
        w("kb_int", "Kettlebell Conditioning", "Swings, cleans and presses", I, null, WorkoutCategory.KETTLEBELL_BANDS,
            s(JJ, 30), r("One-Arm_Kettlebell_Swings", 20), r("Kettlebell_Dead_Clean", 10), r("Front_Squats_With_Two_Kettlebells", 12),
            r("Alternating_Kettlebell_Press", 10), r("Kettlebell_Sumo_High_Pull", 12), r("Alternating_Kettlebell_Row", 12),
            r("Kettlebell_Figure_8", 12), s(MC, 30), s("Childs_Pose", 30), rest = 25),
        w("kb_adv", "Kettlebell Power", "Explosive full-body flow", A, null, WorkoutCategory.KETTLEBELL_BANDS,
            s(JJ, 45), r("One-Arm_Kettlebell_Swings", 25), r("Kettlebell_Thruster", 12), r("One-Arm_Kettlebell_Clean_and_Jerk", 8),
            r("Kettlebell_Turkish_Get-Up_Lunge_style", 4), r("Kettlebell_Windmill", 8), r("Front_Squats_With_Two_Kettlebells", 15),
            r("Kettlebell_Pirate_Ships", 12), r("Double_Kettlebell_Push_Press", 10), s(PLANK, 60), rest = 20),
        w("band_beg", "Resistance Band Full Body", "One band, whole body", B, null, WorkoutCategory.KETTLEBELL_BANDS,
            s("Arm_Circles", 30), r("Squats_-_With_Bands", 15), r("Band_Pull_Apart", 15), r("Shoulder_Press_-_With_Bands", 12),
            r("Hip_Extension_with_Bands", 12), r("Lateral_Raise_-_With_Bands", 12), r("Calf_Raises_-_With_Bands", 15),
            r("Band_Skull_Crusher", 12), s("Shoulder_Stretch", 30)),
        w("band_int", "Band Upper-Body Sculpt", "Shoulders, back and arms", I, null, WorkoutCategory.KETTLEBELL_BANDS,
            s("Arm_Circles", 30), r("Band_Pull_Apart", 20), r("Bench_Press_-_With_Bands", 15), r("Upright_Row_-_With_Bands", 12),
            r("Back_Flyes_-_With_Bands", 15), r("External_Rotation_with_Band", 12), r("Speed_Band_Overhead_Triceps", 15),
            r("Cross_Over_-_With_Bands", 12), r(PUSH, 12), s("Upper_Back_Stretch", 30), rest = 20),

        // ---------- Low impact ----------
        w("low_beg", "Low-Impact Full Body", "Gentle on knees and back", B, null, WorkoutCategory.LOW_IMPACT,
            s("Standing_Hip_Circles", 30), r("Chair_Squat", 10), r("Incline_Push-Up", 8), r(BRIDGE, 12), r("x_Calf_Raises", 15),
            r("Superman", 8), s(SIDE_PLANK, 15), r("Step-up_with_Knee_Raise", 10), s("Childs_Pose", 30)),
        w("low_int", "Joint-Friendly Strength", "Strength without jumping", I, null, WorkoutCategory.LOW_IMPACT,
            s("Arm_Circles", 30), s("x_Wall_Sit", 40), r(PUSH, 12), r("Single_Leg_Glute_Bridge", 12), r("Dead_Bug", 12),
            r("Superman", 14), r("Glute_Kickback", 15), s(PLANK, 40), r("x_Donkey_Kicks", 15), s(COBRA, 30), rest = 25),
        w("low_cardio", "Walking-Pace Cardio", "Steady, low-impact moves", B, null, WorkoutCategory.LOW_IMPACT,
            s("Step-up_with_Knee_Raise", 60), s("Torso_Rotation", 45), s("Arm_Circles", 45), s(SQUAT, 45),
            s("x_Calf_Raises", 45), s("Step-up_with_Knee_Raise", 60), s("Standing_Hip_Circles", 45), s("Standing_Lateral_Stretch", 45),
            rest = 10),

        // ---------- More mobility ----------
        w("desk", "Desk Break Stretch", "5 minutes to undo sitting", B, null, WorkoutCategory.MOBILITY,
            s("Chin_To_Chest_Stretch", 30), s("Side_Neck_Stretch", 30), s("Shoulder_Circles", 30), s("Chair_Upper_Body_Stretch", 30),
            s("Chair_Lower_Back_Stretch", 30), s("Wrist_Circles", 30), s("Seated_Overhead_Stretch", 30), s("Chair_Leg_Extended_Stretch", 30),
            rest = 5, stretch = true),
        w("bedtime", "Bedtime Stretch", "Unwind before sleep", B, null, WorkoutCategory.MOBILITY,
            s("Childs_Pose", 45), s("Hug_Knees_To_Chest", 45), s("Knee_Across_The_Body", 45), s("One_Knee_To_Chest", 45),
            s("Seated_Floor_Hamstring_Stretch", 45), s("Lower_Back_Curl", 30), s(COBRA, 30), s("Childs_Pose", 60), rest = 5, stretch = true),
        w("posture", "Posture Fix", "Open the chest, strengthen the upper back", B, null, WorkoutCategory.MOBILITY,
            s("Shoulder_Circles", 30), s("Elbows_Back", 30), s("Chest_And_Front_Of_Shoulder_Stretch", 30), r("Superman", 10),
            s("Upper_Back_Stretch", 30), s("Shoulder_Raise", 30), s("Cat_Stretch", 30), s("Chin_To_Chest_Stretch", 30),
            rest = 10, stretch = true),

        // ---------- More HIIT / glutes / core ----------
        w("tabata", "Tabata 4×4", "20 s all-out, 10 s rest", A, null, WorkoutCategory.HIIT,
            s(JJ, 20), s("x_Burpees", 20), s(MC, 20), s("Freehand_Jump_Squat", 20), s(HK, 20), s("Star_Jump", 20), s(MC, 20), s("x_Burpees", 20),
            s(JJ, 20), s("Freehand_Jump_Squat", 20), s(HK, 20), s(MC, 20), s("Split_Jump", 20), s("Knee_Tuck_Jump", 20), s(MC, 20), s(PLANK, 20),
            rest = 10),
        w("booty", "Booty Burn", "Glute-focused sculpting", I, BodyArea.LEG, LG,
            s(JJ, 30), r(BRIDGE, 20), r("x_Donkey_Kicks", 16), r("Glute_Kickback", 16), r("Single_Leg_Glute_Bridge", 12),
            s("x_Wall_Sit", 40), r("Leg_Lift", 16), r(LUNGE, 16), r(SQUAT, 20), s("Quad_Stretch", 30), rest = 20),
        w("core_stab", "Core Stability", "Brace, breathe, control", B, BodyArea.ABS, AB,
            r("Dead_Bug", 10), s(PLANK, 25), s(SIDE_PLANK, 20), r("Superman", 10), r(LEG_RAISE, 8), r("Reverse_Crunch", 10),
            s(SIDE_PLANK, 20), s(PLANK, 25), s("Childs_Pose", 30))
    )

    fun byCategory(cat: WorkoutCategory) = workouts.filter { it.category == cat }

    // ---------------- Challenges ----------------

    private fun challenge(id: String, title: String, sub: String, rotation: List<List<HomeItem>>, cat: WorkoutCategory) = Program(
        id = id, title = title, subtitle = sub, level = null, kind = ProgramKind.CHALLENGE, totalDays = 30,
        sessionsPerWeek = 6, minutesPerSession = 12, goalLabel = "30-day challenge",
        rest = { it % 4 == 0 && it < 30 },
        builder = { day ->
            val base = rotation[(day - 1) % rotation.size]
            val f = 1.0 + (day - 1) * 0.03
            val items = base.map {
                if (it.isTimed) it.copy(seconds = (it.seconds * f / 5).roundToInt() * 5) else it.copy(reps = (it.reps * f).roundToInt())
            }
            val level = if (day <= 10) B else if (day <= 20) I else A
            HomeWorkout("c:$id:$day", "$title — Day $day", sub, level, null, items, category = cat)
        }
    )

    val challenges: List<Program> = listOf(
        challenge("full30", "Full Body Challenge", "30 days to a fitter, stronger you", listOf(
            listOf(s(JJ, 30), r(SQUAT, 12), r(KNEE_PUSH, 8), s(MC, 20), r(LUNGE, 10), r(BRIDGE, 12), s(PLANK, 20), s(COBRA, 20)),
            listOf(s(HK, 30), r(PUSH, 6), r(SQUAT, 14), r("Superman", 10), r(BIKE, 12), s(SIDE_PLANK, 15), r("x_Donkey_Kicks", 12), s("Childs_Pose", 20)),
            listOf(s(JJ, 30), r("x_Burpees", 5), r(LUNGE, 12), r("Bench_Dips", 8), s(MC, 25), r(CRUNCH, 12), s("x_Wall_Sit", 20), s(COBRA, 20))
        ), FB),
        challenge("abs30", "Abs Challenge", "30 days to a stronger, flatter core", listOf(
            listOf(s(JJ, 20), r(CRUNCH, 12), r(BIKE, 12), r(LEG_RAISE, 8), s(PLANK, 20), s(COBRA, 20)),
            listOf(s(MC, 20), r(TWIST, 14), r("Reverse_Crunch", 10), s("Flutter_Kicks", 20), s(SIDE_PLANK, 15), s(COBRA, 20)),
            listOf(s(JJ, 20), r("Elbow_to_Knee", 12), r(CRUNCH, 14), r("Dead_Bug", 10), s(PLANK, 25), s("Childs_Pose", 20))
        ), AB),
        challenge("legs30", "Butt & Legs Challenge", "30 days for stronger legs and glutes", listOf(
            listOf(s(JJ, 30), r(SQUAT, 14), r(BRIDGE, 14), r(LUNGE, 10), r("x_Calf_Raises", 15), s("Quad_Stretch", 20)),
            listOf(s(HK, 30), r("x_Donkey_Kicks", 14), r("Single_Leg_Glute_Bridge", 8), s("x_Wall_Sit", 25), r(SQUAT, 14), s("Runners_Stretch", 20)),
            listOf(s(JJ, 30), r("Freehand_Jump_Squat", 8), r(LUNGE, 12), r(BRIDGE, 16), r("Step-up_with_Knee_Raise", 10), s("Quad_Stretch", 20))
        ), LG),
        challenge("push30", "Push-Up Challenge", "From knee push-ups to strong full reps", listOf(
            listOf(s("Arm_Circles", 30), r("Incline_Push-Up", 10), r(KNEE_PUSH, 8), r("Incline_Push-Up_Wide", 8), s(PLANK, 20), s("Chest_And_Front_Of_Shoulder_Stretch", 20)),
            listOf(s("Arm_Circles", 30), r(KNEE_PUSH, 10), r(PUSH, 4), r("Bench_Dips", 8), r(KNEE_PUSH, 8), s("Triceps_Stretch", 20)),
            listOf(s("Arm_Circles", 30), r(PUSH, 5), r("Incline_Push-Up", 12), r("Push-Up_Wide", 4), s(PLANK, 25), s("Chest_And_Front_Of_Shoulder_Stretch", 20))
        ), CH),
        challenge("squat30", "Squat Challenge", "Stronger legs in 30 days", listOf(
            listOf(s("Standing_Hip_Circles", 30), r(SQUAT, 15), r("Chair_Squat", 10), s("x_Wall_Sit", 20), r(BRIDGE, 12), s("Quad_Stretch", 20)),
            listOf(s(JJ, 30), r(SQUAT, 18), r(LUNGE, 10), r("x_Calf_Raises", 15), s("x_Wall_Sit", 25), s("Runners_Stretch", 20)),
            listOf(s(HK, 20), r(SQUAT, 15), r("Freehand_Jump_Squat", 6), r("Single_Leg_Glute_Bridge", 8), s("x_Wall_Sit", 25), s("Quad_Stretch", 20))
        ), LG),
        challenge("flex30", "Flexibility Challenge", "Touch your toes in 30 days", listOf(
            listOf(s("Standing_Toe_Touches", 30), s("Runners_Stretch", 30), s("Seated_Floor_Hamstring_Stretch", 30), s("Childs_Pose", 30), s("Cat_Stretch", 30)),
            listOf(s("Worlds_Greatest_Stretch", 30), s("Kneeling_Hip_Flexor", 30), s("Side_Lying_Groin_Stretch", 30), s(COBRA, 30), s("Spinal_Stretch", 30)),
            listOf(s("Inchworm", 30), s("90_90_Hamstring", 30), s("Ankle_On_The_Knee", 30), s("Upper_Back_Stretch", 30), s("Hug_Knees_To_Chest", 30))
        ), WorkoutCategory.MOBILITY),
        challenge("plank30", "Plank Challenge", "Build a rock-solid core, one hold at a time", listOf(
            listOf(s(PLANK, 20), s(SIDE_PLANK, 15), s(PLANK, 20), s(COBRA, 20)),
            listOf(s(PLANK, 25), s(MC, 20), s(SIDE_PLANK, 15), s("Childs_Pose", 20)),
            listOf(s(PLANK, 20), r("Push_Up_to_Side_Plank", 6), s(PLANK, 25), s(COBRA, 20))
        ), AB)
    )

    // ---------------- Plans (generated) ----------------

    private fun catalogSpec(goal: PlanGoal, level: Difficulty) = PlanSpec(
        goal = goal, level = level,
        days = when (level) { Difficulty.BEGINNER -> 3; Difficulty.INTERMEDIATE -> 4; Difficulty.ADVANCED -> 5 },
        minutes = when (level) { Difficulty.BEGINNER -> 20; Difficulty.INTERMEDIATE -> 30; Difficulty.ADVANCED -> 40 },
        focus = emptySet(), avoid = emptySet(), dumbbells = false,
        weeks = if (goal == PlanGoal.BUILD_MUSCLE) 6 else 4
    )

    val plans: List<Program> = listOf(PlanGoal.LOSE_WEIGHT, PlanGoal.BUILD_MUSCLE, PlanGoal.TONE, PlanGoal.FLEXIBILITY).flatMap { goal ->
        Difficulty.entries.map { level ->
            val spec = catalogSpec(goal, level)
            val id = spec.encode("plan")
            PlanBuilder.program(id, spec, goal.label, "${spec.weeks} weeks • ${spec.days}×/week • ${spec.minutes} min", ProgramKind.PLAN)
        }
    }

    /** The user's own plan, derived from their profile. */
    fun personalSpec(p: UserProfileEntity): PlanSpec {
        val goal = when (Goal.from(p.goal)) {
            Goal.FAT_LOSS -> PlanGoal.LOSE_WEIGHT
            Goal.MUSCLE_GAIN, Goal.LEAN_MUSCLE, Goal.STRENGTH -> PlanGoal.BUILD_MUSCLE
            Goal.RECOMPOSITION -> PlanGoal.TONE
            Goal.GENERAL_FITNESS -> PlanGoal.GENERAL
        }
        val level = when (Experience.from(p.experience)) {
            Experience.BEGINNER, Experience.RETURNING -> Difficulty.BEGINNER
            Experience.INTERMEDIATE -> Difficulty.INTERMEDIATE
            Experience.ADVANCED -> Difficulty.ADVANCED
        }
        val focus = p.focusAreas.split(",").mapNotNull { n -> BodyArea.entries.firstOrNull { it.name == n.trim() } }.toSet()
        // Home sessions only use dumbbells the user actually has at home.
        val homeDumbbells = EquipmentProfile.from(p.equipment) in setOf(EquipmentProfile.DUMBBELLS, EquipmentProfile.HOME_GYM)
        return PlanSpec(
            goal = goal, level = level, days = p.daysPerWeek.coerceIn(2, 6),
            minutes = p.sessionMinutes.coerceIn(15, 45), focus = focus,
            avoid = Limitation.parse(p.limitations), dumbbells = homeDumbbells, weeks = 4
        )
    }

    fun personalProgram(p: UserProfileEntity): Program {
        val spec = personalSpec(p)
        val id = spec.encode("mine")
        return PlanBuilder.program(id, spec, "Your Personal Plan",
            "${spec.goal.label} • ${spec.level.label} • ${spec.days}×/week", ProgramKind.PERSONAL)
    }

    fun program(id: String): Program? {
        challenges.firstOrNull { it.id == id }?.let { return it }
        plans.firstOrNull { it.id == id }?.let { return it }
        if (id.startsWith("mine.") || id.startsWith("plan.")) {
            val spec = PlanSpec.decode(id) ?: return null
            return if (id.startsWith("mine.")) PlanBuilder.program(id, spec, "Your Personal Plan",
                "${spec.goal.label} • ${spec.level.label} • ${spec.days}×/week", ProgramKind.PERSONAL)
            else PlanBuilder.program(id, spec, spec.goal.label, "${spec.weeks} weeks", ProgramKind.PLAN)
        }
        return null
    }

    /** Backwards-compatible name used by older call sites. */
    fun challenge(id: String) = program(id)

    /** Resolve a navigation key: "<workoutKey>", "w:<workoutKey>" or "c:<programId>:<day>". */
    fun resolve(key: String): HomeWorkout? = when {
        key.startsWith("c:") -> {
            val parts = key.split(":")
            program(parts.getOrNull(1) ?: "")?.workoutFor(parts.getOrNull(2)?.toIntOrNull() ?: 1)
        }
        else -> workouts.firstOrNull { it.key == key.removePrefix("w:") }
    }

    /** Recommended static workouts for a level, matched to focus areas first. */
    fun recommended(level: Difficulty, focus: Set<BodyArea>, limit: Int = 4): List<HomeWorkout> {
        val pool = workouts.filter { it.level == level }
        val focused = pool.filter { it.area != null && it.area in focus }
        return (focused + pool.filter { it.category == WorkoutCategory.FULL_BODY || it.category == WorkoutCategory.HIIT } + pool)
            .distinctBy { it.key }.take(limit)
    }

    /** All exercise ids referenced by static content and pools, used to validate against the library. */
    fun referencedIds(): Set<String> =
        (workouts.flatMap { it.items } + challenges.flatMap { c -> (1..30).mapNotNull { c.workoutFor(it) }.flatMap { it.items } })
            .map { it.exerciseId }.toSet() + POOL.map { it.id }
}
