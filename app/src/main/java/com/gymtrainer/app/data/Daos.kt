package com.gymtrainer.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun observe(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile WHERE id = 1")
    suspend fun get(): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: UserProfileEntity)
}

@Dao
interface ExerciseDao {
    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun count(): Int

    @Query("SELECT id || '|' || name || '|' || aliases || '|' || imageOf FROM exercises")
    suspend fun fingerprints(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ExerciseEntity>)

    @Query("SELECT * FROM exercises ORDER BY name")
    fun observeAll(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun byId(id: String): ExerciseEntity?

    @Query("SELECT * FROM exercises ORDER BY name")
    suspend fun all(): List<ExerciseEntity>

    @Query("DELETE FROM exercises")
    suspend fun deleteAll()
}

@Dao
interface VideoDao {
    @Insert
    suspend fun insert(v: UserVideoEntity): Long

    @Query("SELECT * FROM user_videos WHERE exerciseId = :exerciseId ORDER BY addedAt DESC")
    fun observe(exerciseId: String): Flow<List<UserVideoEntity>>

    @Query("DELETE FROM user_videos WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface NutritionDao {
    @Insert
    suspend fun insertFood(f: FoodLogEntity): Long

    @Query("SELECT * FROM food_logs WHERE day = :day ORDER BY loggedAt")
    fun observeFoods(day: Long): Flow<List<FoodLogEntity>>

    @Query("DELETE FROM food_logs WHERE id = :id")
    suspend fun deleteFood(id: Long)

    @Query("SELECT COUNT(DISTINCT day) FROM food_logs")
    fun observeLoggedDays(): Flow<Int>

    @Insert
    suspend fun insertWater(w: WaterLogEntity): Long

    @Query("SELECT COALESCE(SUM(ml), 0) FROM water_logs WHERE day = :day")
    fun observeWater(day: Long): Flow<Int>

    @Query("DELETE FROM water_logs WHERE id = (SELECT id FROM water_logs WHERE day = :day ORDER BY loggedAt DESC LIMIT 1)")
    suspend fun undoWater(day: Long)
}

@Dao
interface ChallengeDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(p: ChallengeProgressEntity)

    @Query("SELECT * FROM challenge_progress ORDER BY day")
    fun observeAll(): Flow<List<ChallengeProgressEntity>>

    @Query("SELECT * FROM challenge_progress WHERE challengeId = :challengeId ORDER BY day")
    fun observe(challengeId: String): Flow<List<ChallengeProgressEntity>>

    @Query("DELETE FROM challenge_progress WHERE challengeId = :challengeId")
    suspend fun reset(challengeId: String)
}

@Dao
interface PlanDao {
    @Insert
    suspend fun insertPlan(plan: WorkoutPlanEntity): Long

    @Insert
    suspend fun insertDay(day: WorkoutDayEntity): Long

    @Insert
    suspend fun insertDayExercise(ex: WorkoutDayExerciseEntity): Long

    @Query("UPDATE workout_plans SET status = 'ARCHIVED' WHERE status = 'ACTIVE'")
    suspend fun archiveActivePlans()

    @Query("SELECT * FROM workout_plans WHERE status = 'ACTIVE' ORDER BY createdAt DESC LIMIT 1")
    fun observeActivePlan(): Flow<WorkoutPlanEntity?>

    @Query("SELECT * FROM workout_plans WHERE status = 'ACTIVE' ORDER BY createdAt DESC LIMIT 1")
    suspend fun activePlan(): WorkoutPlanEntity?

    @Query("SELECT * FROM workout_days WHERE planId = :planId ORDER BY dayOrder")
    fun observeDays(planId: Long): Flow<List<WorkoutDayEntity>>

    @Query("SELECT * FROM workout_days WHERE planId = :planId ORDER BY dayOrder")
    suspend fun days(planId: Long): List<WorkoutDayEntity>

    @Query("SELECT * FROM workout_days WHERE id = :dayId")
    suspend fun day(dayId: Long): WorkoutDayEntity?

