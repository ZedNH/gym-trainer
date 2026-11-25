package com.gymtrainer.app.domain

import com.gymtrainer.app.data.ExerciseEntity
import com.gymtrainer.app.data.UserProfileEntity
import kotlin.math.roundToInt

/** Deterministic, rules-based workout plan generator (section 4 of the product plan).
 *  A rules engine selects and validates the training structure; it respects
 *  equipment, experience, goal, schedule, and health limitations. No network or
 *  AI key is required — the whole generator runs on-device. */
object PlanGenerator {

    data class DraftExercise(
        val exerciseId: String,
        val sets: Int,
        val repMin: Int,
        val repMax: Int,
        val restSeconds: Int,
        val tempo: String
    )

    data class DraftDay(
        val name: String,
        val focus: String,
        val isRest: Boolean,
        val exercises: List<DraftExercise>
    )

    data class DraftPlan(
        val name: String,
        val durationWeeks: Int,
        val notes: String,
        val days: List<DraftDay>
    )

    // A slot asks for an exercise from a muscle group, optionally preferring a compound.
    private data class Slot(val group: MuscleGroup, val preferCompound: Boolean)

    fun generate(profile: UserProfileEntity, library: List<ExerciseEntity>): DraftPlan {
        val goal = Goal.from(profile.goal)
        val experience = Experience.from(profile.experience)
        val allowed = EquipmentProfile.from(profile.equipment).allowed
        val blockedPatterns = blockedPatterns(profile.limitations)
        val maxDifficulty = maxDifficulty(experience)

        val template = splitTemplate(profile.daysPerWeek)
        val slotsPerDay = (profile.sessionMinutes / 10).coerceIn(4, 7)

        val days = template.mapIndexed { index, dayTemplate ->
            val used = mutableSetOf<String>()
            val picked = mutableListOf<DraftExercise>()
            val slots = dayTemplate.slots.take(slotsPerDay)
            for (slot in slots) {
                val ex = pick(slot, library, used, allowed, blockedPatterns, maxDifficulty)
                    // Relax difficulty only — never the injury/limitation filter.
                    ?: pick(slot, library, used, allowed, blockedPatterns, 3)
                    ?: continue
                used += ex.id
                picked += toDraft(ex, goal, experience)
            }
            DraftDay(
                name = dayTemplate.name,
                focus = dayTemplate.focus,
                isRest = false,
                exercises = picked
            )
        }

        return DraftPlan(
            name = "${goal.label} • ${profile.daysPerWeek}-Day ${splitName(profile.daysPerWeek)}",
            durationWeeks = 6,
            notes = buildNotes(profile, goal, experience),
            days = days
        )
    }

    private fun toDraft(ex: ExerciseEntity, goal: Goal, experience: Experience): DraftExercise {
        if (ex.timed) {
            // Held / timed movements: "reps" store seconds.
            val secs = ex.defaultSeconds.coerceAtLeast(20)
            return DraftExercise(ex.id, 3, secs, secs + 15, 45, "hold")
        }
        val (repMin, repMax) = repRange(goal, ex.isCompound)
        val sets = setCount(ex.isCompound, experience, goal)
        val rest = restSeconds(goal, ex)
        return DraftExercise(
            exerciseId = ex.id,
            sets = sets,
            repMin = repMin,
            repMax = repMax,
            restSeconds = rest,
            tempo = "2-0-1"
        )
    }

    private fun pick(
        slot: Slot,
        library: List<ExerciseEntity>,
        used: Set<String>,
        allowed: Set<String>,
        blockedPatterns: Set<String>,
        maxDifficulty: Int
    ): ExerciseEntity? {
        val candidates = library.filter {
            it.featured &&
                MuscleGroup.from(it.muscleGroup) == slot.group &&
                it.id !in used &&
                it.equipment in allowed &&
                it.pattern !in blockedPatterns &&
                difficultyRank(it.difficulty) <= maxDifficulty
        }
        if (candidates.isEmpty()) return null
        return candidates.sortedWith(
            compareByDescending<ExerciseEntity> { it.isCompound == slot.preferCompound }
                .thenByDescending { it.isCompound }
                .thenBy { difficultyRank(it.difficulty) }
        ).first()
    }

