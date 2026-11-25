package com.gymtrainer.app.data

import android.content.res.AssetManager
import com.gymtrainer.app.domain.Deload
import com.gymtrainer.app.domain.DeloadKind
import com.gymtrainer.app.domain.DeloadStatus
import com.gymtrainer.app.domain.Effort
import com.gymtrainer.app.domain.PastSession
import com.gymtrainer.app.domain.PastSet
import com.gymtrainer.app.domain.Progression
import com.gymtrainer.app.domain.Target
import com.gymtrainer.app.domain.Volume
import com.gymtrainer.app.domain.VolumeMuscle
import com.gymtrainer.app.domain.EquipmentProfile
import com.gymtrainer.app.domain.GymWorkouts
import com.gymtrainer.app.domain.PlanGenerator
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.round

/** Single entry point for all data access, plan generation, home and gym sessions. */
class GymRepository(
    private val db: AppDatabase,
    private val assets: AssetManager,
    private val training: TrainingStore = MemoryTrainingStore()
) {

    private val profileDao = db.profileDao()
    private val exerciseDao = db.exerciseDao()
    private val planDao = db.planDao()
    private val sessionDao = db.sessionDao()
    private val measurementDao = db.measurementDao()
    private val recordDao = db.recordDao()
    private val challengeDao = db.challengeDao()

    private val cacheLock = Mutex()
    @Volatile private var exerciseCache: Map<String, ExerciseEntity>? = null

    // ---- Seeding ----
    suspend fun ensureSeeded() {
        // Upsert the bundled library whenever its size changes (new exercises in an update). Logs keep their ids.
        val library = SeedData.loadExercises(assets)
        if (exerciseDao.count() != library.size) {
            exerciseDao.insertAll(library)
            exerciseCache = null
        }
        val templateNames = planDao.days(TEMPLATE_PLAN_ID).map { it.name }
        if (templateNames != GymWorkouts.all.map { it.title }) {
            val known = exerciseMap()
            db.withTransaction { seedTemplates(known) }
        }
    }

    /** (Re)seeds the gym workout catalog as runnable template days. History is kept (sessions store the day name). */
    private suspend fun seedTemplates(known: Map<String, ExerciseEntity>) {
        planDao.deletePlanDayExercises(TEMPLATE_PLAN_ID)
        planDao.deletePlanDays(TEMPLATE_PLAN_ID)
        GymWorkouts.all.forEachIndexed { order, w ->
            val dayId = planDao.insertDay(
                WorkoutDayEntity(planId = TEMPLATE_PLAN_ID, dayOrder = order, name = w.title, focus = w.subtitle, isRest = false)
            )
            w.items.filter { it.id in known }.forEachIndexed { i, it ->
                planDao.insertDayExercise(
                    WorkoutDayExerciseEntity(dayId = dayId, exerciseId = it.id, orderIndex = i, sets = it.sets,
                        repMin = it.repMin, repMax = it.repMax, restSeconds = it.rest, tempo = "2-0-1")
                )
            }
        }
    }

    /** In-memory id → exercise map (the library is read-only at runtime). */
    suspend fun exerciseMap(): Map<String, ExerciseEntity> = exerciseCache ?: cacheLock.withLock {
        exerciseCache ?: exerciseDao.all().associateBy { it.id }.also { exerciseCache = it }
    }

    // ---- Profile ----
    fun observeProfile(): Flow<UserProfileEntity?> = profileDao.observe()
    suspend fun getProfile(): UserProfileEntity? = profileDao.get()
    suspend fun saveProfile(profile: UserProfileEntity) = profileDao.upsert(profile)

    // ---- Exercises ----
    fun observeExercises(): Flow<List<ExerciseEntity>> = exerciseDao.observeAll()
    suspend fun exercise(id: String): ExerciseEntity? = exerciseMap()[id] ?: exerciseDao.byId(id)

    // ---- Plans & routines ----
    fun observeActivePlan(): Flow<WorkoutPlanEntity?> = planDao.observeActivePlan()
    suspend fun activePlan(): WorkoutPlanEntity? = planDao.activePlan()
    fun observeDays(planId: Long): Flow<List<WorkoutDayEntity>> = planDao.observeDays(planId)
    suspend fun days(planId: Long): List<WorkoutDayEntity> = planDao.days(planId)
    suspend fun day(dayId: Long): WorkoutDayEntity? = planDao.day(dayId)
    suspend fun dayExercises(dayId: Long): List<WorkoutDayExerciseEntity> = planDao.dayExercises(dayId)

    /** Build and persist a fresh plan from the profile, archiving any previous plan. */
    suspend fun generateAndSavePlan(profile: UserProfileEntity): Long {
        val library = exerciseMap().values.toList()
        val draft = PlanGenerator.generate(profile, library)
        return db.withTransaction { insertPlan(profile, draft) }
    }

    private suspend fun insertPlan(profile: UserProfileEntity, draft: PlanGenerator.DraftPlan): Long {
        planDao.archiveActivePlans()
        val planId = planDao.insertPlan(
            WorkoutPlanEntity(
                name = draft.name, goal = profile.goal, durationWeeks = draft.durationWeeks,
                daysPerWeek = profile.daysPerWeek, status = "ACTIVE", version = 1,
                createdAt = System.currentTimeMillis(), notes = draft.notes
            )
        )
        draft.days.forEachIndexed { dayIndex, d ->
            val dayId = planDao.insertDay(
                WorkoutDayEntity(planId = planId, dayOrder = dayIndex, name = d.name, focus = d.focus, isRest = d.isRest)
            )
            d.exercises.forEachIndexed { i, e ->
                planDao.insertDayExercise(
                    WorkoutDayExerciseEntity(dayId = dayId, exerciseId = e.exerciseId, orderIndex = i, sets = e.sets,
                        repMin = e.repMin, repMax = e.repMax, restSeconds = e.restSeconds, tempo = e.tempo)
                )
            }
        }
        return planId
    }

    /** Replace the active gym plan with a structured program. */
    suspend fun adoptGymProgram(program: com.gymtrainer.app.domain.GymProgram): Long {
        val profile = profileDao.get() ?: return -1
        val known = exerciseMap()
        val draft = program.toDraft().let { d ->
            d.copy(days = d.days.map { day -> day.copy(exercises = day.exercises.filter { it.exerciseId in known }) })
        }
        return db.withTransaction { insertPlan(profile, draft) }
    }

    data class RoutineItemDraft(val exerciseId: String, val sets: Int, val repMin: Int, val repMax: Int, val rest: Int)

    /** Save a user-built routine; it behaves like any plan day in the active workout screen. */
    suspend fun saveCustomRoutine(name: String, items: List<RoutineItemDraft>): Long {
        val focus = items.mapNotNull { exercise(it.exerciseId)?.muscleGroup }.distinct()
            .joinToString(" • ") { it.lowercase().replaceFirstChar { c -> c.uppercase() } }
        return db.withTransaction { insertCustomRoutine(name, focus, items) }
    }

    private suspend fun insertCustomRoutine(name: String, focus: String, items: List<RoutineItemDraft>): Long {
        val order = planDao.countDays(CUSTOM_PLAN_ID)
        val dayId = planDao.insertDay(
            WorkoutDayEntity(planId = CUSTOM_PLAN_ID, dayOrder = order, name = name.ifBlank { "My routine" }, focus = focus, isRest = false)
        )
        items.forEachIndexed { i, it ->
            planDao.insertDayExercise(
                WorkoutDayExerciseEntity(dayId = dayId, exerciseId = it.exerciseId, orderIndex = i, sets = it.sets,
                    repMin = it.repMin, repMax = it.repMax, restSeconds = it.rest, tempo = "2-0-1")
            )
        }
        return dayId
    }

    suspend fun deleteRoutine(dayId: Long) {
        planDao.deleteDayExercises(dayId)
        planDao.deleteDay(dayId)
    }

    suspend fun alternativesFor(exerciseId: String): List<ExerciseEntity> {
        val ex = exercise(exerciseId) ?: return emptyList()
        val allowed = profileDao.get()?.let { EquipmentProfile.from(it.equipment).allowed }
        val all = exerciseMap().values
        val sameGroup = all.filter {
            it.muscleGroup == ex.muscleGroup && it.id != ex.id && it.category == "strength" &&
                (allowed == null || it.equipment in allowed)
        }
        return (sameGroup.filter { it.featured } + sameGroup.filter { !it.featured && it.pattern == ex.pattern })
            .distinctBy { it.id }.take(20)
    }

    // ---- Gym sessions & set logging ----
    suspend fun startSession(planId: Long, day: WorkoutDayEntity): Long =
        sessionDao.insertSession(
            WorkoutSessionEntity(planId = planId, dayId = day.id, dayName = day.name,
                startedAt = System.currentTimeMillis(), finishedAt = null, durationSec = 0, completed = false, kind = "GYM")
        )

    suspend fun logSet(sessionId: Long, exercise: ExerciseEntity, setNumber: Int, weightKg: Double, reps: Int, effort: String) {
        sessionDao.insertSet(
            SetLogEntity(sessionId = sessionId, exerciseId = exercise.id, exerciseName = exercise.name,
                setNumber = setNumber, weightKg = weightKg, reps = reps, effort = effort, completed = true,
                timestamp = System.currentTimeMillis())
        )
        maybeRecordPr(exercise, weightKg, reps)
    }

    private suspend fun maybeRecordPr(exercise: ExerciseEntity, weightKg: Double, reps: Int) {
        if (weightKg <= 0 || reps <= 0 || exercise.timed) return
        val est = round(weightKg * (1 + reps / 30.0) * 10) / 10.0 // Epley estimated 1RM
        val best = recordDao.bestOneRm(exercise.id) ?: 0.0
        if (est > best) {
            recordDao.insert(
                PersonalRecordEntity(exerciseId = exercise.id, exerciseName = exercise.name, weightKg = weightKg,
                    reps = reps, estimatedOneRm = est, achievedAt = System.currentTimeMillis())
            )
        }
    }

    suspend fun finishSession(sessionId: Long) {
        val s = sessionDao.session(sessionId) ?: return
        val now = System.currentTimeMillis()
        val durationSec = ((now - s.startedAt) / 1000).toInt()
        val weight = profileDao.get()?.weightKg ?: 70.0
        // ~5 MET for resistance training
        val kcal = (5.0 * 3.5 * weight / 200.0 * (durationSec / 60.0)).toInt()
        sessionDao.updateSession(s.copy(finishedAt = now, durationSec = durationSec, completed = true, calories = kcal))
    }

    /** Store a finished guided home workout (no per-set logs). */
    suspend fun recordHomeSession(title: String, durationSec: Int, calories: Int) {
        val now = System.currentTimeMillis()
        sessionDao.insertSession(
            WorkoutSessionEntity(planId = -2, dayId = -2, dayName = title, startedAt = now - durationSec * 1000L,
                finishedAt = now, durationSec = durationSec, completed = true, kind = "HOME", calories = calories)
        )
    }

    suspend fun setsForSession(sessionId: Long) = sessionDao.setsForSession(sessionId)
    suspend fun session(sessionId: Long) = sessionDao.session(sessionId)
    suspend fun recentSets(exerciseId: String, limit: Int = 6) = sessionDao.recentSetsForExercise(exerciseId, limit)
    fun observeCompletedSessions(): Flow<List<WorkoutSessionEntity>> = sessionDao.observeCompleted()
    suspend fun completedSessions(): List<WorkoutSessionEntity> = sessionDao.completed()
    suspend fun allSets(): List<SetLogEntity> = sessionDao.allSets()
    fun observeSetsForExercise(exerciseId: String): Flow<List<SetLogEntity>> = sessionDao.observeSetsForExercise(exerciseId)

    /**
     * Next working weight from recent history and the effort the user reported (product
     * plan section 7). Null when there is no reliable history, so the UI asks for a
     * calibration set instead.
     */
    // ---- Coaching: progression, deload, weekly volume ----

    /** Past sessions of an exercise (newest first), excluding deload sessions. */
    suspend fun exerciseHistory(exerciseId: String, sessions: Int = 4): List<PastSession> {
        val skip = training.deloadSessions()
        return sessionDao.recentSetsForExercise(exerciseId, 60)
            .filter { it.sessionId !in skip }
            .groupBy { it.sessionId }.values          // groupBy keeps first-seen (newest) order
            .take(sessions)
            .map { sets -> PastSession(sets.sortedBy { it.setNumber }.map { PastSet(it.weightKg, it.reps, Effort.from(it.effort)) }) }
    }

    /** Today's target for one exercise (double progression), lightened during a deload. */
    suspend fun targetFor(exercise: ExerciseEntity, repMin: Int, repMax: Int, deload: Boolean): Target {
        val inc = Progression.increment(exercise.equipment, exercise.muscleGroup, exercise.isCompound)
        val t = Progression.next(exerciseHistory(exercise.id), repMin, repMax, inc, exercise.equipment)
        return if (deload) Progression.deload(t, repMin, exercise.equipment) else t
    }

    private suspend fun planProgress(): Triple<WorkoutPlanEntity, Int, Int>? {
        val plan = planDao.activePlan() ?: return null
        val days = planDao.days(plan.id).count { !it.isRest }
        val done = sessionDao.completed().count { it.kind == "GYM" && it.planId == plan.id }
        return Triple(plan, days, done)
    }

    suspend fun deloadStatus(): DeloadStatus {
        val (plan, days, done) = planProgress() ?: return DeloadStatus(false)
        val window = training.deloadWindow(plan.id)
        return Deload.status(done, days, window?.first, window?.second, training.skippedWeek(plan.id))
    }

    /** Start a lighter week now (one session per training day). */
    suspend fun startDeload(kind: DeloadKind = DeloadKind.MANUAL) {
        val (plan, days, done) = planProgress() ?: return
        training.setDeloadWindow(plan.id, done + days, kind)
    }

    /** Skip a planned deload week, or end a fatigue/manual one early. */
    suspend fun skipDeload() {
        val (plan, days, done) = planProgress() ?: return
        if (training.deloadWindow(plan.id)?.let { it.first > done } == true) training.setDeloadWindow(plan.id, null, null)
        else training.setSkippedWeek(plan.id, done / days.coerceAtLeast(1))
    }

    fun markDeloadSession(sessionId: Long) = training.addDeloadSession(sessionId)

    /** After a session: start a deload by itself when the last sessions show fatigue (at most once every 3 weeks). */
    suspend fun checkFatigue(): Boolean {
        val (plan, days, done) = planProgress() ?: return false
        if (deloadStatus().active) return false
        val last = training.lastFatigueAt(plan.id)
        if (last != null && done - last < days * 3) return false
        val skip = training.deloadSessions()
        val recent = sessionDao.completed().filter { it.kind == "GYM" && it.id !in skip }.take(3)
            .map { s -> sessionDao.setsForSession(s.id).map { Effort.from(it.effort) } }
        if (!Deload.fatigued(recent)) return false
        training.setDeloadWindow(plan.id, done + days, DeloadKind.FATIGUE)
        training.setLastFatigueAt(plan.id, done)
        return true
    }

    /** Hard sets per muscle logged in [from, to). */
    suspend fun weeklyVolume(from: Long, to: Long): Map<VolumeMuscle, Double> {
        val map = exerciseMap()
        val sets = sessionDao.setsBetween(from, to).filter { Effort.from(it.effort) != Effort.PAIN }
            .mapNotNull { s -> map[s.exerciseId]?.takeIf { it.category != "stretching" && it.category != "cardio" } }
            .map { it.primaryMuscles to it.secondaryMuscles }
        return Volume.tally(sets)
    }

    // ---- Challenges ----
    fun observeChallenge(challengeId: String): Flow<List<ChallengeProgressEntity>> = challengeDao.observe(challengeId)
    fun observeAllChallengeProgress(): Flow<List<ChallengeProgressEntity>> = challengeDao.observeAll()
    suspend fun completeChallengeDay(challengeId: String, day: Int) =
        challengeDao.insert(ChallengeProgressEntity(challengeId = challengeId, day = day, completedAt = System.currentTimeMillis()))
    suspend fun resetChallenge(challengeId: String) = challengeDao.reset(challengeId)

    // ---- Measurements ----
    suspend fun addMeasurement(type: String, value: Double) =
        measurementDao.insert(BodyMeasurementEntity(type = type, value = value, date = System.currentTimeMillis()))
    suspend fun measurements(type: String) = measurementDao.byType(type)
    fun observeMeasurements(): Flow<List<BodyMeasurementEntity>> = measurementDao.observeAll()

    // ---- Records ----
    fun observeRecords(): Flow<List<PersonalRecordEntity>> = recordDao.observeAll()
}