    @Query("SELECT * FROM workout_day_exercises WHERE dayId = :dayId ORDER BY orderIndex")
    suspend fun dayExercises(dayId: Long): List<WorkoutDayExerciseEntity>

    @Query("SELECT * FROM workout_day_exercises WHERE dayId = :dayId ORDER BY orderIndex")
    fun observeDayExercises(dayId: Long): Flow<List<WorkoutDayExerciseEntity>>

    @Update
    suspend fun updateDayExercise(ex: WorkoutDayExerciseEntity)

    @Query("SELECT COUNT(*) FROM workout_days WHERE planId = :planId")
    suspend fun countDays(planId: Long): Int

    @Query("DELETE FROM workout_day_exercises WHERE dayId = :dayId")
    suspend fun deleteDayExercises(dayId: Long)

    @Query("DELETE FROM workout_days WHERE id = :dayId")
    suspend fun deleteDay(dayId: Long)

    @Query("DELETE FROM workout_day_exercises WHERE dayId IN (SELECT id FROM workout_days WHERE planId = :planId)")
    suspend fun deletePlanDayExercises(planId: Long)

    @Query("DELETE FROM workout_days WHERE planId = :planId")
    suspend fun deletePlanDays(planId: Long)
}

@Dao
interface SessionDao {
    @Insert
    suspend fun insertSession(session: WorkoutSessionEntity): Long

    @Update
    suspend fun updateSession(session: WorkoutSessionEntity)

    @Query("SELECT * FROM workout_sessions WHERE id = :id")
    suspend fun session(id: Long): WorkoutSessionEntity?

    @Query("SELECT * FROM workout_sessions WHERE completed = 1 ORDER BY startedAt DESC")
    fun observeCompleted(): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM workout_sessions WHERE completed = 1 ORDER BY startedAt DESC")
    suspend fun completed(): List<WorkoutSessionEntity>

    @Insert
    suspend fun insertSet(log: SetLogEntity): Long

    @Query("SELECT * FROM set_logs WHERE sessionId = :sessionId ORDER BY id")
    suspend fun setsForSession(sessionId: Long): List<SetLogEntity>

    @Query("SELECT * FROM set_logs WHERE exerciseId = :exerciseId AND completed = 1 ORDER BY timestamp DESC LIMIT :limit")
    suspend fun recentSetsForExercise(exerciseId: String, limit: Int): List<SetLogEntity>

    @Query("SELECT * FROM set_logs WHERE completed = 1 ORDER BY timestamp DESC")
    suspend fun allSets(): List<SetLogEntity>

    @Query("SELECT * FROM set_logs WHERE completed = 1 AND timestamp >= :from AND timestamp < :to")
    suspend fun setsBetween(from: Long, to: Long): List<SetLogEntity>

    @Query("SELECT * FROM set_logs WHERE exerciseId = :exerciseId AND completed = 1 ORDER BY timestamp")
    fun observeSetsForExercise(exerciseId: String): Flow<List<SetLogEntity>>
}

@Dao
interface MeasurementDao {
    @Insert
    suspend fun insert(m: BodyMeasurementEntity): Long

    @Query("SELECT * FROM body_measurements WHERE type = :type ORDER BY date")
    suspend fun byType(type: String): List<BodyMeasurementEntity>

    @Query("SELECT * FROM body_measurements ORDER BY date DESC")
    fun observeAll(): Flow<List<BodyMeasurementEntity>>
}

@Dao
interface RecordDao {
    @Insert
    suspend fun insert(pr: PersonalRecordEntity): Long

    @Query("SELECT * FROM personal_records ORDER BY estimatedOneRm DESC")
    fun observeAll(): Flow<List<PersonalRecordEntity>>

    @Query("SELECT MAX(estimatedOneRm) FROM personal_records WHERE exerciseId = :exerciseId")
    suspend fun bestOneRm(exerciseId: String): Double?
}
