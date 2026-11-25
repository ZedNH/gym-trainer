package com.gymtrainer.app.domain

/** A structured multi-week gym program. Adopting one replaces the active gym plan. */
data class GymProgram(
    val id: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val level: Difficulty,
    val weeks: Int,
    val days: List<GymDay>,
    val equipment: String
) {
    val daysPerWeek get() = days.size

    fun toDraft(): PlanGenerator.DraftPlan = PlanGenerator.DraftPlan(
        name = title,
        durationWeeks = weeks,
        notes = description,
        days = days.map { d ->
            PlanGenerator.DraftDay(d.name, d.focus, false, d.items.map {
                PlanGenerator.DraftExercise(it.id, it.sets, it.repMin, it.repMax, it.rest, if (it.timed) "hold" else "2-0-1")
            })
        }
    )
}

data class GymDay(val name: String, val focus: String, val items: List<GymItem>)
data class GymItem(val id: String, val sets: Int, val repMin: Int, val repMax: Int, val rest: Int, val timed: Boolean = false)

object GymPrograms {
    private fun e(id: String, sets: Int, min: Int, max: Int, rest: Int = 90) = GymItem(id, sets, min, max, rest)
    private fun hold(id: String, sets: Int, secs: Int) = GymItem(id, sets, secs, secs, 45, timed = true)
    private fun day(name: String, focus: String, vararg items: GymItem) = GymDay(name, focus, items.toList())

    private val B = Difficulty.BEGINNER
    private val I = Difficulty.INTERMEDIATE
    private val A = Difficulty.ADVANCED

