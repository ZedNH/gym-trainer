package com.gymtrainer.app.domain

/** Measurement unit system. */
enum class Units(val label: String) {
    METRIC("Metric (kg, cm)"),
    IMPERIAL("Imperial (lb, ft/in)");

    companion object {
        fun from(name: String?) = entries.firstOrNull { it.name == name } ?: METRIC
    }
}

/** Where the user trains; drives what the Today screen recommends. */
enum class TrainingPlace(val label: String, val desc: String) {
    HOME("At home", "Bodyweight or light equipment"),
    GYM("At the gym", "Machines, barbells and dumbbells"),
    BOTH("Both", "Mix home sessions and gym days");

    companion object {
        fun from(name: String?) = entries.firstOrNull { it.name == name } ?: BOTH
    }
}

/** Common limitation tags; free-text limitations are matched against these keywords. */
enum class Limitation(val label: String, val keyword: String) {
    KNEE("Knees", "knee"), SHOULDER("Shoulders", "shoulder"), BACK("Lower back", "back"), WRIST("Wrists", "wrist");

    companion object {
        fun parse(text: String): Set<Limitation> {
            val l = text.lowercase()
            return entries.filter { it.keyword in l || (it == BACK && ("spine" in l || "disc" in l)) }.toSet()
        }
    }
}

/** Training experience level. */
enum class Experience(val label: String, val desc: String) {
    BEGINNER("Beginner", "New to structured training"),
    INTERMEDIATE("Intermediate", "6+ months consistent training"),
    ADVANCED("Advanced", "Years of structured training"),
    RETURNING("Returning", "Getting back after a long break");

    companion object {
        fun from(name: String?) = entries.firstOrNull { it.name == name } ?: BEGINNER
    }
}

/** Primary training goal. */
enum class Goal(val label: String, val desc: String) {
    MUSCLE_GAIN("Muscle gain", "Build size with moderate reps"),
    LEAN_MUSCLE("Lean muscle", "Gain muscle while staying lean"),
    FAT_LOSS("Fat loss", "Train hard, preserve muscle"),
    RECOMPOSITION("Recomposition", "Build muscle, lose fat"),
    STRENGTH("Strength", "Get strong on key lifts"),
    GENERAL_FITNESS("General fitness", "Stay healthy and active");

    companion object {
        fun from(name: String?) = entries.firstOrNull { it.name == name } ?: GENERAL_FITNESS
    }
}

/** Available equipment profile. */
enum class EquipmentProfile(val label: String, val desc: String, val allowed: Set<String>) {
    FULL_GYM("Full gym", "Barbells, machines, cables, dumbbells",
        setOf("Barbell", "Dumbbell", "Machine", "Cable", "Bodyweight", "Kettlebell")),
    BASIC_GYM("Basic gym", "Barbells, dumbbells, some machines",
        setOf("Barbell", "Dumbbell", "Machine", "Bodyweight")),
    HOME_GYM("Home gym", "Dumbbells, bands, bodyweight",
        setOf("Dumbbell", "Bodyweight", "Kettlebell", "Bands")),
    DUMBBELLS("Dumbbells only", "A pair of adjustable dumbbells",
        setOf("Dumbbell", "Bodyweight")),
    BODYWEIGHT("Bodyweight only", "No equipment needed",
        setOf("Bodyweight"));

    companion object {
        fun from(name: String?) = entries.firstOrNull { it.name == name } ?: FULL_GYM
    }
}

/** Muscle groups used for taxonomy and weekly volume. */
enum class MuscleGroup(val label: String) {
    CHEST("Chest"),
    BACK("Back"),
    SHOULDERS("Shoulders"),
    ARMS("Arms"),
    LEGS("Legs"),
    GLUTES("Glutes"),
    CORE("Core");

    companion object {
        fun from(name: String?) = entries.firstOrNull { it.name == name } ?: CHEST
    }
}

enum class Difficulty(val label: String) {
    BEGINNER("Beginner"),
    INTERMEDIATE("Intermediate"),
    ADVANCED("Advanced");

    companion object {
        fun from(name: String?) = entries.firstOrNull { it.name == name } ?: BEGINNER
    }
}

/** Reps-in-reserve / effort scale used after a set. */
enum class Effort(val label: String, val rir: String) {
    EASY("Too easy", "4+ reps in reserve"),
    GOOD("Appropriate", "1-3 reps in reserve"),
    HARD("Too heavy", "0 reps in reserve / failed"),
    PAIN("Pain / discomfort", "Stop the movement");

    companion object {
        fun from(name: String?) = entries.firstOrNull { it.name == name } ?: GOOD
    }
}
