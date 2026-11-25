package com.gymtrainer.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gymtrainer.app.data.AppDatabase
import com.gymtrainer.app.data.GymRepository
import com.gymtrainer.app.data.MemoryTrainingStore
import com.gymtrainer.app.data.TEMPLATE_PLAN_ID
import com.gymtrainer.app.data.UserProfileEntity
import com.gymtrainer.app.domain.Deload
import com.gymtrainer.app.domain.DeloadKind
import com.gymtrainer.app.domain.Difficulty
import com.gymtrainer.app.domain.Effort
import com.gymtrainer.app.domain.PastSession
import com.gymtrainer.app.domain.PastSet
import com.gymtrainer.app.domain.Progression
import com.gymtrainer.app.domain.TargetAction
import com.gymtrainer.app.domain.Volume
import com.gymtrainer.app.domain.VolumeMuscle
import com.gymtrainer.app.domain.VolumeStatus
import com.gymtrainer.app.domain.WarmUp
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CoachingTest {

    private fun s(w: Double, vararg reps: Int, effort: Effort = Effort.GOOD) = PastSession(reps.map { PastSet(w, it, effort) })

    // ---------- Progression ----------

    @Test fun firstTimeHasNoWeight() {
        val t = Progression.next(emptyList(), 8, 12, 2.5)
        assertEquals(TargetAction.START, t.action); assertNull(t.weightKg)
    }

    @Test fun hittingTopOfRangeOnEverySetAddsWeight() {
        val t = Progression.next(listOf(s(60.0, 12, 12, 12)), 8, 12, 2.5)
        assertEquals(TargetAction.INCREASE, t.action); assertEquals(62.5, t.weightKg!!, 0.001); assertEquals(8, t.reps)
    }

    @Test fun notAllSetsAtTopBuildsReps() {
        val t = Progression.next(listOf(s(60.0, 12, 10, 9)), 8, 12, 2.5)
        assertEquals(TargetAction.BUILD_REPS, t.action); assertEquals(60.0, t.weightKg!!, 0.001); assertEquals(10, t.reps)
    }

    @Test fun tooHeavyAtTopDoesNotIncrease() {
        val t = Progression.next(listOf(s(60.0, 12, 12, 12, effort = Effort.HARD)), 8, 12, 2.5)
        assertTrue(t.action != TargetAction.INCREASE)
    }

    @Test fun missingOnceRepeatsMissingTwiceDropsTenPercent() {
        val once = Progression.next(listOf(s(100.0, 6, 5, 5), s(97.5, 8, 8, 8)), 8, 12, 2.5)
        assertEquals(TargetAction.REPEAT, once.action); assertEquals(100.0, once.weightKg!!, 0.001)
        val twice = Progression.next(listOf(s(100.0, 6, 5, 5), s(100.0, 7, 6, 6)), 8, 12, 2.5)
        assertEquals(TargetAction.DECREASE, twice.action); assertEquals(90.0, twice.weightKg!!, 0.001)
    }

    @Test fun painGoesLighter() {
        val t = Progression.next(listOf(s(50.0, 10, 10, effort = Effort.PAIN)), 8, 12, 2.5)
        assertEquals(TargetAction.DECREASE, t.action); assertTrue(t.weightKg!! < 50.0)
    }

    @Test fun bodyweightProgressesReps() {
        val t = Progression.next(listOf(s(0.0, 9, 8)), 8, 12, 0.0, "Bodyweight")
        assertEquals(TargetAction.BUILD_REPS, t.action); assertEquals(10, t.reps); assertNull(t.weightKg)
    }

    @Test fun incrementsByEquipment() {
        assertEquals(5.0, Progression.increment("Barbell", "LEGS", true), 0.0)
        assertEquals(2.5, Progression.increment("Barbell", "CHEST", true), 0.0)
        assertEquals(2.0, Progression.increment("Dumbbell", "ARMS", false), 0.0)
        assertEquals(0.0, Progression.increment("Bodyweight", "CHEST", true), 0.0)
        assertEquals(82.5, Progression.loadable(81.7, "Barbell"), 0.0)
    }

    // ---------- Deload ----------

    @Test fun everyFifthWeekIsADeload() {
        // 3 days/week: weeks 0-3 normal, week 4 (sessions 12..14) deload.
        assertFalse(Deload.status(11, 3, null, null, null).active)
        val d = Deload.status(12, 3, null, null, null)
        assertTrue(d.active); assertEquals(DeloadKind.SCHEDULED, d.kind); assertEquals(3, d.sessionsLeft)
        assertFalse(Deload.status(15, 3, null, null, null).active)
        assertEquals(12, Deload.status(0, 3, null, null, null).nextInSessions)
        // Skipped.
        assertFalse(Deload.status(13, 3, null, null, 4).active)
        // Manual window.
        val m = Deload.status(2, 3, 5, DeloadKind.MANUAL, null)
        assertTrue(m.active); assertEquals(3, m.sessionsLeft)
        assertEquals(2, Deload.sets(3)); assertEquals(3, Deload.sets(5))
    }

    @Test fun fatigueNeedsTwoOfThreeHardSessions() {
        val hard = listOf(Effort.HARD, Effort.HARD, Effort.GOOD)
        val fine = listOf(Effort.GOOD, Effort.GOOD, Effort.EASY)
        assertTrue(Deload.fatigued(listOf(hard, hard, fine)))
        assertFalse(Deload.fatigued(listOf(hard, fine, fine)))
        assertTrue(Deload.fatigued(listOf(listOf(Effort.PAIN), hard)))
    }

    // ---------- Warm-up ----------

    @Test fun barbellWarmUpRampsWithPlates() {
        val w = WarmUp.plan(100.0, "Barbell", true, imperial = false)
        assertEquals(listOf(20.0, 50.0, 70.0, 85.0), w.map { it.weight })
        assertTrue(w.first().isBar)
        assertEquals(listOf(15.0), WarmUp.plates(50.0, false))
        assertEquals(listOf(25.0, 5.0, 2.5), WarmUp.plates(85.0, false))
        assertTrue(WarmUp.plan(25.0, "Barbell", true, false).isEmpty())
        assertTrue(WarmUp.plan(100.0, "Barbell", false, false).isEmpty())
        assertEquals(2, WarmUp.plan(30.0, "Dumbbell", true, false).size)
        assertEquals(listOf(45.0, 45.0), WarmUp.plates(225.0, true))
    }

    // ---------- Volume ----------

    @Test fun volumeCountsPrimaryOneSecondaryHalf() {
        val c = Volume.credit("Quadriceps", "Glutes, Hamstrings, Calves")
        assertEquals(1.0, c[VolumeMuscle.QUADS]!!, 0.0)
        assertEquals(0.5, c[VolumeMuscle.GLUTES]!!, 0.0)
        val t = Volume.tally(List(3) { "Chest" to "Triceps, Shoulders" } + listOf("Lats" to "Biceps"))
        assertEquals(3.0, t[VolumeMuscle.CHEST]!!, 0.0)
        assertEquals(1.5, t[VolumeMuscle.TRICEPS]!!, 0.0)
        assertEquals(1.0, t[VolumeMuscle.BACK]!!, 0.0)
        assertEquals(0.0, t[VolumeMuscle.CALVES]!!, 0.0)
        val r = Volume.target(Difficulty.INTERMEDIATE)
        assertEquals(VolumeStatus.LOW, Volume.status(4.0, r))
        assertEquals(VolumeStatus.OK, Volume.status(12.0, r))
        assertEquals(VolumeStatus.HIGH, Volume.status(30.0, r))
    }

    // ---------- Repository end-to-end ----------

    private lateinit var db: AppDatabase
    private lateinit var repo: GymRepository

    @Before fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java).allowMainThreadQueries().build()
        repo = GymRepository(db, ctx.assets, MemoryTrainingStore())
        runBlocking { repo.ensureSeeded() }
    }

    @After fun tearDown() = db.close()

    @Test fun repositoryProgressesAndSkipsDeloadSessions() = runBlocking {
        val day = repo.days(TEMPLATE_PLAN_ID).first { it.name == "Strength 5×5" }
        val squat = repo.exercise("Barbell_Squat")!!
        // Session 1: 5×5 at 100 kg, all clean.
        val s1 = repo.startSession(day.planId, day)
        repeat(5) { repo.logSet(s1, squat, it + 1, 100.0, 5, Effort.GOOD.name) }
        repo.finishSession(s1)
        val t = repo.targetFor(squat, 5, 5, deload = false)
        assertEquals(TargetAction.INCREASE, t.action); assertEquals(105.0, t.weightKg!!, 0.001)
        // A deload session at 90 kg must not reset progression.
        val s2 = repo.startSession(day.planId, day); repo.markDeloadSession(s2)
        repeat(3) { repo.logSet(s2, squat, it + 1, 90.0, 5, Effort.EASY.name) }
        repo.finishSession(s2)
        assertEquals(105.0, repo.targetFor(squat, 5, 5, deload = false).weightKg!!, 0.001)
        // Deload target is ~10% lighter.
        val d = repo.targetFor(squat, 5, 5, deload = true)
        assertEquals(TargetAction.DELOAD, d.action); assertEquals(95.0, d.weightKg!!, 0.001)
        // Weekly volume counts the 8 squat sets for quads.
        val vol = repo.weeklyVolume(0, Long.MAX_VALUE)
        assertEquals(8.0, vol[VolumeMuscle.QUADS]!!, 0.001)
    }

    @Test fun fatigueStartsADeloadOnThePlan() = runBlocking {
        repo.saveProfile(UserProfileEntity(name = "T", age = 30, heightCm = 178.0, weightKg = 80.0, targetWeightKg = null,
            units = "METRIC", experience = "INTERMEDIATE", goal = "MUSCLE_GAIN", secondaryGoal = null, daysPerWeek = 3,
            sessionMinutes = 60, equipment = "FULL_GYM", limitations = "", preferences = "", createdAt = 0L))
        repo.generateAndSavePlan(repo.getProfile()!!)
        val plan = repo.activePlan()!!
        val day = repo.days(plan.id).first { !it.isRest }
        val ex = repo.exercise(repo.dayExercises(day.id).first().exerciseId)!!
        assertFalse(repo.deloadStatus().active)
        repeat(2) {
            val sid = repo.startSession(plan.id, day)
            repeat(3) { i -> repo.logSet(sid, ex, i + 1, 50.0, 4, Effort.HARD.name) }
            repo.finishSession(sid)
        }
        assertTrue(repo.checkFatigue())
        val st = repo.deloadStatus()
        assertTrue(st.active); assertEquals(DeloadKind.FATIGUE, st.kind)
        repo.skipDeload()
        assertFalse(repo.deloadStatus().active)
        repo.startDeload()
        assertEquals(DeloadKind.MANUAL, repo.deloadStatus().kind)
    }
}