    val all: List<GymProgram> = listOf(
        // ---------------- Beginner ----------------
        GymProgram(
            "gym_beg_full", "Beginner Full Body", "3 days • 8 weeks",
            "Learn the main movement patterns with moderate weights. Add a little weight when every set feels controlled with 2 reps left in the tank.",
            B, 8, listOf(
                day("Full Body A", "Squat • Push • Pull",
                    e("Goblet_Squat", 3, 10, 12), e("Dumbbell_Bench_Press", 3, 10, 12), e("Wide-Grip_Lat_Pulldown", 3, 10, 12),
                    e("Dumbbell_Shoulder_Press", 3, 10, 12), e("Seated_Leg_Curl", 2, 12, 15, 60), hold("Plank", 3, 30)),
                day("Full Body B", "Hinge • Push • Pull",
                    e("Leg_Press", 3, 10, 12), e("Machine_Bench_Press", 3, 10, 12), e("Seated_Cable_Rows", 3, 10, 12),
                    e("Stiff-Legged_Dumbbell_Deadlift", 3, 10, 12), e("Side_Lateral_Raise", 2, 12, 15, 60), e("Crunches", 3, 12, 15, 45)),
                day("Full Body C", "Balanced",
                    e("Goblet_Squat", 3, 10, 12), e("Incline_Dumbbell_Press", 3, 10, 12), e("One-Arm_Dumbbell_Row", 3, 10, 12),
                    e("Leg_Extensions", 2, 12, 15, 60), e("Dumbbell_Bicep_Curl", 2, 12, 15, 60), e("Triceps_Pushdown", 2, 12, 15, 60))
            ), "Gym"
        ),
        GymProgram(
            "gym_beg_machine", "Machine Starter", "3 days • 6 weeks",
            "Guided machines make it easy to train safely while you build confidence.",
            B, 6, listOf(
                day("Machines A", "Full body",
                    e("Leg_Press", 3, 12, 15), e("Machine_Bench_Press", 3, 12, 15), e("Wide-Grip_Lat_Pulldown", 3, 12, 15),
                    e("Machine_Shoulder_Military_Press", 2, 12, 15), e("Seated_Leg_Curl", 2, 12, 15, 60), e("Cable_Crunch", 2, 12, 15, 60)),
                day("Machines B", "Full body",
                    e("Leg_Extensions", 3, 12, 15, 60), e("Butterfly", 3, 12, 15, 60), e("Seated_Cable_Rows", 3, 12, 15),
                    e("Standing_Calf_Raises", 3, 12, 15, 45), e("Triceps_Pushdown", 2, 12, 15, 60), e("Dumbbell_Bicep_Curl", 2, 12, 15, 60))
            ), "Gym"
        ),
        GymProgram(
            "gym_beg_db", "Dumbbell Only", "3 days • 6 weeks",
            "Everything you need with a pair of dumbbells and a bench — great for home gyms.",
            B, 6, listOf(
                day("Dumbbell A", "Lower + push",
                    e("Goblet_Squat", 3, 10, 12), e("Dumbbell_Bench_Press", 3, 10, 12), e("Dumbbell_Lunges", 3, 10, 12),
                    e("Dumbbell_Shoulder_Press", 3, 10, 12), e("Tricep_Dumbbell_Kickback", 2, 12, 15, 60)),
                day("Dumbbell B", "Hinge + pull",
                    e("Stiff-Legged_Dumbbell_Deadlift", 3, 10, 12), e("One-Arm_Dumbbell_Row", 3, 10, 12), e("Dumbbell_Step_Ups", 3, 10, 12),
                    e("Side_Lateral_Raise", 3, 12, 15, 60), e("Hammer_Curls", 2, 12, 15, 60), hold("Plank", 3, 30))
            ), "Dumbbells + bench"
        ),

        GymProgram(
            "gym_beg_lean", "Lean & Toned", "3 days • 6 weeks",
            "Light-to-moderate weights in circuits with short rests — tones muscle and keeps your heart rate up.",
            B, 6, listOf(
                day("Circuit A", "Lower body + core",
                    e("Goblet_Squat", 3, 12, 15, 45), e("Dumbbell_Lunges", 3, 10, 12, 45), e("Seated_Leg_Curl", 3, 12, 15, 45),
                    e("Butt_Lift_Bridge", 3, 15, 20, 45), hold("Plank", 3, 30)),
                day("Circuit B", "Upper body",
                    e("Machine_Bench_Press", 3, 12, 15, 45), e("Wide-Grip_Lat_Pulldown", 3, 12, 15, 45), e("Dumbbell_Shoulder_Press", 3, 12, 15, 45),
                    e("Seated_Cable_Rows", 3, 12, 15, 45), e("Triceps_Pushdown", 2, 15, 15, 45), e("Dumbbell_Bicep_Curl", 2, 15, 15, 45)),
                day("Circuit C", "Full body",
                    e("Leg_Press", 3, 12, 15, 45), e("Dumbbell_Bench_Press", 3, 12, 15, 45), e("One-Arm_Dumbbell_Row", 3, 12, 15, 45),
                    e("Stiff-Legged_Dumbbell_Deadlift", 3, 12, 15, 45), e("Side_Lateral_Raise", 2, 15, 15, 45), e("Crunches", 3, 15, 20, 30))
            ), "Gym"
        ),

        // ---------------- Intermediate ----------------
        GymProgram(
            "gym_int_glute", "Glute Builder", "3 days • 8 weeks",
            "Hip-thrust focused training to build stronger, rounder glutes and hamstrings.",
            I, 8, listOf(
                day("Glutes A", "Thrust focus",
                    e("Barbell_Hip_Thrust", 4, 8, 12, 120), e("Romanian_Deadlift", 3, 8, 10, 120), e("Dumbbell_Rear_Lunge", 3, 10, 12, 90),
                    e("Glute_Kickback", 3, 12, 15, 60), e("Band_Hip_Adductions", 3, 15, 20, 45)),
                day("Glutes B", "Squat focus",
                    e("Goblet_Squat", 4, 10, 12, 90), e("Barbell_Glute_Bridge", 3, 10, 12, 90), e("Split_Squat_with_Dumbbells", 3, 10, 12, 90),
                    e("Lying_Leg_Curls", 3, 10, 12, 75), e("Monster_Walk", 3, 15, 20, 45)),
                day("Upper + Core", "Balance the program",
                    e("Wide-Grip_Lat_Pulldown", 3, 10, 12, 90), e("Dumbbell_Shoulder_Press", 3, 10, 12, 90), e("Seated_Cable_Rows", 3, 10, 12, 75),
                    e("Push-Up_Wide", 3, 10, 15, 60), e("Cable_Crunch", 3, 12, 15, 60))
            ), "Gym"
        ),
        GymProgram(
            "gym_int_dbppl", "Dumbbell Push Pull Legs", "3 days • 8 weeks",
            "A full PPL split using only dumbbells and a bench — perfect for home gyms.",
            I, 8, listOf(
                day("Push", "Chest • Shoulders • Triceps",
                    e("Dumbbell_Bench_Press", 4, 8, 12, 120), e("Incline_Dumbbell_Press", 3, 8, 12, 90), e("Dumbbell_Shoulder_Press", 3, 8, 12, 90),
                    e("Side_Lateral_Raise", 3, 12, 15, 60), e("Dumbbell_Flyes", 3, 10, 12, 60), e("Standing_Dumbbell_Triceps_Extension", 3, 10, 12, 60)),
                day("Pull", "Back • Biceps",
                    e("One-Arm_Dumbbell_Row", 4, 8, 12, 90), e("Bent_Over_Two-Dumbbell_Row_With_Palms_In", 3, 10, 12, 90), e("Reverse_Flyes", 3, 12, 15, 60),
                    e("Dumbbell_Shrug", 3, 12, 15, 60), e("Dumbbell_Bicep_Curl", 3, 10, 12, 60), e("Hammer_Curls", 3, 10, 12, 60)),
                day("Legs", "Quads • Hamstrings • Glutes",
                    e("Goblet_Squat", 4, 8, 12, 120), e("Stiff-Legged_Dumbbell_Deadlift", 3, 8, 12, 120), e("Split_Squat_with_Dumbbells", 3, 10, 12, 90),
                    e("Dumbbell_Step_Ups", 3, 10, 12, 90), e("Standing_Dumbbell_Calf_Raise", 4, 12, 15, 45))
            ), "Dumbbells + bench"
        ),
        GymProgram(
            "gym_int_ul", "Upper / Lower", "4 days • 8 weeks",
            "Each muscle trained twice a week — the sweet spot for building muscle and strength.",
            I, 8, listOf(
                day("Upper A", "Strength focus",
                    e("Barbell_Bench_Press_-_Medium_Grip", 4, 6, 8, 150), e("Bent_Over_Barbell_Row", 4, 6, 8, 150),
                    e("Dumbbell_Shoulder_Press", 3, 8, 10, 120), e("Wide-Grip_Lat_Pulldown", 3, 8, 12), e("Barbell_Curl", 3, 8, 12, 75),
                    e("EZ-Bar_Skullcrusher", 3, 8, 12, 75)),
                day("Lower A", "Squat focus",
                    e("Barbell_Squat", 4, 6, 8, 180), e("Romanian_Deadlift", 3, 8, 10, 120), e("Leg_Press", 3, 10, 12, 120),
                    e("Lying_Leg_Curls", 3, 10, 12, 75), e("Standing_Calf_Raises", 4, 12, 15, 45), e("Cable_Crunch", 3, 12, 15, 60)),
                day("Upper B", "Volume focus",
                    e("Incline_Dumbbell_Press", 4, 8, 12, 120), e("Seated_Cable_Rows", 4, 10, 12), e("Side_Lateral_Raise", 4, 12, 15, 60),
                    e("Cable_Crossover", 3, 12, 15, 60), e("Face_Pull", 3, 12, 15, 60), e("Hammer_Curls", 3, 10, 12, 60),
                    e("Triceps_Pushdown_-_Rope_Attachment", 3, 10, 15, 60)),
                day("Lower B", "Hinge focus",
                    e("Barbell_Deadlift", 3, 4, 6, 180), e("Front_Barbell_Squat", 3, 8, 10, 150), e("Dumbbell_Lunges", 3, 10, 12),
                    e("Leg_Extensions", 3, 12, 15, 60), e("Seated_Calf_Raise", 3, 12, 15, 45), e("Hanging_Leg_Raise", 3, 10, 12, 60))
            ), "Gym"
        ),
        GymProgram(
            "gym_int_ppl", "Push Pull Legs", "3 days • 8 weeks",
            "The classic split: one push day, one pull day and one leg day each week.",
            I, 8, listOf(
                day("Push", "Chest • Shoulders • Triceps",
                    e("Barbell_Bench_Press_-_Medium_Grip", 4, 6, 10, 150), e("Incline_Dumbbell_Press", 3, 8, 12, 120),
                    e("Dumbbell_Shoulder_Press", 3, 8, 12, 120), e("Side_Lateral_Raise", 3, 12, 15, 60),
                    e("Triceps_Pushdown_-_Rope_Attachment", 3, 10, 15, 60), e("Cable_Crossover", 3, 12, 15, 60)),
                day("Pull", "Back • Rear delts • Biceps",
                    e("Barbell_Deadlift", 3, 4, 6, 180), e("Wide-Grip_Lat_Pulldown", 3, 8, 12), e("Seated_Cable_Rows", 3, 10, 12),
                    e("Face_Pull", 3, 12, 15, 60), e("Barbell_Curl", 3, 8, 12, 75), e("Hammer_Curls", 3, 10, 12, 60)),
                day("Legs", "Quads • Hamstrings • Glutes",
                    e("Barbell_Squat", 4, 6, 8, 180), e("Romanian_Deadlift", 3, 8, 10, 120), e("Leg_Press", 3, 10, 12, 120),
                    e("Lying_Leg_Curls", 3, 10, 12, 75), e("Leg_Extensions", 3, 12, 15, 60), e("Standing_Calf_Raises", 4, 12, 15, 45))
            ), "Gym"
        ),
        GymProgram(
            "gym_int_5x5", "Strength 5×5", "3 days • 12 weeks",
            "Five heavy sets of five on the big lifts. Add 2.5 kg each session while every rep stays clean.",
            I, 12, listOf(
                day("Workout A", "Squat • Bench • Row",
                    e("Barbell_Squat", 5, 5, 5, 180), e("Barbell_Bench_Press_-_Medium_Grip", 5, 5, 5, 180), e("Bent_Over_Barbell_Row", 5, 5, 5, 150)),
                day("Workout B", "Squat • Press • Deadlift",
                    e("Barbell_Squat", 5, 5, 5, 180), e("Standing_Military_Press", 5, 5, 5, 180), e("Barbell_Deadlift", 1, 5, 5, 180))
            ), "Barbell + rack"
        ),
        GymProgram(
            "gym_int_split", "Body-Part Split", "5 days • 8 weeks",
            "One muscle group per day with lots of volume — a bodybuilding favourite.",
            I, 8, listOf(
                day("Chest", "Chest & abs",
                    e("Barbell_Bench_Press_-_Medium_Grip", 4, 8, 10, 120), e("Incline_Dumbbell_Press", 3, 8, 12, 120),
                    e("Dumbbell_Flyes", 3, 10, 12, 75), e("Cable_Crossover", 3, 12, 15, 60), e("Cable_Crunch", 3, 12, 15, 60)),
                day("Back", "Lats & mid-back",
                    e("Pullups", 4, 6, 10, 120), e("Bent_Over_Barbell_Row", 4, 8, 10, 120), e("Seated_Cable_Rows", 3, 10, 12),
                    e("One-Arm_Dumbbell_Row", 3, 10, 12), e("Barbell_Shrug", 3, 10, 15, 60)),
                day("Legs", "Full lower body",
                    e("Barbell_Squat", 4, 8, 10, 150), e("Leg_Press", 3, 10, 12, 120), e("Romanian_Deadlift", 3, 8, 10, 120),
                    e("Lying_Leg_Curls", 3, 10, 12, 75), e("Leg_Extensions", 3, 12, 15, 60), e("Standing_Calf_Raises", 4, 12, 15, 45)),
                day("Shoulders", "Delts & traps",
                    e("Standing_Military_Press", 4, 6, 10, 120), e("Arnold_Dumbbell_Press", 3, 8, 12, 90), e("Side_Lateral_Raise", 4, 12, 15, 60),
                    e("Reverse_Flyes", 3, 12, 15, 60), e("Face_Pull", 3, 12, 15, 60)),
                day("Arms", "Biceps & triceps",
                    e("Barbell_Curl", 3, 8, 12, 75), e("Close-Grip_Barbell_Bench_Press", 3, 8, 10, 90), e("Hammer_Curls", 3, 10, 12, 60),
                    e("EZ-Bar_Skullcrusher", 3, 8, 12, 75), e("Concentration_Curls", 3, 10, 15, 60), e("Triceps_Pushdown_-_Rope_Attachment", 3, 12, 15, 60))
            ), "Gym"
        ),

        // ---------------- Advanced ----------------
        GymProgram(
            "gym_adv_ppl6", "PPL × 2 (6-day)", "6 days • 10 weeks",
            "High-frequency Push/Pull/Legs twice a week. Only for well-recovered, experienced lifters.",
            A, 10, listOf(
                day("Push (Heavy)", "Strength",
                    e("Barbell_Bench_Press_-_Medium_Grip", 5, 4, 6, 180), e("Standing_Military_Press", 4, 6, 8, 150),
                    e("Incline_Dumbbell_Press", 3, 8, 10, 120), e("Dips_-_Triceps_Version", 3, 8, 12, 90), e("Side_Lateral_Raise", 4, 12, 15, 60)),
                day("Pull (Heavy)", "Strength",
                    e("Barbell_Deadlift", 4, 3, 5, 180), e("Pullups", 4, 6, 8, 120), e("Bent_Over_Barbell_Row", 4, 6, 8, 120),
                    e("Face_Pull", 3, 12, 15, 60), e("Barbell_Curl", 3, 6, 10, 75)),
                day("Legs (Heavy)", "Strength",
                    e("Barbell_Squat", 5, 4, 6, 180), e("Romanian_Deadlift", 4, 6, 8, 150), e("Leg_Press", 3, 8, 10, 120),
                    e("Lying_Leg_Curls", 3, 8, 12, 75), e("Standing_Calf_Raises", 4, 10, 12, 45)),
                day("Push (Volume)", "Hypertrophy",
                    e("Incline_Dumbbell_Press", 4, 8, 12, 90), e("Dumbbell_Shoulder_Press", 4, 8, 12, 90), e("Cable_Crossover", 4, 12, 15, 60),
                    e("Side_Lateral_Raise", 4, 15, 20, 45), e("Triceps_Pushdown_-_Rope_Attachment", 4, 12, 15, 60)),
                day("Pull (Volume)", "Hypertrophy",
                    e("Wide-Grip_Lat_Pulldown", 4, 10, 12, 75), e("Seated_Cable_Rows", 4, 10, 12, 75), e("One-Arm_Dumbbell_Row", 3, 10, 12),
                    e("Reverse_Flyes", 3, 15, 20, 45), e("Hammer_Curls", 4, 10, 12, 60)),
                day("Legs (Volume)", "Hypertrophy",
                    e("Front_Barbell_Squat", 4, 8, 10, 120), e("Barbell_Hip_Thrust", 4, 10, 12, 90), e("Dumbbell_Lunges", 3, 12, 12, 75),
                    e("Leg_Extensions", 4, 12, 15, 60), e("Seated_Leg_Curl", 4, 12, 15, 60), e("Hanging_Leg_Raise", 3, 12, 15, 60))
            ), "Gym"
        ),
        GymProgram(
            "gym_adv_pb", "Powerbuilding", "4 days • 10 weeks",
            "Heavy main lifts for strength, followed by bodybuilding accessories for size.",
            A, 10, listOf(
                day("Squat Day", "Heavy squat + legs",
                    e("Barbell_Squat", 5, 3, 5, 210), e("Front_Barbell_Squat", 3, 6, 8, 150), e("Lying_Leg_Curls", 3, 10, 12, 75),
                    e("Leg_Extensions", 3, 12, 15, 60), e("Ab_Roller", 3, 8, 12, 60)),
                day("Bench Day", "Heavy bench + chest/arms",
                    e("Barbell_Bench_Press_-_Medium_Grip", 5, 3, 5, 210), e("Close-Grip_Barbell_Bench_Press", 3, 6, 8, 150),
                    e("Incline_Dumbbell_Press", 3, 8, 12, 90), e("EZ-Bar_Skullcrusher", 3, 8, 12, 75)),
                day("Deadlift Day", "Heavy pull + back",
                    e("Barbell_Deadlift", 5, 2, 4, 240), e("Bent_Over_Barbell_Row", 4, 6, 8, 120), e("Pullups", 3, 6, 10, 120),
                    e("Barbell_Shrug", 3, 8, 12, 60), e("Hammer_Curls", 3, 10, 12, 60)),
                day("Press Day", "Overhead + shoulders",
                    e("Standing_Military_Press", 5, 3, 5, 180), e("Arnold_Dumbbell_Press", 3, 8, 10, 90), e("Side_Lateral_Raise", 4, 12, 15, 60),
                    e("Face_Pull", 3, 12, 15, 60), e("Barbell_Curl", 3, 8, 12, 75))
            ), "Barbell + rack"
        )
    )

