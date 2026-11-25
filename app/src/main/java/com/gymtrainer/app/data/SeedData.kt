package com.gymtrainer.app.data

import android.content.res.AssetManager
import org.json.JSONArray

/**
 * Exercise library + template routines bundled with the app.
 *
 * The bulk of the library (870+ exercises with start/end photos) comes from
 * free-exercise-db (https://github.com/yuhonas/free-exercise-db), released into the
 * public domain under the Unlicense. It is converted at build time into
 * assets/exercises.json and assets/ex/<id>/<n>.webp. A handful of home-workout
 * staples missing from that set are written here by hand.
 */
object SeedData {

    fun loadExercises(assets: AssetManager): List<ExerciseEntity> {
        val list = ArrayList<ExerciseEntity>(1200)
        for (file in listOf("exercises.json", "exercises_variants.json", "exercises_requested.json")) {
        val text = runCatching { assets.open(file).bufferedReader(Charsets.UTF_8).use { it.readText() } }.getOrNull() ?: continue
        val arr = JSONArray(text)
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val steps = o.getJSONArray("steps")
            val stepList = (0 until steps.length()).map { steps.getString(it) }
            val name = o.getString("name")
            val pattern = o.getString("pattern")
            list += ExerciseEntity(
                id = o.getString("id"),
                name = name,
                muscleGroup = o.getString("group"),
                primaryMuscles = o.getString("primary"),
                secondaryMuscles = o.optString("secondary", ""),
                equipment = o.getString("equipment"),
                difficulty = o.getString("level"),
                pattern = pattern,
                isCompound = o.getBoolean("compound"),
                setup = o.optString("setup", ""),
                execution = stepList.joinToString(" | "),
                breathing = breathingFor(pattern),
                commonMistakes = o.optString("mistakes", ""),
                repMin = o.getInt("repMin"),
                repMax = o.getInt("repMax"),
                restSeconds = o.getInt("rest"),
                youtubeQuery = "$name exercise proper form",
                category = o.getString("category"),
                imageCount = o.getInt("images"),
                timed = o.getBoolean("timed"),
                defaultSeconds = o.getInt("seconds"),
                featured = o.getBoolean("featured"),
                imageOf = o.optString("imageOf", "")
            )
        }
        }
        list += extras
        // Display names from the user's requested list; the previous name stays searchable.
        val renames = runCatching {
            org.json.JSONObject(assets.open("exercise_renames.json").bufferedReader(Charsets.UTF_8).use { it.readText() })
        }.getOrNull()
        if (renames != null) for (i in list.indices) {
            val ex = list[i]
            val newName = renames.optString(ex.id, "")
            if (newName.isNotEmpty() && newName != ex.name) {
                list[i] = ex.copy(name = newName, aliases = ex.name, youtubeQuery = "$newName exercise proper form")
            }
        }
        return list
    }

    private fun breathingFor(pattern: String): String = when (pattern) {
        "Stretch" -> "Breathe slowly and deeply; relax further into the stretch on each exhale."
        "Stability" -> "Breathe steadily throughout — never hold your breath."
        "Cardio", "Plyometric" -> "Keep a steady rhythm; breathe through the nose and mouth."
        else -> "Exhale on the effort, inhale as you return to the start."
    }

    private fun x(
        id: String, name: String, group: String, primary: String, equipment: String = "Bodyweight",
        level: String = "BEGINNER", pattern: String, compound: Boolean, category: String,
        setup: String, steps: List<String>, mistakes: List<String>,
        repMin: Int = 10, repMax: Int = 15, rest: Int = 30,
        timed: Boolean = false, seconds: Int = 0, featured: Boolean = false
    ) = ExerciseEntity(
        id = id, name = name, muscleGroup = group, primaryMuscles = primary, secondaryMuscles = "",
        equipment = equipment, difficulty = level, pattern = pattern, isCompound = compound,
        setup = setup, execution = steps.joinToString(" | "), breathing = breathingFor(pattern),
        commonMistakes = mistakes.joinToString(" | "), repMin = repMin, repMax = repMax,
        restSeconds = rest, youtubeQuery = "$name exercise proper form", category = category,
        imageCount = 0, timed = timed, defaultSeconds = seconds, featured = featured
    )

    /** Home-workout staples not present in the public-domain set (original text). */
    val extras: List<ExerciseEntity> = listOf(
        x("x_Jumping_Jacks", "Jumping Jacks", "LEGS", "Full Body", pattern = "Cardio", compound = true,
            category = "cardio", setup = "Stand tall with feet together and arms by your sides.",
            steps = listOf("Jump your feet out wide while raising your arms overhead",
                "Jump back to feet together with arms down", "Keep a quick, steady rhythm"),
            mistakes = listOf("Landing heavily on the heels", "Bending forward", "Not reaching full arm extension"),
            timed = true, seconds = 30),
        x("x_High_Knees", "High Knees", "LEGS", "Quadriceps, Hip Flexors", pattern = "Cardio", compound = true,
            category = "cardio", setup = "Stand tall, core braced, arms bent at 90°.",
            steps = listOf("Run on the spot driving one knee up to hip height", "Switch legs quickly",
                "Pump your arms and stay on the balls of your feet"),
            mistakes = listOf("Leaning backward", "Knees not reaching hip height", "Flat-footed landing"),
            timed = true, seconds = 30),
        x("x_Burpees", "Burpees", "CHEST", "Full Body", level = "INTERMEDIATE", pattern = "Plyometric", compound = true,
            category = "plyometrics", setup = "Stand with feet shoulder-width apart.",
            steps = listOf("Squat down and place your hands on the floor", "Jump your feet back into a plank",
                "Do one push-up (optional)", "Jump your feet back to your hands", "Explode up into a jump with arms overhead"),
            mistakes = listOf("Sagging hips in the plank", "Landing with stiff knees", "Rushing with poor form"),
            repMin = 8, repMax = 12),
        x("x_Wall_Sit", "Wall Sit", "LEGS", "Quadriceps", pattern = "Stability", compound = false,
            category = "strength", setup = "Stand with your back flat against a wall.",
            steps = listOf("Slide down until your knees are bent at about 90°", "Keep your knees over your ankles",
                "Hold the position with your back against the wall"),
            mistakes = listOf("Knees travelling past the toes", "Hands pushing on the thighs", "Holding your breath"),
            timed = true, seconds = 30),
        x("x_Bodyweight_Lunges", "Bodyweight Lunges", "LEGS", "Quadriceps, Glutes", pattern = "Lunge", compound = true,
            category = "strength", setup = "Stand tall with hands on your hips.",
            steps = listOf("Step forward with one leg", "Lower until both knees are bent about 90°",
                "Push through the front heel to return", "Alternate legs"),
            mistakes = listOf("Front knee caving inward", "Back knee slamming the floor", "Short step"),
            repMin = 10, repMax = 16, featured = true),
        x("x_Knee_Push-Ups", "Knee Push-Ups", "CHEST", "Chest, Triceps", pattern = "Horizontal Push", compound = true,
            category = "strength", setup = "Start in a push-up position with your knees on the floor.",
            steps = listOf("Keep a straight line from knees to head", "Lower your chest toward the floor",
                "Press back up to straight arms"),
            mistakes = listOf("Hips piking up", "Flaring elbows", "Partial range of motion"),
            repMin = 8, repMax = 15, featured = true),
        x("x_Pike_Push-Ups", "Pike Push-Ups", "SHOULDERS", "Shoulders, Triceps", level = "INTERMEDIATE",
            pattern = "Vertical Push", compound = true, category = "strength",
            setup = "Start in a downward-dog position with hips high.",
            steps = listOf("Bend your elbows to lower your head toward the floor", "Keep your hips high",
                "Press back up to straight arms"),
            mistakes = listOf("Dropping the hips", "Elbows flaring wide", "Rushing the descent"),
            repMin = 6, repMax = 12, featured = true),
        x("x_Cobra_Stretch", "Cobra Stretch", "CORE", "Abdominals, Lower Back", pattern = "Stretch", compound = false,
            category = "stretching", setup = "Lie face down with your hands under your shoulders.",
            steps = listOf("Press your chest up while keeping hips on the floor", "Lengthen through the spine",
                "Hold and breathe"),
            mistakes = listOf("Shrugging the shoulders", "Forcing the lower back", "Lifting the hips"),
            timed = true, seconds = 30),
        x("x_Donkey_Kicks", "Donkey Kicks", "GLUTES", "Glutes", pattern = "Hinge", compound = false,
            category = "strength", setup = "Start on all fours, hands under shoulders and knees under hips.",
            steps = listOf("Keeping the knee bent, kick one foot up toward the ceiling", "Squeeze the glute at the top",
                "Lower with control", "Complete reps, then switch sides"),
            mistakes = listOf("Arching the lower back", "Rotating the hips", "Using momentum"),
            repMin = 12, repMax = 20, featured = true),
        x("x_Lying_Leg_Raise", "Lying Leg Raises", "CORE", "Lower Abs", pattern = "Flexion", compound = false,
            category = "strength", setup = "Lie on your back, legs straight, hands under your hips.",
            steps = listOf("Raise your legs until they're vertical", "Lower slowly without touching the floor",
                "Keep your lower back pressed down"),
            mistakes = listOf("Arching the lower back", "Dropping the legs fast", "Bending the knees a lot"),
            repMin = 10, repMax = 15, featured = true),
        x("x_Calf_Raises", "Bodyweight Calf Raises", "LEGS", "Calves", pattern = "Raise", compound = false,
            category = "strength", setup = "Stand tall, feet hip-width, hold a wall for balance.",
            steps = listOf("Rise up onto your toes", "Pause at the top", "Lower slowly to the floor"),
            mistakes = listOf("Bouncing", "Rolling onto the outside of the feet", "Short range"),
            repMin = 15, repMax = 25, featured = true)
    )
}