    // ---- Rules ----

    private fun repRange(goal: Goal, compound: Boolean): Pair<Int, Int> {
        val base = when (goal) {
            Goal.STRENGTH -> 4 to 6
            Goal.MUSCLE_GAIN -> 6 to 10
            Goal.LEAN_MUSCLE -> 8 to 12
            Goal.RECOMPOSITION -> 8 to 12
            Goal.FAT_LOSS -> 10 to 15
            Goal.GENERAL_FITNESS -> 8 to 12
        }
        return if (compound) base else (base.first + 2 to base.second + 3)
    }

    private fun setCount(compound: Boolean, experience: Experience, goal: Goal): Int {
        val advanced = experience == Experience.ADVANCED || experience == Experience.INTERMEDIATE
        var sets = if (compound) (if (advanced) 4 else 3) else 3
        if (goal == Goal.STRENGTH && compound) sets = if (advanced) 5 else 4
        return sets
    }

    private fun restSeconds(goal: Goal, ex: ExerciseEntity): Int {
        val base = ex.restSeconds
        val raw = when (goal) {
            Goal.STRENGTH -> base * 1.2
            Goal.FAT_LOSS -> base * 0.7
            else -> base.toDouble()
        }
        // Round to a clean 15-second step (e.g. 73 s → 75 s).
        return ((raw / 15.0).roundToInt() * 15).coerceIn(30, 240)
    }

    private fun difficultyRank(name: String): Int = when (Difficulty.from(name)) {
        Difficulty.BEGINNER -> 1
        Difficulty.INTERMEDIATE -> 2
        Difficulty.ADVANCED -> 3
    }

    private fun maxDifficulty(experience: Experience): Int = when (experience) {
        Experience.BEGINNER -> 2
        Experience.RETURNING -> 2
        Experience.INTERMEDIATE -> 3
        Experience.ADVANCED -> 3
    }

    /** Conservative limitation handling (section 3 safety rules). */
    private fun blockedPatterns(limitations: String): Set<String> {
        val l = limitations.lowercase()
        val blocked = mutableSetOf<String>()
        if ("knee" in l) blocked += "Lunge"
        if ("shoulder" in l) { blocked += "Vertical Push"; blocked += "Incline Push" }
        if ("back" in l || "spine" in l || "disc" in l) blocked += "Hinge"
        return blocked
    }

    private fun buildNotes(profile: UserProfileEntity, goal: Goal, experience: Experience): String {
        val sb = StringBuilder()
        sb.append("Structured ${splitName(profile.daysPerWeek)} for ${goal.label.lowercase()}, ")
        sb.append("tuned for a ${experience.label.lowercase()} trainee. ")
        sb.append("Warm up 5–10 minutes before each session and do 1–2 light warm-up sets on the first exercise. ")
        if (experience == Experience.RETURNING) {
            sb.append("Re-entry phase: start lighter than you think and add load gradually. ")
        }
        val l = profile.limitations.lowercase()
        if (l.isNotBlank() && l != "none") {
            sb.append("Noted limitations: ${profile.limitations}. Exercises were filtered conservatively — stop any movement that causes pain and seek professional care where appropriate. ")
        }
        sb.append("Progress load only when you complete all reps with good form and reps in reserve.")
        return sb.toString()
    }

    // ---- Split templates ----

    private data class DayTemplate(val name: String, val focus: String, val slots: List<Slot>)

    private fun splitName(days: Int): String = when (days) {
        2 -> "Full Body"
        3 -> "Full Body"
        4 -> "Upper / Lower"
        5 -> "Upper / Lower + Specialization"
        else -> "Push / Pull / Legs"
    }

    private fun splitTemplate(days: Int): List<DayTemplate> = when (days.coerceIn(2, 6)) {
        2 -> listOf(fullBodyA("Full Body A"), fullBodyB("Full Body B"))
        3 -> listOf(fullBodyA("Full Body A"), fullBodyB("Full Body B"), fullBodyC("Full Body C"))
        4 -> listOf(upper("Upper A"), lower("Lower A"), upper("Upper B"), lower("Lower B"))
        5 -> listOf(upper("Upper"), lower("Lower"), push("Push"), pull("Pull"), legs("Legs"))
        else -> listOf(push("Push A"), pull("Pull A"), legs("Legs A"),
            push("Push B"), pull("Pull B"), legs("Legs B"))
    }

