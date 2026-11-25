package com.gymtrainer.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gymtrainer.app.data.AppDatabase
import com.gymtrainer.app.data.GymRepository
import com.gymtrainer.app.data.TEMPLATE_PLAN_ID
import com.gymtrainer.app.data.UserProfileEntity
import com.gymtrainer.app.domain.EquipmentProfile
import com.gymtrainer.app.domain.Experience
import com.gymtrainer.app.domain.Goal
import com.gymtrainer.app.domain.Difficulty
import com.gymtrainer.app.domain.GymCategory
import com.gymtrainer.app.domain.GymWorkouts
import com.gymtrainer.app.domain.HomePrograms
import com.gymtrainer.app.domain.PlanGenerator
import com.gymtrainer.app.ui.tools.Calc
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class DataLayerTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var repo: GymRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        repo = GymRepository(db, context.assets)
        runBlocking { repo.ensureSeeded() }
    }

    @After
    fun tearDown() = db.close()

    private fun profile(days: Int, eq: EquipmentProfile, exp: Experience, goal: Goal, limitations: String = "") =
        UserProfileEntity(
            name = "Test", age = 30, heightCm = 178.0, weightKg = 80.0, targetWeightKg = null,
            units = "METRIC", experience = exp.name, goal = goal.name, secondaryGoal = null,
            daysPerWeek = days, sessionMinutes = 60, equipment = eq.name, limitations = limitations,
            preferences = "", createdAt = 0L
        )

    @Test
    fun seedsFullLibraryWithPhotos() = runBlocking {
        val map = repo.exerciseMap()
        assertTrue("library size ${map.size}", map.size >= 1340)
        val withPhotos = map.values.filter { it.imageCount > 0 }
        assertTrue("photos for ${withPhotos.size}", withPhotos.size >= 1300)
        // Every photo the library claims must exist in assets (variants point at their base movement).
        withPhotos.forEach { ex ->
            repeat(ex.imageCount) { i -> context.assets.open("ex/${ex.imageOf.ifEmpty { ex.id }}/$i.webp").close() }
        }
        // Equipment variants: unique names, real base photos, app equipment values.
        val variants = map.values.filter { it.id.startsWith("v_") }
        assertTrue("variants ${variants.size}", variants.size >= 200)
        assertEquals(map.values.size, map.values.map { it.name.lowercase() }.toSet().size)
        variants.forEach { v -> assertTrue(v.name, v.imageOf in map && v.equipment in setOf("Bands", "Barbell", "Cable", "Dumbbell", "Kettlebell", "Machine", "Bodyweight", "Other")) }
        listOf("Stiff Leg Deadlift - Kettlebell", "Triceps Kickback - Band", "Upright Row - Kettlebell", "Wrist Curl - Band", "Zercher Carry - Barbell", "Sumo Squat - Smith Machine")
            .forEach { n -> assertTrue("missing $n", map.values.any { it.name == n }) }
        // Re-seeding is idempotent.
        repo.ensureSeeded()
        assertEquals(map.size, repo.exerciseMap().size)
        assertTrue(map.values.count { it.featured } >= 95)
    }

    @Test
    fun everyHomeWorkoutExerciseResolves() = runBlocking {
        val map = repo.exerciseMap()
        HomePrograms.referencedIds().forEach { assertTrue("missing $it", it in map) }
        HomePrograms.workouts.forEach { w ->
            assertTrue(w.key, w.items.isNotEmpty())
            assertNotNull(HomePrograms.resolve(w.key))
        }
    }

    @Test
    fun seedsGymCatalogAsRunnableTemplates() = runBlocking {
        val templates = repo.days(TEMPLATE_PLAN_ID)
        assertEquals(GymWorkouts.all.size, templates.size)
        assertEquals(27, templates.size)
        templates.forEach { assertTrue(it.name, repo.dayExercises(it.id).size >= 3) }
        // Every catalog workout keeps all of its exercises (ids exist in the library).
        GymWorkouts.all.forEach { w ->
            val day = templates.first { it.name == w.title }
            assertEquals(w.title, w.items.size, repo.dayExercises(day.id).size)
        }
        // Every category offers at least one workout; muscle categories cover all three levels.
        GymCategory.entries.forEach { c ->
            val ws = GymWorkouts.byCategory(c)
            assertTrue(c.name, ws.isNotEmpty())
            if (c != GymCategory.SPLIT) assertEquals(c.name, Difficulty.entries.toSet(), ws.map { it.level }.toSet())
        }
        // Seeding twice must not duplicate.
        repo.ensureSeeded()
        assertEquals(27, repo.days(TEMPLATE_PLAN_ID).size)
    }

    @Test
    fun generatesValidPlansForEveryProfile() = runBlocking {
        val lib = repo.exerciseMap().values.toList()
        for (days in 2..6) for (eq in EquipmentProfile.entries) for (exp in Experience.entries) for (goal in listOf(Goal.STRENGTH, Goal.FAT_LOSS, Goal.MUSCLE_GAIN)) {
            val plan = PlanGenerator.generate(profile(days, eq, exp, goal), lib)
            assertEquals(days, plan.days.size)
            plan.days.forEach { d ->
                val min = if (eq == EquipmentProfile.BODYWEIGHT) 3 else 4
                assertTrue("$days/$eq/$exp/${d.name} only ${d.exercises.size}", d.exercises.size >= min)
                assertEquals("duplicate in ${d.name}", d.exercises.size, d.exercises.map { it.exerciseId }.toSet().size)
                d.exercises.forEach { e ->
                    val ex = repo.exercise(e.exerciseId)!!
                    assertTrue("${ex.id} not featured", ex.featured)
                    assertTrue("${ex.id} (${ex.equipment}) not allowed for $eq", ex.equipment in eq.allowed)
                    assertTrue(e.sets in 3..5)
                }
            }
        }
    }

    @Test
    fun limitationsAreNeverRelaxed() = runBlocking {
        val lib = repo.exerciseMap().values.toList()
        for (eq in EquipmentProfile.entries) for (days in 2..6) {
            val knee = PlanGenerator.generate(profile(days, eq, Experience.ADVANCED, Goal.MUSCLE_GAIN, "knee pain"), lib)
            knee.days.flatMap { it.exercises }.forEach { assertFalse(repo.exercise(it.exerciseId)!!.pattern == "Lunge") }
            val shoulder = PlanGenerator.generate(profile(days, eq, Experience.ADVANCED, Goal.STRENGTH, "shoulder injury"), lib)
            shoulder.days.flatMap { it.exercises }.forEach {
                val p = repo.exercise(it.exerciseId)!!.pattern
                assertFalse(p, p == "Vertical Push" || p == "Incline Push")
            }
            val back = PlanGenerator.generate(profile(days, eq, Experience.ADVANCED, Goal.STRENGTH, "lower back disc"), lib)
            back.days.flatMap { it.exercises }.forEach { assertFalse(repo.exercise(it.exerciseId)!!.pattern == "Hinge") }
        }
    }

    @Test
    fun beginnersGetNoAdvancedLifts() = runBlocking {
        val lib = repo.exerciseMap().values.toList()
        for (days in 2..6) {
            val plan = PlanGenerator.generate(profile(days, EquipmentProfile.FULL_GYM, Experience.BEGINNER, Goal.MUSCLE_GAIN), lib)
            plan.days.flatMap { it.exercises }.forEach { assertTrue(repo.exercise(it.exerciseId)!!.difficulty != "ADVANCED") }
        }
    }

    @Test
    fun customRoutinesHomeSessionsAndChallenges() = runBlocking {
        val dayId = repo.saveCustomRoutine("Chest & Tri", listOf(
            GymRepository.RoutineItemDraft("Dumbbell_Bench_Press", 3, 8, 12, 90),
            GymRepository.RoutineItemDraft("Triceps_Pushdown", 3, 10, 15, 60)
        ))
        assertEquals(2, repo.dayExercises(dayId).size)
        assertEquals("Chest & Tri", repo.day(dayId)!!.name)

        repo.recordHomeSession("Abs Beginner", 600, 75)
        val sessions = repo.observeCompletedSessions().first()
        assertEquals(1, sessions.size)
        assertEquals("HOME", sessions[0].kind)
        assertEquals(75, sessions[0].calories)

        repo.completeChallengeDay("full30", 1)
        repo.completeChallengeDay("full30", 1)
        assertEquals(1, repo.observeChallenge("full30").first().size)

        repo.deleteRoutine(dayId)
        assertNull(repo.day(dayId))
    }

    @Test
    fun challengesProgressAndRest() {
        HomePrograms.challenges.forEach { c ->
            (1..30).forEach { d ->
                if (c.isRestDay(d)) assertNull(c.workoutFor(d)) else assertNotNull("${c.id} day $d", c.workoutFor(d))
            }
            val first = c.workoutFor(1)!!.items
            val last = c.workoutFor(29)!!.items
            assertTrue(last.sumOf { it.reps + it.seconds } > first.sumOf { it.reps + it.seconds })
            assertNotNull(HomePrograms.resolve("c:${c.id}:5"))
        }
    }

    @Test
    fun everyProgramAndGymProgramResolves() = runBlocking {
        val map = repo.exerciseMap()
        com.gymtrainer.app.domain.GymPrograms.referencedIds().forEach { assertTrue("gym program uses missing $it", it in map) }
        (HomePrograms.challenges + HomePrograms.plans).forEach { p ->
            assertNotNull(HomePrograms.program(p.id))
            var sessions = 0
            for (d in 1..p.totalDays) {
                val w = p.workoutFor(d)
                if (p.isRestDay(d)) { assertNull(w); continue }
                sessions++
                assertNotNull("${p.id} day $d", w)
                assertTrue("${p.id} day $d too short", w!!.items.size >= 4)
                w.items.forEach { assertTrue("${p.id} uses missing ${it.exerciseId}", it.exerciseId in map) }
                assertEquals(w, HomePrograms.resolve(w.key))
            }
            assertTrue(sessions > 0)
        }
        assertEquals(12, HomePrograms.plans.size)
        assertTrue(HomePrograms.workouts.size >= 35)
    }

    @Test
    fun personalPlansFitEveryProfile() = runBlocking {
        val map = repo.exerciseMap()
        for (goal in Goal.entries) for (exp in Experience.entries) for (days in 2..6) for (lim in listOf("", "knee pain", "bad shoulder, lower back")) {
            val p = profile(days, EquipmentProfile.BODYWEIGHT, exp, goal, lim).copy(sessionMinutes = 30, focusAreas = "ABS,LEG")
            val program = HomePrograms.personalProgram(p)
            val spec = HomePrograms.personalSpec(p)
            assertEquals(days, (1..7).count { !program.isRestDay(it) })
            assertEquals(program.id, HomePrograms.program(program.id)!!.id)
            for (d in 1..program.totalDays) {
                val w = program.workoutFor(d) ?: continue
                w.items.forEach { item ->
                    assertTrue("missing ${item.exerciseId}", item.exerciseId in map)
                    assertFalse("dumbbell move for bodyweight user", map[item.exerciseId]!!.equipment == "Dumbbell")
                }
                // Limitations: no lunges / jumping for knees, no overhead pressing for shoulders.
                if ("knee" in lim) w.items.forEach { assertFalse(it.exerciseId, it.exerciseId in setOf("x_Bodyweight_Lunges", "x_Jumping_Jacks", "x_Burpees", "Freehand_Jump_Squat", "Split_Jump")) }
                if ("shoulder" in lim) w.items.forEach { assertFalse(it.exerciseId, it.exerciseId in setOf("x_Pike_Push-Ups", "Pushups", "Bench_Dips")) }
            }
            assertEquals(spec.level, program.level)
        }
    }

    @Test
    fun adoptingGymProgramReplacesActivePlan() = runBlocking {
        val p = profile(3, EquipmentProfile.FULL_GYM, Experience.INTERMEDIATE, Goal.MUSCLE_GAIN)
        repo.saveProfile(p)
        repo.generateAndSavePlan(p)
        val program = com.gymtrainer.app.domain.GymPrograms.byId("gym_int_ul")!!
        repo.adoptGymProgram(program)
        val active = repo.activePlan()!!
        assertEquals("Upper / Lower", active.name)
        val days = repo.days(active.id)
        assertEquals(4, days.size)
        days.forEach { assertTrue(repo.dayExercises(it.id).size >= 5) }
    }

    @Test
    fun migrationTwoToThreeKeepsData() {
        val name = "migration-test.db"
        context.deleteDatabase(name)
        // Build a v2-shaped database by hand.
        val helper = androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory().create(
            androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context).name(name)
                .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(2) {
                    override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        db.execSQL("CREATE TABLE user_profile (id INTEGER NOT NULL PRIMARY KEY, name TEXT NOT NULL, age INTEGER NOT NULL, heightCm REAL NOT NULL, weightKg REAL NOT NULL, targetWeightKg REAL, units TEXT NOT NULL, experience TEXT NOT NULL, goal TEXT NOT NULL, secondaryGoal TEXT, daysPerWeek INTEGER NOT NULL, sessionMinutes INTEGER NOT NULL, equipment TEXT NOT NULL, limitations TEXT NOT NULL, preferences TEXT NOT NULL, createdAt INTEGER NOT NULL)")
                        db.execSQL("INSERT INTO user_profile VALUES (1,'Sam',28,178.0,80.0,NULL,'METRIC','BEGINNER','FAT_LOSS',NULL,3,30,'BODYWEIGHT','','',0)")
                    }
                    override fun onUpgrade(db: androidx.sqlite.db.SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
                }).build()
        )
        val db2 = helper.writableDatabase
        com.gymtrainer.app.data.AppDatabase.MIGRATION_2_3.migrate(db2)
        db2.query("SELECT name, trainingPlace, focusAreas FROM user_profile WHERE id = 1").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("Sam", c.getString(0))
            assertEquals("BOTH", c.getString(1))
            assertEquals("", c.getString(2))
        }
        helper.close()
        context.deleteDatabase(name)
    }

    @Test
    fun everyRequestedExerciseIsInTheLibrary() = runBlocking {
        val names = repo.exerciseMap().values.associateBy { it.name }
        // A sample across the alphabet from the requested list, new and renamed.
        listOf("Ab Wheel", "Backward Lunge - Smith Machine", "Bicep Curl - Barbell", "Bird Dog", "Chin Up (Assisted)",
            "Curtsey Lunge - Kettlebell", "Fire Hydrant", "Hip Thrust - Cable", "Lat Pulldown (Reverse Grip) - Band",
            "Overhead Press (Wide Grip) - Barbell", "Pendlay Row - Barbell", "Push-Up", "Seated Hip Internal Rotation - Band",
            "Squat Row - Band", "Standing Side Bend - Cable", "Zercher Carry - Barbell", "Burpee", "Jumping Jack")
            .forEach { assertNotNull("missing $it", names[it]) }
        // Renamed entries keep their id (history, routines) and stay findable by the old name.
        val curl = repo.exercise("Barbell_Curl")!!
        assertEquals("Bicep Curl - Barbell", curl.name)
        assertEquals("Barbell Curl", curl.aliases)
        assertEquals("Push-Up", repo.exercise("Pushups")!!.name)
        // Moves without a matching photo use the icon, never someone else's picture.
        assertEquals(0, names.getValue("Bird Dog").imageCount)
    }

    @Test
    fun migration5To6AddsAliases() {
        val name = "mig56.db"
        context.deleteDatabase(name)
        val helper = androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory().create(
            androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context).name(name)
                .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(5) {
                    override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        db.execSQL("CREATE TABLE exercises (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, imageOf TEXT NOT NULL DEFAULT '')")
                        db.execSQL("INSERT INTO exercises (id, name) VALUES ('Pushups','Pushups')")
                    }
                    override fun onUpgrade(db: androidx.sqlite.db.SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
                }).build()
        )
        val db2 = helper.writableDatabase
        com.gymtrainer.app.data.AppDatabase.MIGRATION_5_6.migrate(db2)
        db2.query("SELECT name, aliases FROM exercises").use { c ->
            assertTrue(c.moveToFirst()); assertEquals("Pushups", c.getString(0)); assertEquals("", c.getString(1))
        }
        helper.close()
        context.deleteDatabase(name)
    }

    @Test
    fun migration4To5AddsImageColumnAndKeepsRows() {
        val name = "mig45.db"
        context.deleteDatabase(name)
        val helper = androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory().create(
            androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context).name(name)
                .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(4) {
                    override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        db.execSQL("CREATE TABLE exercises (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL)")
                        db.execSQL("INSERT INTO exercises VALUES ('Barbell_Squat','Barbell Squat')")
                    }
                    override fun onUpgrade(db: androidx.sqlite.db.SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
                }).build()
        )
        val db2 = helper.writableDatabase
        com.gymtrainer.app.data.AppDatabase.MIGRATION_4_5.migrate(db2)
        db2.query("SELECT name, imageOf FROM exercises").use { c ->
            assertTrue(c.moveToFirst()); assertEquals("Barbell Squat", c.getString(0)); assertEquals("", c.getString(1))
        }
        helper.close()
        context.deleteDatabase(name)
    }

    @Test
    fun updateCheckerComparesVersionsAndParsesReleases() {
        val u = com.gymtrainer.app.data.Updates
        assertTrue(u.isNewer("v6.1", "6.0")); assertTrue(u.isNewer("6.0.1", "6.0")); assertTrue(u.isNewer("10.0", "9.9"))
        assertFalse(u.isNewer("v6.0", "6.0")); assertFalse(u.isNewer("5.9", "6.0"))
        val json = """{"tag_name":"v6.1","name":"Gym Trainer 6.1","body":"Fixes","html_url":"https://github.com/x/y/releases/tag/v6.1",
            "assets":[{"name":"notes.txt","browser_download_url":"https://e/x.txt"},{"name":"GymTrainer-v6.1.apk","browser_download_url":"https://e/app.apk"}]}"""
        val r = u.parse(json)!!
        assertEquals("6.1", r.version); assertEquals("https://e/app.apk", r.apkUrl); assertEquals("Fixes", r.notes)
        assertNull(u.parse("""{"tag_name":"v6.1","assets":[]}"""))
    }

    @Test
    fun verifiedVideosPointAtRealExercises() = runBlocking {
        val videos = com.gymtrainer.app.data.VideoRepository(context.assets, db.videoDao())
        val map = repo.exerciseMap()
        assertTrue("verified videos ${videos.verifiedCount()}", videos.verifiedCount() >= 70)
        listOf("Pushups", "Barbell_Squat", "Plank", "Barbell_Deadlift", "x_Burpees").forEach { assertTrue(it, videos.verified(it).isNotEmpty()) }
        val json = org.json.JSONObject(context.assets.open("videos.json").bufferedReader().readText())
        json.keys().forEach { key ->
            assertTrue("video for unknown exercise $key", key in map)
            val arr = json.getJSONArray(key)
            for (i in 0 until arr.length()) assertEquals(11, arr.getJSONObject(i).getString("id").length)
        }
        // Saving a user video.
        videos.save("Pushups", com.gymtrainer.app.data.Tutorial("abcDEF12345", "My video", "Me", com.gymtrainer.app.data.VideoSource.MINE))
        assertEquals(1, videos.observeMine("Pushups").first().size)
    }

    @Test
    fun youtubeLinkParsing() {
        val p = com.gymtrainer.app.data.YouTube::parseId
        assertEquals("abcDEF12345", p("https://www.youtube.com/watch?v=abcDEF12345&t=10s"))
        assertEquals("abcDEF12345", p("https://youtu.be/abcDEF12345"))
        assertEquals("abcDEF12345", p("https://youtube.com/shorts/abcDEF12345?feature=share"))
        assertEquals("abcDEF12345", p("abcDEF12345"))
        assertNull(p("not a link"))
    }

    @Test
    fun v4MigrationSqlMatchesRoomSchema() {
        // Room created these tables from the entities; the migration SQL must produce the same schema.
        val sqlite = db.openHelper.readableDatabase
        val actual = mutableMapOf<String, String>()
        sqlite.query("SELECT name, sql FROM sqlite_master WHERE name IN ('user_videos','food_logs','water_logs','index_user_videos_exerciseId','index_food_logs_day','index_water_logs_day')").use { c ->
            while (c.moveToNext()) actual[c.getString(0)] = c.getString(1)
        }
        fun norm(s: String) = s.replace("IF NOT EXISTS ", "").replace(Regex("\\s+"), " ").trim()
        com.gymtrainer.app.data.AppDatabase.V4_TABLES.forEach { sql ->
            val name = Regex("`([a-z_A-Z]+)`").find(sql.substringAfter(if ("INDEX" in sql) "INDEX IF NOT EXISTS" else "TABLE IF NOT EXISTS"))!!.groupValues[1]
            assertEquals(name, norm(actual[name] ?: "missing $name"), norm(sql))
        }
    }

    @Test
    fun nutritionTargetsAndLogging() = runBlocking {
        val p = profile(4, EquipmentProfile.FULL_GYM, Experience.INTERMEDIATE, Goal.FAT_LOSS)
        val t = com.gymtrainer.app.data.NutritionCalc.targets(p, "MALE")
        assertTrue("kcal ${t.kcal}", t.kcal in 1800..2800)
        assertEquals(160, t.protein) // 2.0 g/kg × 80 kg
        assertTrue(t.waterMl in 2500..3000)
        val gain = com.gymtrainer.app.data.NutritionCalc.targets(p.copy(goal = Goal.MUSCLE_GAIN.name), "MALE")
        assertTrue(gain.kcal > t.kcal)

        val nutrition = com.gymtrainer.app.data.NutritionRepository(db.nutritionDao())
        val day = com.gymtrainer.app.data.NutritionRepository.today()
        val egg = com.gymtrainer.app.data.FoodDb.foods.first { it.name.startsWith("Egg, whole") }
        nutrition.logFood(day, com.gymtrainer.app.data.Meal.BREAKFAST, egg, 100.0, "2 × 1 egg")
        nutrition.addWater(day, 250); nutrition.addWater(day, 250); nutrition.undoWater(day)
        val logged = nutrition.observeFoods(day).first()
        assertEquals(1, logged.size)
        assertEquals(155.0, logged[0].kcal, 0.01)
        assertEquals(250, nutrition.observeWater(day).first())
        assertTrue(com.gymtrainer.app.data.FoodDb.foods.size >= 70)
    }

    @Test
    fun achievementsAndStreaks() {
        val a = com.gymtrainer.app.domain.Achievements
        assertEquals(3, a.longestStreak(listOf(1L, 2L, 3L, 5L, 6L)))
        assertEquals(0, a.longestStreak(emptyList()))
        val list = a.compute(com.gymtrainer.app.domain.AchievementInputs(10, 6, 4, 3, 1200, 300, 1, 7, 2, 0))
        assertTrue(list.first { it.id == "w10" }.unlocked)
        assertTrue(list.first { it.id == "kcal1k" }.unlocked)
        assertFalse(list.first { it.id == "w50" }.unlocked)
    }

    @Test
    fun calculators() {
        assertEquals(100.0, Calc.oneRepMax(100.0, 1), 0.001)
        assertTrue(Calc.oneRepMax(100.0, 5) in 110.0..120.0)
        val (plates, left) = Calc.plates(100.0, 20.0, listOf(25.0, 20.0, 15.0, 10.0, 5.0, 2.5, 1.25))
        assertEquals(listOf(25.0, 15.0), plates)
        assertEquals(0.0, left, 0.001)
        assertEquals(24.7, Math.round(Calc.bmi(80.0, 180.0) * 10) / 10.0, 0.001)
        assertEquals("Healthy weight", Calc.bmiCategory(24.7))
    }
}
