package com.gymtrainer.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Single-row user fitness profile (id is always 1).
 * Enum-backed fields are stored as the enum constant name.
 */
@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = 1,
    val name: String,
    val age: Int,
    val heightCm: Double,
    val weightKg: Double,
    val targetWeightKg: Double?,
    val units: String,            // Units
    val experience: String,       // Experience
    val goal: String,             // Goal
    val secondaryGoal: String?,   // Goal
    val daysPerWeek: Int,
    val sessionMinutes: Int,
    val equipment: String,        // EquipmentProfile
    val limitations: String,      // free text
    val preferences: String,      // free text
    val createdAt: Long,
    @ColumnInfo(defaultValue = "BOTH")
    val trainingPlace: String = "BOTH",   // TrainingPlace: HOME / GYM / BOTH
    @ColumnInfo(defaultValue = "")
    val focusAreas: String = ""           // comma-separated BodyArea names
)

/** Plan id used by user-built routines; template routines use [TEMPLATE_PLAN_ID]. */
const val CUSTOM_PLAN_ID = 0L
const val TEMPLATE_PLAN_ID = -1L

/** Canonical exercise record (seeded, read-only at runtime). */
@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey val id: String,
    val name: String,
    val muscleGroup: String,       // MuscleGroup
    val primaryMuscles: String,
    val secondaryMuscles: String,
    val equipment: String,         // Barbell / Dumbbell / Machine / Cable / Bodyweight / Kettlebell
    val difficulty: String,        // Difficulty
    val pattern: String,           // movement pattern
    val isCompound: Boolean,
    val setup: String,
    val execution: String,         // steps separated by " | "
    val breathing: String,
    val commonMistakes: String,    // separated by " | "
    val repMin: Int,
    val repMax: Int,
    val restSeconds: Int,
    val youtubeQuery: String,      // search term for a demonstration
    val category: String = "strength", // strength / stretching / cardio / plyometrics ...
    val imageCount: Int = 0,       // bundled frames in assets/ex/<id>/<n>.webp
    val timed: Boolean = false,    // performed for time instead of reps
    val defaultSeconds: Int = 0,
    val featured: Boolean = false, // well-known movement the generator may pick
    /** Equipment variants reuse the photos of the base movement: assets/ex/<imageOf>/. */
    @ColumnInfo(defaultValue = "") val imageOf: String = ""
)

/** A YouTube tutorial the user added for an exercise (pasted link or search result). */
@Entity(tableName = "user_videos", indices = [Index("exerciseId")])
data class UserVideoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val exerciseId: String,
    val videoId: String,
    val title: String,
    val channel: String,
    val addedAt: Long
)

/** One logged food item. [day] is the local epoch day. */
@Entity(tableName = "food_logs", indices = [Index("day")])
data class FoodLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val day: Long,
    val meal: String,          // BREAKFAST / LUNCH / DINNER / SNACK
    val name: String,
    val amountLabel: String,   // e.g. "2 × 1 egg (50 g)" or "150 g"
    val kcal: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val loggedAt: Long
)

@Entity(tableName = "water_logs", indices = [Index("day")])
data class WaterLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val day: Long,
    val ml: Int,
    val loggedAt: Long
)

/** A completed day of a 30-day home challenge. */
@Entity(tableName = "challenge_progress", indices = [Index(value = ["challengeId", "day"], unique = true)])
data class ChallengeProgressEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val challengeId: String,
    val day: Int,
    val completedAt: Long
)

@Entity(tableName = "workout_plans")
data class WorkoutPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val goal: String,
    val durationWeeks: Int,
    val daysPerWeek: Int,
    val status: String,            // ACTIVE / ARCHIVED
    val version: Int,
    val createdAt: Long,
    val notes: String
)

@Entity(
    tableName = "workout_days",
    indices = [Index("planId")]
)
data class WorkoutDayEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val planId: Long,
    val dayOrder: Int,
    val name: String,              // e.g. "Upper A", "Full Body A"
    val focus: String,             // muscle focus summary
    val isRest: Boolean
)

@Entity(
    tableName = "workout_day_exercises",
    indices = [Index("dayId"), Index("exerciseId")]
)
data class WorkoutDayExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dayId: Long,
    val exerciseId: String,
    val orderIndex: Int,
    val sets: Int,
    val repMin: Int,
    val repMax: Int,
    val restSeconds: Int,
    val tempo: String
)

@Entity(
    tableName = "workout_sessions",
    indices = [Index("planId"), Index("dayId")]
)
data class WorkoutSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val planId: Long,
    val dayId: Long,
    val dayName: String,
    val startedAt: Long,
    val finishedAt: Long?,
    val durationSec: Int,
    val completed: Boolean,
    val kind: String = "GYM",      // GYM (logged sets) or HOME (guided follow-along)
    val calories: Int = 0
)

@Entity(
    tableName = "set_logs",
    indices = [Index("sessionId"), Index("exerciseId")]
)
data class SetLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val exerciseId: String,
    val exerciseName: String,
    val setNumber: Int,
    val weightKg: Double,
    val reps: Int,
    val effort: String,            // Effort
    val completed: Boolean,
    val timestamp: Long
)

@Entity(tableName = "body_measurements")
data class BodyMeasurementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,              // WEIGHT / WAIST / CHEST / ARMS / THIGHS
    val value: Double,
    val date: Long
)

@Entity(tableName = "personal_records")
data class PersonalRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val exerciseId: String,
    val exerciseName: String,
    val weightKg: Double,
    val reps: Int,
    val estimatedOneRm: Double,
    val achievedAt: Long
)
