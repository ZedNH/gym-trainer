package com.gymtrainer.app.domain

/** A badge with progress toward unlocking it. */
data class Achievement(val id: String, val title: String, val desc: String, val current: Int, val target: Int) {
    val unlocked get() = current >= target
    val progress get() = (current.toFloat() / target).coerceIn(0f, 1f)
}

data class AchievementInputs(
    val workouts: Int,
    val homeWorkouts: Int,
    val gymWorkouts: Int,
    val longestStreak: Int,
    val totalKcal: Int,
    val totalMinutes: Int,
    val records: Int,
    val programDays: Int,
    val foodDays: Int,
    val measurements: Int
)

object Achievements {
    fun compute(i: AchievementInputs): List<Achievement> = listOf(
        Achievement("first", "First step", "Finish your first workout", i.workouts, 1),
        Achievement("w10", "Getting serious", "Complete 10 workouts", i.workouts, 10),
        Achievement("w50", "Committed", "Complete 50 workouts", i.workouts, 50),
        Achievement("w100", "Centurion", "Complete 100 workouts", i.workouts, 100),
        Achievement("s3", "On a roll", "Train 3 days in a row", i.longestStreak, 3),
        Achievement("s7", "Week warrior", "Train 7 days in a row", i.longestStreak, 7),
        Achievement("home10", "Home hero", "10 home workouts", i.homeWorkouts, 10),
        Achievement("gym10", "Iron regular", "10 gym sessions", i.gymWorkouts, 10),
        Achievement("pr1", "Personal best", "Set your first record", i.records, 1),
        Achievement("pr10", "Record breaker", "Set 10 records", i.records, 10),
        Achievement("kcal1k", "Burner", "Burn 1,000 kcal", i.totalKcal, 1000),
        Achievement("kcal10k", "Furnace", "Burn 10,000 kcal", i.totalKcal, 10000),
        Achievement("hours10", "Time invested", "Train for 10 hours", i.totalMinutes, 600),
        Achievement("prog7", "Plan follower", "Complete 7 plan or challenge days", i.programDays, 7),
        Achievement("prog30", "Finisher", "Complete 30 plan or challenge days", i.programDays, 30),
        Achievement("food7", "Mindful eater", "Log food on 7 days", i.foodDays, 7),
        Achievement("body5", "Tracker", "Log 5 body measurements", i.measurements, 5)
    )

    /** Longest run of consecutive calendar days with a workout. */
    fun longestStreak(epochDays: Collection<Long>): Int {
        val days = epochDays.toSortedSet()
        var best = 0; var run = 0; var prev: Long? = null
        for (d in days) {
            run = if (prev != null && d == prev + 1) run + 1 else 1
            best = maxOf(best, run); prev = d
        }
        return best
    }
}