    fun byId(id: String) = all.firstOrNull { it.id == id }
    fun forLevel(level: Difficulty) = all.filter { it.level == level }
    fun referencedIds(): Set<String> =
        (all.flatMap { p -> p.days.flatMap { d -> d.items } } + GymWorkouts.all.flatMap { it.items }).map { it.id }.toSet()
}

/** Muscle-focus categories for single gym workouts. */
enum class GymCategory(val label: String, val blurb: String) {
    CHEST("Chest", "Presses and flyes"),
    BACK("Back", "Rows, pulldowns, pulls"),
    SHOULDERS("Shoulders", "Presses and raises"),
    ARMS("Arms", "Biceps and triceps"),
    LEGS("Legs", "Quads, hamstrings, calves"),
    GLUTES("Glutes", "Thrusts, hinges, lunges"),
    CORE("Core", "Abs and trunk strength"),
    FULL_BODY("Full Body", "Everything in one session"),
    SPLIT("Push · Pull · Legs", "The classic split days")
}

/** A single gym session (not a multi-week program). Seeded as a runnable template day. */
data class GymWorkout(
    val key: String,
    val title: String,
    val subtitle: String,
    val level: Difficulty,
    val category: GymCategory,
    val items: List<GymItem>
) {
    /** ~40 s per set plus rest. */
    val minutes get() = (items.sumOf { it.sets * (40 + it.rest) } / 60.0).toInt().coerceAtLeast(10)
    val sets get() = items.sumOf { it.sets }
}