    private fun fullBodyA(name: String) = DayTemplate(name, "Full-body strength emphasis", listOf(
        Slot(MuscleGroup.LEGS, true),
        Slot(MuscleGroup.CHEST, true),
        Slot(MuscleGroup.BACK, true),
        Slot(MuscleGroup.SHOULDERS, false),
        Slot(MuscleGroup.ARMS, false),
        Slot(MuscleGroup.CORE, false),
        Slot(MuscleGroup.ARMS, false)
    ))

    private fun fullBodyB(name: String) = DayTemplate(name, "Full-body posterior emphasis", listOf(
        Slot(MuscleGroup.LEGS, true),
        Slot(MuscleGroup.BACK, true),
        Slot(MuscleGroup.CHEST, true),
        Slot(MuscleGroup.GLUTES, false),
        Slot(MuscleGroup.ARMS, false),
        Slot(MuscleGroup.CORE, false),
        Slot(MuscleGroup.SHOULDERS, false)
    ))

    private fun fullBodyC(name: String) = DayTemplate(name, "Full-body balanced", listOf(
        Slot(MuscleGroup.LEGS, true),
        Slot(MuscleGroup.CHEST, true),
        Slot(MuscleGroup.BACK, true),
        Slot(MuscleGroup.SHOULDERS, false),
        Slot(MuscleGroup.LEGS, false),
        Slot(MuscleGroup.CORE, false),
        Slot(MuscleGroup.ARMS, false)
    ))

    private fun upper(name: String) = DayTemplate(name, "Chest, back, shoulders, arms", listOf(
        Slot(MuscleGroup.CHEST, true),
        Slot(MuscleGroup.BACK, true),
        Slot(MuscleGroup.SHOULDERS, false),
        Slot(MuscleGroup.BACK, false),
        Slot(MuscleGroup.ARMS, false),
        Slot(MuscleGroup.ARMS, false),
        Slot(MuscleGroup.CORE, false)
    ))

    private fun lower(name: String) = DayTemplate(name, "Quads, hamstrings, glutes, calves", listOf(
        Slot(MuscleGroup.LEGS, true),
        Slot(MuscleGroup.LEGS, true),
        Slot(MuscleGroup.GLUTES, false),
        Slot(MuscleGroup.LEGS, false),
        Slot(MuscleGroup.CORE, false),
        Slot(MuscleGroup.LEGS, false),
        Slot(MuscleGroup.GLUTES, false)
    ))

    private fun push(name: String) = DayTemplate(name, "Chest, shoulders, triceps", listOf(
        Slot(MuscleGroup.CHEST, true),
        Slot(MuscleGroup.SHOULDERS, true),
        Slot(MuscleGroup.CHEST, false),
        Slot(MuscleGroup.SHOULDERS, false),
        Slot(MuscleGroup.ARMS, false),
        Slot(MuscleGroup.ARMS, false),
        Slot(MuscleGroup.CORE, false)
    ))

    private fun pull(name: String) = DayTemplate(name, "Back, rear delts, biceps", listOf(
        Slot(MuscleGroup.BACK, true),
        Slot(MuscleGroup.BACK, true),
        Slot(MuscleGroup.BACK, false),
        Slot(MuscleGroup.SHOULDERS, false),
        Slot(MuscleGroup.ARMS, false),
        Slot(MuscleGroup.ARMS, false),
        Slot(MuscleGroup.CORE, false)
    ))

    private fun legs(name: String) = DayTemplate(name, "Quads, hamstrings, glutes, calves", listOf(
        Slot(MuscleGroup.LEGS, true),
        Slot(MuscleGroup.LEGS, true),
        Slot(MuscleGroup.GLUTES, false),
        Slot(MuscleGroup.LEGS, false),
        Slot(MuscleGroup.LEGS, false),
        Slot(MuscleGroup.GLUTES, false),
        Slot(MuscleGroup.CORE, false)
    ))
}