object GymWorkouts {
    private fun e(id: String, sets: Int, min: Int, max: Int, rest: Int = 90) = GymItem(id, sets, min, max, rest)
    private fun hold(id: String, sets: Int, secs: Int) = GymItem(id, sets, secs, secs, 45, timed = true)
    private fun w(key: String, title: String, sub: String, level: Difficulty, cat: GymCategory, vararg items: GymItem) =
        GymWorkout(key, title, sub, level, cat, items.toList())

    private val B = Difficulty.BEGINNER
    private val I = Difficulty.INTERMEDIATE
    private val A = Difficulty.ADVANCED

    val all: List<GymWorkout> = listOf(
        // Chest
        w("g_chest_b", "Chest Foundations", "Machines and dumbbells to learn pressing", B, GymCategory.CHEST,
            e("Machine_Bench_Press", 3, 10, 12), e("Dumbbell_Bench_Press", 3, 10, 12), e("Butterfly", 3, 12, 15, 60), e("Pushups", 2, 8, 12, 60)),
        w("g_chest_i", "Chest Builder", "Classic hypertrophy chest day", I, GymCategory.CHEST,
            e("Barbell_Bench_Press_-_Medium_Grip", 4, 6, 10, 150), e("Incline_Dumbbell_Press", 3, 8, 12, 120), e("Dumbbell_Flyes", 3, 10, 12, 75),
            e("Cable_Crossover", 3, 12, 15, 60), e("Dips_-_Triceps_Version", 3, 8, 12, 90)),
        w("g_chest_a", "Chest Power", "Heavy presses plus volume", A, GymCategory.CHEST,
            e("Barbell_Bench_Press_-_Medium_Grip", 5, 4, 6, 180), e("Barbell_Incline_Bench_Press_-_Medium_Grip", 4, 6, 8, 150),
            e("Decline_Dumbbell_Bench_Press", 3, 8, 10, 120), e("Incline_Dumbbell_Flyes", 3, 10, 12, 75), e("Cable_Crossover", 3, 12, 15, 60),
            e("Push-Ups_With_Feet_Elevated", 3, 12, 20, 60)),
        // Back
        w("g_back_b", "Back Basics", "Pull patterns with guided machines", B, GymCategory.BACK,
            e("Wide-Grip_Lat_Pulldown", 3, 10, 12), e("Seated_Cable_Rows", 3, 10, 12), e("One-Arm_Dumbbell_Row", 3, 10, 12, 75),
            e("Hyperextensions_With_No_Hyperextension_Bench", 2, 12, 15, 60)),
        w("g_back_i", "Back Width & Thickness", "Vertical and horizontal pulls", I, GymCategory.BACK,
            e("Pullups", 4, 6, 10, 120), e("Bent_Over_Barbell_Row", 4, 8, 10, 120), e("Wide-Grip_Lat_Pulldown", 3, 10, 12),
            e("Seated_Cable_Rows", 3, 10, 12), e("Face_Pull", 3, 12, 15, 60)),
        w("g_back_a", "Back Strength", "Deadlifts and heavy rows", A, GymCategory.BACK,
            e("Barbell_Deadlift", 5, 3, 5, 210), e("Pullups", 4, 6, 8, 150), e("T-Bar_Row_with_Handle", 4, 8, 10, 120),
            e("One-Arm_Dumbbell_Row", 3, 10, 12), e("Barbell_Shrug", 3, 10, 12, 75)),
        // Shoulders
        w("g_sh_b", "Shoulder Starter", "Healthy, balanced delts", B, GymCategory.SHOULDERS,
            e("Machine_Shoulder_Military_Press", 3, 10, 12), e("Side_Lateral_Raise", 3, 12, 15, 60), e("Reverse_Flyes", 3, 12, 15, 60),
            e("Face_Pull", 2, 12, 15, 60)),
        w("g_sh_i", "Boulder Shoulders", "Press, raise, repeat", I, GymCategory.SHOULDERS,
            e("Dumbbell_Shoulder_Press", 4, 8, 12, 120), e("Arnold_Dumbbell_Press", 3, 10, 12), e("Side_Lateral_Raise", 4, 12, 15, 60),
            e("Cable_Rear_Delt_Fly", 3, 12, 15, 60), e("Dumbbell_Shrug", 3, 12, 15, 60)),
        w("g_sh_a", "Overhead Strength", "Heavy standing press + accessories", A, GymCategory.SHOULDERS,
            e("Standing_Military_Press", 5, 5, 5, 180), e("Seated_Dumbbell_Press", 3, 8, 10, 120), e("Upright_Barbell_Row", 3, 10, 12),
            e("Side_Lateral_Raise", 4, 15, 20, 45), e("Face_Pull", 3, 15, 15, 60)),
        // Arms
        w("g_arm_b", "Arm Basics", "Curls and pushdowns done right", B, GymCategory.ARMS,
            e("Dumbbell_Bicep_Curl", 3, 10, 12, 60), e("Triceps_Pushdown", 3, 10, 12, 60), e("Hammer_Curls", 2, 10, 12, 60),
            e("Bench_Dips", 2, 10, 12, 60)),
        w("g_arm_i", "Arm Blast", "Biceps and triceps supersets", I, GymCategory.ARMS,
            e("Barbell_Curl", 3, 8, 12, 75), e("EZ-Bar_Skullcrusher", 3, 8, 12, 75), e("Hammer_Curls", 3, 10, 12, 60),
            e("Triceps_Pushdown_-_Rope_Attachment", 3, 10, 15, 60), e("Concentration_Curls", 3, 10, 15, 60), e("Tricep_Dumbbell_Kickback", 3, 12, 15, 60)),
        w("g_arm_a", "Arm Overload", "Heavy compounds + isolation", A, GymCategory.ARMS,
            e("Barbell_Curl", 4, 6, 10, 90), e("Close-Grip_Barbell_Bench_Press", 4, 6, 8, 120), e("Preacher_Curl", 3, 8, 12, 75),
            e("EZ-Bar_Skullcrusher", 3, 8, 12, 75), e("Concentration_Curls", 3, 10, 12, 60), e("Triceps_Pushdown_-_Rope_Attachment", 3, 12, 15, 60)),
        // Legs
        w("g_leg_b", "Leg Day Basics", "Machine-led lower body", B, GymCategory.LEGS,
            e("Leg_Press", 3, 10, 12, 120), e("Goblet_Squat", 3, 10, 12), e("Seated_Leg_Curl", 3, 12, 15, 60),
            e("Leg_Extensions", 3, 12, 15, 60), e("Standing_Calf_Raises", 3, 12, 15, 45)),
        w("g_leg_i", "Quad & Hamstring Builder", "Squat, press and curl", I, GymCategory.LEGS,
            e("Barbell_Squat", 4, 8, 8, 150), e("Leg_Press", 3, 10, 12, 120), e("Romanian_Deadlift", 3, 10, 10, 120),
            e("Leg_Extensions", 3, 12, 15, 60), e("Lying_Leg_Curls", 3, 12, 12, 60), e("Standing_Calf_Raises", 4, 15, 15, 45)),
        w("g_leg_a", "Squat Day", "Heavy squats and posterior chain", A, GymCategory.LEGS,
            e("Barbell_Squat", 5, 3, 5, 210), e("Front_Barbell_Squat", 3, 6, 8, 150), e("Romanian_Deadlift", 4, 6, 8, 150),
            e("Hack_Squat", 3, 8, 10, 120), e("Lying_Leg_Curls", 3, 10, 12, 75), e("Seated_Calf_Raise", 4, 10, 15, 45)),
        // Glutes
        w("g_gl_b", "Glute Starter", "Learn to feel your glutes", B, GymCategory.GLUTES,
            e("Butt_Lift_Bridge", 3, 12, 15, 60), e("Goblet_Squat", 3, 12, 12), e("Glute_Kickback", 3, 12, 15, 60),
            e("Dumbbell_Step_Ups", 3, 10, 12, 75)),
        w("g_gl_i", "Glutes & Hamstrings", "Posterior chain focus", I, GymCategory.GLUTES,
            e("Barbell_Hip_Thrust", 4, 8, 12, 120), e("Romanian_Deadlift", 3, 8, 10, 120), e("Dumbbell_Lunges", 3, 10, 12),
            e("Lying_Leg_Curls", 3, 10, 12, 75), e("Glute_Kickback", 3, 12, 15, 60)),
        w("g_gl_a", "Glute Strength", "Heavy thrusts and hinges", A, GymCategory.GLUTES,
            e("Barbell_Hip_Thrust", 5, 6, 8, 150), e("Romanian_Deadlift", 4, 8, 8, 120), e("Dumbbell_Rear_Lunge", 3, 10, 10),
            e("Barbell_Glute_Bridge", 3, 10, 12), e("One-Legged_Cable_Kickback", 3, 12, 15, 60)),
        // Core
        w("g_core_b", "Core Basics", "Brace and control", B, GymCategory.CORE,
            hold("Plank", 3, 30), e("Crunches", 3, 15, 15, 45), e("Dead_Bug", 3, 10, 10, 45), e("Cable_Crunch", 2, 12, 15, 60)),
        w("g_core_i", "Core Crusher", "Abs & trunk stability", I, GymCategory.CORE,
            e("Cable_Crunch", 3, 12, 15, 60), e("Hanging_Leg_Raise", 3, 8, 12, 60), e("Russian_Twist", 3, 16, 20, 45),
            hold("Plank", 3, 45), e("Ab_Roller", 3, 8, 12, 60)),
        w("g_core_a", "Core Strength", "Hard core work for strong lifters", A, GymCategory.CORE,
            e("Hanging_Leg_Raise", 4, 10, 15, 60), e("Ab_Roller", 4, 10, 12, 60), e("Cable_Crunch", 3, 15, 15, 45),
            e("Russian_Twist", 3, 20, 20, 45), hold("Side_Bridge", 3, 40), hold("Plank", 3, 60)),
        // Full body
        w("g_full_b", "Machine Starter Circuit", "Beginner-friendly machines", B, GymCategory.FULL_BODY,
            e("Leg_Press", 3, 10, 15), e("Machine_Bench_Press", 3, 10, 15), e("Wide-Grip_Lat_Pulldown", 3, 10, 15),
            e("Machine_Shoulder_Military_Press", 3, 10, 15), e("Seated_Cable_Rows", 3, 10, 15, 75), e("Leg_Extensions", 3, 12, 15, 60),
            e("Seated_Leg_Curl", 3, 12, 15, 60)),
        w("g_full_i", "Dumbbell Full Body", "Only a pair of dumbbells", I, GymCategory.FULL_BODY,
            e("Goblet_Squat", 3, 10, 12), e("Dumbbell_Bench_Press", 3, 8, 12), e("One-Arm_Dumbbell_Row", 3, 10, 12, 75),
            e("Dumbbell_Shoulder_Press", 3, 8, 12), e("Stiff-Legged_Dumbbell_Deadlift", 3, 10, 12), e("Dumbbell_Bicep_Curl", 3, 10, 15, 60)),
        w("g_full_a", "Strength 5×5", "Classic compound strength", A, GymCategory.FULL_BODY,
            e("Barbell_Squat", 5, 5, 5, 180), e("Barbell_Bench_Press_-_Medium_Grip", 5, 5, 5, 180), e("Bent_Over_Barbell_Row", 5, 5, 5, 150)),
        // Push / Pull / Legs
        w("g_push", "Push Day", "Chest • Shoulders • Triceps", I, GymCategory.SPLIT,
            e("Barbell_Bench_Press_-_Medium_Grip", 4, 6, 10, 150), e("Incline_Dumbbell_Press", 3, 8, 12, 120),
            e("Dumbbell_Shoulder_Press", 3, 8, 12, 120), e("Side_Lateral_Raise", 3, 12, 15, 60),
            e("Triceps_Pushdown_-_Rope_Attachment", 3, 10, 15, 60), e("Cable_Crossover", 3, 12, 15, 60)),
        w("g_pull", "Pull Day", "Back • Rear delts • Biceps", I, GymCategory.SPLIT,
            e("Barbell_Deadlift", 3, 4, 6, 180), e("Wide-Grip_Lat_Pulldown", 3, 8, 12), e("Seated_Cable_Rows", 3, 10, 12),
            e("Face_Pull", 3, 12, 15, 60), e("Barbell_Curl", 3, 8, 12, 75), e("Hammer_Curls", 3, 10, 12, 60)),
        w("g_legs", "Leg Day", "Quads • Hamstrings • Glutes • Calves", I, GymCategory.SPLIT,
            e("Barbell_Squat", 4, 6, 8, 180), e("Romanian_Deadlift", 3, 8, 10, 120), e("Leg_Press", 3, 10, 12, 120),
            e("Lying_Leg_Curls", 3, 10, 12, 75), e("Leg_Extensions", 3, 12, 15, 60), e("Standing_Calf_Raises", 4, 12, 15, 45))
    )

    fun byCategory(c: GymCategory) = all.filter { it.category == c }
    fun byKey(key: String) = all.firstOrNull { it.key == key }
}
